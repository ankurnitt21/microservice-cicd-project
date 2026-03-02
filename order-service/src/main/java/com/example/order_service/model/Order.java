package com.example.order_service.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Order entity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Schema(description = "Unique order ID (auto-generated)", example = "1",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Customer name", example = "Ankur Rana")
    private String customerName;

    @Schema(description = "ID of the product being ordered", example = "1")
    private Long productId;

    @Schema(description = "Name of the product (fetched from product-service)", example = "Laptop")
    private String productName;

    @Schema(description = "Quantity ordered", example = "2")
    private Integer quantity;

    @Schema(description = "Unit price at time of order (fetched from product-service)", example = "75000.0")
    private Double unitPrice;

    @Schema(description = "Total price = unitPrice × quantity", example = "150000.0")
    private Double totalPrice;

    @Schema(description = "Order status", example = "CONFIRMED")
    private OrderStatus status;
}
