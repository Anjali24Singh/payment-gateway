package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.entity.PaymentStatus;
import com.talentica.paymentgateway.entity.TransactionType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics Service for Payment Gateway Operations.
 * 
 * Provides comprehensive metrics collection for payment processing, system health,
 * and operational insights. Tracks payment volumes, success rates, error rates,
 * and performance metrics using Micrometer.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    // Payment operation counters
    private final Counter paymentRequestsTotal;
    private final Counter paymentSuccessTotal;
    private final Counter paymentFailuresTotal;
    private final Counter paymentErrorsTotal;
    
    // Transaction type counters
    private final Counter purchaseTransactions;
    private final Counter authorizeTransactions;
    private final Counter captureTransactions;
    private final Counter voidTransactions;
    private final Counter refundTransactions;
    
    // Security and authentication counters
    private final Counter authenticationAttemptsTotal;
    private final Counter authenticationSuccessTotal;
    private final Counter authenticationFailuresTotal;
    private final Counter rateLimitExceededTotal;
    private final Counter apiKeyUsageTotal;
    
    // System health counters
    private final Counter databaseConnectionErrors;
    private final Counter redisConnectionErrors;
    private final Counter authorizeNetErrors;
    
    // Payment processing timers
    private final Timer paymentProcessingDuration;
    private final Timer authorizationDuration;
    private final Timer captureDuration;
    private final Timer refundDuration;
    
    // Database operation timers
    private final Timer databaseQueryDuration;
    private final Timer redisOperationDuration;
    
    // Real-time gauges
    private final AtomicInteger activePaymentProcessing = new AtomicInteger(0);
    private final AtomicLong totalPaymentVolume = new AtomicLong(0);
    private final AtomicInteger databaseConnectionsActive = new AtomicInteger(0);
    private final ConcurrentHashMap<String, AtomicInteger> paymentMethodCounts = new ConcurrentHashMap<>();

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize payment operation counters
        this.paymentRequestsTotal = Counter.builder("payment.requests.total")
                .description("Total number of payment requests")
                .register(meterRegistry);
                
        this.paymentSuccessTotal = Counter.builder("payment.success.total")
                .description("Total number of successful payments")
                .register(meterRegistry);
                
        this.paymentFailuresTotal = Counter.builder("payment.failures.total")
                .description("Total number of failed payments")
                .register(meterRegistry);
                
        this.paymentErrorsTotal = Counter.builder("payment.errors.total")
                .description("Total number of payment processing errors")
                .register(meterRegistry);
        
        // Initialize transaction type counters
        this.purchaseTransactions = Counter.builder("payment.transactions.total")
                .tag("type", "purchase")
                .description("Total purchase transactions")
                .register(meterRegistry);
                
        this.authorizeTransactions = Counter.builder("payment.transactions.total")
                .tag("type", "authorize")
                .description("Total authorize transactions")
                .register(meterRegistry);
                
        this.captureTransactions = Counter.builder("payment.transactions.total")
                .tag("type", "capture")
                .description("Total capture transactions")
                .register(meterRegistry);
                
        this.voidTransactions = Counter.builder("payment.transactions.total")
                .tag("type", "void")
                .description("Total void transactions")
                .register(meterRegistry);
                
        this.refundTransactions = Counter.builder("payment.transactions.total")
                .tag("type", "refund")
                .description("Total refund transactions")
                .register(meterRegistry);
        
        // Initialize security counters
        this.authenticationAttemptsTotal = Counter.builder("auth.attempts.total")
                .description("Total authentication attempts")
                .register(meterRegistry);
                
        this.authenticationSuccessTotal = Counter.builder("auth.success.total")
                .description("Total successful authentications")
                .register(meterRegistry);
                
        this.authenticationFailuresTotal = Counter.builder("auth.failures.total")
                .description("Total failed authentications")
                .register(meterRegistry);
                
        this.rateLimitExceededTotal = Counter.builder("rate.limit.exceeded.total")
                .description("Total rate limit exceeded events")
                .register(meterRegistry);
                
        this.apiKeyUsageTotal = Counter.builder("api.key.usage.total")
                .description("Total API key usage")
                .register(meterRegistry);
        
        // Initialize system health counters
        this.databaseConnectionErrors = Counter.builder("database.connection.errors.total")
                .description("Total database connection errors")
                .register(meterRegistry);
                
        this.redisConnectionErrors = Counter.builder("redis.connection.errors.total")
                .description("Total Redis connection errors")
                .register(meterRegistry);
                
        this.authorizeNetErrors = Counter.builder("authorize.net.errors.total")
                .description("Total Authorize.Net API errors")
                .register(meterRegistry);
        
        // Initialize timers
        this.paymentProcessingDuration = Timer.builder("payment.processing.duration")
                .description("Payment processing duration")
                .register(meterRegistry);
                
        this.authorizationDuration = Timer.builder("payment.authorization.duration")
                .description("Payment authorization duration")
                .register(meterRegistry);
                
        this.captureDuration = Timer.builder("payment.capture.duration")
                .description("Payment capture duration")
                .register(meterRegistry);
                
        this.refundDuration = Timer.builder("payment.refund.duration")
                .description("Payment refund duration")
                .register(meterRegistry);
                
        this.databaseQueryDuration = Timer.builder("database.query.duration")
                .description("Database query duration")
                .register(meterRegistry);
                
        this.redisOperationDuration = Timer.builder("redis.operation.duration")
                .description("Redis operation duration")
                .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("payment.processing.active", activePaymentProcessing, AtomicInteger::doubleValue)
                .description("Number of payment processing operations currently active")
                .register(meterRegistry);
                
        Gauge.builder("payment.volume.total", totalPaymentVolume, AtomicLong::doubleValue)
                .description("Total payment volume in cents")
                .register(meterRegistry);
                
        Gauge.builder("database.connections.active", databaseConnectionsActive, AtomicInteger::doubleValue)
                .description("Number of active database connections")
                .register(meterRegistry);
    }

    // Payment metrics methods
    
    /**
     * Record a payment request.
     */
    public void recordPaymentRequest() {
        paymentRequestsTotal.increment();
        activePaymentProcessing.incrementAndGet();
    }

    /**
     * Record a payment completion.
     * 
     * @param status Payment status
     * @param amount Payment amount in cents
     * @param duration Processing duration
     */
