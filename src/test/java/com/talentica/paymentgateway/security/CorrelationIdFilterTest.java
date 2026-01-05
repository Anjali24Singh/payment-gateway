package com.talentica.paymentgateway.security;

import com.talentica.paymentgateway.config.ApplicationConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private ApplicationConfig.AppProperties props;
    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        props = new ApplicationConfig.AppProperties();
        var corr = new ApplicationConfig.AppProperties.Correlation();
        corr.setHeaderName("X-Trace-Id");
        corr.setMdcKey("correlationId");
        props.setCorrelation(corr);
        filter = new CorrelationIdFilter(props);
    }

    @AfterEach
    void tearDown() {
        CorrelationIdFilter.clearCorrelationId();
    }

    @Test
    @DisplayName("Uses incoming header and sets response header")
    void usesIncomingHeader() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/health");
        req.addHeader("X-Trace-Id", "abc-123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        AtomicReference<String> seenId = new AtomicReference<>();

        FilterChain chain = (request, response) -> seenId.set(CorrelationIdFilter.getCurrentCorrelationId());

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader("X-Trace-Id")).isEqualTo("abc-123");
        assertThat(seenId.get()).isEqualTo("abc-123");
        assertThat(CorrelationIdFilter.getCurrentCorrelationId()).isNull();
    }

    @Test
    @DisplayName("Generates UUID when header is invalid")
    void generatesWhenInvalid() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/health");
        req.addHeader("X-Trace-Id", "invalid id !!!");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (request, response) -> {});

        String header = res.getHeader("X-Trace-Id");
        assertThat(header).isNotBlank();
        assertThat(header).matches("[a-zA-Z0-9\\-_.]+|[a-f0-9\\-]{36}");
    }

    @Test
    @DisplayName("Static utility set/get/clear")
    void staticUtility() {
        CorrelationIdFilter.setCorrelationId("id-1");
        assertThat(CorrelationIdFilter.getCurrentCorrelationId()).isEqualTo("id-1");
        CorrelationIdFilter.clearCorrelationId();
        assertThat(CorrelationIdFilter.getCurrentCorrelationId()).isNull();
    }
}
