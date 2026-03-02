package com.example.order_service.exception;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Thrown by Resilience4j when the circuit is OPEN and no fallback is defined.
     * In our case the fallback handles this, but this handler is a safety net
     * for any future @CircuitBreaker without a fallback.
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitOpen(CallNotPermittedException ex) {
        log.error("Circuit breaker is OPEN — call not permitted: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "message", "Service temporarily unavailable — circuit breaker is open",
                        "circuitBreaker", ex.getCausingCircuitBreakerName()
                ));
    }

    // Order not found in our own store
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", 404, "message", ex.getMessage()));
    }

    // Product is not available for purchase
    @ExceptionHandler(ProductNotAvailableException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotAvailable(ProductNotAvailableException ex) {
        log.warn("Product not available: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("status", 422, "message", ex.getMessage()));
    }

    // Feign got a 404 from product-service — product doesn't exist
    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFound(FeignException.NotFound ex) {
        log.warn("Product not found in product-service: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", 404,
                        "message", "Product not found in product-service",
                        "detail", "The product ID you specified does not exist"
                ));
    }

    // Feign got any other error from product-service (500, 503, connection refused, etc.)
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeignError(FeignException ex) {
        log.error("product-service call failed — status={}, message={}", ex.status(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "status", 502,
                        "message", "product-service is unavailable or returned an error",
                        "upstreamStatus", ex.status()
                ));
    }

    // @Valid failed on request body
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", 400, "errors", fieldErrors));
    }
}
