package com.example.order_service.dto;

import lombok.Data;

/**
 * DTO representing the product data returned by product-service.
 *
 * Why NOT import Product.java from product-service?
 *   - Microservices must be independently deployable.
 *     If order-service imports a class from product-service, they become
 *     tightly coupled — you can't change product-service without breaking order-service.
 *   - Each service owns its data model. order-service defines what fields
 *     IT cares about from the product response (doesn't need ALL fields).
 *   - This is called "contract-based integration" — each service defines
 *     its own view of the other service's data.
 *
 * If product-service adds new fields, order-service ignores them (safe).
 * If product-service removes a field order-service needs, it breaks (contract violation).
 */
@Data
public class ProductDto {
    private Long id;
    private String name;
    private Double price;
    private String category;
    private boolean available;
    private Integer stockQuantity;
}
