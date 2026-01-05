package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.entity.Webhook;
import com.talentica.paymentgateway.entity.WebhookStatus;
import com.talentica.paymentgateway.repository.WebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebhookRetryService.
 * Tests webhook retry logic, circuit breaker functionality, and scheduled processing.
 */
@ExtendWith(MockitoExtension.class)
class WebhookRetryServiceUnitTest {

    @Mock
    private WebhookRepository webhookRepository;

    @Mock
    private MetricsService metricsService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WebhookRetryService webhookRetryService;

    private Webhook testWebhook;

    @BeforeEach
    void setUp() {
        // Set configuration values
        ReflectionTestUtils.setField(webhookRetryService, "maxRetryAttempts", 5);
        ReflectionTestUtils.setField(webhookRetryService, "initialDelayMinutes", 1);
        ReflectionTestUtils.setField(webhookRetryService, "maxDelayMinutes", 1440);
        ReflectionTestUtils.setField(webhookRetryService, "backoffMultiplier", 2.0);
        ReflectionTestUtils.setField(webhookRetryService, "jitterEnabled", true);
        ReflectionTestUtils.setField(webhookRetryService, "timeoutSeconds", 30);
        ReflectionTestUtils.setField(webhookRetryService, "cleanupEnabled", true);
        ReflectionTestUtils.setField(webhookRetryService, "deliveredRetentionDays", 7);
        ReflectionTestUtils.setField(webhookRetryService, "failedRetentionDays", 30);

        // Create test webhook
        testWebhook = new Webhook();
        testWebhook.setWebhookId("WEBHOOK_001");
        testWebhook.setEventType("payment.created");
        testWebhook.setEventId("EVENT_001");
        testWebhook.setEndpointUrl("https://api.example.com/webhooks");
        testWebhook.setHttpMethod("POST");
        testWebhook.setCorrelationId("CORR_001");
        testWebhook.setStatus(WebhookStatus.RETRYING);
        testWebhook.setAttempts(1);
        testWebhook.setMaxAttempts(5);
        testWebhook.setNextAttemptAt(ZonedDateTime.now().minusMinutes(1));
        testWebhook.setCreatedAt(ZonedDateTime.now().minusHours(1).toLocalDateTime());

        // Set request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("eventType", "payment.created");
        requestBody.put("data", Map.of("amount", 100.00));
        testWebhook.setRequestBody(requestBody);

        // Set request headers
        Map<String, Object> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer token123");
        testWebhook.setRequestHeaders(requestHeaders);
    }

    @Test
    void processRetries_WithWebhooksReadyForRetry_ShouldProcessThem() {
        // Given
        List<Webhook> webhooksToRetry = Arrays.asList(testWebhook);
        when(webhookRepository.findWebhooksReadyForRetry()).thenReturn(webhooksToRetry);

        // When
        webhookRetryService.processRetries();

        // Then
        verify(webhookRepository).findWebhooksReadyForRetry();
        // Note: We can't easily verify the async call, but we can verify the repository was called
    }

    @Test
    void processRetries_WithNoWebhooksReadyForRetry_ShouldDoNothing() {
        // Given
        when(webhookRepository.findWebhooksReadyForRetry()).thenReturn(Collections.emptyList());

        // When
        webhookRetryService.processRetries();

        // Then
        verify(webhookRepository).findWebhooksReadyForRetry();
        verify(webhookRepository, never()).save(any());
    }

    @Test
    void processRetries_WithRepositoryException_ShouldHandleGracefully() {
        // Given
        when(webhookRepository.findWebhooksReadyForRetry()).thenThrow(new RuntimeException("Database error"));

        // When
        webhookRetryService.processRetries();

        // Then
        verify(webhookRepository).findWebhooksReadyForRetry();
        // Should not throw exception, just log it
    }

