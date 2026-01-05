package com.talentica.paymentgateway.util;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Utility class for managing correlation IDs throughout the application.
 * Provides methods to generate, get, and set correlation IDs for request tracing.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
public class CorrelationIdUtil {
    
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    
    /**
     * Generates a new correlation ID.
     * 
     * @return New correlation ID as a UUID string
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Gets the current correlation ID from MDC, or generates a new one if not present.
     * 
     * @return Current or new correlation ID
     */
    public static String getOrGenerate() {
        String correlationId = get();
        if (!StringUtils.hasText(correlationId)) {
            correlationId = generate();
            set(correlationId);
        }
        return correlationId;
    }
    
    /**
     * Gets the current correlation ID from MDC.
     * 
     * @return Current correlation ID or null if not set
     */
    public static String get() {
        return MDC.get(CORRELATION_ID_KEY);
    }
    
    /**
     * Sets the correlation ID in MDC.
     * 
     * @param correlationId Correlation ID to set
     */
    public static void set(String correlationId) {
        if (StringUtils.hasText(correlationId)) {
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }
    }
    
    /**
     * Clears the correlation ID from MDC.
     */
    public static void clear() {
        MDC.remove(CORRELATION_ID_KEY);
    }
    
    /**
     * Clears all MDC context.
     */
    public static void clearAll() {
        MDC.clear();
    }
}
