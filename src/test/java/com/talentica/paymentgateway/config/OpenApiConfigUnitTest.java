package com.talentica.paymentgateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpenApiConfig.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class OpenApiConfigUnitTest {

    private OpenApiConfig openApiConfig;

    @BeforeEach
    void setUp() {
        openApiConfig = new OpenApiConfig();
        // Set default values using reflection
        ReflectionTestUtils.setField(openApiConfig, "appVersion", "1.0.0");
        ReflectionTestUtils.setField(openApiConfig, "serverPort", "8080");
        ReflectionTestUtils.setField(openApiConfig, "contextPath", "/api/v1");
    }

    @Test
    void constructor_ShouldCreateConfig() {
        // When & Then
        assertNotNull(openApiConfig);
    }

    @Test
    void paymentGatewayOpenAPI_ShouldReturnConfiguredOpenAPI() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertNotNull(openAPI.getExternalDocs());
        assertNotNull(openAPI.getServers());
        assertNotNull(openAPI.getTags());
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getSecurity());
    }

    @Test
    void paymentGatewayOpenAPI_ShouldHaveCorrectInfo() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertEquals("Payment Gateway API", openAPI.getInfo().getTitle());
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
        assertNotNull(openAPI.getInfo().getDescription());
        assertTrue(openAPI.getInfo().getDescription().contains("Payment Gateway Integration Platform"));
    }

    @Test
    void paymentGatewayOpenAPI_ShouldHaveContact() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertNotNull(openAPI.getInfo().getContact());
        assertEquals("Payment Gateway Team", openAPI.getInfo().getContact().getName());
        assertEquals("support@talentica.com", openAPI.getInfo().getContact().getEmail());
        assertEquals("https://talentica.com/support", openAPI.getInfo().getContact().getUrl());
    }

    @Test
    void paymentGatewayOpenAPI_ShouldHaveLicense() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertNotNull(openAPI.getInfo().getLicense());
        assertEquals("API License", openAPI.getInfo().getLicense().getName());
        assertEquals("https://talentica.com/api-license", openAPI.getInfo().getLicense().getUrl());
    }

    @Test
    void paymentGatewayOpenAPI_ShouldHaveTermsOfService() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertEquals("https://talentica.com/terms", openAPI.getInfo().getTermsOfService());
    }

    @Test
    void paymentGatewayOpenAPI_ShouldHaveExternalDocs() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertNotNull(openAPI.getExternalDocs());
        assertEquals("Payment Gateway Documentation", openAPI.getExternalDocs().getDescription());
        assertEquals("https://docs.talentica.com/payment-gateway", openAPI.getExternalDocs().getUrl());
    }

    @Test
    void paymentGatewayOpenAPI_ShouldHaveServers() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertNotNull(openAPI.getServers());
        assertEquals(4, openAPI.getServers().size());
        
        // Check local server
        assertTrue(openAPI.getServers().stream()
            .anyMatch(server -> server.getUrl().contains("localhost:8080")));
        
        // Check staging server
        assertTrue(openAPI.getServers().stream()
            .anyMatch(server -> server.getUrl().contains("api-staging.talentica.com")));
        
        // Check production server
        assertTrue(openAPI.getServers().stream()
            .anyMatch(server -> server.getUrl().contains("api.talentica.com")));
        
        // Check sandbox server
        assertTrue(openAPI.getServers().stream()
            .anyMatch(server -> server.getUrl().contains("sandbox.talentica.com")));
    }

    @Test
    void paymentGatewayOpenAPI_ShouldHaveTags() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertNotNull(openAPI.getTags());
        assertTrue(openAPI.getTags().size() >= 7);
        
        // Check for expected tags
        assertTrue(openAPI.getTags().stream()
            .anyMatch(tag -> "Authentication".equals(tag.getName())));
        assertTrue(openAPI.getTags().stream()
            .anyMatch(tag -> "Payment Processing".equals(tag.getName())));
        assertTrue(openAPI.getTags().stream()
            .anyMatch(tag -> "Subscriptions".equals(tag.getName())));
        assertTrue(openAPI.getTags().stream()
            .anyMatch(tag -> "Webhooks".equals(tag.getName())));
        assertTrue(openAPI.getTags().stream()
            .anyMatch(tag -> "Analytics".equals(tag.getName())));
    }

    @Test
    void paymentGatewayOpenAPI_ShouldHaveSecurityScheme() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("Bearer Authentication"));
        
        var securityScheme = openAPI.getComponents().getSecuritySchemes().get("Bearer Authentication");
        assertEquals("bearer", securityScheme.getScheme());
        assertEquals("JWT", securityScheme.getBearerFormat());
    }

    @Test
    void paymentGatewayOpenAPI_ShouldHaveErrorSchema() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertNotNull(openAPI.getComponents().getSchemas());
        assertTrue(openAPI.getComponents().getSchemas().containsKey("Error"));
        
        var errorSchema = openAPI.getComponents().getSchemas().get("Error");
        assertNotNull(errorSchema.getProperties());
        assertTrue(errorSchema.getProperties().containsKey("error_code"));
        assertTrue(errorSchema.getProperties().containsKey("message"));
        assertTrue(errorSchema.getProperties().containsKey("correlation_id"));
    }

    @Test
    void paymentGatewayOpenAPI_ShouldHavePaginationSchema() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertTrue(openAPI.getComponents().getSchemas().containsKey("PaginationMeta"));
        
        var paginationSchema = openAPI.getComponents().getSchemas().get("PaginationMeta");
        assertNotNull(paginationSchema.getProperties());
        assertTrue(paginationSchema.getProperties().containsKey("page"));
        assertTrue(paginationSchema.getProperties().containsKey("size"));
        assertTrue(paginationSchema.getProperties().containsKey("total_elements"));
        assertTrue(paginationSchema.getProperties().containsKey("has_next"));
    }

    @Test
    void paymentGatewayOpenAPI_ShouldHaveSecurityRequirement() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertNotNull(openAPI.getSecurity());
        assertFalse(openAPI.getSecurity().isEmpty());
        assertTrue(openAPI.getSecurity().get(0).containsKey("Bearer Authentication"));
    }

    @Test
    void publicApi_ShouldReturnGroupedOpenApi() {
        // When
        GroupedOpenApi groupedOpenApi = openApiConfig.publicApi();

        // Then
        assertNotNull(groupedOpenApi);
        assertEquals("payment-gateway-api", groupedOpenApi.getGroup());
    }

    @Test
    void paymentGatewayOpenAPI_WithCustomVersion_ShouldUseCustomVersion() {
        // Given
        ReflectionTestUtils.setField(openApiConfig, "appVersion", "2.0.0");

        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertEquals("2.0.0", openAPI.getInfo().getVersion());
    }

    @Test
    void paymentGatewayOpenAPI_WithCustomPort_ShouldUseCustomPort() {
        // Given
        ReflectionTestUtils.setField(openApiConfig, "serverPort", "9090");

        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertTrue(openAPI.getServers().stream()
            .anyMatch(server -> server.getUrl().contains("localhost:9090")));
    }

    @Test
    void paymentGatewayOpenAPI_WithCustomContextPath_ShouldUseCustomPath() {
        // Given
        ReflectionTestUtils.setField(openApiConfig, "contextPath", "/api/v2");

        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertTrue(openAPI.getServers().stream()
            .anyMatch(server -> server.getUrl().contains("/api/v2")));
    }

    @Test
    void paymentGatewayOpenAPI_InfoDescription_ShouldContainKeyFeatures() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();
        String description = openAPI.getInfo().getDescription();

        // Then
        assertTrue(description.contains("Secure Payment Processing"));
        assertTrue(description.contains("Multiple Payment Types"));
        assertTrue(description.contains("Real-time Processing"));
        assertTrue(description.contains("Subscription Management"));
        assertTrue(description.contains("Webhook Support"));
        assertTrue(description.contains("Analytics & Reporting"));
        assertTrue(description.contains("Rate Limiting"));
        assertTrue(description.contains("Idempotency"));
    }

    @Test
    void paymentGatewayOpenAPI_InfoDescription_ShouldContainAuthenticationInfo() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();
        String description = openAPI.getInfo().getDescription();

        // Then
        assertTrue(description.contains("Authentication"));
        assertTrue(description.contains("Bearer token authentication"));
        assertTrue(description.contains("/auth/login"));
    }

    @Test
    void paymentGatewayOpenAPI_InfoDescription_ShouldContainRateLimitInfo() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();
        String description = openAPI.getInfo().getDescription();

        // Then
        assertTrue(description.contains("Rate Limiting"));
        assertTrue(description.contains("1000 requests per hour"));
        assertTrue(description.contains("100 requests"));
    }

    @Test
    void paymentGatewayOpenAPI_SecuritySchemeDescription_ShouldContainJWTInfo() {
        // When
        OpenAPI openAPI = openApiConfig.paymentGatewayOpenAPI();
        var securityScheme = openAPI.getComponents().getSecuritySchemes().get("Bearer Authentication");

        // Then
        assertNotNull(securityScheme.getDescription());
        assertTrue(securityScheme.getDescription().contains("JWT Bearer token"));
        assertTrue(securityScheme.getDescription().contains("/auth/login"));
        assertTrue(securityScheme.getDescription().contains("Authorization: Bearer"));
    }

    @Test
    void paymentGatewayOpenAPI_MultipleCallsShouldReturnDifferentInstances() {
        // When
        OpenAPI openAPI1 = openApiConfig.paymentGatewayOpenAPI();
        OpenAPI openAPI2 = openApiConfig.paymentGatewayOpenAPI();

        // Then
        assertNotNull(openAPI1);
        assertNotNull(openAPI2);
        assertNotSame(openAPI1, openAPI2);
    }

    @Test
    void publicApi_MultipleCallsShouldReturnDifferentInstances() {
        // When
        GroupedOpenApi api1 = openApiConfig.publicApi();
        GroupedOpenApi api2 = openApiConfig.publicApi();

        // Then
        assertNotNull(api1);
        assertNotNull(api2);
        assertNotSame(api1, api2);
    }
}
