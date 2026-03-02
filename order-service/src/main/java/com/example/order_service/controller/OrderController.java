package com.example.order_service.controller;

import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.model.Order;
import com.example.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Orders", description = "Order management — places orders by validating products via product-service")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Get all orders")
    @ApiResponse(responseCode = "200", description = "List of all orders")
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        log.info("Request received: GET /api/orders");
        List<Order> orders = orderService.getAllOrders();
        log.info("Returning {} order(s)", orders.size());
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get order by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found",
            content = @Content(schema = @Schema(implementation = Order.class))),
        @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(
            @Parameter(description = "Order ID", example = "1", required = true)
            @PathVariable Long id) {
        log.info("Request received: GET /api/orders/{}", id);
        Order order = orderService.getOrderById(id);
        log.info("Found order: id={}, status={}", order.getId(), order.getStatus());
        return ResponseEntity.ok(order);
    }

    @Operation(
        summary = "Create a new order",
        description = "Validates the product exists and is available by calling product-service. " +
                      "Returns 404 if product not found, 422 if product is unavailable, 502 if product-service is down."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created successfully",
            content = @Content(schema = @Schema(implementation = Order.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed on request body",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Product not found in product-service",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "422", description = "Product exists but is not available",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "502", description = "product-service is down or returned an error",
            content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        log.info("Request received: POST /api/orders — customer={}, productId={}, qty={}",
                request.getCustomerName(), request.getProductId(), request.getQuantity());
        Order order = orderService.createOrder(request);
        log.info("Order placed: id={}, total={}", order.getId(), order.getTotalPrice());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
