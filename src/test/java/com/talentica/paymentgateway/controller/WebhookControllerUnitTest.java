package com.talentica.paymentgateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.talentica.paymentgateway.dto.webhook.AuthorizeNetWebhookRequest;
import com.talentica.paymentgateway.dto.webhook.WebhookResponse;
import com.talentica.paymentgateway.service.MetricsService;
import com.talentica.paymentgateway.service.WebhookProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for WebhookController.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class WebhookControllerUnitTest {

    @Mock
    private WebhookProcessingService webhookProcessingService;

    @Mock
    private MetricsService metricsService;

    private WebhookController webhookController;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        webhookController = new WebhookController();
        // Use reflection to set private fields
        try {
            java.lang.reflect.Field webhookField = WebhookController.class.getDeclaredField("webhookProcessingService");
            webhookField.setAccessible(true);
            webhookField.set(webhookController, webhookProcessingService);
            
            java.lang.reflect.Field metricsField = WebhookController.class.getDeclaredField("metricsService");
            metricsField.setAccessible(true);
            metricsField.set(webhookController, metricsService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test", e);
        }
        
        mockMvc = MockMvcBuilders.standaloneSetup(webhookController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void handleAuthorizeNetWebhook_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        AuthorizeNetWebhookRequest request = createValidWebhookRequest();
        WebhookResponse response = WebhookResponse.success("evt_123", "corr_123", "Webhook processed successfully");
        
        when(webhookProcessingService.processWebhookAsync(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // When & Then
        mockMvc.perform(post("/webhooks/authorize-net")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.eventId").value("evt_123"))
                .andExpect(jsonPath("$.message").value("Webhook processed successfully"));

        verify(metricsService).incrementWebhookReceived("payment.authcapture.created");
        verify(metricsService).recordWebhookProcessingTime(eq("payment.authcapture.created"), anyLong());
        verify(webhookProcessingService).processWebhookAsync(any(), any(), any());
    }

    @Test
    void handleAuthorizeNetWebhook_WithDuplicateEvent_ShouldReturnConflict() throws Exception {
        // Given
        AuthorizeNetWebhookRequest request = createValidWebhookRequest();
        WebhookResponse response = WebhookResponse.duplicateEvent("evt_123", "corr_123");
        
        when(webhookProcessingService.processWebhookAsync(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // When & Then
        mockMvc.perform(post("/webhooks/authorize-net")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("duplicate"))
                .andExpect(jsonPath("$.eventId").value("evt_123"));

        verify(metricsService).incrementWebhookReceived("payment.authcapture.created");
        verify(webhookProcessingService).processWebhookAsync(any(), any(), any());
    }

    @Test
    void handleAuthorizeNetWebhook_WithSignatureError_ShouldReturnUnauthorized() throws Exception {
        // Given
        AuthorizeNetWebhookRequest request = createValidWebhookRequest();
        WebhookResponse response = WebhookResponse.signatureError("evt_123", "corr_123");
        
        when(webhookProcessingService.processWebhookAsync(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // When & Then
        mockMvc.perform(post("/webhooks/authorize-net")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("signature_error"))
                .andExpect(jsonPath("$.message").value("Invalid signature"));

        verify(webhookProcessingService).processWebhookAsync(any(), any(), any());
    }

    @Test
    void handleAuthorizeNetWebhook_WithValidationError_ShouldReturnBadRequest() throws Exception {
        // Given
        AuthorizeNetWebhookRequest request = createValidWebhookRequest();
        WebhookResponse.WebhookError error = new WebhookResponse.WebhookError("VALIDATION_FAILED", "Missing required field", "Ensure all required fields are present");
        WebhookResponse response = WebhookResponse.validationError("evt_123", "corr_123", "Missing required field", error);
        
        when(webhookProcessingService.processWebhookAsync(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // When & Then
        mockMvc.perform(post("/webhooks/authorize-net")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("validation_error"))
                .andExpect(jsonPath("$.message").value("Missing required field"));

        verify(webhookProcessingService).processWebhookAsync(any(), any(), any());
    }

    @Test
    void handleAuthorizeNetWebhook_WithProcessingError_ShouldReturnInternalError() throws Exception {
        // Given
        AuthorizeNetWebhookRequest request = createValidWebhookRequest();
        WebhookResponse response = WebhookResponse.processingError("evt_123", "corr_123", "Database error", new RuntimeException("DB connection failed"));
        
        when(webhookProcessingService.processWebhookAsync(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // When & Then
        mockMvc.perform(post("/webhooks/authorize-net")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("processing_error"))
                .andExpect(jsonPath("$.message").value("Database error"));

        verify(webhookProcessingService).processWebhookAsync(any(), any(), any());
    }

    @Test
    void handleAuthorizeNetWebhook_WithException_ShouldReturnInternalError() throws Exception {
        // Given
        AuthorizeNetWebhookRequest request = createValidWebhookRequest();
        
        when(webhookProcessingService.processWebhookAsync(any(), any(), any()))
            .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        mockMvc.perform(post("/webhooks/authorize-net")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("processing_error"))
                .andExpect(jsonPath("$.message").value("Internal server error"));

        verify(metricsService).incrementWebhookReceived("payment.authcapture.created");
        verify(metricsService).incrementWebhookProcessed("payment.authcapture.created", "error");
        verify(webhookProcessingService).processWebhookAsync(any(), any(), any());
    }

    @Test
    void handleAuthorizeNetWebhook_WithNullRequest_ShouldHandleGracefully() throws Exception {
        // Given - null request body will be handled by validation

        // When & Then
        mockMvc.perform(post("/webhooks/authorize-net")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());

        verify(webhookProcessingService, never()).processWebhookAsync(any(), any(), any());
    }

    @Test
    void handleAuthorizeNetWebhook_WithInvalidJson_ShouldReturnBadRequest() throws Exception {
        // Given - invalid JSON

        // When & Then
        mockMvc.perform(post("/webhooks/authorize-net")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
                .andExpect(status().isBadRequest());

        verify(webhookProcessingService, never()).processWebhookAsync(any(), any(), any());
    }

    @Test
    void handleAuthorizeNetWebhook_WithCustomHeaders_ShouldProcessHeaders() throws Exception {
        // Given
        AuthorizeNetWebhookRequest request = createValidWebhookRequest();
        WebhookResponse response = WebhookResponse.success("evt_123", "corr_123", "Webhook processed successfully");
        
        when(webhookProcessingService.processWebhookAsync(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // When & Then
        mockMvc.perform(post("/webhooks/authorize-net")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-ANET-Signature", "signature123")
                .header("X-Forwarded-For", "192.168.1.1")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(webhookProcessingService).processWebhookAsync(any(), argThat(headers -> 
            headers.containsKey("X-ANET-Signature") && headers.containsKey("X-Forwarded-For")), any());
    }

    @Test
    void getWebhookHealth_ShouldReturnHealthStatus() throws Exception {
        // Given
        Map<String, Object> stats = new HashMap<>();
        stats.put("webhooksLast24Hours", 150);
        stats.put("successfulDeliveries", 145);
        stats.put("failedDeliveries", 3);
        
        when(webhookProcessingService.getProcessingStatistics()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/webhooks/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.statistics.webhooksLast24Hours").value(150))
                .andExpect(jsonPath("$.statistics.successfulDeliveries").value(145))
                .andExpect(jsonPath("$.statistics.failedDeliveries").value(3))
                .andExpect(jsonPath("$.configuration.signatureVerificationEnabled").value(true))
                .andExpect(jsonPath("$.configuration.duplicateDetectionEnabled").value(true))
                .andExpect(jsonPath("$.configuration.asyncProcessingEnabled").value(true));

        verify(webhookProcessingService).getProcessingStatistics();
    }

    @Test
    void getWebhookHealth_WithDatabaseError_ShouldReturnFallbackStats() throws Exception {
        // Given
        when(webhookProcessingService.getProcessingStatistics())
            .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(get("/webhooks/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.statistics.webhooksLast24Hours").value(0))
                .andExpect(jsonPath("$.statistics.note").value("Statistics unavailable - database connection issue"))
                .andExpect(jsonPath("$.configuration.signatureVerificationEnabled").value(true));

        verify(webhookProcessingService).getProcessingStatistics();
    }

    @Test
    void testWebhook_WithValidPayload_ShouldReturnSuccess() throws Exception {
        // Given
        Map<String, Object> testPayload = new HashMap<>();
        testPayload.put("test", "data");

        // When & Then
        mockMvc.perform(post("/webhooks/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.eventId").value("test-event-id"))
                .andExpect(jsonPath("$.message").value("Test webhook processed successfully"));
    }

    @Test
    void testWebhook_WithoutPayload_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/webhooks/test")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.eventId").value("test-event-id"));
    }

    @Test
    void testWebhook_WithException_ShouldReturnError() throws Exception {
        // Given - We'll simulate an exception by using an invalid content type that causes parsing issues
        
        // When & Then
        mockMvc.perform(post("/webhooks/test")
                .contentType(MediaType.TEXT_PLAIN)
                .content("invalid content"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void handleAuthorizeNetWebhook_WithRefundEvent_ShouldProcessCorrectly() throws Exception {
        // Given
        AuthorizeNetWebhookRequest request = createRefundWebhookRequest();
        WebhookResponse response = WebhookResponse.success("evt_refund_123", "corr_123", "Refund webhook processed");
        
        when(webhookProcessingService.processWebhookAsync(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // When & Then
        mockMvc.perform(post("/webhooks/authorize-net")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt_refund_123"));

        verify(metricsService).incrementWebhookReceived("payment.refund.created");
        verify(webhookProcessingService).processWebhookAsync(any(), any(), any());
    }

    @Test
    void handleAuthorizeNetWebhook_WithVoidEvent_ShouldProcessCorrectly() throws Exception {
        // Given
        AuthorizeNetWebhookRequest request = createVoidWebhookRequest();
        WebhookResponse response = WebhookResponse.success("evt_void_123", "corr_123", "Void webhook processed");
        
        when(webhookProcessingService.processWebhookAsync(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(response));

        // When & Then
        mockMvc.perform(post("/webhooks/authorize-net")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt_void_123"));

        verify(metricsService).incrementWebhookReceived("payment.void.created");
        verify(webhookProcessingService).processWebhookAsync(any(), any(), any());
    }

    private AuthorizeNetWebhookRequest createValidWebhookRequest() {
        AuthorizeNetWebhookRequest request = new AuthorizeNetWebhookRequest();
        request.setNotificationId("evt_123");
        request.setEventType("payment.authcapture.created");
        request.setEventDate(java.time.ZonedDateTime.now());
        request.setWebhookId("webhook_123");
        
        // Create payload
        AuthorizeNetWebhookRequest.AuthorizeNetPayload payload = new AuthorizeNetWebhookRequest.AuthorizeNetPayload();
        payload.setId("TXN_123");
        payload.setAuthAmount(99.99);
        payload.setSettleAmount(99.99);
        payload.setResponseCode(1);
        payload.setAuthCode("ABC123");
        request.setPayload(payload);
        
        return request;
    }

    private AuthorizeNetWebhookRequest createRefundWebhookRequest() {
        AuthorizeNetWebhookRequest request = new AuthorizeNetWebhookRequest();
        request.setNotificationId("evt_refund_123");
        request.setEventType("payment.refund.created");
        request.setEventDate(java.time.ZonedDateTime.now());
        request.setWebhookId("webhook_refund_123");
        
        AuthorizeNetWebhookRequest.AuthorizeNetPayload payload = new AuthorizeNetWebhookRequest.AuthorizeNetPayload();
        payload.setId("TXN_REFUND_123");
        payload.setSettleAmount(50.00);
        payload.setResponseCode(1);
        request.setPayload(payload);
        
        return request;
    }

    private AuthorizeNetWebhookRequest createVoidWebhookRequest() {
        AuthorizeNetWebhookRequest request = new AuthorizeNetWebhookRequest();
        request.setNotificationId("evt_void_123");
        request.setEventType("payment.void.created");
        request.setEventDate(java.time.ZonedDateTime.now());
        request.setWebhookId("webhook_void_123");
        
        AuthorizeNetWebhookRequest.AuthorizeNetPayload payload = new AuthorizeNetWebhookRequest.AuthorizeNetPayload();
        payload.setId("TXN_VOID_123");
        payload.setResponseCode(1);
        request.setPayload(payload);
        
        return request;
    }
}
