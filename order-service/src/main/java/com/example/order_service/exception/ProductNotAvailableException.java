package com.example.order_service.exception;

public class ProductNotAvailableException extends RuntimeException {
    public ProductNotAvailableException(Long productId) {
        super("Product with id " + productId + " is not available for purchase");
    }
}
