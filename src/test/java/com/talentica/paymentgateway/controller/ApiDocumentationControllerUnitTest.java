package com.talentica.paymentgateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ApiDocumentationController.
 * Tests all documentation endpoints without Spring context.
 */
@ExtendWith(MockitoExtension.class)
class ApiDocumentationControllerUnitTest {

    @InjectMocks
    private ApiDocumentationController apiDocumentationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(apiDocumentationController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetIntegrationGuide_ReturnsComprehensiveGuide() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/docs/integration-guide"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Payment Gateway API Integration Guide"))
                .andExpect(jsonPath("$.data.version").value("1.0.0"))
                .andExpect(jsonPath("$.data.quickStart").exists())
                .andExpect(jsonPath("$.data.quickStart.steps").isArray())
                .andExpect(jsonPath("$.data.quickStart.authenticationExample").exists())
                .andExpect(jsonPath("$.data.quickStart.paymentExample").exists())
                .andExpect(jsonPath("$.data.authentication").exists())
                .andExpect(jsonPath("$.data.authentication.type").value("JWT Bearer Token Authentication"))
                .andExpect(jsonPath("$.data.paymentExamples").isArray())
                .andExpect(jsonPath("$.data.bestPractices").isArray())
                .andExpect(jsonPath("$.data.errorHandling").exists())
                .andExpect(jsonPath("$.data.webhooks").exists());
    }

    @Test
    void testGetCodeExamples_JavaScript_ReturnsJavaScriptExamples() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/docs/examples/javascript"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.authentication").exists())
                .andExpect(jsonPath("$.data.payment").exists())
                .andExpect(jsonPath("$.data.authentication").value(org.hamcrest.Matchers.containsString("fetch")))
                .andExpect(jsonPath("$.data.payment").value(org.hamcrest.Matchers.containsString("fetch")));
    }

    @Test
    void testGetCodeExamples_Python_ReturnsPythonExamples() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/docs/examples/python"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.authentication").exists())
                .andExpect(jsonPath("$.data.payment").exists())
                .andExpect(jsonPath("$.data.authentication").value(org.hamcrest.Matchers.containsString("requests")))
                .andExpect(jsonPath("$.data.payment").value(org.hamcrest.Matchers.containsString("requests")));
    }

    @Test
    void testGetCodeExamples_Java_ReturnsJavaExamples() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/docs/examples/java"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.authentication").exists())
                .andExpect(jsonPath("$.data.payment").exists())
                .andExpect(jsonPath("$.data.authentication").value(org.hamcrest.Matchers.containsString("RestTemplate")))
                .andExpect(jsonPath("$.data.payment").value(org.hamcrest.Matchers.containsString("restTemplate")));
    }

    @Test
    void testGetCodeExamples_Curl_ReturnsCurlExamples() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/docs/examples/curl"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.authentication").exists())
                .andExpect(jsonPath("$.data.payment").exists())
                .andExpect(jsonPath("$.data.authentication").value(org.hamcrest.Matchers.containsString("curl")))
                .andExpect(jsonPath("$.data.payment").value(org.hamcrest.Matchers.containsString("curl")));
    }

    @Test
    void testGetCodeExamples_UnsupportedLanguage_ReturnsNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/docs/examples/unsupported"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetTestingScenarios_ReturnsTestScenarios() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/docs/testing-scenarios"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value("successful_payment"))
                .andExpect(jsonPath("$.data[0].title").value("Successful Payment Processing"))
                .andExpect(jsonPath("$.data[0].description").exists())
                .andExpect(jsonPath("$.data[0].steps").isArray())
                .andExpect(jsonPath("$.data[0].testData").exists())
                .andExpect(jsonPath("$.data[1].id").value("declined_payment"))
                .andExpect(jsonPath("$.data[2].id").value("authentication"))
                .andExpect(jsonPath("$.data[3].id").value("refund_processing"));
    }

    @Test
    void testGetChangelog_ReturnsVersionHistory() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/docs/changelog"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].version").value("v1.0.0"))
                .andExpect(jsonPath("$.data[0].date").value("2025-09-10"))
                .andExpect(jsonPath("$.data[0].description").value("Initial release"))
                .andExpect(jsonPath("$.data[0].added").isArray())
                .andExpect(jsonPath("$.data[0].changed").isArray())
                .andExpect(jsonPath("$.data[0].removed").isArray());
    }

    @Test
    void testIntegrationGuide_ContainsQuickStartSteps() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/docs/integration-guide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quickStart.steps").isArray())
                .andExpect(jsonPath("$.data.quickStart.steps[0]").value(org.hamcrest.Matchers.containsString("Register for a sandbox account")))
                .andExpect(jsonPath("$.data.quickStart.steps[1]").value(org.hamcrest.Matchers.containsString("Obtain your API credentials")))
                .andExpect(jsonPath("$.data.quickStart.steps[2]").value(org.hamcrest.Matchers.containsString("Authenticate using POST")));
    }

    @Test
    void testIntegrationGuide_ContainsBestPractices() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/docs/integration-guide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bestPractices").isArray())
                .andExpect(jsonPath("$.data.bestPractices[0]").value(org.hamcrest.Matchers.containsString("HTTPS")))
                .andExpect(jsonPath("$.data.bestPractices[1]").value(org.hamcrest.Matchers.containsString("API credentials securely")));
    }

    @Test
    void testIntegrationGuide_ContainsErrorHandling() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/docs/integration-guide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.errorHandling.description").exists())
                .andExpect(jsonPath("$.data.errorHandling.statusCodes").exists())
                .andExpect(jsonPath("$.data.errorHandling.statusCodes.400").value(org.hamcrest.Matchers.containsString("Bad Request")))
                .andExpect(jsonPath("$.data.errorHandling.statusCodes.401").value(org.hamcrest.Matchers.containsString("Unauthorized")))
                .andExpect(jsonPath("$.data.errorHandling.errorResponseExample").exists());
    }

    @Test
    void testIntegrationGuide_ContainsWebhookGuide() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/docs/integration-guide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.webhooks.description").exists())
                .andExpect(jsonPath("$.data.webhooks.eventTypes").isArray())
                .andExpect(jsonPath("$.data.webhooks.eventTypes[0]").value("payment.completed"))
                .andExpect(jsonPath("$.data.webhooks.eventTypes[1]").value("payment.failed"))
                .andExpect(jsonPath("$.data.webhooks.examplePayload").exists());
    }

    @Test
    void testTestingScenarios_ContainsValidTestData() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/docs/testing-scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].testData.card_number").value("4111111111111111"))
                .andExpect(jsonPath("$.data[0].testData.expected_status").value("COMPLETED"))
                .andExpect(jsonPath("$.data[1].testData.card_number").value("4000000000000002"))
                .andExpect(jsonPath("$.data[1].testData.expected_status").value("FAILED"));
    }
}
