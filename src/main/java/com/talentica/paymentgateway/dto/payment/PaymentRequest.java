package com.talentica.paymentgateway.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.talentica.paymentgateway.validation.ValidAmount;
import com.talentica.paymentgateway.validation.ValidCreditCard;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Base payment request DTO for Authorize.Net payment processing.
 * Contains common fields required for all payment operations.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment request containing transaction details")
public class PaymentRequest {

    @NotNull(message = "Amount is required")
    @ValidAmount
    @Schema(description = "Transaction amount", example = "99.99", required = true)
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
    @Schema(description = "Currency code", example = "USD", required = true)
    @JsonProperty("currency")
    private String currency = "USD";

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Schema(description = "Transaction description", example = "Payment for Order #12345")
    @JsonProperty("description")
    private String description;

    @Size(max = 50, message = "Customer ID must not exceed 50 characters")
    @Schema(description = "Customer identifier", example = "CUST_12345")
    @JsonProperty("customerId")
    private String customerId;

    @Size(max = 25, message = "Invoice number must not exceed 25 characters")
    @Schema(description = "Invoice number", example = "INV-2024-001")
    @JsonProperty("invoiceNumber")
    private String invoiceNumber;

    @Size(max = 50, message = "Order number must not exceed 50 characters")
    @Schema(description = "Order number", example = "ORDER-2024-001")
    @JsonProperty("orderNumber")
    private String orderNumber;

    @Valid
    @Schema(description = "Payment method details")
    @JsonProperty("paymentMethod")
    private PaymentMethodRequest paymentMethod;

    @Valid
    @Schema(description = "Customer information")
    @JsonProperty("customer")
    private CustomerRequest customer;

    @Valid
    @Schema(description = "Billing address information")
    @JsonProperty("billingAddress")
    private AddressRequest billingAddress;

    @Valid
    @Schema(description = "Shipping address information")
    @JsonProperty("shippingAddress")
    private AddressRequest shippingAddress;

    @Schema(description = "Additional metadata for the transaction")
    @JsonProperty("metadata")
    private Map<String, String> metadata;

    @Size(max = 255, message = "Idempotency key must not exceed 255 characters")
    @Schema(description = "Idempotency key to prevent duplicate transactions")
    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    @Schema(description = "Test mode flag", example = "false")
    @JsonProperty("testMode")
    private Boolean testMode = false;
}

