package com.talentica.paymentgateway.security;

import com.talentica.paymentgateway.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * API Key Authentication Filter for external service integrations.
 * Handles API key validation for webhook endpoints and external service calls.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_KEY_PARAM = "api_key";
    
    private final ApiKeyService apiKeyService;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // Only process API key authentication for specific endpoints
            if (!requiresApiKeyAuthentication(request)) {
                return;
            }

            // Extract API key from request
            String apiKey = extractApiKey(request);
            
            if (!StringUtils.hasText(apiKey)) {
                log.debug("No API key found for endpoint: {}", request.getRequestURI());
                return;
            }

            try {
                // Validate API key
                if (apiKeyService.isValidApiKey(apiKey)) {
                    // Get API key details
                    String clientId = apiKeyService.getClientId(apiKey);
                    List<String> permissions = apiKeyService.getPermissions(apiKey);
                    
                    // Create authentication token with clientId as principal (allows null/empty)
                    List<GrantedAuthority> authorities = new java.util.ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_API_CLIENT"));
                    
                    if (permissions != null) {
                        permissions.stream()
                                .map(permission -> new SimpleGrantedAuthority("SCOPE_" + permission))
                                .forEach(authorities::add);
                    }
                    
                    // Create authentication token with clientId as principal
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            clientId, // Use clientId directly as principal
                            apiKey,
                            authorities
                    );
                    
                    // Set authentication details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    // Add API key information to MDC for logging
                    MDC.put("clientId", clientId);
                    MDC.put("authType", "API_KEY");
                    
                    log.debug("Successfully authenticated API key for client: {}", clientId);
                } else {
                    log.warn("Invalid API key provided for endpoint: {}", request.getRequestURI());
                }
            } catch (Exception e) {
                log.warn("API key validation failed: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error processing API key authentication filter: {}", e.getMessage(), e);
        } finally {
            try {
                filterChain.doFilter(request, response);
            } finally {
                // Clean up MDC
                MDC.remove("clientId");
                MDC.remove("authType");
            }
        }
    }

    /**
     * Check if the endpoint requires API key authentication.
     * 
     * @param request HTTP request
     * @return true if API key authentication is required
     */
    private boolean requiresApiKeyAuthentication(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        // List of endpoints that require API key authentication
        List<String> apiKeyEndpoints = Arrays.asList(
                "/api/v1/webhook",
                "/api/v1/external",
                "/api/v1/integration"
        );
        
        return apiKeyEndpoints.stream().anyMatch(requestURI::startsWith);
    }

    /**
     * Extract API key from request headers or parameters.
     * 
     * @param request HTTP request
     * @return API key string or null if not found
     */
    private String extractApiKey(HttpServletRequest request) {
        // First, try to get API key from header
        String apiKey = request.getHeader(API_KEY_HEADER);
        
        // If not in header, try query parameter
        if (!StringUtils.hasText(apiKey)) {
            apiKey = request.getParameter(API_KEY_PARAM);
        }
        
        return apiKey;
    }

    /**
     * Create UserDetails for API key authentication.
     * 
     * @param clientId Client identifier
     * @param permissions List of permissions
     * @return UserDetails object
     */
    private UserDetails createApiKeyUserDetails(String clientId, List<String> permissions) {
        // Convert permissions to granted authorities
        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        
        if (permissions != null) {
            permissions.stream()
                    .map(permission -> new SimpleGrantedAuthority("SCOPE_" + permission))
                    .forEach(authorities::add);
        }
        
        // Add default API key role
        authorities.add(new SimpleGrantedAuthority("ROLE_API_CLIENT"));
        
        // Handle null or empty clientId gracefully - use placeholder
        String username = (clientId != null && !clientId.isBlank()) ? clientId : "anonymous";
        
        return User.builder()
                .username(username)
                .password("") // Password not needed for API key authentication
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    /**
     * Check if filter should not be applied to this request.
     * 
     * @param request HTTP request
     * @return true if filter should be skipped
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only apply to specific endpoints that require API key authentication
        return !requiresApiKeyAuthentication(request);
    }
}