public void recordPaymentCompletion(PaymentStatus status, BigDecimal amount, Duration duration) {
        activePaymentProcessing.decrementAndGet();
        
        if (status == PaymentStatus.SETTLED || status == PaymentStatus.CAPTURED || status == PaymentStatus.AUTHORIZED) {
            paymentSuccessTotal.increment();
            if (amount != null) {
                totalPaymentVolume.addAndGet(amount.longValue());
            }
        } else {
            paymentFailuresTotal.increment();
        }
        
        paymentProcessingDuration.record(duration);
    }

    /**
     * Record a transaction by type.
     * 
     * @param transactionType Type of transaction
     * @param duration Processing duration
     */
    public void recordTransaction(TransactionType transactionType, Duration duration) {
        switch (transactionType) {
            case PURCHASE -> {
                purchaseTransactions.increment();
                paymentProcessingDuration.record(duration);
            }
            case AUTHORIZE -> {
                authorizeTransactions.increment();
                authorizationDuration.record(duration);
            }
            case CAPTURE -> {
                captureTransactions.increment();
                captureDuration.record(duration);
            }
            case VOID -> voidTransactions.increment();
            case REFUND -> {
                refundTransactions.increment();
                refundDuration.record(duration);
            }
            case PARTIAL_REFUND -> {
                refundTransactions.increment();
                refundDuration.record(duration);
            }
        }
    }

    /**
     * Record analytics request.
     * 
     * @param requestType Type of analytics request
     */
    public void recordAnalyticsRequest(String requestType) {
        Counter analyticsRequestCounter = Counter.builder("analytics.requests.total")
                .tag("type", requestType)
                .description("Total analytics requests by type")
                .register(meterRegistry);
        analyticsRequestCounter.increment();
    }

    /**
     * Record payment method usage.
     * 
     * @param paymentMethod Payment method type
     */
    public void recordPaymentMethodUsage(String paymentMethod) {
        paymentMethodCounts.computeIfAbsent(paymentMethod, k -> {
            AtomicInteger counter = new AtomicInteger(0);
            Gauge.builder("payment.method.usage", counter, AtomicInteger::doubleValue)
                    .tag("method", k)
                    .description("Payment method usage count")
                    .register(meterRegistry);
            return counter;
        }).incrementAndGet();
    }

    /**
     * Record a payment processing error.
     * 
     * @param errorType Type of error
     * @param errorCode Error code
     */
    public void recordPaymentError(String errorType, String errorCode) {
        paymentErrorsTotal.increment();
        activePaymentProcessing.decrementAndGet();
        
        Counter.builder("payment.errors.by.type")
                .tag("error.type", errorType)
                .tag("error.code", errorCode)
                .description("Payment errors by type and code")
                .register(meterRegistry)
                .increment();
    }

    // Authentication metrics methods
    
    /**
     * Record an authentication attempt.
     * 
     * @param method Authentication method (jwt, api_key)
     */
    public void recordAuthenticationAttempt(String method) {
        authenticationAttemptsTotal.increment();
        
        Counter.builder("auth.attempts.by.method")
                .tag("method", method)
                .description("Authentication attempts by method")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record authentication success.
     * 
     * @param method Authentication method
     */
    public void recordAuthenticationSuccess(String method) {
        authenticationSuccessTotal.increment();
        
        Counter.builder("auth.success.by.method")
                .tag("method", method)
                .description("Successful authentications by method")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record authentication failure.
     * 
     * @param method Authentication method
     * @param reason Failure reason
     */
    public void recordAuthenticationFailure(String method, String reason) {
        authenticationFailuresTotal.increment();
        
        Counter.builder("auth.failures.by.method")
                .tag("method", method)
                .tag("reason", reason)
                .description("Failed authentications by method and reason")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record rate limit exceeded event.
     * 
     * @param identifier Rate limit identifier
     */
    public void recordRateLimitExceeded(String identifier) {
        rateLimitExceededTotal.increment();
        
        String identifierType = identifier.startsWith("api:") ? "api_key" : 
                               identifier.startsWith("user:") ? "user" : "ip";
        
        Counter.builder("rate.limit.exceeded.by.type")
                .tag("type", identifierType)
                .description("Rate limit exceeded by identifier type")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record API key usage.
     * 
     * @param apiKeyId API key identifier
     */
    public void recordApiKeyUsage(String apiKeyId) {
        apiKeyUsageTotal.increment();
        
        Counter.builder("api.key.usage.by.key")
                .tag("key_id", apiKeyId)
                .description("API key usage by key ID")
                .register(meterRegistry)
                .increment();
    }

    // System health metrics methods
    
    /**
     * Record database operation.
     * 
     * @param operation Operation type
     * @param duration Operation duration
     */
    public void recordDatabaseOperation(String operation, Duration duration) {
        Timer.builder("database.operation.duration")
                .tag("operation", operation)
                .description("Database operation duration by type")
                .register(meterRegistry)
                .record(duration);
        
        databaseQueryDuration.record(duration);
    }

    /**
     * Record database connection error.
     */
    public void recordDatabaseConnectionError() {
        databaseConnectionErrors.increment();
    }

    /**
     * Record Redis operation.
     * 
     * @param operation Operation type
     * @param duration Operation duration
     */
    public void recordRedisOperation(String operation, Duration duration) {
        Timer.builder("redis.operation.duration")
                .tag("operation", operation)
                .description("Redis operation duration by type")
                .register(meterRegistry)
                .record(duration);
        
        redisOperationDuration.record(duration);
    }

    /**
     * Record Redis connection error.
     */
    public void recordRedisConnectionError() {
        redisConnectionErrors.increment();
    }

    /**
     * Record Authorize.Net API error.
     * 
     * @param errorCode Error code from Authorize.Net
     */
    public void recordAuthorizeNetError(String errorCode) {
        authorizeNetErrors.increment();
        
        Counter.builder("authorize.net.errors.by.code")
                .tag("error.code", errorCode)
                .description("Authorize.Net errors by error code")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Update active database connections count.
     * 
     * @param count Current active connections
     */
    public void updateActiveDatabaseConnections(int count) {
        databaseConnectionsActive.set(count);
    }

    /**
     * Get current payment processing statistics.
     * 
     * @return Processing statistics
     */
    public PaymentStats getPaymentStats() {
        return new PaymentStats(
                (long) paymentRequestsTotal.count(),
                (long) paymentSuccessTotal.count(),
                (long) paymentFailuresTotal.count(),
                activePaymentProcessing.get(),
                totalPaymentVolume.get()
        );
    }

    /**
     * Payment statistics data class.
     */
    public record PaymentStats(
            long totalRequests,
            long totalSuccess,
            long totalFailures,
            int activeProcessing,
            long totalVolume
    ) {}

    // Webhook metrics methods
    
    /**
     * Record webhook received.
     * 
     * @param eventType Webhook event type
     */
    public void incrementWebhookReceived(String eventType) {
        Counter.builder("webhook.received.total")
                .tag("event_type", eventType)
                .description("Total webhooks received by event type")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record webhook processed.
     * 
     * @param eventType Webhook event type
     * @param status Processing status
     */
    public void incrementWebhookProcessed(String eventType, String status) {
        Counter.builder("webhook.processed.total")
                .tag("event_type", eventType)
                .tag("status", status)
                .description("Total webhooks processed by event type and status")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record webhook signature failure.
     */
    public void incrementWebhookSignatureFailure() {
        Counter.builder("webhook.signature.failures.total")
                .description("Total webhook signature verification failures")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record webhook duplicate.
     * 
     * @param eventType Webhook event type
     */
    public void incrementWebhookDuplicate(String eventType) {
        Counter.builder("webhook.duplicates.total")
                .tag("event_type", eventType)
                .description("Total duplicate webhooks by event type")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record webhook processing time.
     * 
     * @param eventType Webhook event type
     * @param processingTimeMs Processing time in milliseconds
     */
    public void recordWebhookProcessingTime(String eventType, long processingTimeMs) {
        Timer.builder("webhook.processing.duration")
                .tag("event_type", eventType)
                .description("Webhook processing duration by event type")
                .register(meterRegistry)
                .record(Duration.ofMillis(processingTimeMs));
    }

    /**
     * Record webhook delivery success.
     * 
     * @param eventType Webhook event type
     * @param attempts Number of attempts required
     */
    public void incrementWebhookDeliverySuccess(String eventType, int attempts) {
        Counter.builder("webhook.delivery.success.total")
                .tag("event_type", eventType)
                .description("Total successful webhook deliveries")
                .register(meterRegistry)
                .increment();

        Timer.builder("webhook.delivery.attempts")
                .tag("event_type", eventType)
                .tag("result", "success")
                .description("Number of attempts for webhook delivery")
                .register(meterRegistry)
                .record(attempts, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * Record webhook delivery failure.
     * 
     * @param eventType Webhook event type
     * @param attempts Number of attempts
     * @param failureType Type of failure
     */
    public void incrementWebhookDeliveryFailure(String eventType, int attempts, String failureType) {
        Counter.builder("webhook.delivery.failures.total")
                .tag("event_type", eventType)
                .tag("failure_type", failureType)
                .description("Total failed webhook deliveries")
                .register(meterRegistry)
                .increment();

        Timer.builder("webhook.delivery.attempts")
                .tag("event_type", eventType)
                .tag("result", "failure")
                .description("Number of attempts for webhook delivery")
                .register(meterRegistry)
                .record(attempts, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * Record webhook max attempts reached.
     * 
     * @param eventType Webhook event type
     */
    public void incrementWebhookMaxAttemptsReached(String eventType) {
        Counter.builder("webhook.max.attempts.reached.total")
                .tag("event_type", eventType)
                .description("Total webhooks that reached max retry attempts")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record webhook cleanup.
     * 
     * @param cleanedCount Number of webhooks cleaned up
     */
    public void incrementWebhookCleanup(int cleanedCount) {
        Counter.builder("webhook.cleanup.total")
                .description("Total webhooks cleaned up")
                .register(meterRegistry)
                .increment(cleanedCount);
    }

    // Subscription metrics
    public void recordSubscriptionCreated(String planCode) {
        Counter.builder("subscriptions.created")
                .tag("plan", planCode)
                .description("Subscriptions created")
                .register(meterRegistry)
                .increment();
    }

    public void recordSubscriptionCancelled(String planCode, String reason) {
        Counter.builder("subscriptions.cancelled")
                .tag("plan", planCode)
                .tag("reason", reason)
                .description("Subscriptions cancelled")
                .register(meterRegistry)
                .increment();
    }

    public void recordPlanChange(String fromPlan, String toPlan) {
        Counter.builder("subscriptions.plan_change")
                .tag("from_plan", fromPlan)
                .tag("to_plan", toPlan)
                .description("Subscription plan changes")
                .register(meterRegistry)
                .increment();
    }

    public void recordPlanCreated(String planCode, java.math.BigDecimal amount) {
        Counter.builder("plans.created")
                .tag("plan", planCode)
                .description("Plans created")
                .register(meterRegistry)
                .increment();
    }

    // Billing metrics
    public void recordSuccessfulBilling(String planCode, java.math.BigDecimal amount) {
        Counter.builder("billing.successful")
                .tag("plan", planCode)
                .description("Successful billing operations")
                .register(meterRegistry)
                .increment();
        
        Counter.builder("billing.revenue")
                .tag("plan", planCode)
                .description("Billing revenue")
                .register(meterRegistry)
                .increment(amount.doubleValue());
    }

    public void recordFailedBilling(String planCode, java.math.BigDecimal amount) {
        Counter.builder("billing.failed")
                .tag("plan", planCode)
                .description("Failed billing operations")
                .register(meterRegistry)
                .increment();
    }

    public void recordBillingError(String planCode) {
        Counter.builder("billing.errors")
                .tag("plan", planCode)
                .description("Billing errors")
                .register(meterRegistry)
                .increment();
    }

    public void recordPaymentRetry(String planCode, Integer attemptNumber, boolean successful) {
        Counter.builder("payment.retry")
                .tag("plan", planCode)
                .tag("attempt", attemptNumber.toString())
                .tag("successful", successful ? "true" : "false")
                .description("Payment retry attempts")
                .register(meterRegistry)
                .increment();
    }

    public void recordSubscriptionCancelledForNonPayment(String planCode) {
        Counter.builder("subscriptions.cancelled_nonpayment")
                .tag("plan", planCode)
                .description("Subscriptions cancelled for non-payment")
                .register(meterRegistry)
                .increment();
    }
}
