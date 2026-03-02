package com.example.order_service.client;

import com.example.order_service.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for product-service — uses Eureka service discovery.
 *
 * BEFORE (hardcoded URL):
 *   @FeignClient(name = "product-service", url = "${product.service.url}")
 *   → Always called http://localhost:8082 — no load balancing, no discovery
 *
 * AFTER (Eureka-based discovery):
 *   @FeignClient(name = "product-service")
 *   → Spring Cloud LoadBalancer looks up "product-service" in Eureka registry
 *   → Gets list of healthy instances (e.g. [{host:localhost, port:8082}])
 *   → Picks one using round-robin strategy
 *   → Routes the Feign call to that instance
 *
 * This means: if you run two instances of product-service (on 8082 and 8092),
 * Feign automatically distributes calls between them — zero code change needed.
 *
 * ERROR BEHAVIOUR (unchanged):
 *   - product-service returns 404 → FeignException.NotFound
 *   - product-service is down     → FeignException / RetryableException
 *   Both caught by GlobalExceptionHandler + Resilience4j circuit breaker.
 */
@FeignClient(name = "product-service")
public interface ProductClient {

    /**
     * Calls: GET http://product-service/api/products/{id}
     * "product-service" is resolved to a real host:port by Spring Cloud LoadBalancer via Eureka.
     */
    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable("id") Long id);
}
