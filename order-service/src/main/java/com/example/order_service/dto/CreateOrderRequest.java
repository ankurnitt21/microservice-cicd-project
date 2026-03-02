package com.example.order_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO (Data Transfer Object) for the POST /api/orders request body.
 *
 * Why a separate DTO instead of using Order directly?
 *   - Order has server-generated fields (id, totalPrice, status, productName)
 *     that the client should NEVER set — they're computed by the server.
 *   - Using Order as @RequestBody would let clients send those fields,
 *     which is a security and correctness issue.
 *   - DTOs separate "what the client sends" from "what the server stores".
 */
@Schema(description = "Request body for creating a new order")
@Data
public class CreateOrderRequest {

    @Schema(description = "Customer name", example = "Ankur Rana", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Customer name is required")
    private String customerName;

    @Schema(description = "ID of the product to order", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Product ID is required")
    private Long productId;

    @Schema(description = "Quantity to order (minimum 1)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
