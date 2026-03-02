package com.example.product_service.health;

import com.example.product_service.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom Health Indicator for the in-memory product store.
 *
 * Spring Boot Actuator automatically discovers any bean that implements
 * HealthIndicator and includes it in GET /actuator/health.
 *
 * The component name in the health response is derived from the class name:
 *   ProductStoreHealthIndicator → "productStore" (strips "HealthIndicator" suffix)
 *
 * Real-world equivalent: you'd use this same pattern to check:
 *   - Database connectivity (can we run SELECT 1?)
 *   - Redis cache (can we PING?)
 *   - External API (can we reach the payment gateway?)
 *   - Disk space (is there enough free space to write files?)
 */
@Component
public class ProductStoreHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(ProductStoreHealthIndicator.class);

    // Maximum products we'll allow in the store before marking as degraded
    private static final int MAX_PRODUCTS = 1000;

    private final ProductService productService;

    public ProductStoreHealthIndicator(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Called by Actuator every time GET /actuator/health is hit.
     *
     * Return Health.up()   → this component is healthy   → contributes to overall UP
     * Return Health.down() → this component has a problem → overall status becomes DOWN
     *
     * .withDetail("key", value) adds extra info shown in the health response JSON.
     */
    @Override
    public Health health() {
        try {
            int productCount = productService.getAllProducts().size();

            log.debug("Health check — product store size: {}", productCount);

            // Degraded state: store is over capacity
            if (productCount >= MAX_PRODUCTS) {
                log.warn("Health check DEGRADED — product store at capacity: {}/{}", productCount, MAX_PRODUCTS);
                return Health.down()
                        .withDetail("status", "Store at capacity")
                        .withDetail("productCount", productCount)
                        .withDetail("maxProducts", MAX_PRODUCTS)
                        .withDetail("action", "Archive or delete old products")
                        .build();
            }

            // Healthy state: store is operational and within limits
            return Health.up()
                    .withDetail("productCount", productCount)
                    .withDetail("maxProducts", MAX_PRODUCTS)
                    .withDetail("storeType", "In-memory HashMap")
                    .withDetail("status", "Operational")
                    .build();

        } catch (Exception e) {
            // If anything goes wrong during the check itself, mark as DOWN
            log.error("Health check failed with exception: {}", e.getMessage(), e);
            return Health.down(e)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
