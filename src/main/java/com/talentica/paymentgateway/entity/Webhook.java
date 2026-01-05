package com.talentica.paymentgateway.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing a webhook delivery attempt.
 * Webhooks are used to notify external systems about payment events.
 */
@Entity
@Table(name = "webhooks",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "webhookId")
       },
       indexes = {
           @Index(name = "idx_webhooks_status", columnList = "status"),
           @Index(name = "idx_webhooks_event_type", columnList = "eventType"),
           @Index(name = "idx_webhooks_scheduled_at", columnList = "scheduledAt"),
           @Index(name = "idx_webhooks_next_attempt_at", columnList = "nextAttemptAt"),
           @Index(name = "idx_webhooks_correlation_id", columnList = "correlationId")
       })
public class Webhook extends BaseEntity {

    @NotBlank(message = "Webhook ID is required")
    @Size(max = 100, message = "Webhook ID must not exceed 100 characters")
    @Column(name = "webhook_id", nullable = false, unique = true, length = 100)
    private String webhookId;

    @NotBlank(message = "Event type is required")
    @Size(max = 100, message = "Event type must not exceed 100 characters")
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @NotBlank(message = "Event ID is required")
    @Size(max = 100, message = "Event ID must not exceed 100 characters")
    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @NotBlank(message = "Endpoint URL is required")
    @Size(max = 500, message = "Endpoint URL must not exceed 500 characters")
    @Column(name = "endpoint_url", nullable = false, length = 500)
    private String endpointUrl;

    @Size(max = 10, message = "HTTP method must not exceed 10 characters")
    @Column(name = "http_method", length = 10)
    private String httpMethod = "POST";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private WebhookStatus status = WebhookStatus.PENDING;

    @Min(value = 0, message = "Attempts must be non-negative")
    @Column(name = "attempts")
    private Integer attempts = 0;

    @Min(value = 1, message = "Max attempts must be at least 1")
    @Column(name = "max_attempts")
    private Integer maxAttempts = 5;

    @Column(name = "scheduled_at")
    private ZonedDateTime scheduledAt;

    @Column(name = "next_attempt_at")
    private ZonedDateTime nextAttemptAt;

    @Column(name = "delivered_at")
    private ZonedDateTime deliveredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_headers", columnDefinition = "JSONB")
    private Map<String, Object> requestHeaders = new HashMap<>();

    @NotNull(message = "Request body is required")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_body", nullable = false, columnDefinition = "JSONB")
    private Map<String, Object> requestBody = new HashMap<>();

    @Column(name = "response_status_code")
    private Integer responseStatusCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_headers", columnDefinition = "JSONB")
    private Map<String, Object> responseHeaders = new HashMap<>();

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Size(max = 100, message = "Correlation ID must not exceed 100 characters")
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    // Constructors
    public Webhook() {
        super();
        this.scheduledAt = ZonedDateTime.now();
        this.nextAttemptAt = ZonedDateTime.now();
    }

    public Webhook(String webhookId, String eventType, String eventId, String endpointUrl, Map<String, Object> requestBody) {
        this();
        this.webhookId = webhookId;
        this.eventType = eventType;
        this.eventId = eventId;
        this.endpointUrl = endpointUrl;
        this.requestBody = requestBody;
    }

    // Getters and Setters
    public String getWebhookId() {
        return webhookId;
    }

    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public WebhookStatus getStatus() {
        return status;
    }

    public void setStatus(WebhookStatus status) {
        this.status = status;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public ZonedDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(ZonedDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public ZonedDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(ZonedDateTime nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public ZonedDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(ZonedDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Map<String, Object> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, Object> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public Map<String, Object> getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(Map<String, Object> requestBody) {
        this.requestBody = requestBody;
    }

    public Integer getResponseStatusCode() {
        return responseStatusCode;
    }

    public void setResponseStatusCode(Integer responseStatusCode) {
        this.responseStatusCode = responseStatusCode;
    }

    public Map<String, Object> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, Object> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    // Utility methods
    public boolean isDelivered() {
        return status == WebhookStatus.DELIVERED;
    }

    public boolean isFailed() {
        return status == WebhookStatus.FAILED;
    }

    public boolean isPending() {
        return status == WebhookStatus.PENDING;
    }

    public boolean isRetrying() {
        return status == WebhookStatus.RETRYING;
    }

    public boolean canRetry() {
        return attempts < maxAttempts && 
               (status == WebhookStatus.FAILED || status == WebhookStatus.RETRYING) &&
               nextAttemptAt != null && 
               ZonedDateTime.now().isAfter(nextAttemptAt);
    }

    public void markAsProcessing() {
        this.status = WebhookStatus.PROCESSING;
    }

    public void markAsDelivered(int responseStatusCode, Map<String, Object> responseHeaders, String responseBody) {
        this.status = WebhookStatus.DELIVERED;
        this.deliveredAt = ZonedDateTime.now();
        this.responseStatusCode = responseStatusCode;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.errorMessage = null;
        this.nextAttemptAt = null;
    }

    public void markAsFailed(String errorMessage) {
        this.attempts++;
        if (attempts >= maxAttempts) {
            this.status = WebhookStatus.FAILED;
            this.nextAttemptAt = null;
        } else {
            this.status = WebhookStatus.RETRYING;
            scheduleNextAttempt();
        }
        this.errorMessage = errorMessage;
    }

    public void markAsFailedWithResponse(int responseStatusCode, Map<String, Object> responseHeaders, 
                                       String responseBody, String errorMessage) {
        this.responseStatusCode = responseStatusCode;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        markAsFailed(errorMessage);
    }

    private void scheduleNextAttempt() {
        // Exponential backoff: 1 min, 5 min, 15 min, 60 min, 240 min
        int[] retryMinutes = {1, 5, 15, 60, 240};
        int minuteIndex = Math.min(attempts - 1, retryMinutes.length - 1);
        this.nextAttemptAt = ZonedDateTime.now().plusMinutes(retryMinutes[minuteIndex]);
    }

    public boolean isSuccessfulResponse() {
        return responseStatusCode != null && 
               responseStatusCode >= 200 && 
               responseStatusCode < 300;
    }

    public int getRemainingAttempts() {
        return Math.max(0, maxAttempts - attempts);
    }

    public String getNextAttemptDescription() {
        if (nextAttemptAt == null) {
            return "No more attempts scheduled";
        }
        
        long minutesUntilNext = java.time.temporal.ChronoUnit.MINUTES.between(ZonedDateTime.now(), nextAttemptAt);
        if (minutesUntilNext <= 0) {
            return "Ready to retry";
        } else if (minutesUntilNext < 60) {
            return String.format("Next attempt in %d minutes", minutesUntilNext);
        } else {
            long hoursUntilNext = minutesUntilNext / 60;
            return String.format("Next attempt in %d hours", hoursUntilNext);
        }
    }

    public void addRequestHeader(String key, Object value) {
        if (requestHeaders == null) {
            requestHeaders = new HashMap<>();
        }
        requestHeaders.put(key, value);
    }

    public void setStandardHeaders() {
        addRequestHeader("Content-Type", "application/json");
        addRequestHeader("User-Agent", "PaymentGateway-Webhook/1.0");
        if (correlationId != null) {
            addRequestHeader("X-Correlation-ID", correlationId);
        }
    }
}
