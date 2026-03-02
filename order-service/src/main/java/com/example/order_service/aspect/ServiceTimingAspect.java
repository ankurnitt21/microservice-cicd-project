package com.example.order_service.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect — measures execution time of every method in the service layer.
 *
 * This is especially useful in order-service because createOrder() makes a
 * downstream Feign call to product-service — so we can see exactly how long
 * the service logic (including the remote call) takes vs the full HTTP round-trip
 * (measured by PerformanceInterceptor).
 *
 * Pointcut targets:
 *   com.example.order_service.service.*  → all classes in the service package
 *   *.*(..)                              → all methods with any args
 */
@Aspect
@Component
public class ServiceTimingAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceTimingAspect.class);

    @Pointcut("execution(* com.example.order_service.service.*.*(..))")
    public void serviceLayer() {}

    @Around("serviceLayer()")
    public Object measureServiceMethodTime(ProceedingJoinPoint pjp) throws Throwable {
        String className = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();

        long startTime = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.debug("[AOP] {}.{}() completed in {}ms", className, methodName, duration);
            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("[AOP] {}.{}() threw {} after {}ms",
                    className, methodName, ex.getClass().getSimpleName(), duration);
            throw ex;
        }
    }
}
