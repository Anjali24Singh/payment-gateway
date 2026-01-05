package com.talentica.paymentgateway.dto.webhook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

/**
 * DTO for webhook response.
 * Represents the response sent back to webhook callers.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhookResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("eventId")
    private String eventId;
    
    @JsonProperty("processedAt")
    private ZonedDateTime processedAt;
    
    @JsonProperty("correlationId")
    private String correlationId;
    
    @JsonProperty("error")
    private WebhookError error;
    
    // Default constructor
    public WebhookResponse() {
        this.processedAt = ZonedDateTime.now();
    }
    
    // Success constructor
    public WebhookResponse(String eventId, String correlationId) {
        this();
        this.status = "success";
        this.message = "Webhook processed successfully";
        this.eventId = eventId;
        this.correlationId = correlationId;
    }
    
    // Error constructor
    public WebhookResponse(String status, String message, String eventId, 
                          String correlationId, WebhookError error) {
        this();
        this.status = status;
        this.message = message;
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.error = error;
    }
    
    // Static factory methods
    public static WebhookResponse success(String eventId, String correlationId) {
        return new WebhookResponse(eventId, correlationId);
    }
    
    public static WebhookResponse success(String eventId, String correlationId, String message) {
        WebhookResponse response = new WebhookResponse(eventId, correlationId);
        response.setMessage(message);
        return response;
    }
    
    public static WebhookResponse error(String eventId, String correlationId, String message) {
        return new WebhookResponse("error", message, eventId, correlationId, null);
    }
    
    public static WebhookResponse error(String eventId, String correlationId, 
                                      String message, WebhookError error) {
        return new WebhookResponse("error", message, eventId, correlationId, error);
    }
    
    public static WebhookResponse validationError(String eventId, String correlationId, 
                                                String message, WebhookError error) {
        return new WebhookResponse("validation_error", message, eventId, correlationId, error);
    }
    
    public static WebhookResponse duplicateEvent(String eventId, String correlationId) {
        return new WebhookResponse("duplicate", "Event already processed", eventId, correlationId, null);
    }
    
    public static WebhookResponse signatureError(String eventId, String correlationId) {
        WebhookError error = new WebhookError("SIGNATURE_INVALID", 
                                            "Webhook signature verification failed", 
                                            "Verify webhook signature configuration");
        return new WebhookResponse("signature_error", "Invalid signature", 
                                 eventId, correlationId, error);
    }
    
    public static WebhookResponse processingError(String eventId, String correlationId, 
                                                String message, Exception exception) {
        WebhookError error = new WebhookError("PROCESSING_ERROR", 
                                            exception.getMessage(), 
                                            "Check application logs for details");
        return new WebhookResponse("processing_error", message, eventId, correlationId, error);
    }
    
    // Getters and Setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public ZonedDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(ZonedDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public WebhookError getError() {
        return error;
    }
    
    public void setError(WebhookError error) {
        this.error = error;
    }
    
    // Utility methods
    public boolean isSuccess() {
        return "success".equals(status);
    }
    
    public boolean isError() {
        return status != null && status.contains("error");
    }
    
    public boolean isDuplicate() {
        return "duplicate".equals(status);
    }
    
    @Override
    public String toString() {
        return "WebhookResponse{" +
               "status='" + status + '\'' +
               ", message='" + message + '\'' +
               ", eventId='" + eventId + '\'' +
               ", correlationId='" + correlationId + '\'' +
               ", processedAt=" + processedAt +
               '}';
    }
    
    /**
     * Nested class representing webhook error details.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WebhookError {
        
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("suggestion")
        private String suggestion;
        
        @JsonProperty("timestamp")
        private ZonedDateTime timestamp;
        
        // Default constructor
        public WebhookError() {
            this.timestamp = ZonedDateTime.now();
        }
        
        // Parameterized constructor
        public WebhookError(String code, String description, String suggestion) {
            this();
            this.code = code;
            this.description = description;
            this.suggestion = suggestion;
        }
        
        // Getters and Setters
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getSuggestion() {
            return suggestion;
        }
        
        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
        
        public ZonedDateTime getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(ZonedDateTime timestamp) {
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return "WebhookError{" +
                   "code='" + code + '\'' +
                   ", description='" + description + '\'' +
                   ", timestamp=" + timestamp +
                   '}';
        }
    }
}
