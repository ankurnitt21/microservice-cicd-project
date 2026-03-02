package com.example.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global Gateway Filter — logs every request and response passing through the gateway.
 *
 * GlobalFilter vs GatewayFilter:
 *   GatewayFilter: applied per-route (configured on individual routes in application.yml)
 *   GlobalFilter:  applied to ALL routes automatically — perfect for cross-cutting concerns
 *                  like logging, auth validation, rate limiting headers, correlation IDs
 *
 * Reactive programming note:
 *   Spring Cloud Gateway uses Project Reactor (WebFlux).
 *   Filter methods return Mono<Void> (not void) — everything is non-blocking.
 *   chain.filter(exchange) is the reactive equivalent of FilterChain.doFilter().
 *   .then(Mono.fromRunnable(...)) = "after the downstream filter chain completes, run this"
 *
 * Ordered interface:
 *   Determines filter execution order. Lower value = higher priority (runs first).
 *   Ordered.HIGHEST_PRECEDENCE = Integer.MIN_VALUE (runs before all other filters)
 *   We use -1 so we run before most filters but after Spring's own built-in ones.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String uri = request.getURI().toString();
        String requestId = request.getId();

        long startTime = System.currentTimeMillis();

        log.info("[GATEWAY] --> {} {} | requestId={}", method, uri, requestId);

        // Proceed with the filter chain, then log the response when complete
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : 0;
            log.info("[GATEWAY] <-- {} {} | status={} | {}ms", method, uri, statusCode, duration);
        }));
    }

    @Override
    public int getOrder() {
        return -1; // Run early, but after Spring's own infrastructure filters
    }
}
