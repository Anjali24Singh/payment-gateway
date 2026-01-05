package com.talentica.paymentgateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.*;

/**
 * HTTP request wrapper that adds auto-generated tracking headers.
 * Makes correlation ID and idempotency key available to controllers without manual extraction.
 */
public class RequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders;

    public RequestWrapper(HttpServletRequest request, String correlationId, String idempotencyKey) {
        super(request);
        this.customHeaders = new HashMap<>();
        
        // Add auto-generated headers
        customHeaders.put("X-Correlation-ID", correlationId);
        if (idempotencyKey != null) {
            customHeaders.put("Idempotency-Key", idempotencyKey);
        }
    }

    @Override
    public String getHeader(String name) {
        // Return custom header if exists, otherwise delegate to original request
        String customHeader = customHeaders.get(name);
        if (customHeader != null) {
            return customHeader;
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        // Combine original headers with custom headers
        Set<String> headerNames = new HashSet<>();
        
        // Add original headers
        Enumeration<String> originalHeaders = super.getHeaderNames();
        while (originalHeaders.hasMoreElements()) {
            headerNames.add(originalHeaders.nextElement());
        }
        
        // Add custom headers
        headerNames.addAll(customHeaders.keySet());
        
        return Collections.enumeration(headerNames);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        // Return custom header if exists, otherwise delegate to original request
        String customHeader = customHeaders.get(name);
        if (customHeader != null) {
            return Collections.enumeration(Arrays.asList(customHeader));
        }
        return super.getHeaders(name);
    }

    /**
     * Get the correlation ID for this request
     */
    public String getCorrelationId() {
        return customHeaders.get("X-Correlation-ID");
    }

    /**
     * Get the idempotency key for this request (may be null for non-payment operations)
     */
    public String getIdempotencyKey() {
        return customHeaders.get("Idempotency-Key");
    }
}
