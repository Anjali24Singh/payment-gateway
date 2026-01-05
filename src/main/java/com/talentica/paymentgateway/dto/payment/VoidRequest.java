package com.talentica.paymentgateway.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Void request DTO for cancelling authorized transactions before capture.
 * Used to release held funds without completing the transaction.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Void request for cancelling authorized transactions")
public class VoidRequest {

    @NotBlank(message = "Transaction ID is required")
    @Size(max = 255, message = "Transaction ID must not exceed 255 characters")
    @Schema(description = "Original transaction ID to void", example = "auth_1234567890", required = true)
    @JsonProperty("transactionId")
    private String transactionId;

    @Size(max = 255, message = "Reason must not exceed 255 characters")
    @Schema(description = "Reason for voiding the transaction", example = "Order cancelled by customer")
    @JsonProperty("reason")
    private String reason;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Schema(description = "Void description", example = "Void authorization for cancelled Order #12345")
    @JsonProperty("description")
    private String description;

    @Size(max = 25, message = "Reference number must not exceed 25 characters")
    @Schema(description = "Internal reference number for the void", example = "VOID-2024-001")
    @JsonProperty("referenceNumber")
    private String referenceNumber;

    @Schema(description = "Notify customer about the void", example = "true")
    @JsonProperty("notifyCustomer")
    private Boolean notifyCustomer = true;

    @Size(max = 255, message = "Idempotency key must not exceed 255 characters")
    @Schema(description = "Idempotency key to prevent duplicate voids")
    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    // Backward compatibility methods
    public String getOriginalTransactionId() {
        return transactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.transactionId = originalTransactionId;
    }
}
