package com.talentica.paymentgateway.dto.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Payment error response DTO for detailed error information.
 * Provides comprehensive error details for failed payment transactions.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Schema(description = "Error information for failed payment transactions")
public class PaymentErrorResponse {

    @Schema(description = "Error code", example = "PAYMENT_DECLINED")
    @JsonProperty("code")
    private String code;

    @Schema(description = "Error message", example = "The credit card was declined")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Detailed error description", example = "The transaction was declined by the issuing bank")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Error category", example = "CARD_ERROR")
    @JsonProperty("category")
    private String category;

    @Schema(description = "Authorize.Net response code", example = "2")
    @JsonProperty("gatewayCode")
    private String gatewayCode;

    @Schema(description = "Authorize.Net response reason code", example = "27")
    @JsonProperty("gatewayReasonCode")
    private String gatewayReasonCode;

    @Schema(description = "Gateway response text", example = "The transaction resulted in an AVS mismatch")
    @JsonProperty("gatewayReasonText")
    private String gatewayReasonText;

    @Schema(description = "Decline reason", example = "INSUFFICIENT_FUNDS")
    @JsonProperty("declineReason")
    private String declineReason;

    @Schema(description = "Retry possible indicator", example = "false")
    @JsonProperty("retryable")
    private Boolean retryable;

    @Schema(description = "Suggested action for merchant", example = "Ask customer to use a different payment method")
    @JsonProperty("suggestedAction")
    private String suggestedAction;

    @Schema(description = "Correlation ID for error tracking", example = "corr-123456789")
    @JsonProperty("correlationId")
    private String correlationId;

    @Schema(description = "Error timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;

    @Schema(description = "Additional error details")
    @JsonProperty("details")
    private List<ErrorDetail> details;

    // Default constructor
    public PaymentErrorResponse() {
        this.timestamp = ZonedDateTime.now();
    }

    // Constructor with basic error info
    public PaymentErrorResponse(String code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    // Constructor with detailed error info
    public PaymentErrorResponse(String code, String message, String description, 
                               String category, String correlationId) {
        this();
        this.code = code;
        this.message = message;
        this.description = description;
        this.category = category;
        this.correlationId = correlationId;
    }

    // Static factory methods for common error types
    public static PaymentErrorResponse cardDeclined(String gatewayReasonText, String correlationId) {
        PaymentErrorResponse error = new PaymentErrorResponse(
            "CARD_DECLINED",
            "The credit card was declined",
            gatewayReasonText,
            "CARD_ERROR",
            correlationId
        );
        error.setRetryable(false);
        error.setSuggestedAction("Ask customer to use a different payment method");
        return error;
    }

    public static PaymentErrorResponse insufficientFunds(String correlationId) {
        PaymentErrorResponse error = new PaymentErrorResponse(
            "INSUFFICIENT_FUNDS",
            "Insufficient funds in the account",
            "The transaction was declined due to insufficient funds",
            "CARD_ERROR",
            correlationId
        );
        error.setDeclineReason("INSUFFICIENT_FUNDS");
        error.setRetryable(false);
        error.setSuggestedAction("Ask customer to check account balance or use a different payment method");
        return error;
    }

    public static PaymentErrorResponse invalidCard(String correlationId) {
        PaymentErrorResponse error = new PaymentErrorResponse(
            "INVALID_CARD",
            "Invalid card information",
            "The credit card number or expiration date is invalid",
            "CARD_ERROR",
            correlationId
        );
        error.setRetryable(true);
        error.setSuggestedAction("Verify card information and try again");
        return error;
    }

    public static PaymentErrorResponse processingError(String message, String correlationId) {
        PaymentErrorResponse error = new PaymentErrorResponse(
            "PROCESSING_ERROR",
            "Payment processing error",
            message,
            "GATEWAY_ERROR",
            correlationId
        );
        error.setRetryable(true);
        error.setSuggestedAction("Please try again later");
        return error;
    }

    public static PaymentErrorResponse networkError(String correlationId) {
        PaymentErrorResponse error = new PaymentErrorResponse(
            "NETWORK_ERROR",
            "Network communication error",
            "Unable to communicate with payment processor",
            "NETWORK_ERROR",
            correlationId
        );
        error.setRetryable(true);
        error.setSuggestedAction("Please try again in a few minutes");
        return error;
    }

    public static PaymentErrorResponse validationError(String message, String correlationId) {
        PaymentErrorResponse error = new PaymentErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            message,
            "VALIDATION_ERROR",
            correlationId
        );
        error.setRetryable(true);
        error.setSuggestedAction("Please check the request data and try again");
        return error;
    }

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getGatewayCode() {
        return gatewayCode;
    }

    public void setGatewayCode(String gatewayCode) {
        this.gatewayCode = gatewayCode;
    }

    public String getGatewayReasonCode() {
        return gatewayReasonCode;
    }

    public void setGatewayReasonCode(String gatewayReasonCode) {
        this.gatewayReasonCode = gatewayReasonCode;
    }

    public String getGatewayReasonText() {
        return gatewayReasonText;
    }

    public void setGatewayReasonText(String gatewayReasonText) {
        this.gatewayReasonText = gatewayReasonText;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }

    public Boolean getRetryable() {
        return retryable;
    }

    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<ErrorDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ErrorDetail> details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "PaymentErrorResponse{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", category='" + category + '\'' +
                ", gatewayCode='" + gatewayCode + '\'' +
                ", gatewayReasonCode='" + gatewayReasonCode + '\'' +
                ", retryable=" + retryable +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }

    /**
     * Additional error detail information.
     */
    @Schema(description = "Additional error detail")
    public static class ErrorDetail {
        
        @Schema(description = "Detail field name", example = "cardNumber")
        @JsonProperty("field")
        private String field;

        @Schema(description = "Detail error code", example = "INVALID_FORMAT")
        @JsonProperty("code")
        private String code;

        @Schema(description = "Detail error message", example = "Card number format is invalid")
        @JsonProperty("message")
        private String message;

        public ErrorDetail() {}

        public ErrorDetail(String field, String code, String message) {
            this.field = field;
            this.code = code;
            this.message = message;
        }

        // Getters and Setters
        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "ErrorDetail{" +
                    "field='" + field + '\'' +
                    ", code='" + code + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
