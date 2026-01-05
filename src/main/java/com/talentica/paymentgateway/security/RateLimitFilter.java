package com.talentica.paymentgateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentica.paymentgateway.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rate Limiting Filter to control request rates per client.
 * Applies rate limits based on IP address, user ID, or API key.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    private static final String X_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    private static final String X_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String X_RATE_LIMIT_RESET = "X-RateLimit-Reset";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";
    
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // Skip rate limiting for excluded endpoints
            if (shouldSkipRateLimit(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Determine rate limit identifier
            String identifier = getRateLimitIdentifier(request);
            
            if (!StringUtils.hasText(identifier)) {
                log.debug("No rate limit identifier found, using IP address");
                identifier = getClientIpAddress(request);
            }

            // Check rate limit
            RateLimitService.RateLimitResult result = rateLimitService.isAllowed(identifier);
            
            // Add rate limit headers
            addRateLimitHeaders(response, result);
            
            if (!result.isAllowed()) {
                // Rate limit exceeded
                handleRateLimitExceeded(request, response, identifier);
                return;
            }

            // Request allowed, continue with filter chain
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Error in rate limit filter: {}", e.getMessage(), e);
            // Continue with request if rate limiting fails
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Determine rate limit identifier based on authentication type.
     * 
     * @param request HTTP request
     * @return Rate limit identifier
     */
    private String getRateLimitIdentifier(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Object credentials = authentication.getCredentials();
            
            // If credentials contain API key, use it for rate limiting
            if (credentials instanceof String apiKey && apiKey.startsWith("pgw_")) {
                return "api:" + apiKey;
            }
            
            // Use authenticated username
            if (StringUtils.hasText(username) && !"anonymousUser".equals(username)) {
                return "user:" + username;
            }
        }
        
        // Fall back to IP address
        return "ip:" + getClientIpAddress(request);
    }

    /**
     * Get client IP address from request.
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check X-Forwarded-For header first (common in load balancer setups)
        String xForwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (StringUtils.hasText(xForwardedFor)) {
            // Take the first IP in the chain
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }
        
        // Check X-Real-IP header
        String xRealIp = request.getHeader(X_REAL_IP);
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }
        
        // Fall back to remote address
        return request.getRemoteAddr();
    }

    /**
     * Add rate limit headers to response.
     * 
     * @param response HTTP response
     * @param result Rate limit result
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitService.RateLimitResult result) {
        response.addHeader(X_RATE_LIMIT_LIMIT, String.valueOf(result.getLimit()));
        response.addHeader(X_RATE_LIMIT_REMAINING, String.valueOf(result.getRemaining()));
        response.addHeader(X_RATE_LIMIT_RESET, String.valueOf(result.getResetTimeSeconds()));
    }

    /**
     * Handle rate limit exceeded scenario.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param identifier Rate limit identifier
     * @throws IOException if writing response fails
     */
    private void handleRateLimitExceeded(
            HttpServletRequest request, 
            HttpServletResponse response, 
            String identifier
    ) throws IOException {
        
        log.warn("Rate limit exceeded for identifier: {} on endpoint: {}", 
                   identifier, request.getRequestURI());
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "rate_limit_exceeded");
        errorResponse.put("message", "Rate limit exceeded. Please try again later.");
        errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getRequestURI());
        
        // Add retry after header
        RateLimitService.RateLimitResult status = rateLimitService.isAllowed(identifier);
        long retryAfter = Math.max(1, status.getResetTimeSeconds() - (System.currentTimeMillis() / 1000));
        response.addHeader("Retry-After", String.valueOf(retryAfter));
        
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    /**
     * Check if rate limiting should be skipped for this request.
     * 
     * @param request HTTP request
     * @return true if should skip rate limiting
     */
    private boolean shouldSkipRateLimit(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        // List of endpoints to exclude from rate limiting
        List<String> excludedEndpoints = Arrays.asList(
                "/api/v1/health",
                "/api/v1/actuator/health",
                "/api/v1/actuator/prometheus",
                "/api/v1/swagger-ui",
                "/api/v1/api-docs"
        );
        
        return excludedEndpoints.stream().anyMatch(requestURI::startsWith) ||
               requestURI.startsWith("/actuator/") ||
               requestURI.startsWith("/swagger-ui/") ||
               requestURI.startsWith("/api-docs/");
    }

    /**
     * Check if filter should not be applied to this request.
     * 
     * @param request HTTP request
     * @return true if filter should be skipped
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Apply rate limiting to most endpoints
        return shouldSkipRateLimit(request);
    }
}
