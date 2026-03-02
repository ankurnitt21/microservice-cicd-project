package com.example.product_service.config;

import com.example.product_service.interceptor.PerformanceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers Spring MVC interceptors.
 *
 * Why implement WebMvcConfigurer (not extend WebMvcConfigurationSupport)?
 *   WebMvcConfigurationSupport: takes FULL control of MVC config — disables Spring Boot's
 *     auto-configured defaults (Jackson, validators, message converters). Avoid unless needed.
 *   WebMvcConfigurer: ADDS to the existing Spring Boot MVC config without replacing it.
 *     → Always prefer this for customizations.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final PerformanceInterceptor performanceInterceptor;

    public WebMvcConfig(PerformanceInterceptor performanceInterceptor) {
        this.performanceInterceptor = performanceInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(performanceInterceptor)
                // Only intercept API calls — not Swagger UI, actuator, static resources
                .addPathPatterns("/api/**");
    }
}
