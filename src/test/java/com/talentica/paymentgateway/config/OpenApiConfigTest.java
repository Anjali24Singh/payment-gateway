package com.talentica.paymentgateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    @DisplayName("Builds OpenAPI bean with components and security")
    void buildsOpenAPI() {
        OpenApiConfig cfg = new OpenApiConfig();
        ReflectionTestUtils.setField(cfg, "appVersion", "2.0.0");
        ReflectionTestUtils.setField(cfg, "serverPort", "8081");
        ReflectionTestUtils.setField(cfg, "contextPath", "/api/v1");
        OpenAPI api = cfg.paymentGatewayOpenAPI();
        assertThat(api.getInfo().getVersion()).isEqualTo("2.0.0");
        assertThat(api.getServers()).isNotEmpty();
        assertThat(api.getComponents().getSecuritySchemes()).containsKey("Bearer Authentication");
        assertThat(api.getSecurity()).isNotEmpty();
        assertThat(cfg.publicApi().getGroup()).isEqualTo("payment-gateway-api");
    }
}
