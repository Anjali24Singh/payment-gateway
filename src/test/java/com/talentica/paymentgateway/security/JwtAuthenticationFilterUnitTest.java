package com.talentica.paymentgateway.security;

import com.talentica.paymentgateway.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterUnitTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void constructor_WithJwtService_ShouldCreateFilter() {
        // When & Then
        assertNotNull(jwtAuthenticationFilter);
    }

    @Test
    void doFilterInternal_WithPublicEndpoint_ShouldSkipAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithHealthEndpoint_ShouldSkipAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/health");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_WithSwaggerEndpoint_ShouldSkipAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_WithNoAuthHeader_ShouldContinueWithoutAuth() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithInvalidAuthHeader_ShouldContinueWithoutAuth() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Basic dGVzdA==");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithValidJwtToken_ShouldAuthenticateUser() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String username = "testuser";
        String userId = "123";
        
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-correlation-id");
        
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(jwtService.isAccessToken(token)).thenReturn(true);
        when(jwtService.extractAuthorities(token)).thenReturn("ROLE_USER");
        when(jwtService.isTokenValid(eq(token), any(UserDetails.class))).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtService).extractUsername(token);
        verify(jwtService).isAccessToken(token);
        verify(jwtService).isTokenValid(eq(token), any(UserDetails.class));
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(username, authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void doFilterInternal_WithRefreshToken_ShouldNotAuthenticate() throws ServletException, IOException {
        // Given
        String token = "refresh.jwt.token";
        String username = "testuser";
        
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.isAccessToken(token)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtService).extractUsername(token);
        verify(jwtService).isAccessToken(token);
        verify(jwtService, never()).isTokenValid(any(), any());
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithInvalidToken_ShouldNotAuthenticate() throws ServletException, IOException {
        // Given
        String token = "invalid.jwt.token";
        String username = "testuser";
        
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.isAccessToken(token)).thenReturn(true);
        when(jwtService.extractAuthorities(token)).thenReturn("ROLE_USER");
        when(jwtService.isTokenValid(eq(token), any(UserDetails.class))).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtService).extractUsername(token);
        verify(jwtService).isAccessToken(token);
        verify(jwtService).extractAuthorities(token);
        verify(jwtService).isTokenValid(eq(token), any(UserDetails.class));
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithJwtServiceException_ShouldContinueWithoutAuth() throws ServletException, IOException {
        // Given
        String token = "malformed.token";
        
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        
        when(jwtService.extractUsername(token)).thenThrow(new RuntimeException("Invalid token"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithExistingAuthentication_ShouldNotOverride() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String username = "testuser";
        
        // Set existing authentication
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);
        
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        
        when(jwtService.extractUsername(token)).thenReturn(username);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).isAccessToken(any());
        assertEquals(existingAuth, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithEmptyUsername_ShouldNotAuthenticate() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        
        when(jwtService.extractUsername(token)).thenReturn("");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).isAccessToken(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithNullUsername_ShouldNotAuthenticate() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        
        when(jwtService.extractUsername(token)).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).isAccessToken(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithMultipleRoles_ShouldParseCorrectly() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String username = "adminuser";
        
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.isAccessToken(token)).thenReturn(true);
        when(jwtService.extractAuthorities(token)).thenReturn("ROLE_USER,ROLE_ADMIN");
        when(jwtService.isTokenValid(eq(token), any(UserDetails.class))).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(2, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void doFilterInternal_WithEmptyAuthorities_ShouldUseDefaultRole() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String username = "testuser";
        
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.isAccessToken(token)).thenReturn(true);
        when(jwtService.extractAuthorities(token)).thenReturn("");
        when(jwtService.isTokenValid(eq(token), any(UserDetails.class))).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(1, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void doFilterInternal_WithNullAuthorities_ShouldUseDefaultRole() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String username = "testuser";
        
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);
        
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.isAccessToken(token)).thenReturn(true);
        when(jwtService.extractAuthorities(token)).thenReturn(null);
        when(jwtService.isTokenValid(eq(token), any(UserDetails.class))).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(1, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void doFilterInternal_ShouldSetCorrelationId() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Correlation-ID")).thenReturn("existing-correlation-id");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertEquals("existing-correlation-id", MDC.get("correlationId"));
    }

    @Test
    void doFilterInternal_WithWebhookEndpoint_ShouldSkipAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/webhooks/authorize-net");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_WithActuatorEndpoint_ShouldSkipAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_WithRootPath_ShouldSkipAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_WithFilterException_ShouldContinueChain() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractUsername(any())).thenThrow(new RuntimeException("Service error"));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        });
        
        verify(filterChain).doFilter(request, response);
    }
}
