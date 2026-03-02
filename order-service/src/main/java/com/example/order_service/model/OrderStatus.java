package com.example.order_service.model;

/**
 * Lifecycle of an order:
 *
 *  PENDING   → order received, not yet validated
 *  CONFIRMED → product exists, is available, and order was saved successfully
 *  REJECTED  → product not found, not available, or insufficient stock
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    REJECTED
}
