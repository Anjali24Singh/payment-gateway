package com.talentica.paymentgateway.security;

import com.talentica.paymentgateway.config.ApplicationConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation ID Filter for distributed tracing.
 * 
 * This filter ensures every request has a unique correlation ID that can be tracked
 * across the entire request lifecycle and distributed systems. The correlation ID
 * is extracted from request headers or generated if not present, then added to
 * MDC for logging and response headers for tracing.
 * 
 * Features:
 * - Automatic correlation ID generation for new requests
 * - Header extraction for external correlation IDs
 * - MDC integration for structured logging
 * - Response header injection for client tracking
 * - Proper cleanup to prevent memory leaks
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // Execute early in filter chain
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    // Default configuration if properties are not available
    private static final String DEFAULT_HEADER_NAME = "X-Correlation-ID";
    private static final String DEFAULT_MDC_KEY = "correlationId";
    
    private final ApplicationConfig.AppProperties appProperties;
    
    public CorrelationIdFilter(ApplicationConfig.AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        String correlationId = null;
        
        try {
            // Get configuration values
            String headerName = getHeaderName();
            String mdcKey = getMdcKey();
            
            // Extract correlation ID from request header
            correlationId = extractCorrelationId(request, headerName);
            
            // Generate new correlation ID if not present
            if (!StringUtils.hasText(correlationId)) {
                correlationId = generateCorrelationId();
                log.debug("Generated new correlation ID: {}", correlationId);
            } else {
                log.debug("Using existing correlation ID: {}", correlationId);
            }
            
            // Add correlation ID to MDC for logging
            MDC.put(mdcKey, correlationId);
            
            // Add correlation ID to response header for tracing
            response.setHeader(headerName, correlationId);
            
            // Log request start with correlation ID
            log.info("Request started: {} {} [correlationId={}]", 
                       request.getMethod(), request.getRequestURI(), correlationId);
            
            // Continue with filter chain
            filterChain.doFilter(request, response);
            
            // Log request completion
            log.info("Request completed: {} {} [correlationId={}] [status={}]", 
                       request.getMethod(), request.getRequestURI(), 
                       correlationId, response.getStatus());
            
        } catch (Exception e) {
            log.error("Error in correlation ID filter [correlationId={}]: {}", 
                        correlationId, e.getMessage(), e);
            throw e;
        } finally {
            // Always clear MDC to prevent memory leaks
            MDC.clear();
        }
    }

    /**
     * Extract correlation ID from request headers.
     * Supports multiple header formats for interoperability.
     * 
     * @param request HTTP request
     * @param primaryHeaderName Primary header name to check
     * @return Correlation ID if found, null otherwise
     */
    private String extractCorrelationId(HttpServletRequest request, String primaryHeaderName) {
        // Check primary header first
        String correlationId = request.getHeader(primaryHeaderName);
        if (StringUtils.hasText(correlationId)) {
            return sanitizeCorrelationId(correlationId);
        }
        
        // Check alternative header names for interoperability
        String[] alternativeHeaders = {
            "X-Request-ID",
            "X-Trace-ID", 
            "Request-ID",
            "Trace-ID",
            "X-Request-Id",
            "X-Trace-Id"
        };
        
        for (String headerName : alternativeHeaders) {
            correlationId = request.getHeader(headerName);
            if (StringUtils.hasText(correlationId)) {
                log.debug("Found correlation ID in alternative header {}: {}", 
                           headerName, correlationId);
                return sanitizeCorrelationId(correlationId);
            }
        }
        
        return null;
    }

    /**
     * Sanitize correlation ID to ensure it's safe for logging and headers.
     * 
     * @param correlationId Raw correlation ID
     * @return Sanitized correlation ID
     */
    private String sanitizeCorrelationId(String correlationId) {
        if (!StringUtils.hasText(correlationId)) {
            return null;
        }
        
        // Remove any whitespace and limit length
        String sanitized = correlationId.trim();
        
        // Limit length to prevent header size issues
        if (sanitized.length() > 128) {
            sanitized = sanitized.substring(0, 128);
            log.debug("Truncated correlation ID to 128 characters");
        }
        
        // Validate format - should contain only alphanumeric, hyphens, and underscores
        if (!sanitized.matches("^[a-zA-Z0-9\\-_]+$")) {
            log.warn("Invalid correlation ID format, generating new one. Original: {}", correlationId);
            return null;
        }
        
        return sanitized;
    }

    /**
     * Generate a new UUID-based correlation ID.
     * 
     * @return New correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get correlation ID header name from configuration.
     * 
     * @return Header name for correlation ID
     */
    private String getHeaderName() {
        if (appProperties != null && 
            appProperties.getCorrelation() != null && 
            StringUtils.hasText(appProperties.getCorrelation().getHeaderName())) {
            return appProperties.getCorrelation().getHeaderName();
        }
        return DEFAULT_HEADER_NAME;
    }

    /**
     * Get MDC key name from configuration.
     * 
     * @return MDC key for correlation ID
     */
    private String getMdcKey() {
        if (appProperties != null && 
            appProperties.getCorrelation() != null && 
            StringUtils.hasText(appProperties.getCorrelation().getMdcKey())) {
            return appProperties.getCorrelation().getMdcKey();
        }
        return DEFAULT_MDC_KEY;
    }

    /**
     * Get current correlation ID from MDC.
     * This is a utility method that can be used by other components.
     * 
     * @return Current correlation ID or null if not set
     */
    public static String getCurrentCorrelationId() {
        return MDC.get(DEFAULT_MDC_KEY);
    }

    /**
     * Set correlation ID in MDC programmatically.
     * This can be used in async operations or when processing external events.
     * 
     * @param correlationId Correlation ID to set
     */
    public static void setCorrelationId(String correlationId) {
        if (StringUtils.hasText(correlationId)) {
            MDC.put(DEFAULT_MDC_KEY, correlationId);
        }
    }

    /**
     * Clear correlation ID from MDC.
     * Should be called when finishing async operations.
     */
    public static void clearCorrelationId() {
        MDC.remove(DEFAULT_MDC_KEY);
    }
}
