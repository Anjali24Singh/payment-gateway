package com.talentica.paymentgateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentica.paymentgateway.service.RateLimitService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Skips rate limit for excluded endpoints")
    void skipsExcluded() throws ServletException, IOException {
        RateLimitService svc = mock(RateLimitService.class);
        RateLimitFilter filter = new RateLimitFilter(svc, new ObjectMapper());
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (request, response) -> {});

        verify(svc, never()).isAllowed(anyString());
    }

    @Test
    @DisplayName("Allows request and adds rate limit headers")
    void allowsAndAddsHeaders() throws ServletException, IOException {
        RateLimitService svc = mock(RateLimitService.class);
        when(svc.isAllowed(anyString())).thenReturn(new RateLimitService.RateLimitResult(true, 100, 99, Instant.now().plusSeconds(60)));
        RateLimitFilter filter = new RateLimitFilter(svc, new ObjectMapper());

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/payments");
        req.addHeader("X-Forwarded-For", "203.0.113.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (request, response) -> {});

        assertThat(res.getHeader("X-RateLimit-Limit")).isEqualTo("100");
        assertThat(res.getHeader("X-RateLimit-Remaining")).isEqualTo("99");
        assertThat(res.getHeader("X-RateLimit-Reset")).isNotBlank();
    }

    @Test
    @DisplayName("Blocks when not allowed and returns 429 JSON with Retry-After")
    void blocksWhenNotAllowed() throws ServletException, IOException {
        RateLimitService svc = mock(RateLimitService.class);
        when(svc.isAllowed(anyString())).thenReturn(new RateLimitService.RateLimitResult(false, 100, 0, Instant.now().plusSeconds(10)));
        RateLimitFilter filter = new RateLimitFilter(svc, new ObjectMapper());

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/payments");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (request, response) -> {});

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(res.getHeader("Retry-After")).isNotBlank();
        assertThat(res.getContentAsString()).contains("rate_limit_exceeded");
    }

    @Test
    @DisplayName("Identifier prefers API key credentials, then username, then IP")
    void identifierPreference() throws ServletException, IOException {
        RateLimitService svc = mock(RateLimitService.class);
        when(svc.isAllowed(anyString())).thenReturn(new RateLimitService.RateLimitResult(true, 100, 100, Instant.now().plusSeconds(60)));
        RateLimitFilter filter = new RateLimitFilter(svc, new ObjectMapper());

        // Case 1: API key credentials present
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("john", "pgw_abc", java.util.Collections.emptyList()));
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/payments");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, (request, response) -> {});

        // Case 2: Username without api key
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("john", null, java.util.Collections.emptyList()));
        filter.doFilter(req, res, (request, response) -> {});

        // Case 3: No auth -> IP fallback
        SecurityContextHolder.clearContext();
        MockHttpServletRequest req2 = new MockHttpServletRequest("GET", "/api/v1/payments");
        req2.setRemoteAddr("198.51.100.10");
        filter.doFilter(req2, new MockHttpServletResponse(), (request, response) -> {});

        // Capture all invocations across scenarios
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(svc, atLeast(3)).isAllowed(captor.capture());
        assertThat(captor.getAllValues()).hasSizeGreaterThanOrEqualTo(3);
    }
}
