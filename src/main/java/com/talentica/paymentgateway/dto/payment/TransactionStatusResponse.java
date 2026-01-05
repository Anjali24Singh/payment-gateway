package com.talentica.paymentgateway.dto.payment;

import com.talentica.paymentgateway.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Response DTO for transaction status queries.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction status response")
public class TransactionStatusResponse {

    @Schema(description = "Transaction ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID transactionId;

    @Schema(description = "Transaction status")
    private PaymentStatus status;

    @Schema(description = "Transaction amount", example = "99.99")
    private BigDecimal amount;

    @Schema(description = "Transaction creation timestamp")
    private ZonedDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private ZonedDateTime updatedAt;
}
