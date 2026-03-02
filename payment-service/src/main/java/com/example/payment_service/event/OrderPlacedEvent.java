package com.example.payment_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published by order-service to the "orders.placed" Kafka topic
 * when an order is successfully created and confirmed.
 *
 * This class must match the fields published by order-service's OrderPlacedEvent.
 * JSON deserialization ignores the class name header (configured via
 * spring.kafka.consumer.properties.spring.json.use.type.headers=false)
 * so package differences don't matter — only field names must match.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {

    /** Auto-generated order ID from order-service */
    private Long orderId;

    /** Customer who placed the order */
    private String customerName;

    /** Product ordered */
    private Long productId;
    private String productName;

    /** How many units */
    private Integer quantity;

    /** Total amount to charge = unitPrice × quantity */
    private Double totalPrice;
}
