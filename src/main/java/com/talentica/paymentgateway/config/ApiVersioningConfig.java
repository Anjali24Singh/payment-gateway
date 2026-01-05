package com.talentica.paymentgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for API versioning and backward compatibility.
 * Supports URL path versioning and header-based versioning.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    /**
     * API version interceptor for handling version-specific logic.
     */
    @Bean
    public ApiVersionInterceptor apiVersionInterceptor() {
        return new ApiVersionInterceptor();
    }

    @Override
    public void addInterceptors(@org.springframework.lang.NonNull InterceptorRegistry registry) {
        registry.addInterceptor(apiVersionInterceptor())
                .addPathPatterns("/api/**");
    }
}
