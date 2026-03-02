package com.example.order_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Provides a load-balanced RestTemplate bean.
 *
 * What @LoadBalanced does:
 *   Without it: RestTemplate("http://product-service/api/products/1") → DNS lookup → fails
 *               because "product-service" is not a real DNS hostname.
 *
 *   With it:    RestTemplate("http://product-service/api/products/1")
 *                 → Spring Cloud LoadBalancer intercepts the request
 *                 → looks up "product-service" in Eureka registry
 *                 → gets a list of actual instances (e.g. localhost:8082)
 *                 → picks one (round-robin by default)
 *                 → replaces the service name with the real host:port
 *                 → makes the actual HTTP call to localhost:8082/api/products/1
 *
 * Why both RestTemplate AND Feign?
 *   Feign:         declarative, annotation-based, cleaner for microservice-to-microservice calls
 *   RestTemplate:  more explicit, good for dynamic URLs, fine-grained control over request/response
 *   Both support @LoadBalanced service discovery — they're complementary tools.
 *
 * In this project:
 *   OrderService uses Feign (ProductClient) as the primary client.
 *   ProductRestTemplateClient demonstrates the RestTemplate alternative.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced // Enables Eureka-based service discovery for this RestTemplate
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
