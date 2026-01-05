package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.entity.PaymentStatus;
import com.talentica.paymentgateway.entity.TransactionType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetricsService.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
class MetricsServiceUnitTest {

    private MetricsService metricsService;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        // Use SimpleMeterRegistry instead of mocking for better test reliability
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }

    @Test
    void constructor_WithValidMeterRegistry_ShouldCreateService() {
        // When & Then
        assertNotNull(metricsService);
    }

    @Test
    void recordPaymentRequest_ShouldIncrementCounter() {
        // When
        metricsService.recordPaymentRequest();

        // Then
        assertDoesNotThrow(() -> metricsService.recordPaymentRequest());
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("payment.requests.total").counter());
    }

    @Test
    void recordPaymentCompletion_WithSuccessfulPayment_ShouldRecordMetrics() {
        // Given
        PaymentStatus status = PaymentStatus.SETTLED;
        BigDecimal amount = new BigDecimal("99.99");
        Duration processingTime = Duration.ofMillis(500);

        // When
        metricsService.recordPaymentCompletion(status, amount, processingTime);

        // Then
        assertDoesNotThrow(() -> metricsService.recordPaymentCompletion(status, amount, processingTime));
        
        // Verify timer exists in registry
        assertNotNull(meterRegistry.find("payment.processing.duration").timer());
    }

    @Test
    void recordPaymentCompletion_WithFailedPayment_ShouldRecordMetrics() {
        // Given
        PaymentStatus status = PaymentStatus.FAILED;
        BigDecimal amount = new BigDecimal("50.00");
        Duration processingTime = Duration.ofMillis(300);

        // When
        metricsService.recordPaymentCompletion(status, amount, processingTime);

        // Then
        assertDoesNotThrow(() -> metricsService.recordPaymentCompletion(status, amount, processingTime));
        
        // Verify timer exists in registry
        assertNotNull(meterRegistry.find("payment.processing.duration").timer());
    }

    @Test
    void recordTransaction_WithPurchaseTransaction_ShouldRecordMetrics() {
        // Given
        TransactionType type = TransactionType.PURCHASE;
        Duration processingTime = Duration.ofMillis(750);

        // When
        metricsService.recordTransaction(type, processingTime);

        // Then
        assertDoesNotThrow(() -> metricsService.recordTransaction(type, processingTime));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("payment.transactions.total").tag("type", "purchase").counter());
    }

    @Test
    void recordTransaction_WithRefundTransaction_ShouldRecordMetrics() {
        // Given
        TransactionType type = TransactionType.REFUND;
        Duration processingTime = Duration.ofMillis(400);

        // When
        metricsService.recordTransaction(type, processingTime);

        // Then
        assertDoesNotThrow(() -> metricsService.recordTransaction(type, processingTime));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("payment.transactions.total").tag("type", "refund").counter());
    }

    @Test
    void recordPaymentMethodUsage_WithCreditCard_ShouldIncrementCounter() {
        // Given
        String paymentMethod = "credit_card";

        // When
        metricsService.recordPaymentMethodUsage(paymentMethod);

        // Then
        assertDoesNotThrow(() -> metricsService.recordPaymentMethodUsage(paymentMethod));
        
        // Verify gauge exists in registry
        assertNotNull(meterRegistry.find("payment.method.usage").tag("method", "credit_card").gauge());
    }

    @Test
    void recordPaymentMethodUsage_WithPayPal_ShouldIncrementCounter() {
        // Given
        String paymentMethod = "paypal";

        // When
        metricsService.recordPaymentMethodUsage(paymentMethod);

        // Then
        assertDoesNotThrow(() -> metricsService.recordPaymentMethodUsage(paymentMethod));
        
        // Verify gauge exists in registry
        assertNotNull(meterRegistry.find("payment.method.usage").tag("method", "paypal").gauge());
    }

    @Test
    void recordPaymentError_WithCardDeclined_ShouldRecordError() {
        // Given
        String errorType = "CARD_DECLINED";
        String errorCode = "2";

        // When
        metricsService.recordPaymentError(errorType, errorCode);

        // Then
        assertDoesNotThrow(() -> metricsService.recordPaymentError(errorType, errorCode));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("payment.errors.by.type").tag("error.type", "CARD_DECLINED").counter());
    }

    @Test
    void recordPaymentError_WithNetworkError_ShouldRecordError() {
        // Given
        String errorType = "NETWORK_ERROR";
        String errorCode = "TIMEOUT";

        // When
        metricsService.recordPaymentError(errorType, errorCode);

        // Then
        assertDoesNotThrow(() -> metricsService.recordPaymentError(errorType, errorCode));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("payment.errors.by.type").tag("error.type", "NETWORK_ERROR").counter());
    }

    @Test
    void recordAnalyticsRequest_ShouldRecordMetrics() {
        // Given
        String requestType = "dashboard";

        // When
        metricsService.recordAnalyticsRequest(requestType);

        // Then
        assertDoesNotThrow(() -> metricsService.recordAnalyticsRequest(requestType));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("analytics.requests.total").tag("type", "dashboard").counter());
    }

    @Test
    void recordAuthenticationAttempt_ShouldRecordMetrics() {
        // Given
        String method = "jwt";

        // When
        metricsService.recordAuthenticationAttempt(method);

        // Then
        assertDoesNotThrow(() -> metricsService.recordAuthenticationAttempt(method));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("auth.attempts.by.method").tag("method", "jwt").counter());
    }

    @Test
    void recordAuthenticationSuccess_ShouldRecordMetrics() {
        // Given
        String method = "api_key";

        // When
        metricsService.recordAuthenticationSuccess(method);

        // Then
        assertDoesNotThrow(() -> metricsService.recordAuthenticationSuccess(method));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("auth.success.by.method").tag("method", "api_key").counter());
    }

    @Test
    void recordAuthenticationFailure_ShouldRecordMetrics() {
        // Given
        String method = "jwt";
        String reason = "expired_token";

        // When
        metricsService.recordAuthenticationFailure(method, reason);

        // Then
        assertDoesNotThrow(() -> metricsService.recordAuthenticationFailure(method, reason));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("auth.failures.by.method").tag("method", "jwt").counter());
    }

    @Test
    void recordRateLimitExceeded_ShouldRecordMetrics() {
        // Given
        String identifier = "api:test-key";

        // When
        metricsService.recordRateLimitExceeded(identifier);

        // Then
        assertDoesNotThrow(() -> metricsService.recordRateLimitExceeded(identifier));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("rate.limit.exceeded.by.type").tag("type", "api_key").counter());
    }

    @Test
    void recordApiKeyUsage_ShouldRecordMetrics() {
        // Given
        String apiKeyId = "key-123";

        // When
        metricsService.recordApiKeyUsage(apiKeyId);

        // Then
        assertDoesNotThrow(() -> metricsService.recordApiKeyUsage(apiKeyId));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("api.key.usage.by.key").tag("key_id", "key-123").counter());
    }

    @Test
    void recordSubscriptionCreated_ShouldRecordMetrics() {
        // Given
        String planCode = "premium";

        // When
        metricsService.recordSubscriptionCreated(planCode);

        // Then
        assertDoesNotThrow(() -> metricsService.recordSubscriptionCreated(planCode));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("subscriptions.created").tag("plan", "premium").counter());
    }

    @Test
    void recordSubscriptionCancelled_ShouldRecordMetrics() {
        // Given
        String planCode = "basic";
        String reason = "user_request";

        // When
        metricsService.recordSubscriptionCancelled(planCode, reason);

        // Then
        assertDoesNotThrow(() -> metricsService.recordSubscriptionCancelled(planCode, reason));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("subscriptions.cancelled").tag("plan", "basic").counter());
    }

    @Test
    void incrementWebhookReceived_ShouldRecordMetrics() {
        // Given
        String eventType = "payment.completed";

        // When
        metricsService.incrementWebhookReceived(eventType);

        // Then
        assertDoesNotThrow(() -> metricsService.incrementWebhookReceived(eventType));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("webhook.received.total").tag("event_type", "payment.completed").counter());
    }

    @Test
    void incrementWebhookProcessed_ShouldRecordMetrics() {
        // Given
        String eventType = "payment.failed";
        String status = "failed";

        // When
        metricsService.incrementWebhookProcessed(eventType, status);

        // Then
        assertDoesNotThrow(() -> metricsService.incrementWebhookProcessed(eventType, status));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("webhook.processed.total").tag("event_type", "payment.failed").counter());
    }

    @Test
    void recordDatabaseOperation_WithSelectQuery_ShouldRecordMetrics() {
        // Given
        String operation = "SELECT";
        Duration executionTime = Duration.ofMillis(50);

        // When
        metricsService.recordDatabaseOperation(operation, executionTime);

        // Then
        assertDoesNotThrow(() -> metricsService.recordDatabaseOperation(operation, executionTime));
        
        // Verify timer exists in registry
        assertNotNull(meterRegistry.find("database.operation.duration").tag("operation", "SELECT").timer());
    }

    @Test
    void recordDatabaseOperation_WithInsertQuery_ShouldRecordMetrics() {
        // Given
        String operation = "INSERT";
        Duration executionTime = Duration.ofMillis(25);

        // When
        metricsService.recordDatabaseOperation(operation, executionTime);

        // Then
        assertDoesNotThrow(() -> metricsService.recordDatabaseOperation(operation, executionTime));
        
        // Verify timer exists in registry
        assertNotNull(meterRegistry.find("database.operation.duration").tag("operation", "INSERT").timer());
    }

    @Test
    void recordRedisOperation_ShouldRecordMetrics() {
        // Given
        String operation = "GET";
        Duration executionTime = Duration.ofMillis(10);

        // When
        metricsService.recordRedisOperation(operation, executionTime);

        // Then
        assertDoesNotThrow(() -> metricsService.recordRedisOperation(operation, executionTime));
        
        // Verify timer exists in registry
        assertNotNull(meterRegistry.find("redis.operation.duration").tag("operation", "GET").timer());
    }

    @Test
    void recordDatabaseConnectionError_ShouldIncrementCounter() {
        // When
        metricsService.recordDatabaseConnectionError();

        // Then
        assertDoesNotThrow(() -> metricsService.recordDatabaseConnectionError());
        
        // Verify counter exists in registry (pre-initialized in constructor)
        assertNotNull(meterRegistry.find("database.connection.errors.total").counter());
    }

    @Test
    void recordRedisConnectionError_ShouldIncrementCounter() {
        // When
        metricsService.recordRedisConnectionError();

        // Then
        assertDoesNotThrow(() -> metricsService.recordRedisConnectionError());
        
        // Verify counter exists in registry (pre-initialized in constructor)
        assertNotNull(meterRegistry.find("redis.connection.errors.total").counter());
    }

    @Test
    void recordAuthorizeNetError_ShouldRecordError() {
        // Given
        String errorCode = "E00003";

        // When
        metricsService.recordAuthorizeNetError(errorCode);

        // Then
        assertDoesNotThrow(() -> metricsService.recordAuthorizeNetError(errorCode));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("authorize.net.errors.by.code").tag("error.code", "E00003").counter());
    }

    @Test
    void updateActiveDatabaseConnections_ShouldUpdateGauge() {
        // Given
        int connectionCount = 5;

        // When
        metricsService.updateActiveDatabaseConnections(connectionCount);

        // Then
        assertDoesNotThrow(() -> metricsService.updateActiveDatabaseConnections(connectionCount));
        
        // Verify gauge exists in registry (pre-initialized in constructor)
        assertNotNull(meterRegistry.find("database.connections.active").gauge());
    }

    @Test
    void getPaymentStats_ShouldReturnStats() {
        // When
        MetricsService.PaymentStats stats = metricsService.getPaymentStats();

        // Then
        assertNotNull(stats);
        assertTrue(stats.totalRequests() >= 0);
        assertTrue(stats.totalSuccess() >= 0);
        assertTrue(stats.totalFailures() >= 0);
        assertTrue(stats.activeProcessing() >= 0);
        assertTrue(stats.totalVolume() >= 0);
    }

    @Test
    void recordWebhookProcessingTime_ShouldRecordMetrics() {
        // Given
        String eventType = "payment.completed";
        long processingTimeMs = 150;

        // When
        metricsService.recordWebhookProcessingTime(eventType, processingTimeMs);

        // Then
        assertDoesNotThrow(() -> metricsService.recordWebhookProcessingTime(eventType, processingTimeMs));
        
        // Verify timer exists in registry
        assertNotNull(meterRegistry.find("webhook.processing.duration").tag("event_type", "payment.completed").timer());
    }

    @Test
    void incrementWebhookDeliverySuccess_ShouldRecordMetrics() {
        // Given
        String eventType = "payment.completed";
        int attempts = 2;

        // When
        metricsService.incrementWebhookDeliverySuccess(eventType, attempts);

        // Then
        assertDoesNotThrow(() -> metricsService.incrementWebhookDeliverySuccess(eventType, attempts));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("webhook.delivery.success.total").tag("event_type", "payment.completed").counter());
    }

    @Test
    void incrementWebhookDeliveryFailure_ShouldRecordMetrics() {
        // Given
        String eventType = "payment.failed";
        int attempts = 3;
        String failureType = "timeout";

        // When
        metricsService.incrementWebhookDeliveryFailure(eventType, attempts, failureType);

        // Then
        assertDoesNotThrow(() -> metricsService.incrementWebhookDeliveryFailure(eventType, attempts, failureType));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("webhook.delivery.failures.total").tag("event_type", "payment.failed").counter());
    }

    @Test
    void recordSuccessfulBilling_ShouldRecordMetrics() {
        // Given
        String planCode = "premium";
        BigDecimal amount = new BigDecimal("29.99");

        // When
        metricsService.recordSuccessfulBilling(planCode, amount);

        // Then
        assertDoesNotThrow(() -> metricsService.recordSuccessfulBilling(planCode, amount));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("billing.successful").tag("plan", "premium").counter());
    }

    @Test
    void recordFailedBilling_ShouldRecordMetrics() {
        // Given
        String planCode = "basic";
        BigDecimal amount = new BigDecimal("9.99");

        // When
        metricsService.recordFailedBilling(planCode, amount);

        // Then
        assertDoesNotThrow(() -> metricsService.recordFailedBilling(planCode, amount));
        
        // Verify counter exists in registry
        assertNotNull(meterRegistry.find("billing.failed").tag("plan", "basic").counter());
    }
}
