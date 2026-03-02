package com.example.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Payment Service — Assignment 7
 *
 * Async payment processing via Kafka:
 *   1. order-service publishes an OrderPlacedEvent to the "orders.placed" Kafka topic
 *      when a new order is confirmed.
 *   2. This service listens to "orders.placed" and simulates payment processing.
 *
 * Why Kafka (async) instead of Feign (sync)?
 *   - order-service does NOT need to wait for payment to complete before responding.
 *   - payment-service can be down without breaking order creation.
 *   - Kafka persists events, so payment-service processes them when it comes back up.
 *   - Naturally decouples the two services — they evolve independently.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
