package com.talentica.paymentgateway.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.talentica.paymentgateway.validation.ValidAmount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Refund request DTO for refunding captured transactions.
 * Supports both full and partial refunds.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Refund request for returning funds to customer")
public class RefundRequest {

    @NotBlank(message = "Transaction ID is required")
    @Size(max = 255, message = "Transaction ID must not exceed 255 characters")
    @Schema(description = "Original transaction ID to refund", example = "txn_1234567890", required = true)
    @JsonProperty("transactionId")
    private String transactionId;

    @ValidAmount
    @Schema(description = "Amount to refund (leave empty for full refund)", example = "25.00")
    @JsonProperty("amount")
    private BigDecimal amount;

    @Size(max = 255, message = "Reason must not exceed 255 characters")
    @Schema(description = "Reason for the refund", example = "Customer requested refund for damaged item")
    @JsonProperty("reason")
    private String reason;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Schema(description = "Refund description", example = "Partial refund for Order #12345")
    @JsonProperty("description")
    private String description;

    @Size(max = 25, message = "Reference number must not exceed 25 characters")
    @Schema(description = "Internal reference number for the refund", example = "REF-2024-001")
    @JsonProperty("referenceNumber")
    private String referenceNumber;

    @Schema(description = "Notify customer about the refund", example = "true")
    @JsonProperty("notifyCustomer")
    private Boolean notifyCustomer = true;

    @Size(max = 255, message = "Idempotency key must not exceed 255 characters")
    @Schema(description = "Idempotency key to prevent duplicate refunds")
    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    // Backward compatibility methods
    public String getOriginalTransactionId() {
        return transactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.transactionId = originalTransactionId;
    }

    /**
     * Determines if this is a full refund (no amount specified).
     */
    public boolean isFullRefund() {
        return amount == null;
    }
}
