package com.example.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway — the single entry point for all client requests.
 *
 * What it does:
 *   Clients (browser, mobile app, frontend) call ONE URL: http://localhost:8080
 *   The gateway routes each request to the correct microservice:
 *     /api/products/**  →  product-service (port 8082)
 *     /api/orders/**    →  order-service   (port 8083)
 *
 * Benefits:
 *   - Clients don't need to know individual service ports
 *   - Cross-cutting concerns (auth, rate limiting, logging) applied in ONE place
 *   - Can add/remove/scale services without changing client URLs
 *   - Load balancing: if multiple product-service instances run, gateway distributes traffic
 *
 * Spring Cloud Gateway vs Zuul:
 *   Zuul (Netflix, 1st gen): blocking, based on Servlet API
 *   Spring Cloud Gateway: non-blocking, based on Project Reactor + WebFlux
 *     → better performance, supports long-lived connections (SSE, WebSocket)
 *
 * Routes are configured in application.yml (see src/main/resources/application.yml).
 * No @EnableDiscoveryClient needed — spring-cloud-starter-netflix-eureka-client
 * auto-configures it when it's on the classpath.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
