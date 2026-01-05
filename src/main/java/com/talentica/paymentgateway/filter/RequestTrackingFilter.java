package com.talentica.paymentgateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to automatically generate and manage request tracking headers.
 * 
 * Features:
 * - Auto-generates X-Correlation-ID if not provided
 * - Auto-generates Idempotency-Key for payment operations if not provided
 * - Sets up MDC for structured logging
 * - Adds headers to response for client tracking
 */
@Slf4j
@Component
@Order(1)
public class RequestTrackingFilter extends OncePerRequestFilter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_IDEMPOTENCY_KEY = "idempotencyKey";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Generate or extract correlation ID
            String correlationId = getOrGenerateCorrelationId(request);
            
            // Generate or extract idempotency key for payment operations
            String idempotencyKey = getOrGenerateIdempotencyKey(request);
            
            // Set up MDC for structured logging
            MDC.put(MDC_CORRELATION_ID, correlationId);
            if (idempotencyKey != null) {
                MDC.put(MDC_IDEMPOTENCY_KEY, idempotencyKey);
            }
            
            // Add headers to response
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            if (idempotencyKey != null) {
                response.setHeader(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
            }
            
            // Create wrapper to make headers available to controllers
            RequestWrapper wrappedRequest = new RequestWrapper(request, correlationId, idempotencyKey);
            
            log.debug("Request tracking setup - URI: {}, Method: {}, CorrelationId: {}, IdempotencyKey: {}", 
                        request.getRequestURI(), request.getMethod(), correlationId, idempotencyKey);
            
            filterChain.doFilter(wrappedRequest, response);
            
        } finally {
            // Clean up MDC
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_IDEMPOTENCY_KEY);
        }
    }

    /**
     * Get existing correlation ID from request or generate a new one
     */
    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = "corr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using provided correlation ID: {}", correlationId);
        }
        
        return correlationId;
    }

    /**
     * Get existing idempotency key from request or generate one for payment operations
     */
    private String getOrGenerateIdempotencyKey(HttpServletRequest request) {
        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        
        // Only generate idempotency key for payment operations
        if (isPaymentOperation(request)) {
            if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
                idempotencyKey = generateIdempotencyKey(request);
                log.debug("Generated new idempotency key: {}", idempotencyKey);
            } else {
                log.debug("Using provided idempotency key: {}", idempotencyKey);
            }
        }
        
        return idempotencyKey;
    }

    /**
     * Check if the request is a payment operation that requires idempotency
     */
    private boolean isPaymentOperation(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        // Payment operations that need idempotency
        return ("POST".equals(method) || "PUT".equals(method)) && (
            uri.contains("/payments/") ||
            uri.contains("/subscriptions") ||
            uri.contains("/arb-subscriptions") ||
            uri.contains("/customers/profiles")
        );
    }

    /**
     * Generate a unique idempotency key based on request characteristics
     */
    private String generateIdempotencyKey(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        
        String operation = extractOperationType(uri);
        return String.format("%s_%s_%s", operation, timestamp, random);
    }

    /**
     * Extract operation type from URI for idempotency key prefix
     */
    private String extractOperationType(String uri) {
        if (uri.contains("/payments/purchase")) return "purchase";
        if (uri.contains("/payments/authorize")) return "authorize";
        if (uri.contains("/payments/capture")) return "capture";
        if (uri.contains("/payments/refund")) return "refund";
        if (uri.contains("/payments/void")) return "void";
        if (uri.contains("/subscriptions")) return "subscription";
        if (uri.contains("/arb-subscriptions")) return "arb_sub";
        if (uri.contains("/customers/profiles")) return "customer";
        return "payment";
    }
}
