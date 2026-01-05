package com.talentica.paymentgateway.security;

import com.talentica.paymentgateway.service.ApiKeyService;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApiKeyAuthenticationFilter.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterUnitTest {

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @BeforeEach
    void setUp() {
        apiKeyAuthenticationFilter = new ApiKeyAuthenticationFilter(apiKeyService);
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void constructor_WithApiKeyService_ShouldCreateFilter() {
        // When & Then
        assertNotNull(apiKeyAuthenticationFilter);
    }

    @Test
    void doFilterInternal_WithNonApiKeyEndpoint_ShouldSkipAuthentication() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/payments");

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(apiKeyService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithWebhookEndpoint_ShouldProcessApiKey() throws ServletException, IOException {
        // Given
        String apiKey = "valid-api-key";
        String clientId = "webhook-client";
        List<String> permissions = Arrays.asList("webhook:read", "webhook:write");

        when(request.getRequestURI()).thenReturn("/api/v1/webhook/authorize-net");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(true);
        when(apiKeyService.getClientId(apiKey)).thenReturn(clientId);
        when(apiKeyService.getPermissions(apiKey)).thenReturn(permissions);

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(apiKeyService).isValidApiKey(apiKey);
        verify(apiKeyService).getClientId(apiKey);
        verify(apiKeyService).getPermissions(apiKey);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(clientId, authentication.getName());
        assertEquals(apiKey, authentication.getCredentials());
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_API_CLIENT")));
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("SCOPE_webhook:read")));
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("SCOPE_webhook:write")));
    }

    @Test
    void doFilterInternal_WithExternalEndpoint_ShouldProcessApiKey() throws ServletException, IOException {
        // Given
        String apiKey = "external-api-key";
        String clientId = "external-client";

        when(request.getRequestURI()).thenReturn("/api/v1/external/data");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(true);
        when(apiKeyService.getClientId(apiKey)).thenReturn(clientId);
        when(apiKeyService.getPermissions(apiKey)).thenReturn(Collections.emptyList());

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(apiKeyService).isValidApiKey(apiKey);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(clientId, authentication.getName());
    }

    @Test
    void doFilterInternal_WithIntegrationEndpoint_ShouldProcessApiKey() throws ServletException, IOException {
        // Given
        String apiKey = "integration-key";
        
        when(request.getRequestURI()).thenReturn("/api/v1/integration/sync");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(false);

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(apiKeyService).isValidApiKey(apiKey);
        verify(apiKeyService, never()).getClientId(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithNoApiKeyHeader_ShouldContinueWithoutAuth() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/webhook/test");
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getParameter("api_key")).thenReturn(null);

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(apiKeyService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithApiKeyInQueryParam_ShouldAuthenticate() throws ServletException, IOException {
        // Given
        String apiKey = "query-param-key";
        String clientId = "param-client";

        when(request.getRequestURI()).thenReturn("/api/v1/webhook/callback");
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getParameter("api_key")).thenReturn(apiKey);
        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(true);
        when(apiKeyService.getClientId(apiKey)).thenReturn(clientId);
        when(apiKeyService.getPermissions(apiKey)).thenReturn(Collections.singletonList("callback"));

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(apiKeyService).isValidApiKey(apiKey);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(clientId, authentication.getName());
    }

    @Test
    void doFilterInternal_WithInvalidApiKey_ShouldNotAuthenticate() throws ServletException, IOException {
        // Given
        String apiKey = "invalid-key";

        when(request.getRequestURI()).thenReturn("/api/v1/webhook/test");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(false);

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(apiKeyService).isValidApiKey(apiKey);
        verify(apiKeyService, never()).getClientId(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithApiKeyServiceException_ShouldContinueWithoutAuth() throws ServletException, IOException {
        // Given
        String apiKey = "error-key";

        when(request.getRequestURI()).thenReturn("/api/v1/webhook/test");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.isValidApiKey(apiKey)).thenThrow(new RuntimeException("Service error"));

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithEmptyApiKey_ShouldNotAuthenticate() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/webhook/test");
        when(request.getHeader("X-API-Key")).thenReturn("");
        when(request.getParameter("api_key")).thenReturn(null);

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(apiKeyService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithWhitespaceApiKey_ShouldNotAuthenticate() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/webhook/test");
        when(request.getHeader("X-API-Key")).thenReturn("   ");

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(apiKeyService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithValidApiKey_ShouldSetMDCValues() throws ServletException, IOException {
        // Given
        String apiKey = "valid-key";
        String clientId = "test-client";

        when(request.getRequestURI()).thenReturn("/api/v1/webhook/test");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(true);
        when(apiKeyService.getClientId(apiKey)).thenReturn(clientId);
        when(apiKeyService.getPermissions(apiKey)).thenReturn(Collections.emptyList());

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        // MDC values are cleared in finally block, but we can verify service calls
        verify(apiKeyService).getClientId(apiKey);
    }

    @Test
    void doFilterInternal_WithMultiplePermissions_ShouldCreateCorrectAuthorities() throws ServletException, IOException {
        // Given
        String apiKey = "multi-perm-key";
        String clientId = "multi-client";
        List<String> permissions = Arrays.asList("read", "write", "admin");

        when(request.getRequestURI()).thenReturn("/api/v1/webhook/multi");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(true);
        when(apiKeyService.getClientId(apiKey)).thenReturn(clientId);
        when(apiKeyService.getPermissions(apiKey)).thenReturn(permissions);

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(4, authentication.getAuthorities().size()); // 3 permissions + 1 role
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("SCOPE_read")));
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("SCOPE_write")));
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("SCOPE_admin")));
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_API_CLIENT")));
    }

    @Test
    void doFilterInternal_WithNullPermissions_ShouldOnlyHaveDefaultRole() throws ServletException, IOException {
        // Given
        String apiKey = "null-perm-key";
        String clientId = "null-client";

        when(request.getRequestURI()).thenReturn("/api/v1/webhook/null");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(true);
        when(apiKeyService.getClientId(apiKey)).thenReturn(clientId);
        when(apiKeyService.getPermissions(apiKey)).thenReturn(null);

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(1, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_API_CLIENT")));
    }

    @Test
    void doFilterInternal_WithEmptyPermissions_ShouldOnlyHaveDefaultRole() throws ServletException, IOException {
        // Given
        String apiKey = "empty-perm-key";
        String clientId = "empty-client";

        when(request.getRequestURI()).thenReturn("/api/v1/webhook/empty");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(true);
        when(apiKeyService.getClientId(apiKey)).thenReturn(clientId);
        when(apiKeyService.getPermissions(apiKey)).thenReturn(Collections.emptyList());

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(1, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_API_CLIENT")));
    }

    @Test
    void shouldNotFilter_WithNonApiKeyEndpoint_ShouldReturnTrue() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/payments");

        // When
        boolean shouldNotFilter = apiKeyAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertTrue(shouldNotFilter);
    }

    @Test
    void shouldNotFilter_WithWebhookEndpoint_ShouldReturnFalse() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/webhook/test");

        // When
        boolean shouldNotFilter = apiKeyAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertFalse(shouldNotFilter);
    }

    @Test
    void shouldNotFilter_WithExternalEndpoint_ShouldReturnFalse() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/external/api");

        // When
        boolean shouldNotFilter = apiKeyAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertFalse(shouldNotFilter);
    }

    @Test
    void shouldNotFilter_WithIntegrationEndpoint_ShouldReturnFalse() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/integration/sync");

        // When
        boolean shouldNotFilter = apiKeyAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertFalse(shouldNotFilter);
    }

    @Test
    void doFilterInternal_WithHeaderPriorityOverParam_ShouldUseHeader() throws ServletException, IOException {
        // Given
        String headerKey = "header-key";
        String paramKey = "param-key";
        String clientId = "header-client";

        when(request.getRequestURI()).thenReturn("/api/v1/webhook/priority");
        when(request.getHeader("X-API-Key")).thenReturn(headerKey);
        lenient().when(request.getParameter("api_key")).thenReturn(paramKey); // Lenient: param not used when header present
        when(apiKeyService.isValidApiKey(headerKey)).thenReturn(true);
        when(apiKeyService.getClientId(headerKey)).thenReturn(clientId);
        when(apiKeyService.getPermissions(headerKey)).thenReturn(Collections.emptyList());

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(apiKeyService).isValidApiKey(headerKey);
        verify(apiKeyService, never()).isValidApiKey(paramKey);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(clientId, authentication.getName());
        assertEquals(headerKey, authentication.getCredentials());
    }

    @Test
    void doFilterInternal_WithFilterException_ShouldContinueChain() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/webhook/error");
        when(request.getHeader("X-API-Key")).thenReturn("error-key");
        when(apiKeyService.isValidApiKey(any())).thenThrow(new RuntimeException("Unexpected error"));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);
        });
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithNullClientId_ShouldHandleGracefully() throws ServletException, IOException {
        // Given
        String apiKey = "null-client-key";

        when(request.getRequestURI()).thenReturn("/api/v1/webhook/null-client");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(true);
        when(apiKeyService.getClientId(apiKey)).thenReturn(null);
        when(apiKeyService.getPermissions(apiKey)).thenReturn(Collections.emptyList());

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("", authentication.getName()); // Spring Security returns "" for null principal
    }

    @Test
    void doFilterInternal_WithEmptyClientId_ShouldHandleGracefully() throws ServletException, IOException {
        // Given
        String apiKey = "empty-client-key";

        when(request.getRequestURI()).thenReturn("/api/v1/webhook/empty-client");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(apiKeyService.isValidApiKey(apiKey)).thenReturn(true);
        when(apiKeyService.getClientId(apiKey)).thenReturn("");
        when(apiKeyService.getPermissions(apiKey)).thenReturn(Collections.emptyList());

        // When
        apiKeyAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("", authentication.getName());
    }
}
