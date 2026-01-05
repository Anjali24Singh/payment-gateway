package com.talentica.paymentgateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ApiVersionInterceptorTest {

    private final ApiVersionInterceptor interceptor = new ApiVersionInterceptor();

    @Test
    @DisplayName("Extracts version from URL path and sets header/attribute")
    void fromPath() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v2/payments");
        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean cont = interceptor.preHandle(req, res, new Object());
        assertThat(cont).isTrue();
        assertThat(req.getAttribute(ApiVersionInterceptor.API_VERSION_ATTRIBUTE)).isEqualTo("v2");
        assertThat(res.getHeader(ApiVersionInterceptor.VERSION_HEADER)).isEqualTo("v2");
    }

    @Test
    @DisplayName("Falls back to header, then default v1")
    void fromHeaderOrDefault() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/health");
        req.addHeader(ApiVersionInterceptor.VERSION_HEADER, "v3");
        MockHttpServletResponse res = new MockHttpServletResponse();
        interceptor.preHandle(req, res, new Object());
        assertThat(res.getHeader(ApiVersionInterceptor.VERSION_HEADER)).isEqualTo("v3");

        MockHttpServletRequest req2 = new MockHttpServletRequest("GET", "/no/version");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        interceptor.preHandle(req2, res2, new Object());
        assertThat(res2.getHeader(ApiVersionInterceptor.VERSION_HEADER)).isEqualTo(ApiVersionInterceptor.DEFAULT_VERSION);
    }
}
