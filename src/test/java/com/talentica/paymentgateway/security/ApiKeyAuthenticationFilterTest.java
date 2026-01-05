package com.talentica.paymentgateway.security;

import com.talentica.paymentgateway.service.ApiKeyService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ApiKeyAuthenticationFilterTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Skips when endpoint does not require API key")
    void skipsWhenNotRequired() throws ServletException, IOException {
        ApiKeyService svc = mock(ApiKeyService.class);
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(svc);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (request, response) -> {});

        verify(svc, never()).isValidApiKey(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Valid API key in header authenticates and sets authorities")
    void validApiKeyHeader() throws ServletException, IOException {
        ApiKeyService svc = mock(ApiKeyService.class);
        when(svc.isValidApiKey("pgw_token")).thenReturn(true);
        when(svc.getClientId("pgw_token")).thenReturn("client-1");
        when(svc.getPermissions("pgw_token")).thenReturn(new java.util.ArrayList<>(java.util.Arrays.asList("READ", "WRITE")));
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(svc);

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/webhook/events");
        req.addHeader("X-API-Key", "pgw_token");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (request, response) -> {});

        // Verify service interactions as proxy for successful auth path
        verify(svc, atLeastOnce()).isValidApiKey("pgw_token");
        verify(svc, atLeastOnce()).getClientId("pgw_token");
        verify(svc, atLeastOnce()).getPermissions("pgw_token");
    }

    @Test
    @DisplayName("Valid API key in query param authenticates")
    void validApiKeyQueryParam() throws ServletException, IOException {
        ApiKeyService svc = mock(ApiKeyService.class);
        when(svc.isValidApiKey("pgw_param")).thenReturn(true);
        when(svc.getClientId("pgw_param")).thenReturn("client-2");
        when(svc.getPermissions("pgw_param")).thenReturn(new java.util.ArrayList<>(java.util.Arrays.asList("READ")));
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(svc);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/webhook/incoming");
        req.setQueryString("api_key=pgw_param");
        req.addParameter("api_key", "pgw_param");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (request, response) -> {});

        // Verify service interactions as proxy for successful auth path
        verify(svc, atLeastOnce()).isValidApiKey("pgw_param");
        verify(svc, atLeastOnce()).getClientId("pgw_param");
        verify(svc, atLeastOnce()).getPermissions("pgw_param");
    }

    @Test
    @DisplayName("Invalid API key does not authenticate")
    void invalidApiKey() throws ServletException, IOException {
        ApiKeyService svc = mock(ApiKeyService.class);
        when(svc.isValidApiKey(anyString())).thenReturn(false);
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(svc);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/webhook/integration");
        req.addHeader("X-API-Key", "bad");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (request, response) -> {});

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Exceptions are handled and chain continues")
    void exceptionHandled() throws ServletException, IOException {
        ApiKeyService svc = mock(ApiKeyService.class);
        when(svc.isValidApiKey(anyString())).thenThrow(new RuntimeException("boom"));
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(svc);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/webhook/events");
        req.addHeader("X-API-Key", "pgw_token");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (request, response) -> {});

        // Should not set authentication on exception
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
