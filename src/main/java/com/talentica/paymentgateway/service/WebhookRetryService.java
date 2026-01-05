package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.entity.Webhook;
import com.talentica.paymentgateway.entity.WebhookStatus;
import com.talentica.paymentgateway.repository.WebhookRepository;
import com.talentica.paymentgateway.util.CorrelationIdUtil;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for handling webhook delivery retries with exponential backoff.
 * Implements industry-standard retry patterns for reliable webhook delivery.
 * 
 * Features:
 * - Exponential backoff with jitter
 * - Dead letter queue for permanently failed webhooks
 * - Scheduled retry processing
 * - Comprehensive delivery tracking
 * - Circuit breaker pattern for problematic endpoints
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
@Transactional
public class WebhookRetryService {
    
    @Autowired
    private WebhookRepository webhookRepository;
    
    @Autowired
    private MetricsService metricsService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${app.webhook.retry.max-attempts:5}")
    private int maxRetryAttempts;
    
    @Value("${app.webhook.retry.initial-delay-minutes:1}")
    private int initialDelayMinutes;
    
    @Value("${app.webhook.retry.max-delay-minutes:1440}") // 24 hours
    private int maxDelayMinutes;
    
    @Value("${app.webhook.retry.multiplier:2.0}")
    private double backoffMultiplier;
    
    @Value("${app.webhook.retry.jitter-enabled:true}")
    private boolean jitterEnabled;
    
    @Value("${app.webhook.retry.timeout-seconds:30}")
    private int timeoutSeconds;
    
    @Value("${app.webhook.cleanup.enabled:true}")
    private boolean cleanupEnabled;
    
    @Value("${app.webhook.cleanup.delivered-retention-days:7}")
    private int deliveredRetentionDays;
    
    @Value("${app.webhook.cleanup.failed-retention-days:30}")
    private int failedRetentionDays;
    
    // Circuit breaker state for problematic endpoints
    private final Map<String, EndpointCircuitBreaker> circuitBreakers = new HashMap<>();
    