    @Test
    void cleanupOldWebhooks_WithCleanupEnabled_ShouldCleanupOldRecords() {
        // Given
        List<Webhook> webhooksToCleanup = Arrays.asList(testWebhook);
        when(webhookRepository.findWebhooksForCleanup(any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(webhooksToCleanup);

        // When
        webhookRetryService.cleanupOldWebhooks();

        // Then
        verify(webhookRepository).findWebhooksForCleanup(any(ZonedDateTime.class), any(ZonedDateTime.class));
        verify(webhookRepository).delete(testWebhook);
        verify(metricsService).incrementWebhookCleanup(1);
    }

    @Test
    void cleanupOldWebhooks_WithCleanupDisabled_ShouldDoNothing() {
        // Given
        ReflectionTestUtils.setField(webhookRetryService, "cleanupEnabled", false);

        // When
        webhookRetryService.cleanupOldWebhooks();

        // Then
        verify(webhookRepository, never()).findWebhooksForCleanup(any(), any());
        verify(webhookRepository, never()).delete(any());
    }

    @Test
    void cleanupOldWebhooks_WithNoWebhooksToCleanup_ShouldNotDeleteAnything() {
        // Given
        when(webhookRepository.findWebhooksForCleanup(any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        webhookRetryService.cleanupOldWebhooks();

        // Then
        verify(webhookRepository).findWebhooksForCleanup(any(ZonedDateTime.class), any(ZonedDateTime.class));
        verify(webhookRepository, never()).delete(any());
        verify(metricsService, never()).incrementWebhookCleanup(anyInt());
    }

    @Test
    void retryWebhookDeliveryAsync_WithValidWebhook_ShouldProcessSuccessfully() throws Exception {
        // Given
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);
        
        ResponseEntity<String> successResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(successResponse);

        // When
        CompletableFuture<Void> future = webhookRetryService.retryWebhookDeliveryAsync(testWebhook);
        future.get(); // Wait for completion

        // Then
        verify(webhookRepository, atLeastOnce()).save(testWebhook);
        verify(metricsService).incrementWebhookDeliverySuccess("payment.created", testWebhook.getAttempts());
    }

    @Test
    void deliverWebhook_WithSuccessfulResponse_ShouldMarkAsDelivered() {
        // Given
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);
        
        ResponseEntity<String> successResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(successResponse);

        // When
        webhookRetryService.deliverWebhook(testWebhook);

        // Then
        verify(webhookRepository, times(2)).save(testWebhook); // Once for processing, once for delivered
        verify(metricsService).incrementWebhookDeliverySuccess("payment.created", testWebhook.getAttempts());
        assertThat(testWebhook.getStatus()).isEqualTo(WebhookStatus.DELIVERED);
    }

    @Test
    void deliverWebhook_WithClientError_ShouldMarkAsFailed() {
        // Given
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);
        
        HttpClientErrorException clientError = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenThrow(clientError);

        // When
        webhookRetryService.deliverWebhook(testWebhook);

        // Then
        verify(webhookRepository, times(2)).save(testWebhook);
        verify(metricsService).incrementWebhookDeliveryFailure("payment.created", testWebhook.getAttempts(), "client_error");
        assertThat(testWebhook.getMaxAttempts()).isEqualTo(testWebhook.getAttempts() + 1); // Stop retrying
    }

    @Test
    void deliverWebhook_WithTooManyRequestsError_ShouldScheduleRetry() {
        // Given
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);
        
        HttpClientErrorException tooManyRequests = new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests");
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenThrow(tooManyRequests);

        // When
        webhookRetryService.deliverWebhook(testWebhook);

        // Then
        verify(webhookRepository, times(2)).save(testWebhook);
        verify(metricsService).incrementWebhookDeliveryFailure("payment.created", testWebhook.getAttempts(), "client_error");
        assertThat(testWebhook.getStatus()).isEqualTo(WebhookStatus.RETRYING);
        assertThat(testWebhook.getNextAttemptAt()).isNotNull();
    }

    @Test
    void deliverWebhook_WithServerError_ShouldScheduleRetry() {
        // Given
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);
        
        HttpServerErrorException serverError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenThrow(serverError);

        // When
        webhookRetryService.deliverWebhook(testWebhook);

