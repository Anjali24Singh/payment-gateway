package com.talentica.paymentgateway.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.talentica.paymentgateway.validation.ValidAmount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Capture request DTO for capturing previously authorized transactions.
 * Used to complete the second step of authorize/capture transactions.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Capture request for completing authorized transactions")
public class CaptureRequest {

    @NotBlank(message = "Transaction ID is required")
    @Size(max = 255, message = "Transaction ID must not exceed 255 characters")
    @Schema(description = "Original authorization transaction ID", example = "auth_1234567890", required = true)
    @JsonProperty("transactionId")
    private String transactionId;

    @ValidAmount
    @Schema(description = "Amount to capture (can be less than authorized amount for partial capture)", 
            example = "50.00")
    @JsonProperty("amount")
    private BigDecimal amount;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Schema(description = "Capture description", example = "Partial capture for shipped items")
    @JsonProperty("description")
    private String description;

    @Size(max = 25, message = "Invoice number must not exceed 25 characters")
    @Schema(description = "Invoice number for the capture", example = "INV-2024-001-PARTIAL")
    @JsonProperty("invoiceNumber")
    private String invoiceNumber;

    @Schema(description = "Final capture indicator (no more captures allowed)", example = "false")
    @JsonProperty("finalCapture")
    private Boolean finalCapture = false;

    @Size(max = 255, message = "Idempotency key must not exceed 255 characters")
    @Schema(description = "Idempotency key to prevent duplicate captures")
    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    // Backward compatibility methods
    public String getAuthorizationTransactionId() {
        return transactionId;
    }

    public void setAuthorizationTransactionId(String authorizationTransactionId) {
        this.transactionId = authorizationTransactionId;
    }
}