    /**
     * Processes webhook delivery retries on a scheduled basis.
     * Runs every 5 minutes to check for webhooks ready for retry.
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void processRetries() {
        try {
            log.debug("Starting scheduled webhook retry processing");
            
            List<Webhook> webhooksToRetry = webhookRepository.findWebhooksReadyForRetry();
            
            if (webhooksToRetry.isEmpty()) {
                log.debug("No webhooks ready for retry");
                return;
            }
            
            log.info("Found {} webhooks ready for retry", webhooksToRetry.size());
            
            for (Webhook webhook : webhooksToRetry) {
                try {
                    retryWebhookDeliveryAsync(webhook);
                } catch (Exception e) {
                    log.error("Error scheduling retry for webhook {}: {}", 
                                webhook.getWebhookId(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error in scheduled retry processing: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Processes webhook delivery cleanup on a scheduled basis.
     * Runs daily at 2 AM to clean up old webhook records.
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void cleanupOldWebhooks() {
        if (!cleanupEnabled) {
            log.debug("Webhook cleanup is disabled");
            return;
        }
        
        try {
            log.info("Starting webhook cleanup process");
            
            ZonedDateTime deliveredCutoff = ZonedDateTime.now().minusDays(deliveredRetentionDays);
            ZonedDateTime failedCutoff = ZonedDateTime.now().minusDays(failedRetentionDays);
            
            List<Webhook> webhooksToCleanup = webhookRepository.findWebhooksForCleanup(
                deliveredCutoff, failedCutoff);
            
            if (!webhooksToCleanup.isEmpty()) {
                log.info("Cleaning up {} old webhook records", webhooksToCleanup.size());
                
                for (Webhook webhook : webhooksToCleanup) {
                    webhookRepository.delete(webhook);
                }
                
                metricsService.incrementWebhookCleanup(webhooksToCleanup.size());
            }
            
            log.info("Webhook cleanup completed");
            
        } catch (Exception e) {
            log.error("Error in webhook cleanup process: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Retries webhook delivery asynchronously.
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> retryWebhookDeliveryAsync(Webhook webhook) {
        String correlationId = webhook.getCorrelationId();
        if (correlationId == null) {
            correlationId = CorrelationIdUtil.generate();
        }
        
        try {
            MDC.put("correlationId", correlationId);
            
            log.info("Starting webhook retry - WebhookID: {}, Attempt: {}/{}, EndpointURL: {}", 
                       webhook.getWebhookId(), webhook.getAttempts() + 1, 
                       webhook.getMaxAttempts(), webhook.getEndpointUrl());
            
            // Check circuit breaker for this endpoint
            EndpointCircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(webhook.getEndpointUrl());
            if (circuitBreaker.isOpen()) {
                log.warn("Circuit breaker is open for endpoint: {}, skipping retry", 
                           webhook.getEndpointUrl());
                scheduleNextRetry(webhook, "Circuit breaker is open");
                return CompletableFuture.completedFuture(null);
            }
            
            // Attempt webhook delivery
            deliverWebhook(webhook);
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Error in async webhook retry - WebhookID: {}, Error: {}", 
                        webhook.getWebhookId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(null);
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Delivers webhook to the target endpoint.
     */
    @Retryable(value = {ResourceAccessException.class}, 
               maxAttempts = 2, 
               backoff = @Backoff(delay = 1000))
    public void deliverWebhook(Webhook webhook) {
        try {
            // Mark webhook as processing
            webhook.markAsProcessing();
            webhookRepository.save(webhook);
            
            // Prepare HTTP request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Add custom headers from webhook configuration
            if (webhook.getRequestHeaders() != null) {
                for (Map.Entry<String, Object> entry : webhook.getRequestHeaders().entrySet()) {
                    headers.add(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            
            // Add correlation ID and timestamp
            headers.add("X-Correlation-ID", webhook.getCorrelationId());
            headers.add("X-Webhook-ID", webhook.getWebhookId());
            headers.add("X-Event-Type", webhook.getEventType());
            headers.add("X-Attempt", String.valueOf(webhook.getAttempts() + 1));
            headers.add("X-Timestamp", String.valueOf(System.currentTimeMillis()));
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(webhook.getRequestBody(), headers);
            
            // Make HTTP request
            ResponseEntity<String> response = restTemplate.exchange(
                webhook.getEndpointUrl(),
                HttpMethod.valueOf(webhook.getHttpMethod()),
                entity,
                String.class
            );
            
            // Process successful response
            handleSuccessfulDelivery(webhook, response);
            
            // Record success in circuit breaker
            EndpointCircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(webhook.getEndpointUrl());
            circuitBreaker.recordSuccess();
            
        } catch (HttpClientErrorException e) {
            handleClientError(webhook, e);
        } catch (HttpServerErrorException e) {
            handleServerError(webhook, e);
        } catch (ResourceAccessException e) {
            handleTimeoutError(webhook, e);
        } catch (Exception e) {
            handleUnknownError(webhook, e);
        }
    }
    
    /**
     * Handles successful webhook delivery.
     */
    private void handleSuccessfulDelivery(Webhook webhook, ResponseEntity<String> response) {
        Map<String, Object> responseHeaders = new HashMap<>();
        response.getHeaders().forEach((key, values) -> 
            responseHeaders.put(key, values.size() == 1 ? values.get(0) : values));
        
        webhook.markAsDelivered(response.getStatusCodeValue(), responseHeaders, response.getBody());
        webhookRepository.save(webhook);
        
        log.info("Webhook delivered successfully - WebhookID: {}, StatusCode: {}, Attempt: {}", 
                   webhook.getWebhookId(), response.getStatusCodeValue(), webhook.getAttempts());
        
        metricsService.incrementWebhookDeliverySuccess(webhook.getEventType(), webhook.getAttempts());
    }
    
    /**
     * Handles client errors (4xx status codes).
     */
    private void handleClientError(Webhook webhook, HttpClientErrorException e) {
        String errorMessage = String.format("Client error: %d %s", 
                                           e.getStatusCode().value(), e.getStatusText());
        
        Map<String, Object> responseHeaders = new HashMap<>();
        if (e.getResponseHeaders() != null) {
            e.getResponseHeaders().forEach((key, values) -> 
                responseHeaders.put(key, values.size() == 1 ? values.get(0) : values));
        }
        
        // For client errors, don't retry (except for specific cases like 429 Too Many Requests)
        if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            scheduleNextRetry(webhook, errorMessage);
        } else {
            webhook.markAsFailedWithResponse(e.getStatusCode().value(), responseHeaders, 
                                           e.getResponseBodyAsString(), errorMessage);
            webhook.setMaxAttempts(webhook.getAttempts() + 1); // Stop retrying
            webhookRepository.save(webhook);
        }
        
        log.warn("Webhook client error - WebhookID: {}, StatusCode: {}, Error: {}", 
                   webhook.getWebhookId(), e.getStatusCode().value(), errorMessage);
        
        metricsService.incrementWebhookDeliveryFailure(webhook.getEventType(), 
                                                      webhook.getAttempts(), "client_error");
    }
    
    /**
     * Handles server errors (5xx status codes).
     */
    private void handleServerError(Webhook webhook, HttpServerErrorException e) {
        String errorMessage = String.format("Server error: %d %s", 
                                           e.getStatusCode().value(), e.getStatusText());
        
        Map<String, Object> responseHeaders = new HashMap<>();
        if (e.getResponseHeaders() != null) {
            e.getResponseHeaders().forEach((key, values) -> 
                responseHeaders.put(key, values.size() == 1 ? values.get(0) : values));
        }
        
        webhook.markAsFailedWithResponse(e.getStatusCode().value(), responseHeaders, 
                                       e.getResponseBodyAsString(), errorMessage);
        
        // Schedule retry for server errors
        scheduleNextRetry(webhook, errorMessage);
        
        log.warn("Webhook server error - WebhookID: {}, StatusCode: {}, Attempt: {}/{}", 
                   webhook.getWebhookId(), e.getStatusCode().value(), 
                   webhook.getAttempts(), webhook.getMaxAttempts());
        
        // Record failure in circuit breaker
        EndpointCircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(webhook.getEndpointUrl());
        circuitBreaker.recordFailure();
        
        metricsService.incrementWebhookDeliveryFailure(webhook.getEventType(), 
                                                      webhook.getAttempts(), "server_error");
    }
    
    /**
     * Handles timeout errors.
     */
    private void handleTimeoutError(Webhook webhook, ResourceAccessException e) {
        String errorMessage = "Timeout error: " + e.getMessage();
        
        webhook.markAsFailed(errorMessage);
        scheduleNextRetry(webhook, errorMessage);
        
        log.warn("Webhook timeout error - WebhookID: {}, Attempt: {}/{}, Error: {}", 
                   webhook.getWebhookId(), webhook.getAttempts(), 
                   webhook.getMaxAttempts(), e.getMessage());
        
        // Record failure in circuit breaker
        EndpointCircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(webhook.getEndpointUrl());
        circuitBreaker.recordFailure();
        
        metricsService.incrementWebhookDeliveryFailure(webhook.getEventType(), 
                                                      webhook.getAttempts(), "timeout");
    }
    
    /**
     * Handles unknown/unexpected errors.
     */
    private void handleUnknownError(Webhook webhook, Exception e) {
        String errorMessage = "Unknown error: " + e.getMessage();
        
        webhook.markAsFailed(errorMessage);
        scheduleNextRetry(webhook, errorMessage);
        
        log.error("Webhook unknown error - WebhookID: {}, Attempt: {}/{}, Error: {}", 
                    webhook.getWebhookId(), webhook.getAttempts(), 
                    webhook.getMaxAttempts(), e.getMessage(), e);
        
        metricsService.incrementWebhookDeliveryFailure(webhook.getEventType(), 
                                                      webhook.getAttempts(), "unknown");
    }
    
    /**
     * Schedules the next retry attempt with exponential backoff.
     */
    private void scheduleNextRetry(Webhook webhook, String errorMessage) {
        if (webhook.getAttempts() >= webhook.getMaxAttempts()) {
            webhook.setStatus(WebhookStatus.FAILED);
            webhook.setNextAttemptAt(null);
            log.warn("Webhook max attempts reached - WebhookID: {}, giving up", 
                       webhook.getWebhookId());
            metricsService.incrementWebhookMaxAttemptsReached(webhook.getEventType());
        } else {
            webhook.setStatus(WebhookStatus.RETRYING);
            
            // Calculate next retry time with exponential backoff
            int delayMinutes = calculateBackoffDelay(webhook.getAttempts());
            ZonedDateTime nextAttempt = ZonedDateTime.now().plusMinutes(delayMinutes);
            webhook.setNextAttemptAt(nextAttempt);
            
            log.info("Scheduled webhook retry - WebhookID: {}, NextAttempt: {}, DelayMinutes: {}", 
                       webhook.getWebhookId(), nextAttempt, delayMinutes);
        }
        
        webhookRepository.save(webhook);
    }
    
    /**
     * Calculates exponential backoff delay with optional jitter.
     */
    private int calculateBackoffDelay(int attemptNumber) {
        // Calculate exponential backoff: initial * (multiplier ^ attempt)
        double delay = initialDelayMinutes * Math.pow(backoffMultiplier, attemptNumber);
        
        // Apply maximum delay limit
        delay = Math.min(delay, maxDelayMinutes);
        
        // Add jitter to prevent thundering herd
        if (jitterEnabled) {
            double jitter = delay * 0.1; // 10% jitter
            delay += ThreadLocalRandom.current().nextDouble(-jitter, jitter);
        }
        
        return Math.max(1, (int) delay); // Minimum 1 minute delay
    }
    
    /**
     * Gets or creates a circuit breaker for an endpoint.
     */
    private EndpointCircuitBreaker getOrCreateCircuitBreaker(String endpointUrl) {
        return circuitBreakers.computeIfAbsent(endpointUrl, 
            url -> new EndpointCircuitBreaker(url, 5, Duration.ofMinutes(5)));
    }
    
    /**
     * Gets retry statistics for monitoring.
     */
    public Map<String, Object> getRetryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        ZonedDateTime last24Hours = ZonedDateTime.now().minusHours(24);
        
        stats.put("pendingRetries", webhookRepository.countByStatus(WebhookStatus.RETRYING));
        stats.put("failedWebhooks", webhookRepository.countByStatus(WebhookStatus.FAILED));
        stats.put("webhooksReadyForRetry", webhookRepository.findWebhooksReadyForRetry().size());
        
        // Circuit breaker states
        Map<String, Object> circuitBreakerStates = new HashMap<>();
        for (Map.Entry<String, EndpointCircuitBreaker> entry : circuitBreakers.entrySet()) {
            EndpointCircuitBreaker cb = entry.getValue();
            Map<String, Object> cbState = new HashMap<>();
            cbState.put("state", cb.isOpen() ? "OPEN" : "CLOSED");
            cbState.put("failures", cb.getFailureCount());
            cbState.put("lastFailure", cb.getLastFailureTime());
            circuitBreakerStates.put(entry.getKey(), cbState);
        }
        stats.put("circuitBreakers", circuitBreakerStates);
        
        return stats;
    }
    
    /**
     * Simple circuit breaker implementation for webhook endpoints.
     */
    private static class EndpointCircuitBreaker {
        private final String endpointUrl;
        private final int failureThreshold;
        private final Duration timeout;
        private int failureCount = 0;
        private ZonedDateTime lastFailureTime;
        private boolean isOpen = false;
        
        public EndpointCircuitBreaker(String endpointUrl, int failureThreshold, Duration timeout) {
            this.endpointUrl = endpointUrl;
            this.failureThreshold = failureThreshold;
            this.timeout = timeout;
        }
        
        public void recordSuccess() {
            this.failureCount = 0;
            this.isOpen = false;
        }
        
        public void recordFailure() {
            this.failureCount++;
            this.lastFailureTime = ZonedDateTime.now();
            
            if (this.failureCount >= this.failureThreshold) {
                this.isOpen = true;
            }
        }
        
        public boolean isOpen() {
            if (isOpen && lastFailureTime != null) {
                // Check if timeout period has passed
                if (ZonedDateTime.now().isAfter(lastFailureTime.plus(timeout))) {
                    isOpen = false; // Half-open state
                    failureCount = 0;
                }
            }
            return isOpen;
        }
        
        public int getFailureCount() {
            return failureCount;
        }
        
        public ZonedDateTime getLastFailureTime() {
            return lastFailureTime;
        }
    }
}
