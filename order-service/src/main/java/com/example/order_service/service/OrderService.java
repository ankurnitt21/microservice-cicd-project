package com.example.order_service.service;

import com.example.order_service.client.ProductClient;
import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.dto.ProductDto;
import com.example.order_service.event.OrderPlacedEvent;
import com.example.order_service.exception.OrderNotFoundException;
import com.example.order_service.exception.ProductNotAvailableException;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderStatus;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    /** Kafka topic name — must match what payment-service listens on */
    private static final String ORDERS_TOPIC = "orders.placed";

    private final ProductClient productClient;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    private final Map<Long, Order> orderStore = new HashMap<>();
    private long idCounter = 1;

    public OrderService(ProductClient productClient,
                        KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate) {
        this.productClient = productClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Creates an order with Resilience4j protection on the product-service call.
     *
     * Annotation execution order (outermost → innermost):
     *   CircuitBreaker → Retry → actual Feign call
     *
     * Flow:
     *   1. CircuitBreaker checks if circuit is OPEN → if yes, run fallback immediately
     *   2. Retry wraps the Feign call → on failure, retries up to 3x (500ms apart)
     *   3. If all retries fail → CircuitBreaker records the failure
     *   4. If failure rate > 50% over last 10 calls → circuit OPENS
     *   5. While circuit is OPEN → fallback runs immediately (no network call made)
     *   6. After 10s → circuit goes HALF_OPEN → a few test calls allowed through
     *   7. If test calls succeed → circuit CLOSES again (healthy)
     *
     * fallbackMethod = "createOrderFallback" — called when:
     *   - Circuit is OPEN
     *   - All retries exhausted
     *   - Any exception that isn't in ignore-exceptions list
     *
     * IMPORTANT: 404 (product not found) and ProductNotAvailableException are in ignore-exceptions
     * for Retry — they go straight to the fallback / exception handler without retrying.
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "createOrderFallback")
    @Retry(name = "productService")
    public Order createOrder(CreateOrderRequest request) {
        log.info("Creating order — customer={}, productId={}, quantity={}",
                request.getCustomerName(), request.getProductId(), request.getQuantity());

        // ── Step 1: Call product-service via Feign ──────────────────────────────
        log.debug("Calling product-service for productId={}", request.getProductId());
        ProductDto product = productClient.getProductById(request.getProductId());
        log.debug("product-service response: name={}, price={}, available={}",
                product.getName(), product.getPrice(), product.isAvailable());

        // ── Step 2: Check product availability ─────────────────────────────────
        if (!product.isAvailable()) {
            log.warn("Product id={} is not available", request.getProductId());
            throw new ProductNotAvailableException(request.getProductId());
        }

        // ── Step 3: Build and save the order ───────────────────────────────────
        double totalPrice = product.getPrice() * request.getQuantity();

        Order order = Order.builder()
                .id(idCounter++)
                .customerName(request.getCustomerName())
                .productId(product.getId())
                .productName(product.getName())
                .quantity(request.getQuantity())
                .unitPrice(product.getPrice())
                .totalPrice(totalPrice)
                .status(OrderStatus.CONFIRMED)
                .build();

        orderStore.put(order.getId(), order);

        log.info("Order created — id={}, product={}, qty={}, total={}, status={}",
                order.getId(), order.getProductName(), order.getQuantity(),
                order.getTotalPrice(), order.getStatus());

        // ── Step 4: Publish async event to Kafka ───────────────────────────────
        // payment-service will pick this up and process the payment independently.
        // We do NOT wait for payment to complete — order-service returns immediately.
        publishOrderPlacedEvent(order);

        return order;
    }

    /**
     * Publishes an OrderPlacedEvent to the "orders.placed" Kafka topic.
     *
     * Key = orderId (String) — ensures all events for the same order go to the
     * same Kafka partition, preserving ordering for that order.
     *
     * We publish only for CONFIRMED orders; REJECTED fallback orders are not
     * published because no product was actually reserved.
     */
    private void publishOrderPlacedEvent(Order order) {
        OrderPlacedEvent event = new OrderPlacedEvent(
                order.getId(),
                order.getCustomerName(),
                order.getProductId(),
                order.getProductName(),
                order.getQuantity(),
                order.getTotalPrice()
        );

        kafkaTemplate.send(ORDERS_TOPIC, String.valueOf(order.getId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[KAFKA] Failed to publish OrderPlacedEvent for orderId={}: {}",
                                order.getId(), ex.getMessage());
                    } else {
                        log.info("[KAFKA] Published OrderPlacedEvent → topic={}, partition={}, offset={}, orderId={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                order.getId());
                    }
                });
    }

    /**
     * Fallback method — called when createOrder() fails after all retries,
     * or when the circuit breaker is OPEN.
     *
     * RULES for fallback methods:
     *   1. Same name as specified in fallbackMethod= parameter
     *   2. Same parameters as the original method PLUS a Throwable as the last param
     *   3. Same return type as the original method
     *
     * What to do in a fallback:
     *   - Return a safe default (REJECTED order here)
     *   - Log the failure with context
     *   - Do NOT call the same failing service again — that defeats the purpose
     *   - Optionally: publish to a retry queue, alert on-call, cache last known result
     */
    public Order createOrderFallback(CreateOrderRequest request, Throwable throwable) {
        log.error("Fallback triggered for createOrder — customer={}, productId={}, reason={}",
                request.getCustomerName(), request.getProductId(), throwable.getMessage());

        // Re-throw business exceptions — these should NOT be swallowed by the fallback:
        // 404 means the product genuinely doesn't exist → the client made a bad request
        // 422 means the product is unavailable → the client should know, not get a silent REJECTED
        if (throwable instanceof FeignException.NotFound) {
            throw (FeignException.NotFound) throwable;
        }
        if (throwable instanceof ProductNotAvailableException) {
            throw (ProductNotAvailableException) throwable;
        }

        // For everything else (service down, timeout, circuit open):
        // Return a REJECTED order so the caller knows the request was received
        // but could not be fulfilled due to a downstream issue
        Order rejectedOrder = Order.builder()
                .id(idCounter++)
                .customerName(request.getCustomerName())
                .productId(request.getProductId())
                .productName("UNKNOWN — product-service unavailable")
                .quantity(request.getQuantity())
                .unitPrice(0.0)
                .totalPrice(0.0)
                .status(OrderStatus.REJECTED)
                .build();

        orderStore.put(rejectedOrder.getId(), rejectedOrder);

        log.warn("Order REJECTED (fallback) — id={}, productId={}",
                rejectedOrder.getId(), rejectedOrder.getProductId());

        return rejectedOrder;
    }

    public List<Order> getAllOrders() {
        log.debug("Fetching all orders — count={}", orderStore.size());
        return new ArrayList<>(orderStore.values());
    }

    public Order getOrderById(Long id) {
        log.debug("Looking up order id={}", id);
        Order order = orderStore.get(id);
        if (order == null) {
            log.warn("Order not found: id={}", id);
            throw new OrderNotFoundException(id);
        }
        return order;
    }
}
