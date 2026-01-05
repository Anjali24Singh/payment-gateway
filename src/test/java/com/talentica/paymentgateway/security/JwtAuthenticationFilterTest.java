package com.talentica.paymentgateway.security;

import com.talentica.paymentgateway.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        filter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static class RecordingChain implements FilterChain {
        boolean invoked;
        @Override public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            invoked = true;
        }
    }

    @Test
    @DisplayName("Skips authentication for public endpoints")
    void skipsPublicEndpoints() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/health/status");
        MockHttpServletResponse res = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, res, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("No Authorization header proceeds without auth")
    void noAuthHeader() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/transactions");
        MockHttpServletResponse res = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, res, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Non-Bearer Authorization proceeds without auth")
    void nonBearerHeader() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/payments");
        req.addHeader("Authorization", "Basic abc");
        MockHttpServletResponse res = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        filter.doFilter(req, res, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Valid access token authenticates user and sets authorities")
    void validAccessTokenAuthenticates() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/payments");
        req.addHeader("Authorization", "Bearer token123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        when(jwtService.extractUsername("token123")).thenReturn("john");
        when(jwtService.isAccessToken("token123")).thenReturn(true);
        when(jwtService.extractAuthorities("token123")).thenReturn("[ROLE_USER, ROLE_ADMIN]");
        when(jwtService.isTokenValid(eq("token123"), any())).thenReturn(true);
        when(jwtService.extractUserId("token123")).thenReturn("u-1");

        filter.doFilter(req, res, chain);

        assertThat(chain.invoked).isTrue();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("john");
        assertThat(auth.getAuthorities()).extracting("authority")
                .contains("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Non-access token does not authenticate")
    void nonAccessToken() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/payments");
        req.addHeader("Authorization", "Bearer token123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        when(jwtService.extractUsername("token123")).thenReturn("john");
        when(jwtService.isAccessToken("token123")).thenReturn(false);

        filter.doFilter(req, res, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("JWT exception is handled and continues chain")
    void jwtExceptionHandled() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/payments");
        req.addHeader("Authorization", "Bearer token123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        when(jwtService.extractUsername(anyString())).thenThrow(new RuntimeException("boom"));

        filter.doFilter(req, res, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
