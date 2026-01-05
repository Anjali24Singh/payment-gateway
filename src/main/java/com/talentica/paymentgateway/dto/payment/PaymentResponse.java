package com.talentica.paymentgateway.dto.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Payment response DTO containing transaction results from Authorize.Net.
 * Provides comprehensive information about payment processing outcomes.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@Schema(description = "Payment response containing transaction results")
public class PaymentResponse {

    @Schema(description = "Internal transaction ID", example = "txn_1234567890")
    @JsonProperty("transactionId")
    private String transactionId;

    @Schema(description = "Authorize.Net transaction ID", example = "40000001234")
    @JsonProperty("authnetTransactionId")
    private String authnetTransactionId;

    @Schema(description = "Transaction status", example = "AUTHORIZED")
    @JsonProperty("status")
    private String status;

    @Schema(description = "Transaction type", example = "PURCHASE")
    @JsonProperty("transactionType")
    private String transactionType;

    @Schema(description = "Transaction amount", example = "99.99")
    @JsonProperty("amount")
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "USD")
    @JsonProperty("currency")
    private String currency;

    @Schema(description = "Response message", example = "Transaction successful")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Authorization code from payment processor", example = "ABC123")
    @JsonProperty("authorizationCode")
    private String authorizationCode;

    @Schema(description = "Address Verification Service result", example = "Y")
    @JsonProperty("avsResult")
    private String avsResult;

    @Schema(description = "Card Verification Value result", example = "M")
    @JsonProperty("cvvResult")
    private String cvvResult;

    @Schema(description = "Response code from payment processor", example = "1")
    @JsonProperty("responseCode")
    private String responseCode;

    @Schema(description = "Response reason code", example = "1")
    @JsonProperty("responseReasonCode")
    private String responseReasonCode;

    @Schema(description = "Response reason text", example = "This transaction has been approved.")
    @JsonProperty("responseReasonText")
    private String responseReasonText;

    @Schema(description = "Success indicator", example = "true")
    @JsonProperty("success")
    private Boolean success;

    @Schema(description = "Test mode indicator", example = "false")
    @JsonProperty("testMode")
    private Boolean testMode;

    @Schema(description = "Gateway reference ID", example = "REF123456")
    @JsonProperty("gatewayReference")
    private String gatewayReference;

    @Schema(description = "Processing fees", example = "2.99")
    @JsonProperty("processingFees")
    private BigDecimal processingFees;

    @Schema(description = "Net amount after fees", example = "97.00")
    @JsonProperty("netAmount")
    private BigDecimal netAmount;

    @Schema(description = "Settlement date and time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @JsonProperty("settlementDate")
    private ZonedDateTime settlementDate;

    @Schema(description = "Transaction creation date and time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @JsonProperty("createdAt")
    private ZonedDateTime createdAt;

    @Schema(description = "Risk score from fraud detection", example = "25.5")
    @JsonProperty("riskScore")
    private BigDecimal riskScore;

    @Schema(description = "Risk assessment result", example = "LOW")
    @JsonProperty("riskAssessment")
    private String riskAssessment;

    @Schema(description = "Payment method information used")
    @JsonProperty("paymentMethod")
    private PaymentMethodResponse paymentMethod;

    @Schema(description = "Error details if transaction failed")
    @JsonProperty("error")
    private PaymentErrorResponse error;

    @Schema(description = "Additional metadata", example = "{\"processor\": \"authorize_net\"}")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Correlation ID for tracking", example = "corr-123456789")
    @JsonProperty("correlationId")
    private String correlationId;

    @Schema(description = "Customer ID from local database", example = "CUST_A1B2C3D4")
    @JsonProperty("customerId")
    private String customerId;

    @Schema(description = "Customer reference/external ID", example = "CUST_A1B2C3D4")
    @JsonProperty("customerReference")
    private String customerReference;

    @Schema(description = "Authorize.Net customer profile ID", example = "123456789")
    @JsonProperty("customerProfileId")
    private String customerProfileId;

    @Schema(description = "Error code for failed transactions", example = "CARD_DECLINED")
    @JsonProperty("errorCode")
    private String errorCode;

    @Schema(description = "Human-readable error message", example = "Card was declined by the issuing bank")
    @JsonProperty("errorMessage")
    private String errorMessage;

    @Schema(description = "Error category for programmatic handling", example = "CARD_DECLINED")
    @JsonProperty("errorCategory")
    private String errorCategory;

    @Schema(description = "Whether the transaction can be retried", example = "true")
    @JsonProperty("retryable")
    private Boolean retryable;

    @Schema(description = "Suggested action for the user", example = "Try a different payment method")
    @JsonProperty("suggestedAction")
    private String suggestedAction;

    @Schema(description = "Detailed technical error message", example = "AVS verification failed")
    @JsonProperty("detailedError")
    private String detailedError;

    @Schema(description = "Seconds to wait before retry", example = "30")
    @JsonProperty("retryAfterSeconds")
    private Integer retryAfterSeconds;

    @Schema(description = "Maximum retry attempts allowed", example = "3")
    @JsonProperty("maxRetryAttempts")
    private Integer maxRetryAttempts;

    @Schema(description = "Validation errors for specific fields")
    @JsonProperty("validationErrors")
    private List<ValidationError> validationErrors;

    // Custom initialization constructor
    {
        this.metadata = new HashMap<>();
        this.validationErrors = new ArrayList<>();
    }

    // Constructor for successful response
    public PaymentResponse(String transactionId, String authnetTransactionId, 
                          String status, BigDecimal amount, String currency) {
        this();
        this.transactionId = transactionId;
        this.authnetTransactionId = authnetTransactionId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.success = true;
        this.createdAt = ZonedDateTime.now();
    }

    // Constructor for error response
    public PaymentResponse(String transactionId, String status, PaymentErrorResponse error) {
        this();
        this.transactionId = transactionId;
        this.status = status;
        this.error = error;
        this.success = false;
        this.createdAt = ZonedDateTime.now();
    }

    // Helper method to add validation errors
    public void addValidationError(String field, String message) {
        if (this.validationErrors == null) {
            this.validationErrors = new ArrayList<>();
        }
        this.validationErrors.add(new ValidationError(field, message));
    }

    /**
     * Inner class for validation errors.
     */
    @Data
    @NoArgsConstructor
    public static class ValidationError {
        @JsonProperty("field")
        private String field;
        
        @JsonProperty("message")
        private String message;
        
        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
