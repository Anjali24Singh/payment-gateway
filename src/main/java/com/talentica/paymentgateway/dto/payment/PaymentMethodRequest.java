package com.talentica.paymentgateway.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.talentica.paymentgateway.validation.ValidCreditCard;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment method request DTO for credit card and token-based payments.
 * Supports both direct credit card input and tokenized payment methods.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment method information for transaction processing")
public class PaymentMethodRequest {

    @NotBlank(message = "Payment method type is required")
    @Pattern(regexp = "^(CREDIT_CARD|TOKEN|BANK_ACCOUNT)$", 
             message = "Payment method type must be CREDIT_CARD, TOKEN, or BANK_ACCOUNT")
    @Schema(description = "Payment method type", example = "CREDIT_CARD", required = true,
            allowableValues = {"CREDIT_CARD", "TOKEN", "BANK_ACCOUNT"})
    @JsonProperty("type")
    private String type;

    // Credit Card Fields
    @ValidCreditCard(message = "Invalid credit card number")
    @Schema(description = "Credit card number (for CREDIT_CARD type)", example = "4111111111111111")
    @JsonProperty("cardNumber")
    private String cardNumber;

    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Expiry month must be between 01 and 12")
    @Schema(description = "Credit card expiry month (MM)", example = "12")
    @JsonProperty("expiryMonth")
    private String expiryMonth;

    @Pattern(regexp = "^\\d{4}$", message = "Expiry year must be a 4-digit year")
    @Schema(description = "Credit card expiry year (YYYY)", example = "2025")
    @JsonProperty("expiryYear")
    private String expiryYear;

    @Pattern(regexp = "^\\d{3,4}$", message = "CVV must be 3 or 4 digits")
    @Schema(description = "Credit card CVV/CVC", example = "123")
    @JsonProperty("cvv")
    private String cvv;

    @Size(max = 100, message = "Cardholder name must not exceed 100 characters")
    @Schema(description = "Cardholder name", example = "John Doe")
    @JsonProperty("cardholderName")
    private String cardholderName;

    // Token Fields
    @Size(max = 255, message = "Payment token must not exceed 255 characters")
    @Schema(description = "Payment token (for TOKEN type)", example = "pm_1234567890abcdef")
    @JsonProperty("token")
    private String token;

    // Bank Account Fields (for ACH payments)
    @Size(max = 17, message = "Routing number must not exceed 17 characters")
    @Pattern(regexp = "^\\d{9}$", message = "Routing number must be 9 digits", 
             groups = BankAccountValidation.class)
    @Schema(description = "Bank routing number (for BANK_ACCOUNT type)", example = "123456789")
    @JsonProperty("routingNumber")
    private String routingNumber;

    @Size(max = 20, message = "Account number must not exceed 20 characters")
    @Pattern(regexp = "^\\d{4,20}$", message = "Account number must be 4-20 digits", 
             groups = BankAccountValidation.class)
    @Schema(description = "Bank account number (for BANK_ACCOUNT type)", example = "1234567890")
    @JsonProperty("accountNumber")
    private String accountNumber;

    @Pattern(regexp = "^(CHECKING|SAVINGS|BUSINESS_CHECKING)$", 
             message = "Account type must be CHECKING, SAVINGS, or BUSINESS_CHECKING",
             groups = BankAccountValidation.class)
    @Schema(description = "Bank account type (for BANK_ACCOUNT type)", 
            allowableValues = {"CHECKING", "SAVINGS", "BUSINESS_CHECKING"})
    @JsonProperty("accountType")
    private String accountType;

    @Size(max = 22, message = "Bank name must not exceed 22 characters")
    @Schema(description = "Bank name (for BANK_ACCOUNT type)", example = "Wells Fargo")
    @JsonProperty("bankName")
    private String bankName;

    @Size(max = 50, message = "Account holder name must not exceed 50 characters")
    @Schema(description = "Account holder name (for BANK_ACCOUNT type)", example = "John Doe")
    @JsonProperty("accountHolderName")
    private String accountHolderName;

    // Validation interface for bank account fields
    public interface BankAccountValidation {
    }

    /**
     * Masks sensitive payment information for logging.
     */
    @Override
    public String toString() {
        return "PaymentMethodRequest{" +
                "type='" + type + '\'' +
                ", cardNumber='" + maskCardNumber(cardNumber) + '\'' +
                ", expiryMonth='" + expiryMonth + '\'' +
                ", expiryYear='" + expiryYear + '\'' +
                ", cardholderName='" + cardholderName + '\'' +
                ", token='" + maskToken(token) + '\'' +
                ", routingNumber='" + maskRoutingNumber(routingNumber) + '\'' +
                ", accountNumber='" + maskAccountNumber(accountNumber) + '\'' +
                ", accountType='" + accountType + '\'' +
                ", bankName='" + bankName + '\'' +
                ", accountHolderName='" + accountHolderName + '\'' +
                '}';
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    private String maskRoutingNumber(String routingNumber) {
        if (routingNumber == null || routingNumber.length() < 4) {
            return "****";
        }
        return "****" + routingNumber.substring(routingNumber.length() - 4);
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
