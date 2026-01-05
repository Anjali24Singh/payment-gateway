package com.talentica.paymentgateway.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Request/Response Logging Filter with Security Considerations.
 * 
 * This filter provides comprehensive request and response logging while ensuring
 * sensitive data is properly masked or excluded. It supports structured logging
 * with correlation IDs for distributed tracing.
 * 
 * Features:
 * - Request/response body logging with size limits
 * - Automatic PII and sensitive data masking
 * - Configurable exclusion patterns for endpoints
 * - Performance metrics integration
 * - Security-aware header filtering
 * - JSON request/response formatting
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2) // Execute after correlation ID filter
public class RequestResponseLoggingFilter extends OncePerRequestFilter {
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    private static final Logger paymentLogger = LoggerFactory.getLogger("PAYMENT");
    
    private static final int MAX_PAYLOAD_LENGTH = 10000;
    private static final String MASKED_VALUE = "***MASKED***";
    
    private final ObjectMapper objectMapper;
    
    // Sensitive field patterns for masking
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password", "cardNumber", "cvv", "cvv2", "cvc", "cvc2", "securityCode", 
        "transactionKey", "apiKey", "token", "authToken", "accessToken", 
        "refreshToken", "sessionId", "accountNumber", "bankAccount", "ssn",
        "socialSecurityNumber", "pin", "signature", "authorization"
    );
    
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
        "authorization", "x-api-key", "cookie", "set-cookie", "x-auth-token",
        "x-access-token", "x-refresh-token", "x-session-id"
    );
    
    // Patterns for sensitive data in URLs or text
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    
    public RequestResponseLoggingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Skip logging for excluded endpoints
        if (shouldSkipLogging(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Wrap request and response for content caching
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        Instant startTime = Instant.now();
        String correlationId = MDC.get("correlationId");
        
        try {
            // Log request
            logRequest(requestWrapper, correlationId, startTime);
            
            // Process the request
            filterChain.doFilter(requestWrapper, responseWrapper);
            
            // Log response
            Instant endTime = Instant.now();
            logResponse(requestWrapper, responseWrapper, correlationId, startTime, endTime);
            
        } catch (Exception e) {
            log.error("Error in request/response logging filter [correlationId={}]: {}", 
                        correlationId, e.getMessage(), e);
            throw e;
        } finally {
            // Copy response body to actual response
            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * Log incoming request with security considerations.
     */
    private void logRequest(ContentCachingRequestWrapper request, String correlationId, Instant startTime) {
        try {
            Map<String, Object> requestLog = new HashMap<>();
            requestLog.put("type", "REQUEST");
            requestLog.put("correlationId", correlationId);
            requestLog.put("timestamp", startTime.toString());
            requestLog.put("method", request.getMethod());
            requestLog.put("uri", request.getRequestURI());
            requestLog.put("queryString", maskSensitiveData(request.getQueryString()));
            requestLog.put("remoteAddr", getClientIpAddress(request));
            requestLog.put("userAgent", request.getHeader("User-Agent"));
            requestLog.put("contentType", request.getContentType());
            requestLog.put("contentLength", request.getContentLength());
            
            // Add filtered headers
            requestLog.put("headers", getFilteredHeaders(request));
            
            // Skip request body logging to avoid consuming the stream
            // TODO: Implement proper request body logging that doesn't interfere with controllers
            // if (shouldLogRequestBody(request)) {
            //     String requestBody = getRequestBody(request);
            //     if (StringUtils.hasText(requestBody)) {
            //         requestLog.put("body", maskSensitiveDataInJson(requestBody));
            //     }
            // }
            
            // Choose appropriate logger based on endpoint
            if (isPaymentEndpoint(request)) {
                paymentLogger.info("Payment request: {}", objectMapper.writeValueAsString(requestLog));
            } else if (isAuditableEndpoint(request)) {
                auditLogger.info("Auditable request: {}", objectMapper.writeValueAsString(requestLog));
            } else {
                log.info("Request: {}", objectMapper.writeValueAsString(requestLog));
            }
            
        } catch (Exception e) {
            log.warn("Failed to log request [correlationId={}]: {}", correlationId, e.getMessage());
        }
    }

    /**
     * Log outgoing response with security considerations.
     */
    private void logResponse(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            String correlationId,
            Instant startTime,
            Instant endTime
    ) {
        try {
            long duration = endTime.toEpochMilli() - startTime.toEpochMilli();
            
            Map<String, Object> responseLog = new HashMap<>();
            responseLog.put("type", "RESPONSE");
            responseLog.put("correlationId", correlationId);
            responseLog.put("timestamp", endTime.toString());
            responseLog.put("method", request.getMethod());
            responseLog.put("uri", request.getRequestURI());
            responseLog.put("status", response.getStatus());
            responseLog.put("contentType", response.getContentType());
            responseLog.put("contentLength", response.getContentSize());
            responseLog.put("duration", duration + "ms");
            
            // Add filtered response headers
            responseLog.put("headers", getFilteredResponseHeaders(response));
            
            // Add response body if appropriate
            if (shouldLogResponseBody(request, response)) {
                String responseBody = getResponseBody(response);
                if (StringUtils.hasText(responseBody)) {
                    responseLog.put("body", maskSensitiveDataInJson(responseBody));
                }
            }
            
            // Choose appropriate logger based on endpoint and status
            if (isPaymentEndpoint(request)) {
                paymentLogger.info("Payment response: {}", objectMapper.writeValueAsString(responseLog));
            } else if (isAuditableEndpoint(request) || isErrorStatus(response.getStatus())) {
                auditLogger.info("Auditable response: {}", objectMapper.writeValueAsString(responseLog));
            } else {
                log.info("Response: {}", objectMapper.writeValueAsString(responseLog));
            }
            
        } catch (Exception e) {
            log.warn("Failed to log response [correlationId={}]: {}", correlationId, e.getMessage());
        }
    }

    /**
     * Get filtered request headers excluding sensitive information.
     */
    private Map<String, String> getFilteredHeaders(HttpServletRequest request) {
        Map<String, String> filteredHeaders = new HashMap<>();
        
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            
            if (isSensitiveHeader(headerName)) {
                filteredHeaders.put(headerName, MASKED_VALUE);
            } else {
                filteredHeaders.put(headerName, headerValue);
            }
        }
        
        return filteredHeaders;
    }

    /**
     * Get filtered response headers excluding sensitive information.
     */
    private Map<String, String> getFilteredResponseHeaders(HttpServletResponse response) {
        Map<String, String> filteredHeaders = new HashMap<>();
        
        Collection<String> headerNames = response.getHeaderNames();
        for (String headerName : headerNames) {
            String headerValue = response.getHeader(headerName);
            
            if (isSensitiveHeader(headerName)) {
                filteredHeaders.put(headerName, MASKED_VALUE);
            } else {
                filteredHeaders.put(headerName, headerValue);
            }
        }
        
        return filteredHeaders;
    }

    /**
     * Get request body with size limitations.
     */
    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        
        if (content.length > MAX_PAYLOAD_LENGTH) {
            String truncated = new String(content, 0, MAX_PAYLOAD_LENGTH, StandardCharsets.UTF_8);
            return truncated + "... [TRUNCATED - Original size: " + content.length + " bytes]";
        }
        
        return new String(content, StandardCharsets.UTF_8);
    }

    /**
     * Get response body with size limitations.
     */
    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        
        if (content.length > MAX_PAYLOAD_LENGTH) {
            String truncated = new String(content, 0, MAX_PAYLOAD_LENGTH, StandardCharsets.UTF_8);
            return truncated + "... [TRUNCATED - Original size: " + content.length + " bytes]";
        }
        
        return new String(content, StandardCharsets.UTF_8);
    }

    /**
     * Mask sensitive data in JSON content.
     */
    private String maskSensitiveDataInJson(String jsonContent) {
        if (!StringUtils.hasText(jsonContent)) {
            return jsonContent;
        }
        
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonContent);
            maskSensitiveFieldsInNode(jsonNode);
            String maskedJson = objectMapper.writeValueAsString(jsonNode);
            
            // Additional pattern-based masking for any remaining sensitive data
            return maskSensitiveDataWithPatterns(maskedJson);
            
        } catch (Exception e) {
            // If JSON parsing fails, apply pattern-based masking to the raw string
            return maskSensitiveDataWithPatterns(jsonContent);
        }
    }

    /**
     * Recursively mask sensitive fields in JSON node.
     */
    private void maskSensitiveFieldsInNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fieldNames().forEachRemaining(fieldName -> {
                if (isSensitiveField(fieldName)) {
                    objectNode.put(fieldName, MASKED_VALUE);
                } else {
                    maskSensitiveFieldsInNode(objectNode.get(fieldName));
                }
            });
        } else if (node.isArray()) {
            for (JsonNode arrayItem : node) {
                maskSensitiveFieldsInNode(arrayItem);
            }
        }
    }

    /**
     * Mask sensitive data using regex patterns.
     */
    private String maskSensitiveDataWithPatterns(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        
        String masked = content;
        masked = CREDIT_CARD_PATTERN.matcher(masked).replaceAll("****-****-****-****");
        masked = SSN_PATTERN.matcher(masked).replaceAll("***-**-****");
        
        return masked;
    }

    /**
     * Apply general sensitive data masking to any string.
     */
    private String maskSensitiveData(String data) {
        if (!StringUtils.hasText(data)) {
            return data;
        }
        
        return maskSensitiveDataWithPatterns(data);
    }

    /**
     * Get client IP address considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Check if header is sensitive and should be masked.
     */
    private boolean isSensitiveHeader(String headerName) {
        return SENSITIVE_HEADERS.contains(headerName.toLowerCase());
    }

    /**
     * Check if field is sensitive and should be masked.
     */
    private boolean isSensitiveField(String fieldName) {
        return SENSITIVE_FIELDS.contains(fieldName.toLowerCase());
    }

    /**
     * Check if logging should be skipped for this request.
     */
    private boolean shouldSkipLogging(HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        // Skip health checks and metrics endpoints
        return uri.startsWith("/actuator/") ||
               uri.startsWith("/health/") ||
               uri.startsWith("/metrics/") ||
               uri.contains("/swagger-ui/") ||
               uri.contains("/api-docs/") ||
               uri.endsWith(".css") ||
               uri.endsWith(".js") ||
               uri.endsWith(".png") ||
               uri.endsWith(".ico");
    }

    /**
     * Check if request body should be logged.
     */
    private boolean shouldLogRequestBody(HttpServletRequest request) {
        String contentType = request.getContentType();
        
        return contentType != null && 
               (contentType.contains("application/json") || 
                contentType.contains("application/xml") ||
                contentType.contains("text/")) &&
               request.getContentLength() > 0 &&
               request.getContentLength() <= MAX_PAYLOAD_LENGTH * 2; // Allow larger payloads for truncation
    }

    /**
     * Check if response body should be logged.
     */
    private boolean shouldLogResponseBody(HttpServletRequest request, HttpServletResponse response) {
        String contentType = response.getContentType();
        
        return contentType != null && 
               (contentType.contains("application/json") || 
                contentType.contains("application/xml") ||
                contentType.contains("text/")) &&
               response.getStatus() < 300; // Only log successful responses by default
    }

    /**
     * Check if endpoint is payment-related.
     */
    private boolean isPaymentEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.contains("/payments/") || uri.contains("/transactions/");
    }

    /**
     * Check if endpoint should be audited.
     */
    private boolean isAuditableEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.contains("/auth/") || 
               uri.contains("/admin/") || 
               uri.contains("/management/") ||
               isPaymentEndpoint(request);
    }

    /**
     * Check if status code indicates an error.
     */
    private boolean isErrorStatus(int status) {
        return status >= 400;
    }
}
