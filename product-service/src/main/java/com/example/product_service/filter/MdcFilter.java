package com.example.product_service.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC Filter — runs on EVERY incoming HTTP request (before any controller).
 *
 * What it does:
 *   - Generates (or reads from header) a unique requestId for the request
 *   - Puts requestId, HTTP method, and URI into the MDC (Mapped Diagnostic Context)
 *   - The logback pattern %X{requestId} then prints this value on EVERY log line
 *     for the duration of this request — no need to pass it to every method manually
 *   - Clears MDC after the request finishes (CRITICAL — thread pool reuse)
 *
 * Result in logs:
 *   INFO [abc-1234] [GET] [/api/products/1] ProductController - Request received: GET /api/products/1
 *   DEBUG [abc-1234] [GET] [/api/products/1] ProductService   - Looking up product by id=1
 */
@Component
@Order(1)   // Run first — before any other filter
public class MdcFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Use X-Request-Id from caller if present (for tracing across services)
        // otherwise generate a short random ID for this request
        String requestId = httpRequest.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }

        // Put values into MDC — these are now available to ALL log calls in this thread
        MDC.put("requestId", requestId);
        MDC.put("method", httpRequest.getMethod());
        MDC.put("uri", httpRequest.getRequestURI());

        try {
            chain.doFilter(request, response);  // process the request
        } finally {
            // ALWAYS clear MDC after request — threads are reused from pool,
            // old values would bleed into the next request on the same thread
            MDC.clear();
        }
    }
}
