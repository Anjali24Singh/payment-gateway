package com.talentica.paymentgateway.dto.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response format for all API endpoints.
 * Provides consistent error information with correlation tracking and detailed validation errors.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response format")
public class StandardErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Unique error code", example = "VALIDATION_ERROR")
    private String errorCode;

    @Schema(description = "Human-readable error message", example = "Request validation failed")
    private String message;

    @Schema(description = "Detailed error description")
    private String description;

    @Schema(description = "Error category", example = "VALIDATION_ERROR", 
            allowableValues = {"VALIDATION_ERROR", "AUTHENTICATION_ERROR", "AUTHORIZATION_ERROR", 
                              "PAYMENT_ERROR", "BUSINESS_ERROR", "SYSTEM_ERROR", "NETWORK_ERROR"})
    private String category;

    @Schema(description = "Correlation ID for tracking", example = "corr-12345678")
    private String correlationId;

    @Schema(description = "Error timestamp", example = "2025-09-10T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "Request path where error occurred", example = "/api/v1/payments/purchase")
    private String path;

    @Schema(description = "HTTP method", example = "POST")
    private String method;

    @Schema(description = "Detailed validation errors")
    private List<ValidationError> validationErrors;

    @Schema(description = "Additional error context")
    private Object additionalInfo;

    public StandardErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public StandardErrorResponse(int status, String errorCode, String message) {
        this();
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
    }

    public StandardErrorResponse(int status, String errorCode, String message, String correlationId) {
        this(status, errorCode, message);
        this.correlationId = correlationId;
    }

    // Static factory methods
    public static StandardErrorResponse validation(String message, String correlationId) {
        StandardErrorResponse error = new StandardErrorResponse(400, "VALIDATION_ERROR", message, correlationId);
        error.setCategory("VALIDATION_ERROR");
        error.setDescription("Request validation failed due to invalid input parameters");
        return error;
    }

    public static StandardErrorResponse authentication(String message, String correlationId) {
        StandardErrorResponse error = new StandardErrorResponse(401, "AUTHENTICATION_ERROR", message, correlationId);
        error.setCategory("AUTHENTICATION_ERROR");
        error.setDescription("Authentication failed - invalid or missing credentials");
        return error;
    }

    public static StandardErrorResponse authorization(String message, String correlationId) {
        StandardErrorResponse error = new StandardErrorResponse(403, "AUTHORIZATION_ERROR", message, correlationId);
        error.setCategory("AUTHORIZATION_ERROR");
        error.setDescription("Authorization failed - insufficient permissions");
        return error;
    }

    public static StandardErrorResponse notFound(String message, String correlationId) {
        StandardErrorResponse error = new StandardErrorResponse(404, "NOT_FOUND", message, correlationId);
        error.setCategory("BUSINESS_ERROR");
        error.setDescription("Requested resource was not found");
        return error;
    }

    public static StandardErrorResponse paymentError(String message, String correlationId) {
        StandardErrorResponse error = new StandardErrorResponse(422, "PAYMENT_ERROR", message, correlationId);
        error.setCategory("PAYMENT_ERROR");
        error.setDescription("Payment processing failed");
        return error;
    }

    public static StandardErrorResponse businessError(String errorCode, String message, String correlationId) {
        StandardErrorResponse error = new StandardErrorResponse(422, errorCode, message, correlationId);
        error.setCategory("BUSINESS_ERROR");
        error.setDescription("Business rule validation failed");
        return error;
    }

    public static StandardErrorResponse systemError(String message, String correlationId) {
        StandardErrorResponse error = new StandardErrorResponse(500, "SYSTEM_ERROR", message, correlationId);
        error.setCategory("SYSTEM_ERROR");
        error.setDescription("An unexpected system error occurred");
        return error;
    }

    public static StandardErrorResponse rateLimitExceeded(String message, String correlationId) {
        StandardErrorResponse error = new StandardErrorResponse(429, "RATE_LIMIT_EXCEEDED", message, correlationId);
        error.setCategory("SYSTEM_ERROR");
        error.setDescription("Rate limit exceeded - too many requests");
        return error;
    }

    // Getters and setters
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public List<ValidationError> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(List<ValidationError> validationErrors) { this.validationErrors = validationErrors; }
    public Object getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(Object additionalInfo) { this.additionalInfo = additionalInfo; }

    /**
     * Validation error detail.
     */
    @Schema(description = "Validation error detail")
    public static class ValidationError {
        @Schema(description = "Field name that failed validation", example = "amount")
        private String field;

        @Schema(description = "Validation error code", example = "INVALID_VALUE")
        private String code;

        @Schema(description = "Validation error message", example = "Amount must be greater than 0")
        private String message;

        @Schema(description = "Invalid value provided")
        private Object rejectedValue;

        public ValidationError() {}

        public ValidationError(String field, String code, String message) {
            this.field = field;
            this.code = code;
            this.message = message;
        }

        public ValidationError(String field, String code, String message, Object rejectedValue) {
            this(field, code, message);
            this.rejectedValue = rejectedValue;
        }

        // Getters and setters
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getRejectedValue() { return rejectedValue; }
        public void setRejectedValue(Object rejectedValue) { this.rejectedValue = rejectedValue; }
    }
}
