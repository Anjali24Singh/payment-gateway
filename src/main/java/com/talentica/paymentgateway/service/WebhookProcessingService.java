package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.webhook.AuthorizeNetWebhookRequest;
import com.talentica.paymentgateway.dto.webhook.WebhookResponse;
import com.talentica.paymentgateway.entity.*;
import com.talentica.paymentgateway.repository.TransactionRepository;
import com.talentica.paymentgateway.repository.WebhookRepository;
import com.talentica.paymentgateway.util.CorrelationIdUtil;
import com.talentica.paymentgateway.util.WebhookSignatureVerifier;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for processing webhook events asynchronously.
 * Handles Authorize.Net webhook events including payment status updates,
 * fraud checks, and transaction state changes.
 * 
 * Features:
 * - Asynchronous processing with thread pools
 * - Duplicate event detection and prevention
 * - Retry mechanism with exponential backoff
 * - Transaction status synchronization
 * - Comprehensive logging and metrics
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
@Transactional
public class WebhookProcessingService {
    
    @Autowired
    private WebhookRepository webhookRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private WebhookSignatureVerifier signatureVerifier;
    
    @Autowired
    private MetricsService metricsService;
    
    @Value("${app.webhook.duplicate-detection.enabled:true}")
    private boolean duplicateDetectionEnabled;
    
    @Value("${app.webhook.duplicate-detection.window-minutes:60}")
    private int duplicateDetectionWindowMinutes;
    
    @Value("${app.webhook.processing.timeout-seconds:30}")
    private int processingTimeoutSeconds;
    
    // Event type constants for Authorize.Net webhooks
    private static final String EVENT_PAYMENT_AUTHCAPTURE_CREATED = "net.authorize.payment.authcapture.created";
    private static final String EVENT_PAYMENT_AUTHORIZATION_CREATED = "net.authorize.payment.authorization.created";
    private static final String EVENT_PAYMENT_CAPTURE_CREATED = "net.authorize.payment.capture.created";
    private static final String EVENT_PAYMENT_REFUND_CREATED = "net.authorize.payment.refund.created";
    private static final String EVENT_PAYMENT_VOID_CREATED = "net.authorize.payment.void.created";
    private static final String EVENT_PAYMENT_FRAUD_APPROVED = "net.authorize.payment.fraud.approved";
    private static final String EVENT_PAYMENT_FRAUD_DECLINED = "net.authorize.payment.fraud.declined";
    private static final String EVENT_PAYMENT_FRAUD_HELD = "net.authorize.payment.fraud.held";
    
    /**
     * Processes webhook request asynchronously.
     * 
     * @param webhookRequest Incoming webhook request
     * @param headers HTTP headers for signature verification
     * @param rawPayload Raw webhook payload for signature verification
     * @return CompletableFuture with webhook response
     */
    @Async("taskExecutor")
    public CompletableFuture<WebhookResponse> processWebhookAsync(
            AuthorizeNetWebhookRequest webhookRequest, 
            Map<String, String> headers,
            String rawPayload) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        String eventId = webhookRequest.getNotificationId();
        
