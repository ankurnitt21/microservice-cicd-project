package com.example.product_service.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * HandlerInterceptor — measures and logs the total HTTP request duration.
 *
 * How it works:
 *   preHandle  → called BEFORE the controller method runs  → record start time in request attribute
 *   postHandle → called AFTER the controller method runs, BEFORE view rendering (not used here)
 *   afterCompletion → called AFTER the full response is committed → log total duration
 *
 * Why HandlerInterceptor vs Filter?
 *   Filter:              operates at Servlet level — runs for ALL requests (incl. static resources, Swagger)
 *   HandlerInterceptor:  operates at Spring MVC level — runs only for requests mapped to a controller
 *                        → knows the handler (controller + method) → better for API timing
 *
 * Why store startTime in request attribute (not ThreadLocal)?
 *   Virtual threads and async processing can migrate between OS threads.
 *   Request attributes are tied to the request object, not the thread — always safe.
 */
@Component
public class PerformanceInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PerformanceInterceptor.class);
    private static final String START_TIME_ATTR = "requestStartTime";

    /**
     * Called before the controller method.
     * Return true to continue processing; false to abort the request.
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // Store the start time in the request so afterCompletion can read it
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true; // continue the request
    }

    /**
     * Called after the controller method returns but before the view is rendered.
     * Not useful here since we use @RestController (no view rendering).
     */
    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        // No-op for REST APIs
    }

    /**
     * Called after the complete request is finished (including error handling).
     * This is where we log the total duration.
     *
     * @param ex Any exception thrown by the handler (null if no exception)
     */
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

        if (ex != null) {
            // Request ended with an unhandled exception
            log.warn("[PERF] {} {} → {} | {}ms | ERROR: {}",
                    method, uri, status, durationMs, ex.getMessage());
        } else if (durationMs > 1000) {
            // Slow request threshold: log at WARN if > 1 second
            log.warn("[PERF] {} {} → {} | {}ms ⚠ SLOW REQUEST", method, uri, status, durationMs);
        } else {
            log.info("[PERF] {} {} → {} | {}ms", method, uri, status, durationMs);
        }
    }
}
