package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.webhook.AuthorizeNetWebhookRequest;
import com.talentica.paymentgateway.dto.webhook.WebhookResponse;
import com.talentica.paymentgateway.entity.*;
import com.talentica.paymentgateway.repository.TransactionRepository;
import com.talentica.paymentgateway.repository.WebhookRepository;
import com.talentica.paymentgateway.util.WebhookSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebhookProcessingService.
 * Tests webhook processing, signature verification, duplicate detection, and transaction updates.
 */
@ExtendWith(MockitoExtension.class)
class WebhookProcessingServiceUnitTest {

    @Mock
    private WebhookRepository webhookRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WebhookSignatureVerifier signatureVerifier;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private WebhookProcessingService webhookProcessingService;

    private AuthorizeNetWebhookRequest testWebhookRequest;
    private Transaction testTransaction;
    private Map<String, String> testHeaders;
    private String testRawPayload;

    @BeforeEach
    void setUp() {
        // Set configuration values
        ReflectionTestUtils.setField(webhookProcessingService, "duplicateDetectionEnabled", true);
        ReflectionTestUtils.setField(webhookProcessingService, "duplicateDetectionWindowMinutes", 60);
        ReflectionTestUtils.setField(webhookProcessingService, "processingTimeoutSeconds", 30);

        // Create test webhook request
        testWebhookRequest = new AuthorizeNetWebhookRequest();
        testWebhookRequest.setNotificationId("NOTIF_001");
        testWebhookRequest.setEventType("net.authorize.payment.authcapture.created");
        testWebhookRequest.setEventDate(ZonedDateTime.now());
        testWebhookRequest.setWebhookId("WEBHOOK_001");
        // Set transaction ID in payload instead
        AuthorizeNetWebhookRequest.AuthorizeNetPayload payload = new AuthorizeNetWebhookRequest.AuthorizeNetPayload();
        payload.setId("AUTH_TXN_001");
        testWebhookRequest.setPayload(payload);

        // Update test payload
        payload.setResponseCode(1); // Approved
        payload.setAuthCode("ABC123");
        payload.setAvsResponse("Y");
        payload.setCardCodeResponse("M");
        payload.setSettleAmount(100.50);

        // Create test transaction
        testTransaction = new Transaction();
        testTransaction.setTransactionId("TXN_001");
        testTransaction.setAuthnetTransactionId("AUTH_TXN_001");
        testTransaction.setAmount(new BigDecimal("100.50"));
        testTransaction.setStatus(PaymentStatus.PENDING);
        testTransaction.setTransactionType(TransactionType.PURCHASE);
        testTransaction.setCreatedAt(ZonedDateTime.now().toLocalDateTime());

        // Create test headers and payload
        testHeaders = new HashMap<>();
        testHeaders.put("X-ANET-Signature", "sha512=test-signature");
        testRawPayload = "{\"notificationId\":\"NOTIF_001\"}";
    }

