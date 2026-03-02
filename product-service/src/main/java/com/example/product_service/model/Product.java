package com.example.product_service.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// @Schema on the class tells Swagger: "this is a model — show it in the Schemas section"
@Schema(description = "Product entity representing an item in the catalog")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    // READ_ONLY means Swagger will NOT show 'id' in the request body form — client should never send it
    @Schema(description = "Unique identifier (auto-generated)", example = "1",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Product name", example = "MacBook Pro")
    @NotBlank
    private String name;

    @Schema(description = "Product description", example = "Apple M3 chip, 18GB RAM")
    private String description;

    @Schema(description = "Price in INR", example = "150000.0")
    @NotNull
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private Double price;

    @Schema(description = "Product category", example = "electronics")
    private String category;

    @Schema(description = "Available stock quantity", example = "50")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Schema(description = "Whether the product is available for purchase", example = "true")
    private boolean available;
}
