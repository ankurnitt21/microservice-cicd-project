package com.example.product_service.aspect;

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
 * Key AOP concepts:
 *
 *   Aspect       → this class (a module of cross-cutting concern)
 *   Join Point   → a point in execution (method call, exception throw, field access)
 *   Pointcut     → expression that selects which join points to intercept
 *   Advice       → the code that runs at the join point (@Before, @After, @Around)
 *   Weaving      → the process of linking aspects with the target object (Spring does this at runtime via proxy)
 *
 * @Around vs @Before/@After:
 *   @Before → runs before the method, cannot stop it or change return value
 *   @After  → runs after the method (always), cannot change return value
 *   @Around → wraps the method completely — you control when it runs, can change args/return/throw
 *             → use @Around when you need to measure time (need both before and after)
 *
 * Pointcut expression:
 *   execution(* com.example.product_service.service.*.*(..))
 *   └── *            any return type
 *       com.example.product_service.service.*   any class in service package
 *       .*(..)       any method with any arguments
 */
@Aspect
@Component
public class ServiceTimingAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceTimingAspect.class);

    /**
     * Named pointcut — reusable expression that matches all methods in the service package.
     * Naming it lets us reference it in multiple @Around/@Before/@After annotations.
     */
    @Pointcut("execution(* com.example.product_service.service.*.*(..))")
    public void serviceLayer() {}

    /**
     * @Around advice — runs around every method matched by serviceLayer() pointcut.
     *
     * ProceedingJoinPoint gives us:
     *   - pjp.proceed()               → actually call the original method
     *   - pjp.getSignature()          → method signature (class + method name + params)
     *   - pjp.getSignature().getName() → just the method name
     *   - pjp.getArgs()               → the arguments passed to the method
     *
     * IMPORTANT: you MUST call pjp.proceed() or the original method never runs.
     */
    @Around("serviceLayer()")
    public Object measureServiceMethodTime(ProceedingJoinPoint pjp) throws Throwable {
        String className = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();

        long startTime = System.currentTimeMillis();
        try {
            // Proceed with the actual method call
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.debug("[AOP] {}.{}() completed in {}ms", className, methodName, duration);
            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            // Log at WARN level — exceptions are expected business flows (not necessarily errors)
            log.warn("[AOP] {}.{}() threw {} after {}ms",
                    className, methodName, ex.getClass().getSimpleName(), duration);
            throw ex; // re-throw — don't swallow exceptions in an aspect
        }
    }
}