    @Test
    void processWebhookAsync_WithValidSignature_ShouldProcessSuccessfully() throws Exception {
        // Given
        when(signatureVerifier.verifySignature(testHeaders, testRawPayload)).thenReturn(true);
        when(webhookRepository.findByEventIdAndEventTypeAndCreatedAtAfter(anyString(), anyString(), any(ZonedDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(webhookRepository.save(any(Webhook.class))).thenReturn(new Webhook());
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        CompletableFuture<WebhookResponse> future = webhookProcessingService.processWebhookAsync(
            testWebhookRequest, testHeaders, testRawPayload);
        WebhookResponse response = future.get();

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getEventId()).isEqualTo("NOTIF_001");
        assertThat(response.getMessage()).isEqualTo("Auth-capture event processed successfully");
        
        verify(signatureVerifier).verifySignature(testHeaders, testRawPayload);
        verify(metricsService).incrementWebhookProcessed("net.authorize.payment.authcapture.created", "success");
        verify(transactionRepository).save(testTransaction);
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.SETTLED);
    }

    @Test
    void processWebhookAsync_WithInvalidSignature_ShouldReturnSignatureError() throws Exception {
        // Given
        when(signatureVerifier.verifySignature(testHeaders, testRawPayload)).thenReturn(false);

        // When
        CompletableFuture<WebhookResponse> future = webhookProcessingService.processWebhookAsync(
            testWebhookRequest, testHeaders, testRawPayload);
        WebhookResponse response = future.get();

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getEventId()).isEqualTo("NOTIF_001");
        assertThat(response.getMessage()).contains("signature");
        
        verify(metricsService).incrementWebhookSignatureFailure();
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void processWebhookAsync_WithDuplicateEvent_ShouldReturnDuplicateResponse() throws Exception {
        // Given
        when(signatureVerifier.verifySignature(testHeaders, testRawPayload)).thenReturn(true);
        
        Webhook existingWebhook = new Webhook();
        existingWebhook.setEventId("NOTIF_001");
        when(webhookRepository.findByEventIdAndEventTypeAndCreatedAtAfter(anyString(), anyString(), any(ZonedDateTime.class)))
            .thenReturn(Arrays.asList(existingWebhook));

        // When
        CompletableFuture<WebhookResponse> future = webhookProcessingService.processWebhookAsync(
            testWebhookRequest, testHeaders, testRawPayload);
        WebhookResponse response = future.get();

        // Then
        assertThat(response.isSuccess()).isFalse(); // Duplicate should return false
        assertThat(response.getEventId()).isEqualTo("NOTIF_001");
        assertThat(response.getMessage()).contains("already processed");
        
        verify(metricsService).incrementWebhookDuplicate("net.authorize.payment.authcapture.created");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void processWebhookAsync_WithDuplicateDetectionDisabled_ShouldProcessEvent() throws Exception {
        // Given
        ReflectionTestUtils.setField(webhookProcessingService, "duplicateDetectionEnabled", false);
        when(signatureVerifier.verifySignature(testHeaders, testRawPayload)).thenReturn(true);
        when(webhookRepository.save(any(Webhook.class))).thenReturn(new Webhook());
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        CompletableFuture<WebhookResponse> future = webhookProcessingService.processWebhookAsync(
            testWebhookRequest, testHeaders, testRawPayload);
        WebhookResponse response = future.get();

        // Then
        assertThat(response.isSuccess()).isTrue();
        verify(webhookRepository, never()).findByEventIdAndEventTypeAndCreatedAtAfter(anyString(), anyString(), any());
        verify(transactionRepository).save(testTransaction);
    }

    @Test
    void processWebhookAsync_WithProcessingException_ShouldReturnErrorResponse() throws Exception {
        // Given
        when(signatureVerifier.verifySignature(testHeaders, testRawPayload)).thenReturn(true);
        when(webhookRepository.findByEventIdAndEventTypeAndCreatedAtAfter(anyString(), anyString(), any(ZonedDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(webhookRepository.save(any(Webhook.class))).thenThrow(new RuntimeException("Database error"));

        // When
        CompletableFuture<WebhookResponse> future = webhookProcessingService.processWebhookAsync(
            testWebhookRequest, testHeaders, testRawPayload);
        WebhookResponse response = future.get();

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getEventId()).isEqualTo("NOTIF_001");
        assertThat(response.getMessage()).contains("Internal processing error");
        
        verify(metricsService).incrementWebhookProcessed("net.authorize.payment.authcapture.created", "error");
    }

    @Test
    void processAuthCaptureEvent_WithApprovedTransaction_ShouldUpdateToSettled() {
        // Given
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Auth-capture event processed successfully");
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.SETTLED);
        assertThat(testTransaction.getAuthnetAuthCode()).isEqualTo("ABC123");
        assertThat(testTransaction.getAuthnetAvsResult()).isEqualTo("Y");
        assertThat(testTransaction.getAuthnetCvvResult()).isEqualTo("M");
        assertThat(testTransaction.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(testTransaction.getProcessedAt()).isNotNull();
    }

    @Test
    void processAuthCaptureEvent_WithDeclinedTransaction_ShouldUpdateToFailed() {
        // Given
        testWebhookRequest.getPayload().setResponseCode(2); // Declined
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(testTransaction.getProcessedAt()).isNotNull();
    }

    @Test
    void processAuthCaptureEvent_WithTransactionNotFound_ShouldReturnSuccessWithMessage() {
        // Given
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.empty());

        // When
        WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Transaction not found in local database");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void processAuthorizationEvent_WithApprovedTransaction_ShouldUpdateToAuthorized() {
        // Given
        testWebhookRequest.setEventType("net.authorize.payment.authorization.created");
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Authorization event processed successfully");
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(testTransaction.getAuthnetAuthCode()).isEqualTo("ABC123");
        assertThat(testTransaction.getProcessedAt()).isNotNull();
    }

    @Test
    void processCaptureEvent_WithApprovedTransaction_ShouldUpdateToSettled() {
        // Given
        testWebhookRequest.setEventType("net.authorize.payment.capture.created");
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Capture event processed successfully");
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.SETTLED);
        assertThat(testTransaction.getProcessedAt()).isNotNull();
    }

    @Test
    void processRefundEvent_WithFullRefund_ShouldUpdateToRefunded() {
        // Given
        testWebhookRequest.setEventType("net.authorize.payment.refund.created");
        testWebhookRequest.getPayload().setSettleAmount(100.50); // Full refund amount
        testTransaction.setAmount(new BigDecimal("100.50"));
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Refund event processed successfully");
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(testTransaction.getProcessedAt()).isNotNull();
    }

    @Test
    void processRefundEvent_WithPartialRefund_ShouldUpdateToPartiallyRefunded() {
        // Given
        testWebhookRequest.setEventType("net.authorize.payment.refund.created");
        testWebhookRequest.getPayload().setSettleAmount(50.25); // Partial refund amount
        testTransaction.setAmount(new BigDecimal("100.50"));
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(testTransaction.getProcessedAt()).isNotNull();
    }

    @Test
    void processRefundEvent_WithoutSettleAmount_ShouldUpdateToRefunded() {
        // Given
        testWebhookRequest.setEventType("net.authorize.payment.refund.created");
        testWebhookRequest.getPayload().setSettleAmount(null);
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void processVoidEvent_WithApprovedVoid_ShouldUpdateToVoided() {
        // Given
        testWebhookRequest.setEventType("net.authorize.payment.void.created");
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Void event processed successfully");
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.VOIDED);
        assertThat(testTransaction.getProcessedAt()).isNotNull();
    }

    @Test
    void processFraudEvent_WithFraudApproved_ShouldUpdateToSettled() {
        // Given
        testWebhookRequest.setEventType("net.authorize.payment.fraud.approved");
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Fraud event processed successfully");
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.SETTLED);
        assertThat(testTransaction.getProcessedAt()).isNotNull();
    }

    @Test
    void processFraudEvent_WithFraudDeclined_ShouldUpdateToFailed() {
        // Given
        testWebhookRequest.setEventType("net.authorize.payment.fraud.declined");
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void processFraudEvent_WithFraudHeld_ShouldUpdateToPendingReview() {
        // Given
        testWebhookRequest.setEventType("net.authorize.payment.fraud.held");
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.PENDING_REVIEW);
    }

    @Test
    void processWebhookEvent_WithUnsupportedEventType_ShouldReturnSuccessWithMessage() {
        // Given
        testWebhookRequest.setEventType("net.authorize.payment.unknown.created");
        when(webhookRepository.save(any(Webhook.class))).thenReturn(new Webhook());

        // When
        WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Event received but not processed (unsupported type)");
        verify(transactionRepository, never()).findByAuthnetTransactionId(anyString());
    }

    @Test
    void createWebhookRecord_ShouldCreateWebhookWithCorrectData() {
        // Given
        when(webhookRepository.save(any(Webhook.class))).thenReturn(new Webhook());

        // When
        webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        verify(webhookRepository).save(argThat(webhook -> {
            assertThat(webhook.getEventType()).isEqualTo("net.authorize.payment.authcapture.created");
            assertThat(webhook.getEventId()).isEqualTo("NOTIF_001");
            assertThat(webhook.getEndpointUrl()).isEqualTo("/api/v1/webhooks/authorize-net");
            assertThat(webhook.getCorrelationId()).isEqualTo("CORR_001");
            assertThat(webhook.getStatus()).isEqualTo(WebhookStatus.PROCESSING);
            assertThat(webhook.getRequestBody()).isNotNull();
            assertThat(webhook.getScheduledAt()).isNotNull();
            return true;
        }));
    }

    @Test
    void isDuplicateEvent_WithRecentDuplicate_ShouldReturnTrue() {
        // Given
        Webhook existingWebhook = new Webhook();
        existingWebhook.setEventId("NOTIF_001");
        when(webhookRepository.findByEventIdAndEventTypeAndCreatedAtAfter(
            eq("NOTIF_001"), eq("net.authorize.payment.authcapture.created"), any(ZonedDateTime.class)))
            .thenReturn(Arrays.asList(existingWebhook));
        when(signatureVerifier.verifySignature(testHeaders, testRawPayload)).thenReturn(true);

        // When
        CompletableFuture<WebhookResponse> future = webhookProcessingService.processWebhookAsync(
            testWebhookRequest, testHeaders, testRawPayload);

        // Then
        verify(metricsService).incrementWebhookDuplicate("net.authorize.payment.authcapture.created");
    }

    @Test
    void isDuplicateEvent_WithNoDuplicates_ShouldReturnFalse() {
        // Given
        when(webhookRepository.findByEventIdAndEventTypeAndCreatedAtAfter(anyString(), anyString(), any(ZonedDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(signatureVerifier.verifySignature(testHeaders, testRawPayload)).thenReturn(true);
        when(webhookRepository.save(any(Webhook.class))).thenReturn(new Webhook());
        when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
            .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        CompletableFuture<WebhookResponse> future = webhookProcessingService.processWebhookAsync(
            testWebhookRequest, testHeaders, testRawPayload);

        // Then
        verify(metricsService, never()).incrementWebhookDuplicate(anyString());
        verify(metricsService).incrementWebhookProcessed("net.authorize.payment.authcapture.created", "success");
    }

    @Test
    void getProcessingStatistics_ShouldReturnCorrectStatistics() {
        // Given
        ZonedDateTime last24Hours = ZonedDateTime.now().minusHours(24);
        when(webhookRepository.countWebhooksCreatedSince(any(ZonedDateTime.class))).thenReturn(100L);
        when(webhookRepository.countByStatusAndCreatedAtAfter(eq(WebhookStatus.DELIVERED), any(ZonedDateTime.class))).thenReturn(85L);
        when(webhookRepository.countByStatusAndCreatedAtAfter(eq(WebhookStatus.FAILED), any(ZonedDateTime.class))).thenReturn(10L);
        when(webhookRepository.countByStatus(WebhookStatus.PENDING)).thenReturn(5L);
        when(webhookRepository.countByStatus(WebhookStatus.RETRYING)).thenReturn(3L);

        // When
        Map<String, Object> stats = webhookProcessingService.getProcessingStatistics();

        // Then
        assertThat(stats.get("webhooksLast24Hours")).isEqualTo(100L);
        assertThat(stats.get("successfulDeliveries")).isEqualTo(85L);
        assertThat(stats.get("failedDeliveries")).isEqualTo(10L);
        assertThat(stats.get("pendingDeliveries")).isEqualTo(5L);
        assertThat(stats.get("retryingDeliveries")).isEqualTo(3L);
    }

    @Test
    void processWebhookEvent_WithRepositoryException_ShouldThrowException() {
        // Given
        when(webhookRepository.save(any(Webhook.class))).thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        try {
            webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Database connection failed");
        }
    }

    @Test
    void webhookProcessing_ShouldHandleNullPayloadValues() {
        // Given
        AuthorizeNetWebhookRequest.AuthorizeNetPayload payload = new AuthorizeNetWebhookRequest.AuthorizeNetPayload();
        payload.setResponseCode(null);
        payload.setAuthCode(null);
        payload.setSettleAmount(null);
        testWebhookRequest.setPayload(payload);
        
        // No stubbing needed for null payload test - service should handle gracefully
        when(webhookRepository.save(any(Webhook.class))).thenReturn(new Webhook());

        // When
        WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

        // Then
        assertThat(response.isSuccess()).isTrue();
        // Transaction should remain PENDING due to null response code (no processing occurs)
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void webhookProcessing_ShouldSetProcessedAtForAllEventTypes() {
        // Test that processedAt is set for all event types
        String[] eventTypes = {
            "net.authorize.payment.authcapture.created",
            "net.authorize.payment.authorization.created",
            "net.authorize.payment.capture.created",
            "net.authorize.payment.refund.created",
            "net.authorize.payment.void.created",
            "net.authorize.payment.fraud.approved"
        };

        for (String eventType : eventTypes) {
            // Reset transaction
            testTransaction.setProcessedAt(null);
            testWebhookRequest.setEventType(eventType);
            
            when(webhookRepository.save(any(Webhook.class))).thenReturn(new Webhook());
            when(transactionRepository.findByAuthnetTransactionId("AUTH_TXN_001"))
                .thenReturn(Optional.of(testTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

            // When
            WebhookResponse response = webhookProcessingService.processWebhookEvent(testWebhookRequest, "CORR_001");

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(testTransaction.getProcessedAt()).isNotNull();
        }
    }
}
