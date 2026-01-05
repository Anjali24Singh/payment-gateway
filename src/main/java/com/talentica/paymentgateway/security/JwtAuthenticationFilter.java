package com.talentica.paymentgateway.security;

import com.talentica.paymentgateway.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter to handle JWT token validation and user authentication.
 * Processes incoming requests and validates JWT tokens from Authorization header.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // Set correlation ID for request tracing
            setCorrelationId(request);
            
            // Skip authentication for public endpoints
            if (isPublicEndpoint(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            final String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            log.debug("Processing request: {} {}, Auth header: {}", 
                        request.getMethod(), request.getRequestURI(), 
                        authHeader != null ? "present" : "missing");
            
            // Check if Authorization header is present and has Bearer token
            if (!StringUtils.hasText(authHeader)) {
                log.debug("No Authorization header found for: {}", request.getRequestURI());
                // proceed without authentication
            } else if (!authHeader.startsWith(BEARER_PREFIX)) {
                log.debug("Authorization header doesn't start with Bearer for: {} - header: {}", 
                           request.getRequestURI(), authHeader);
                // proceed without authentication
            } else {
                final String jwt = authHeader.substring(BEARER_PREFIX.length());
                try {
                    // Extract username from token
                    final String username = jwtService.extractUsername(jwt);
                    
                    // Check if user is not already authenticated
                    if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // Validate if it's an access token
                        if (!jwtService.isAccessToken(jwt)) {
                            log.warn("Invalid token type provided. Expected access token for user: {}", username);
                        } else {
                            // Create user details from token
                            UserDetails userDetails = createUserDetailsFromToken(jwt, username);
                            
                            // Validate token
                            if (jwtService.isTokenValid(jwt, userDetails)) {
                                // Create authentication token
                                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                                
                                // Set authentication details
                                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                
                                // Set authentication in security context
                                SecurityContextHolder.getContext().setAuthentication(authToken);
                                
                                // Add user information to MDC for logging
                                MDC.put("username", username);
                                MDC.put("userId", jwtService.extractUserId(jwt));
                                
                                log.info("Successfully authenticated user: {} for request: {} {}", 
                                          username, request.getMethod(), request.getRequestURI());
                            } else {
                                log.warn("Invalid JWT token for user: {} on request: {} {}", 
                                          username, request.getMethod(), request.getRequestURI());
                            }
                        }
                    } else {
                        log.debug("User already authenticated or invalid username in token");
                    }
                } catch (Exception e) {
                    log.warn("JWT token validation failed: {}", e.getMessage());
                    // Continue without authentication - let security context handle unauthorized access
                }
            }
        } catch (Exception e) {
            log.error("Error processing JWT authentication filter: {}", e.getMessage(), e);
        } finally {
            // Clean up MDC keys set by this filter
            // Note: do not clear correlationId set by upstream CorrelationIdFilter
            MDC.remove("username");
            MDC.remove("userId");
        }
        
        // Continue filter chain exactly once
        filterChain.doFilter(request, response);
    }

    /**
     * Set correlation ID for request tracing.
     * 
     * @param request HTTP request
     */
    private void setCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
    }

    /**
     * Check if the endpoint is public and doesn't require authentication.
     * 
     * @param request HTTP request
     * @return true if public endpoint
     */
    private boolean isPublicEndpoint(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        // List of public endpoints that don't require authentication
        List<String> publicEndpoints = Arrays.asList(
                "/api/v1/auth/login",
                "/api/v1/auth/register",
                "/api/v1/auth/refresh",
                "/api/v1/health",
                "/api/v1/actuator",
                "/api/v1/swagger-ui",
                "/api/v1/api-docs",
                "/api/v1/webhook"  // Webhook endpoints use different authentication
        );
        
        return publicEndpoints.stream().anyMatch(requestURI::startsWith) ||
               requestURI.equals("/") ||
               requestURI.startsWith("/actuator/") ||
               requestURI.startsWith("/swagger-ui/") ||
               requestURI.startsWith("/api-docs/");
    }

    /**
     * Create UserDetails from JWT token.
     * 
     * @param jwt JWT token
     * @param username Username
     * @return UserDetails object
     */
    private UserDetails createUserDetailsFromToken(String jwt, String username) {
        try {
            // Extract authorities from token
            String authoritiesString = jwtService.extractAuthorities(jwt);
            List<SimpleGrantedAuthority> authorities = Collections.emptyList();
            
            if (StringUtils.hasText(authoritiesString)) {
                // Parse authorities string (format: [ROLE_USER, ROLE_ADMIN])
                String cleanAuthorities = authoritiesString.replaceAll("[\\[\\]\\s]", "");
                if (StringUtils.hasText(cleanAuthorities)) {
                    authorities = Arrays.stream(cleanAuthorities.split(","))
                            .filter(StringUtils::hasText)
                            .map(String::trim)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                }
                log.debug("Parsed authorities for user {}: {}", username, authorities);
            }
            
            // Default authority if none found
            if (authorities.isEmpty()) {
                authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            }
            
            return User.builder()
                    .username(username)
                    .password("") // Password not needed for JWT authentication
                    .authorities(authorities)
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(false)
                    .build();
                    
        } catch (Exception e) {
            log.warn("Error creating user details from token: {}", e.getMessage());
            // Return basic user with default role
            return User.builder()
                    .username(username)
                    .password("")
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();
        }
    }
}