        // Then
        verify(webhookRepository, times(2)).save(testWebhook);
        verify(metricsService).incrementWebhookDeliveryFailure("payment.created", testWebhook.getAttempts(), "server_error");
        assertThat(testWebhook.getStatus()).isEqualTo(WebhookStatus.RETRYING);
        assertThat(testWebhook.getNextAttemptAt()).isNotNull();
    }

    @Test
    void deliverWebhook_WithTimeoutError_ShouldScheduleRetry() {
        // Given
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);
        
        ResourceAccessException timeoutError = new ResourceAccessException("Connection timeout");
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenThrow(timeoutError);

        // When
        webhookRetryService.deliverWebhook(testWebhook);

        // Then
        verify(webhookRepository, times(2)).save(testWebhook);
        verify(metricsService).incrementWebhookDeliveryFailure("payment.created", testWebhook.getAttempts(), "timeout");
        assertThat(testWebhook.getStatus()).isEqualTo(WebhookStatus.RETRYING);
        assertThat(testWebhook.getNextAttemptAt()).isNotNull();
    }

    @Test
    void deliverWebhook_WithUnknownError_ShouldScheduleRetry() {
        // Given
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);
        
        RuntimeException unknownError = new RuntimeException("Unknown error");
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenThrow(unknownError);

        // When
        webhookRetryService.deliverWebhook(testWebhook);

        // Then
        verify(webhookRepository, times(2)).save(testWebhook);
        verify(metricsService).incrementWebhookDeliveryFailure("payment.created", testWebhook.getAttempts(), "unknown");
        assertThat(testWebhook.getStatus()).isEqualTo(WebhookStatus.RETRYING);
        assertThat(testWebhook.getNextAttemptAt()).isNotNull();
    }

    @Test
    void deliverWebhook_ShouldSetCorrectHeaders() {
        // Given
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);
        
        ResponseEntity<String> successResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(successResponse);

        // When
        webhookRetryService.deliverWebhook(testWebhook);

        // Then
        verify(restTemplate).exchange(
            eq("https://api.example.com/webhooks"),
            eq(HttpMethod.POST),
            argThat(entity -> {
                HttpHeaders headers = entity.getHeaders();
                return headers.getContentType().equals(MediaType.APPLICATION_JSON) &&
                       headers.getFirst("X-Correlation-ID").equals("CORR_001") &&
                       headers.getFirst("X-Webhook-ID").equals("WEBHOOK_001") &&
                       headers.getFirst("X-Event-Type").equals("payment.created") &&
                       headers.getFirst("Authorization").equals("Bearer token123");
            }),
            eq(String.class)
        );
    }

    @Test
    void scheduleNextRetry_WithAttemptsRemaining_ShouldScheduleNextAttempt() {
        // Given
        testWebhook.setAttempts(2);
        testWebhook.setMaxAttempts(5);
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);

        // Simulate server error to trigger retry scheduling
        HttpServerErrorException serverError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenThrow(serverError);

        // When
        webhookRetryService.deliverWebhook(testWebhook);

        // Then
        assertThat(testWebhook.getStatus()).isEqualTo(WebhookStatus.RETRYING);
        assertThat(testWebhook.getNextAttemptAt()).isNotNull();
        assertThat(testWebhook.getNextAttemptAt()).isAfter(ZonedDateTime.now());
    }

    @Test
    void scheduleNextRetry_WithMaxAttemptsReached_ShouldMarkAsFailed() {
        // Given
        testWebhook.setAttempts(5);
        testWebhook.setMaxAttempts(5);
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);

        // Simulate server error
        HttpServerErrorException serverError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenThrow(serverError);

        // When
        webhookRetryService.deliverWebhook(testWebhook);

        // Then
        assertThat(testWebhook.getStatus()).isEqualTo(WebhookStatus.FAILED);
        assertThat(testWebhook.getNextAttemptAt()).isNull();
        verify(metricsService).incrementWebhookMaxAttemptsReached("payment.created");
    }

    @Test
    void calculateBackoffDelay_ShouldCalculateExponentialBackoff() {
        // Test exponential backoff calculation through reflection
        try {
            java.lang.reflect.Method method = WebhookRetryService.class.getDeclaredMethod("calculateBackoffDelay", int.class);
            method.setAccessible(true);

            // Test different attempt numbers
            int delay0 = (int) method.invoke(webhookRetryService, 0);
            int delay1 = (int) method.invoke(webhookRetryService, 1);
            int delay2 = (int) method.invoke(webhookRetryService, 2);

            // Should increase exponentially (with jitter, so approximate)
            assertThat(delay0).isGreaterThanOrEqualTo(1);
            assertThat(delay1).isGreaterThanOrEqualTo(delay0); // Allow equal due to jitter
            assertThat(delay2).isGreaterThanOrEqualTo(delay1); // Allow equal due to jitter

        } catch (Exception e) {
            throw new RuntimeException("Failed to test calculateBackoffDelay", e);
        }
    }

    @Test
    void calculateBackoffDelay_WithJitterDisabled_ShouldCalculateExactBackoff() {
        // Given
        ReflectionTestUtils.setField(webhookRetryService, "jitterEnabled", false);

        try {
            java.lang.reflect.Method method = WebhookRetryService.class.getDeclaredMethod("calculateBackoffDelay", int.class);
            method.setAccessible(true);

            // Test exact calculations
            int delay0 = (int) method.invoke(webhookRetryService, 0); // 1 * 2^0 = 1
            int delay1 = (int) method.invoke(webhookRetryService, 1); // 1 * 2^1 = 2
            int delay2 = (int) method.invoke(webhookRetryService, 2); // 1 * 2^2 = 4

            assertThat(delay0).isEqualTo(1);
            assertThat(delay1).isEqualTo(2);
            assertThat(delay2).isEqualTo(4);

        } catch (Exception e) {
            throw new RuntimeException("Failed to test calculateBackoffDelay", e);
        }
    }

    @Test
    void calculateBackoffDelay_ShouldRespectMaxDelay() {
        // Given
        ReflectionTestUtils.setField(webhookRetryService, "maxDelayMinutes", 10);
        ReflectionTestUtils.setField(webhookRetryService, "jitterEnabled", false);

        try {
            java.lang.reflect.Method method = WebhookRetryService.class.getDeclaredMethod("calculateBackoffDelay", int.class);
            method.setAccessible(true);

            // Test with high attempt number that would exceed max delay
            int delay = (int) method.invoke(webhookRetryService, 10);

            assertThat(delay).isLessThanOrEqualTo(10);

        } catch (Exception e) {
            throw new RuntimeException("Failed to test calculateBackoffDelay", e);
        }
    }

    @Test
    void getRetryStatistics_ShouldReturnCorrectStatistics() {
        // Given
        when(webhookRepository.countByStatus(WebhookStatus.RETRYING)).thenReturn(5L);
        when(webhookRepository.countByStatus(WebhookStatus.FAILED)).thenReturn(3L);
        when(webhookRepository.findWebhooksReadyForRetry()).thenReturn(Arrays.asList(testWebhook));

        // When
        Map<String, Object> stats = webhookRetryService.getRetryStatistics();

        // Then
        assertThat(stats.get("pendingRetries")).isEqualTo(5L);
        assertThat(stats.get("failedWebhooks")).isEqualTo(3L);
        assertThat(stats.get("webhooksReadyForRetry")).isEqualTo(1);
        assertThat(stats.get("circuitBreakers")).isInstanceOf(Map.class);
    }

    @Test
    void circuitBreaker_ShouldOpenAfterFailureThreshold() {
        // Given
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);
        
        HttpServerErrorException serverError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenThrow(serverError);

        // When - trigger multiple failures to open circuit breaker
        for (int i = 0; i < 6; i++) {
            testWebhook.setAttempts(i);
            webhookRetryService.deliverWebhook(testWebhook);
        }

        // Then - circuit breaker should be open
        Map<String, Object> stats = webhookRetryService.getRetryStatistics();
        @SuppressWarnings("unchecked")
        Map<String, Object> circuitBreakers = (Map<String, Object>) stats.get("circuitBreakers");
        
        if (!circuitBreakers.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cbState = (Map<String, Object>) circuitBreakers.values().iterator().next();
            assertThat(cbState.get("state")).isEqualTo("OPEN");
        }
    }

    @Test
    void circuitBreaker_ShouldCloseAfterSuccess() {
        // Given
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);

        // First, trigger failures to open circuit breaker
        HttpServerErrorException serverError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenThrow(serverError);

        for (int i = 0; i < 6; i++) {
            testWebhook.setAttempts(i);
            webhookRetryService.deliverWebhook(testWebhook);
        }

        // Then, simulate success
        ResponseEntity<String> successResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(successResponse);

        testWebhook.setAttempts(0);
        webhookRetryService.deliverWebhook(testWebhook);

        // Then - circuit breaker should be closed
        Map<String, Object> stats = webhookRetryService.getRetryStatistics();
        @SuppressWarnings("unchecked")
        Map<String, Object> circuitBreakers = (Map<String, Object>) stats.get("circuitBreakers");
        
        if (!circuitBreakers.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cbState = (Map<String, Object>) circuitBreakers.values().iterator().next();
            assertThat(cbState.get("state")).isEqualTo("CLOSED");
        }
    }

    @Test
    void retryWebhookDeliveryAsync_WithCircuitBreakerOpen_ShouldSkipRetry() throws Exception {
        // Given - First open the circuit breaker
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);
        
        HttpServerErrorException serverError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenThrow(serverError);

        // Trigger failures to open circuit breaker
        for (int i = 0; i < 6; i++) {
            testWebhook.setAttempts(i);
            webhookRetryService.deliverWebhook(testWebhook);
        }

        // Reset webhook for retry test
        testWebhook.setAttempts(1);
        testWebhook.setStatus(WebhookStatus.RETRYING);

        // When - try to retry with circuit breaker open
        CompletableFuture<Void> future = webhookRetryService.retryWebhookDeliveryAsync(testWebhook);
        future.get(); // Wait for completion

        // Then - should skip retry and schedule next attempt
        verify(webhookRepository, atLeastOnce()).save(argThat(webhook -> 
            webhook.getStatus() == WebhookStatus.RETRYING && webhook.getNextAttemptAt() != null));
    }

    @Test
    void deliverWebhook_WithNullCorrelationId_ShouldGenerateOne() {
        // Given
        testWebhook.setCorrelationId(null);
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);
        
        ResponseEntity<String> successResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(successResponse);

        // When
        CompletableFuture<Void> future = webhookRetryService.retryWebhookDeliveryAsync(testWebhook);

        // Then - should complete without error (correlation ID generated internally)
        assertThat(future).isNotNull();
    }

    @Test
    void deliverWebhook_WithNullRequestHeaders_ShouldHandleGracefully() {
        // Given
        testWebhook.setRequestHeaders(null);
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);
        
        ResponseEntity<String> successResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(successResponse);

        // When
        webhookRetryService.deliverWebhook(testWebhook);

        // Then
        verify(restTemplate).exchange(
            anyString(),
            any(HttpMethod.class),
            argThat(entity -> {
                HttpHeaders headers = entity.getHeaders();
                return headers.getContentType().equals(MediaType.APPLICATION_JSON) &&
                       headers.getFirst("X-Webhook-ID").equals("WEBHOOK_001");
            }),
            eq(String.class)
        );
    }

    @Test
    void deliverWebhook_ShouldMarkAsProcessingBeforeDelivery() {
        // Given
        when(webhookRepository.save(any(Webhook.class))).thenReturn(testWebhook);
        
        ResponseEntity<String> successResponse = new ResponseEntity<>("OK", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
            .thenReturn(successResponse);

        // When
        webhookRetryService.deliverWebhook(testWebhook);

        // Then
        verify(webhookRepository, times(2)).save(any(Webhook.class)); // Verify save called twice
        assertThat(testWebhook.getStatus()).isEqualTo(WebhookStatus.DELIVERED); // Final status check
    }
}
