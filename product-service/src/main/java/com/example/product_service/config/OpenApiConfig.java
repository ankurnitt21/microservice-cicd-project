package com.example.product_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

// @OpenAPIDefinition sets the global metadata shown at the TOP of Swagger UI
@OpenAPIDefinition(
    info = @Info(
        title = "Product Service API",
        version = "v1.0",
        description = "REST API for managing products in the e-commerce platform"
    ),
    servers = {
        @Server(url = "http://localhost:8082", description = "Local development")
    }
)
@Configuration
public class OpenApiConfig {
    // No beans needed — the annotation above is enough for global API info
}
