package com.example.payment_service.service;

import com.example.payment_service.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer that processes payment for every confirmed order.
 *
 * Flow:
 *   order-service → Kafka topic "orders.placed" → this listener → payment processed
 *
 * groupId = "payment-service-group"
 *   - All instances of payment-service share this group
 *   - Kafka guarantees each message is processed by exactly ONE instance
 *   - This enables horizontal scaling: add more instances, Kafka distributes load
 *
 * Kafka guarantees:
 *   - At-least-once delivery by default (auto-commit disabled → manual ack is safer for prod)
 *   - Messages are replayed if payment-service was down (offset not committed)
 *   - Partition ordering: all events for the same orderId go to the same partition
 *     (because we use orderId as the Kafka message key in order-service)
 */
@Service
public class PaymentConsumerService {

    private static final Logger log = LoggerFactory.getLogger(PaymentConsumerService.class);

    /**
     * Listens to the "orders.placed" topic.
     *
     * Spring Kafka auto-deserializes the JSON payload into an OrderPlacedEvent
     * using the JsonDeserializer configured in application.properties.
     *
     * @param event the order event published by order-service
     */
    @KafkaListener(topics = "orders.placed", groupId = "payment-service-group")
    public void processPayment(OrderPlacedEvent event) {
        log.info("══════════════════════════════════════════════════════════");
        log.info("[PAYMENT] Received OrderPlacedEvent");
        log.info("[PAYMENT]   orderId      : {}", event.getOrderId());
        log.info("[PAYMENT]   customer     : {}", event.getCustomerName());
        log.info("[PAYMENT]   product      : {} (id={})", event.getProductName(), event.getProductId());
        log.info("[PAYMENT]   quantity     : {}", event.getQuantity());
        log.info("[PAYMENT]   totalPrice   : ₹{}", event.getTotalPrice());
        log.info("══════════════════════════════════════════════════════════");

        // Simulate payment processing (in a real system: call payment gateway, update DB, etc.)
        log.info("[PAYMENT] Processing payment of ₹{} for orderId={} ...", event.getTotalPrice(), event.getOrderId());

        try {
            // Simulate a small delay for payment gateway call
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate approval (in real life: APPROVED / DECLINED based on card details)
        log.info("[PAYMENT] Payment APPROVED ✔  orderId={}, amount=₹{}, customer={}",
                event.getOrderId(), event.getTotalPrice(), event.getCustomerName());
    }
}
