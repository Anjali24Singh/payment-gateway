package com.talentica.paymentgateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * OpenAPI 3.0 configuration for the Payment Gateway API.
 * Provides comprehensive API documentation with security schemes, examples, and server configurations.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Bean
    public OpenAPI paymentGatewayOpenAPI() {
        return new OpenAPI()
            .info(createApiInfo())
            .externalDocs(createExternalDocumentation())
            .servers(createServers())
            .tags(createTags())
            .components(createComponents())
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("payment-gateway-api")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    private Info createApiInfo() {
        return new Info()
            .title("Payment Gateway API")
            .description("""
                # Payment Gateway Integration Platform
                
                A comprehensive payment processing API with Authorize.Net integration, designed for enterprise-grade 
                payment processing solutions.
                
                ## Features
                - **Secure Payment Processing**: Full PCI DSS compliant payment processing
                - **Multiple Payment Types**: Support for credit cards, ACH, and digital wallets
                - **Real-time Processing**: Instant authorization and capture capabilities
                - **Subscription Management**: Recurring billing and subscription handling
                - **Webhook Support**: Real-time event notifications
                - **Analytics & Reporting**: Comprehensive transaction analytics
                - **Rate Limiting**: Built-in protection against abuse
                - **Idempotency**: Safe retry mechanisms for reliable processing
                
                ## Authentication
                All API endpoints require Bearer token authentication. Obtain your access token using the 
                `/auth/login` endpoint.
                
                ## Error Handling
                The API uses standard HTTP status codes and returns detailed error information in a 
                consistent format with correlation IDs for tracking.
                
                ## Rate Limiting
                API requests are rate limited to prevent abuse. Current limits:
                - 1000 requests per hour per API key
                - Burst capacity of 100 requests
                
                ## Idempotency
                All mutation operations support idempotency using the `Idempotency-Key` header to safely 
                retry requests.
                
                ## Versioning
                This API uses URL path versioning (v1). Breaking changes will be introduced in new versions.
                """)
            .version(appVersion)
            .contact(new Contact()
                .name("Payment Gateway Team")
                .email("support@talentica.com")
                .url("https://talentica.com/support"))
            .license(new License()
                .name("API License")
                .url("https://talentica.com/api-license"))
            .termsOfService("https://talentica.com/terms");
    }

    private ExternalDocumentation createExternalDocumentation() {
        return new ExternalDocumentation()
            .description("Payment Gateway Documentation")
            .url("https://docs.talentica.com/payment-gateway");
    }

    private List<Server> createServers() {
        return Arrays.asList(
            new Server()
                .url("http://localhost:" + serverPort + contextPath)
                .description("Local Development Server"),
            new Server()
                .url("https://api-staging.talentica.com/v1")
                .description("Staging Environment"),
            new Server()
                .url("https://api.talentica.com/v1")
                .description("Production Environment"),
            new Server()
                .url("https://sandbox.talentica.com/v1")
                .description("Sandbox Environment for Testing")
        );
    }

    private List<Tag> createTags() {
        return Arrays.asList(
            new Tag()
                .name("Authentication")
                .description("User authentication and token management operations"),
            new Tag()
                .name("Payment Processing")
                .description("Core payment processing operations including purchase, authorize, capture, void, and refund"),
            new Tag()
                .name("Subscriptions")
                .description("Recurring billing and subscription management"),
            new Tag()
                .name("Subscription Plans")
                .description("Subscription plan configuration and management"),
            new Tag()
                .name("Webhooks")
                .description("Webhook management and event processing"),
            new Tag()
                .name("Analytics")
                .description("Transaction analytics and reporting"),
            new Tag()
                .name("Health")
                .description("Health check and monitoring endpoints")
        );
    }

    private Components createComponents() {
        return new Components()
            .addSecuritySchemes("Bearer Authentication", createSecurityScheme())
            .addSchemas("Error", createErrorSchema())
            .addSchemas("PaginationMeta", createPaginationSchema());
    }

    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("""
                JWT Bearer token authentication. 
                
                To authenticate:
                1. Obtain an access token using the `/auth/login` endpoint
                2. Include the token in the Authorization header: `Authorization: Bearer <token>`
                
                Example:
                ```
                Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                ```
                """);
    }

    private io.swagger.v3.oas.models.media.Schema<?> createErrorSchema() {
        return new io.swagger.v3.oas.models.media.Schema<>()
            .type("object")
            .description("Standard error response format")
            .addProperty("error_code", new io.swagger.v3.oas.models.media.StringSchema()
                .description("Unique error code for the error type"))
            .addProperty("message", new io.swagger.v3.oas.models.media.StringSchema()
                .description("Human-readable error message"))
            .addProperty("description", new io.swagger.v3.oas.models.media.StringSchema()
                .description("Detailed error description"))
            .addProperty("category", new io.swagger.v3.oas.models.media.StringSchema()
                .description("Error category (VALIDATION_ERROR, PAYMENT_ERROR, etc.)"))
            .addProperty("correlation_id", new io.swagger.v3.oas.models.media.StringSchema()
                .description("Unique correlation ID for tracking the request"))
            .addProperty("timestamp", new io.swagger.v3.oas.models.media.StringSchema()
                .format("date-time")
                .description("Error timestamp"))
            .addProperty("details", new io.swagger.v3.oas.models.media.ArraySchema()
                .items(new io.swagger.v3.oas.models.media.Schema<>()
                    .type("object")
                    .addProperty("field", new io.swagger.v3.oas.models.media.StringSchema())
                    .addProperty("code", new io.swagger.v3.oas.models.media.StringSchema())
                    .addProperty("message", new io.swagger.v3.oas.models.media.StringSchema()))
                .description("Detailed validation errors"));
    }

    private io.swagger.v3.oas.models.media.Schema<?> createPaginationSchema() {
        return new io.swagger.v3.oas.models.media.Schema<>()
            .type("object")
            .description("Pagination metadata for list responses")
            .addProperty("page", new io.swagger.v3.oas.models.media.IntegerSchema()
                .description("Current page number (0-based)"))
            .addProperty("size", new io.swagger.v3.oas.models.media.IntegerSchema()
                .description("Number of items per page"))
            .addProperty("total_elements", new io.swagger.v3.oas.models.media.IntegerSchema()
                .format("int64")
                .description("Total number of items"))
            .addProperty("total_pages", new io.swagger.v3.oas.models.media.IntegerSchema()
                .description("Total number of pages"))
            .addProperty("has_next", new io.swagger.v3.oas.models.media.BooleanSchema()
                .description("Whether there are more pages"))
            .addProperty("has_previous", new io.swagger.v3.oas.models.media.BooleanSchema()
                .description("Whether there are previous pages"));
    }
}