        try {
            // Set correlation ID for this processing thread
            MDC.put("correlationId", correlationId);
            
            log.info("Starting async webhook processing - EventID: {}, Type: {}, TransactionID: {}", 
                       eventId, webhookRequest.getEventType(), webhookRequest.getTransactionId());
            
            // Verify webhook signature
            if (!signatureVerifier.verifySignature(headers, rawPayload)) {
                log.error("Webhook signature verification failed - EventID: {}", eventId);
                metricsService.incrementWebhookSignatureFailure();
                return CompletableFuture.completedFuture(
                    WebhookResponse.signatureError(eventId, correlationId));
            }
            
            // Check for duplicate events
            if (duplicateDetectionEnabled && isDuplicateEvent(webhookRequest)) {
                log.warn("Duplicate webhook event detected - EventID: {}, Type: {}", 
                           eventId, webhookRequest.getEventType());
                metricsService.incrementWebhookDuplicate(webhookRequest.getEventType());
                return CompletableFuture.completedFuture(
                    WebhookResponse.duplicateEvent(eventId, correlationId));
            }
            
            // Process the webhook event
            WebhookResponse response = processWebhookEvent(webhookRequest, correlationId);
            
            // Record successful processing
            metricsService.incrementWebhookProcessed(webhookRequest.getEventType(), "success");
            
            log.info("Webhook processing completed successfully - EventID: {}, Type: {}", 
                       eventId, webhookRequest.getEventType());
            
            return CompletableFuture.completedFuture(response);
            
        } catch (Exception e) {
            log.error("Error processing webhook - EventID: {}, Type: {}, Error: {}", 
                        eventId, webhookRequest.getEventType(), e.getMessage(), e);
            
            metricsService.incrementWebhookProcessed(webhookRequest.getEventType(), "error");
            
            return CompletableFuture.completedFuture(
                WebhookResponse.processingError(eventId, correlationId, 
                                              "Internal processing error", e));
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Processes a webhook event and updates transaction status.
     * 
     * @param webhookRequest Webhook request to process
     * @param correlationId Correlation ID for tracking
     * @return Webhook response
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public WebhookResponse processWebhookEvent(AuthorizeNetWebhookRequest webhookRequest, String correlationId) {
        String eventId = webhookRequest.getNotificationId();
        String eventType = webhookRequest.getEventType();
        
        try {
            // Create webhook record for audit trail
            Webhook webhook = createWebhookRecord(webhookRequest, correlationId);
            webhook = webhookRepository.save(webhook);
            
            // Process based on event type
            switch (eventType) {
                case EVENT_PAYMENT_AUTHCAPTURE_CREATED:
                    return processAuthCaptureEvent(webhookRequest, correlationId);
                    
                case EVENT_PAYMENT_AUTHORIZATION_CREATED:
                    return processAuthorizationEvent(webhookRequest, correlationId);
                    
                case EVENT_PAYMENT_CAPTURE_CREATED:
                    return processCaptureEvent(webhookRequest, correlationId);
                    
                case EVENT_PAYMENT_REFUND_CREATED:
                    return processRefundEvent(webhookRequest, correlationId);
                    
                case EVENT_PAYMENT_VOID_CREATED:
                    return processVoidEvent(webhookRequest, correlationId);
                    
                case EVENT_PAYMENT_FRAUD_APPROVED:
                case EVENT_PAYMENT_FRAUD_DECLINED:
                case EVENT_PAYMENT_FRAUD_HELD:
                    return processFraudEvent(webhookRequest, correlationId);
                    
                default:
                    log.warn("Unsupported webhook event type: {} - EventID: {}", eventType, eventId);
                    return WebhookResponse.success(eventId, correlationId, 
                                                 "Event received but not processed (unsupported type)");
            }
            
        } catch (Exception e) {
            log.error("Error processing webhook event - EventID: {}, Type: {}, Error: {}", 
                        eventId, eventType, e.getMessage(), e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }
    
    /**
     * Processes auth-capture (purchase) webhook events.
     */
    private WebhookResponse processAuthCaptureEvent(AuthorizeNetWebhookRequest webhookRequest, String correlationId) {
        String transactionId = webhookRequest.getTransactionId();
        String eventId = webhookRequest.getNotificationId();
        
        Optional<Transaction> transactionOpt = transactionRepository.findByAuthnetTransactionId(transactionId);
        if (transactionOpt.isEmpty()) {
            log.warn("Transaction not found for auth-capture webhook - AuthNetID: {}, EventID: {}", 
                       transactionId, eventId);
            return WebhookResponse.success(eventId, correlationId, "Transaction not found in local database");
        }
        
        Transaction transaction = transactionOpt.get();
        AuthorizeNetWebhookRequest.AuthorizeNetPayload payload = webhookRequest.getPayload();
        
        // Update transaction status based on response code
        if (payload.getResponseCode() != null && payload.getResponseCode() == 1) {
            // Approved
            transaction.setStatus(PaymentStatus.SETTLED);
            transaction.setAuthnetAuthCode(payload.getAuthCode());
            transaction.setAuthnetAvsResult(payload.getAvsResponse());
            transaction.setAuthnetCvvResult(payload.getCardCodeResponse());
            
            if (payload.getSettleAmount() != null) {
                transaction.setAmount(BigDecimal.valueOf(payload.getSettleAmount()));
            }
            
            log.info("Updated transaction status to SETTLED - TransactionID: {}, AuthNetID: {}", 
                       transaction.getTransactionId(), transactionId);
        } else {
            // Declined or error
            transaction.setStatus(PaymentStatus.FAILED);
            log.info("Updated transaction status to FAILED - TransactionID: {}, AuthNetID: {}, ResponseCode: {}", 
                       transaction.getTransactionId(), transactionId, payload.getResponseCode());
        }
        
        transaction.setProcessedAt(ZonedDateTime.now());
        transactionRepository.save(transaction);
        
        return WebhookResponse.success(eventId, correlationId, "Auth-capture event processed successfully");
    }
    
    /**
     * Processes authorization webhook events.
     */
    private WebhookResponse processAuthorizationEvent(AuthorizeNetWebhookRequest webhookRequest, String correlationId) {
        String transactionId = webhookRequest.getTransactionId();
        String eventId = webhookRequest.getNotificationId();
        
        Optional<Transaction> transactionOpt = transactionRepository.findByAuthnetTransactionId(transactionId);
        if (transactionOpt.isEmpty()) {
            log.warn("Transaction not found for authorization webhook - AuthNetID: {}, EventID: {}", 
                       transactionId, eventId);
            return WebhookResponse.success(eventId, correlationId, "Transaction not found in local database");
        }
        
        Transaction transaction = transactionOpt.get();
        AuthorizeNetWebhookRequest.AuthorizeNetPayload payload = webhookRequest.getPayload();
        
        // Update transaction status
        if (payload.getResponseCode() != null && payload.getResponseCode() == 1) {
            transaction.setStatus(PaymentStatus.AUTHORIZED);
            transaction.setAuthnetAuthCode(payload.getAuthCode());
            transaction.setAuthnetAvsResult(payload.getAvsResponse());
            transaction.setAuthnetCvvResult(payload.getCardCodeResponse());
            
            log.info("Updated transaction status to AUTHORIZED - TransactionID: {}, AuthNetID: {}", 
                       transaction.getTransactionId(), transactionId);
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            log.info("Updated transaction status to FAILED - TransactionID: {}, AuthNetID: {}, ResponseCode: {}", 
                       transaction.getTransactionId(), transactionId, payload.getResponseCode());
        }
        
        transaction.setProcessedAt(ZonedDateTime.now());
        transactionRepository.save(transaction);
        
        return WebhookResponse.success(eventId, correlationId, "Authorization event processed successfully");
    }
    
    /**
     * Processes capture webhook events.
     */
    private WebhookResponse processCaptureEvent(AuthorizeNetWebhookRequest webhookRequest, String correlationId) {
        String transactionId = webhookRequest.getTransactionId();
        String eventId = webhookRequest.getNotificationId();
        
        Optional<Transaction> transactionOpt = transactionRepository.findByAuthnetTransactionId(transactionId);
        if (transactionOpt.isEmpty()) {
            log.warn("Transaction not found for capture webhook - AuthNetID: {}, EventID: {}", 
                       transactionId, eventId);
            return WebhookResponse.success(eventId, correlationId, "Transaction not found in local database");
        }
        
        Transaction transaction = transactionOpt.get();
        AuthorizeNetWebhookRequest.AuthorizeNetPayload payload = webhookRequest.getPayload();
        
        // Update transaction status
        if (payload.getResponseCode() != null && payload.getResponseCode() == 1) {
            transaction.setStatus(PaymentStatus.SETTLED);
            
            if (payload.getSettleAmount() != null) {
                transaction.setAmount(BigDecimal.valueOf(payload.getSettleAmount()));
            }
            
            log.info("Updated transaction status to SETTLED - TransactionID: {}, AuthNetID: {}", 
                       transaction.getTransactionId(), transactionId);
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            log.info("Updated transaction status to FAILED - TransactionID: {}, AuthNetID: {}, ResponseCode: {}", 
                       transaction.getTransactionId(), transactionId, payload.getResponseCode());
        }
        
        transaction.setProcessedAt(ZonedDateTime.now());
        transactionRepository.save(transaction);
        
        return WebhookResponse.success(eventId, correlationId, "Capture event processed successfully");
    }
    
    /**
     * Processes refund webhook events.
     */
    private WebhookResponse processRefundEvent(AuthorizeNetWebhookRequest webhookRequest, String correlationId) {
        String transactionId = webhookRequest.getTransactionId();
        String eventId = webhookRequest.getNotificationId();
        
        Optional<Transaction> transactionOpt = transactionRepository.findByAuthnetTransactionId(transactionId);
        if (transactionOpt.isEmpty()) {
            log.warn("Transaction not found for refund webhook - AuthNetID: {}, EventID: {}", 
                       transactionId, eventId);
            return WebhookResponse.success(eventId, correlationId, "Transaction not found in local database");
        }
        
        Transaction transaction = transactionOpt.get();
        AuthorizeNetWebhookRequest.AuthorizeNetPayload payload = webhookRequest.getPayload();
        
        // Update transaction status
        if (payload.getResponseCode() != null && payload.getResponseCode() == 1) {
            // Check if this is a full or partial refund
            if (payload.getSettleAmount() != null) {
                BigDecimal refundAmount = BigDecimal.valueOf(payload.getSettleAmount());
                if (refundAmount.compareTo(transaction.getAmount()) == 0) {
                    transaction.setStatus(PaymentStatus.REFUNDED);
                } else {
                    transaction.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
                }
            } else {
                transaction.setStatus(PaymentStatus.REFUNDED);
            }
            
            log.info("Updated transaction status for refund - TransactionID: {}, AuthNetID: {}, Status: {}", 
                       transaction.getTransactionId(), transactionId, transaction.getStatus());
        } else {
            log.warn("Refund failed - TransactionID: {}, AuthNetID: {}, ResponseCode: {}", 
                       transaction.getTransactionId(), transactionId, payload.getResponseCode());
        }
        
        transaction.setProcessedAt(ZonedDateTime.now());
        transactionRepository.save(transaction);
        
        return WebhookResponse.success(eventId, correlationId, "Refund event processed successfully");
    }
    
    /**
     * Processes void webhook events.
     */
    private WebhookResponse processVoidEvent(AuthorizeNetWebhookRequest webhookRequest, String correlationId) {
        String transactionId = webhookRequest.getTransactionId();
        String eventId = webhookRequest.getNotificationId();
        
        Optional<Transaction> transactionOpt = transactionRepository.findByAuthnetTransactionId(transactionId);
        if (transactionOpt.isEmpty()) {
            log.warn("Transaction not found for void webhook - AuthNetID: {}, EventID: {}", 
                       transactionId, eventId);
            return WebhookResponse.success(eventId, correlationId, "Transaction not found in local database");
        }
        
        Transaction transaction = transactionOpt.get();
        AuthorizeNetWebhookRequest.AuthorizeNetPayload payload = webhookRequest.getPayload();
        
        // Update transaction status
        if (payload.getResponseCode() != null && payload.getResponseCode() == 1) {
            transaction.setStatus(PaymentStatus.VOIDED);
            log.info("Updated transaction status to VOIDED - TransactionID: {}, AuthNetID: {}", 
                       transaction.getTransactionId(), transactionId);
        } else {
            log.warn("Void failed - TransactionID: {}, AuthNetID: {}, ResponseCode: {}", 
                       transaction.getTransactionId(), transactionId, payload.getResponseCode());
        }
        
        transaction.setProcessedAt(ZonedDateTime.now());
        transactionRepository.save(transaction);
        
        return WebhookResponse.success(eventId, correlationId, "Void event processed successfully");
    }
    
    /**
     * Processes fraud review webhook events.
     */
    private WebhookResponse processFraudEvent(AuthorizeNetWebhookRequest webhookRequest, String correlationId) {
        String transactionId = webhookRequest.getTransactionId();
        String eventId = webhookRequest.getNotificationId();
        String eventType = webhookRequest.getEventType();
        
        Optional<Transaction> transactionOpt = transactionRepository.findByAuthnetTransactionId(transactionId);
        if (transactionOpt.isEmpty()) {
            log.warn("Transaction not found for fraud webhook - AuthNetID: {}, EventID: {}", 
                       transactionId, eventId);
            return WebhookResponse.success(eventId, correlationId, "Transaction not found in local database");
        }
        
        Transaction transaction = transactionOpt.get();
        
        // Update transaction status based on fraud decision
        if (eventType.contains(".fraud.approved")) {
            transaction.setStatus(PaymentStatus.SETTLED);
            log.info("Fraud review approved - TransactionID: {}, AuthNetID: {}", 
                       transaction.getTransactionId(), transactionId);
        } else if (eventType.contains(".fraud.declined")) {
            transaction.setStatus(PaymentStatus.FAILED);
            log.info("Fraud review declined - TransactionID: {}, AuthNetID: {}", 
                       transaction.getTransactionId(), transactionId);
        } else if (eventType.contains(".fraud.held")) {
            transaction.setStatus(PaymentStatus.PENDING_REVIEW);
            log.info("Transaction held for fraud review - TransactionID: {}, AuthNetID: {}", 
                       transaction.getTransactionId(), transactionId);
        }
        
        transaction.setProcessedAt(ZonedDateTime.now());
        transactionRepository.save(transaction);
        
        return WebhookResponse.success(eventId, correlationId, "Fraud event processed successfully");
    }
    
    /**
     * Creates a webhook record for audit trail.
     */
    private Webhook createWebhookRecord(AuthorizeNetWebhookRequest webhookRequest, String correlationId) {
        Webhook webhook = new Webhook();
        webhook.setWebhookId(UUID.randomUUID().toString());
        webhook.setEventType(webhookRequest.getEventType());
        webhook.setEventId(webhookRequest.getNotificationId());
        webhook.setEndpointUrl("/api/v1/webhooks/authorize-net");
        webhook.setCorrelationId(correlationId);
        webhook.setStatus(WebhookStatus.PROCESSING);
        
        // Store request payload
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("notificationId", webhookRequest.getNotificationId());
        requestBody.put("eventType", webhookRequest.getEventType());
        requestBody.put("eventDate", webhookRequest.getEventDate());
        requestBody.put("webhookId", webhookRequest.getWebhookId());
        requestBody.put("payload", webhookRequest.getPayload());
        webhook.setRequestBody(requestBody);
        
        webhook.setStandardHeaders();
        webhook.setScheduledAt(ZonedDateTime.now());
        
        return webhook;
    }
    
    /**
     * Checks if the webhook event is a duplicate.
     */
    private boolean isDuplicateEvent(AuthorizeNetWebhookRequest webhookRequest) {
        if (!duplicateDetectionEnabled) {
            return false;
        }
        
        String eventId = webhookRequest.getNotificationId();
        String eventType = webhookRequest.getEventType();
        
        // Check for duplicate events within the detection window
        ZonedDateTime cutoffTime = ZonedDateTime.now().minusMinutes(duplicateDetectionWindowMinutes);
        
        List<Webhook> recentWebhooks = webhookRepository.findByEventIdAndEventTypeAndCreatedAtAfter(
            eventId, eventType, cutoffTime);
        
        boolean isDuplicate = !recentWebhooks.isEmpty();
        
        if (isDuplicate) {
            log.warn("Duplicate webhook event detected - EventID: {}, Type: {}, Previous events: {}", 
                       eventId, eventType, recentWebhooks.size());
        }
        
        return isDuplicate;
    }
    
    /**
     * Gets processing statistics for monitoring.
     */
    public Map<String, Object> getProcessingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        ZonedDateTime last24Hours = ZonedDateTime.now().minusHours(24);
        
        stats.put("webhooksLast24Hours", webhookRepository.countWebhooksCreatedSince(last24Hours));
        stats.put("successfulDeliveries", webhookRepository.countByStatusAndCreatedAtAfter(
            WebhookStatus.DELIVERED, last24Hours));
        stats.put("failedDeliveries", webhookRepository.countByStatusAndCreatedAtAfter(
            WebhookStatus.FAILED, last24Hours));
        stats.put("pendingDeliveries", webhookRepository.countByStatus(WebhookStatus.PENDING));
        stats.put("retryingDeliveries", webhookRepository.countByStatus(WebhookStatus.RETRYING));
        
        return stats;
    }
}
