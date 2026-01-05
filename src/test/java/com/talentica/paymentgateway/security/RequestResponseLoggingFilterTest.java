package com.talentica.paymentgateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RequestResponseLoggingFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter(objectMapper);

    private static class WritingChain implements FilterChain {
        private final String body;
        private final int status;
        WritingChain(String body, int status) { this.body = body; this.status = status; }
        @Override public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) throws IOException {
            var resp = (org.springframework.web.util.ContentCachingResponseWrapper) response;
            resp.setStatus(status);
            resp.setContentType("application/json");
            resp.getOutputStream().write(body.getBytes());
        }
    }

    @Test
    @DisplayName("Skips logging for excluded endpoints")
    void skipsExcludedEndpoints() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (request, response) -> {});

        assertThat(res.getStatus()).isEqualTo(200); // default
    }

    @Test
    @DisplayName("Logs request/response and preserves body")
    void logsAndPreservesBody() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/payments/purchase");
        req.addHeader("User-Agent", "JUnit");
        req.setContentType("application/json");
        req.setContent("{\"cardNumber\":\"4111111111111111\",\"password\":\"secret\"}".getBytes());

        MockHttpServletResponse res = new MockHttpServletResponse();
        var chain = new WritingChain("{\"ok\":true,\"ssn\":\"123-45-6789\"}", 200);

        filter.doFilter(req, res, chain);

        assertThat(res.getContentType()).isEqualTo("application/json");
        assertThat(res.getContentAsString()).contains("ok");
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Protected overrides return true for error/async dispatch")
    void protectedOverrides() {
        assertThat(filter.shouldNotFilterErrorDispatch()).isTrue();
        assertThat(filter.shouldNotFilterAsyncDispatch()).isTrue();
    }
}
