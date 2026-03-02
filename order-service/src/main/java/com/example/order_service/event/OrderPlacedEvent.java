package com.example.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published to Kafka topic "orders.placed" after an order is confirmed.
 *
 * Why publish to Kafka instead of calling payment-service directly (Feign)?
 *
 *   SYNC (Feign):  order-service → payment-service
 *     ✗ order-service waits for payment response → slower API
 *     ✗ if payment-service is down → order creation fails
 *     ✗ tight coupling: both services must be running simultaneously
 *
 *   ASYNC (Kafka): order-service → Kafka topic ← payment-service
 *     ✔ order-service returns immediately after publishing the event
 *     ✔ payment-service can be down; Kafka retains the message until it's back up
 *     ✔ loose coupling: services don't know about each other
 *     ✔ easy to add more consumers (e.g. notification-service, inventory-service)
 *       without changing order-service at all
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {

    /** The confirmed order's ID */
    private Long orderId;

    /** Customer who placed the order */
    private String customerName;

    /** Product details at time of order */
    private Long productId;
    private String productName;

    /** How many units */
    private Integer quantity;

    /** Total charge = unitPrice × quantity */
    private Double totalPrice;
}
