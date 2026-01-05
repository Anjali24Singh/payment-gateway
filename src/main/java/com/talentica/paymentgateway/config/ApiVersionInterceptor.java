package com.talentica.paymentgateway.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for handling API versioning logic.
 * Extracts version information from URL path or headers and adds to request attributes.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class ApiVersionInterceptor implements HandlerInterceptor {
    
    public static final String API_VERSION_ATTRIBUTE = "apiVersion";
    public static final String DEFAULT_VERSION = "v1";
    public static final String VERSION_HEADER = "API-Version";

    @Override
    public boolean preHandle(@org.springframework.lang.NonNull HttpServletRequest request, 
                           @org.springframework.lang.NonNull HttpServletResponse response, 
                           @org.springframework.lang.NonNull Object handler) {
        String version = extractApiVersion(request);
        request.setAttribute(API_VERSION_ATTRIBUTE, version);
        
        // Add version to response headers
        response.setHeader(VERSION_HEADER, version);
        
        log.debug("API request for version: {} - {}", version, request.getRequestURI());
        
        return true;
    }

    /**
     * Extract API version from request URL path or headers.
     * 
     * @param request HTTP request
     * @return API version string
     */
    private String extractApiVersion(HttpServletRequest request) {
        // First try to extract from URL path (e.g., /api/v1/payments)
        String path = request.getRequestURI();
        if (path.contains("/api/v")) {
            String[] pathParts = path.split("/");
            for (String part : pathParts) {
                if (part.matches("v\\d+")) {
                    return part;
                }
            }
        }
        
        // Fall back to header-based versioning
        String headerVersion = request.getHeader(VERSION_HEADER);
        if (headerVersion != null && headerVersion.matches("v\\d+")) {
            return headerVersion;
        }
        
        // Default version
        return DEFAULT_VERSION;
    }
}
