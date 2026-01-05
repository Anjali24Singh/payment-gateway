package com.talentica.paymentgateway.config;

import com.talentica.paymentgateway.security.ApiKeyAuthenticationFilter;
import com.talentica.paymentgateway.security.CorrelationIdFilter;
import com.talentica.paymentgateway.security.JwtAuthenticationFilter;
import com.talentica.paymentgateway.security.RateLimitFilter;
import com.talentica.paymentgateway.security.RequestResponseLoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.http.HttpStatus;

/**
 * Comprehensive Security Configuration for Payment Gateway Application.
 * Implements JWT authentication, API key validation, rate limiting, and security headers.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CorrelationIdFilter correlationIdFilter;
    private final RequestResponseLoggingFilter requestResponseLoggingFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
            RateLimitFilter rateLimitFilter,
            CorrelationIdFilter correlationIdFilter,
            RequestResponseLoggingFilter requestResponseLoggingFilter,
            CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.correlationIdFilter = correlationIdFilter;
        this.requestResponseLoggingFilter = requestResponseLoggingFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * Main security filter chain configuration.
     * 
     * @param http HttpSecurity configuration
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            // Configure session management (stateless for JWT)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers(
                    "/health/**",
                    "/auth/login",
                    "/auth/register",
                    "/auth/refresh",
                    "/test/**",  // Add test endpoints
                    "/actuator/**",  // All actuator endpoints
                    "/h2-console/**",  // H2 database console
                    "/swagger-ui/**",
                    "/api-docs/**", 
                    "/v3/api-docs/**"
                ).permitAll()
                
                // Webhook endpoints - API key authentication only
                .requestMatchers("/webhooks/**").hasRole("API_CLIENT")
                
                // Admin endpoints - admin role required
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Management endpoints - authenticated users
                .requestMatchers("/management/**").hasAnyRole("ADMIN", "MANAGER")
                
                // Payment endpoints - authenticated users
                .requestMatchers("/payments/**").hasAnyRole("USER", "ADMIN", "MANAGER")
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Configure security headers
            .headers(headers -> headers
                // Frame options - allow frames for H2 console
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
                
                // Content type options
                .contentTypeOptions(contentTypeOptions -> {})
                
                // XSS protection
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
                
                // Referrer policy
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            )
            
            // Additional security headers
            .headers(headers -> headers
                .addHeaderWriter((request, response) -> {
                    response.setHeader("Permissions-Policy", 
                        "geolocation=(), microphone=(), camera=(), payment=()");
                    
                    // More permissive CSP for Swagger UI
                    String path = request.getRequestURI();
                    if (path.contains("/swagger-ui") || path.contains("/api-docs")) {
                        response.setHeader("Content-Security-Policy",
                            "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data: https:; " +
                            "font-src 'self'; " +
                            "connect-src 'self'; " +
                            "frame-ancestors 'none'");
                    } else {
                        response.setHeader("Content-Security-Policy",
                            "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data: https:; " +
                            "font-src 'self'; " +
                            "connect-src 'self'; " +
                            "frame-ancestors 'none'");
                    }
                })
            )
            
            // Configure exception handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.getWriter().write("{\"error\":\"Access Denied\",\"message\":\"" + accessDeniedException.getMessage() + "\"}");
                })
            )
            
            // Add custom filters in correct order
            .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(requestResponseLoggingFilter, CorrelationIdFilter.class)
            .addFilterAfter(rateLimitFilter, RequestResponseLoggingFilter.class)
            .addFilterAfter(jwtAuthenticationFilter, RateLimitFilter.class)
            .addFilterAfter(apiKeyAuthenticationFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Password encoder bean for secure password hashing.
     * 
     * @return BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
