package com.example.order_service.client;

import com.example.order_service.dto.ProductDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate-based alternative to ProductClient (Feign).
 *
 * Demonstrates how to call product-service using @LoadBalanced RestTemplate:
 *   - Uses the logical service name "product-service" (resolved via Eureka)
 *   - Spring Cloud LoadBalancer picks a live instance automatically
 *   - Manual error handling vs Feign's automatic exception translation
 *
 * When to use RestTemplate vs Feign:
 *   RestTemplate → dynamic URLs, streaming responses, fine control over headers/body
 *   Feign        → clean interface, automatic retry/CB integration, less boilerplate
 *
 * In this project OrderService uses Feign (ProductClient).
 * This class is available as an alternative bean — swap in OrderService if needed.
 */
@Component
public class ProductRestTemplateClient {

    private static final Logger log = LoggerFactory.getLogger(ProductRestTemplateClient.class);

    // Uses the @LoadBalanced RestTemplate from RestTemplateConfig
    // "product-service" is the Eureka service name — LoadBalancer resolves it to host:port
    private static final String PRODUCT_SERVICE_URL = "http://product-service";

    private final RestTemplate restTemplate;

    public ProductRestTemplateClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetches a product by ID using RestTemplate.
     *
     * restTemplate.getForObject(url, responseType, uriVars):
     *   - Makes a GET request to the URL
     *   - Deserializes the JSON response body into the specified class
     *   - uriVars replace the {id} placeholder in the URL
     *
     * @throws HttpClientErrorException.NotFound if product doesn't exist (404)
     * @throws org.springframework.web.client.ResourceAccessException on connection failure
     */
    public ProductDto getProductById(Long productId) {
        String url = PRODUCT_SERVICE_URL + "/api/products/{id}";
        log.debug("[RestTemplate] GET {} with id={}", url, productId);

        try {
            ProductDto product = restTemplate.getForObject(url, ProductDto.class, productId);
            log.debug("[RestTemplate] Response: {}", product);
            return product;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("[RestTemplate] Product not found: id={}", productId);
            throw e; // caller handles this
        }
    }
}
