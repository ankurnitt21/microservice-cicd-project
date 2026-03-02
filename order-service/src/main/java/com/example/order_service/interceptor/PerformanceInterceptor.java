package com.example.order_service.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * HandlerInterceptor — measures and logs total HTTP request duration.
 *
 * Lifecycle:
 *   preHandle       → before controller   → store start time
 *   postHandle      → after controller    → (not used for REST)
 *   afterCompletion → after full response → calculate and log duration
 */
@Component
public class PerformanceInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PerformanceInterceptor.class);
    private static final String START_TIME_ATTR = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        // No-op for REST APIs
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        if (startTime == null) return;

        long durationMs = System.currentTimeMillis() - startTime;
        int status = response.getStatus();
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // For order-service, a "slow" threshold of 2s is reasonable
        // (it makes a downstream Feign call to product-service)
        if (ex != null) {
            log.warn("[PERF] {} {} → {} | {}ms | ERROR: {}",
                    method, uri, status, durationMs, ex.getMessage());
        } else if (durationMs > 2000) {
            log.warn("[PERF] {} {} → {} | {}ms ⚠ SLOW REQUEST", method, uri, status, durationMs);
        } else {
            log.info("[PERF] {} {} → {} | {}ms", method, uri, status, durationMs);
        }
    }
}
