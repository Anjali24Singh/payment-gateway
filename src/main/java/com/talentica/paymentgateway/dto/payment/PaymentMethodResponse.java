package com.talentica.paymentgateway.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payment method response DTO containing masked payment information.
 * Used in payment responses to provide payment method details without exposing sensitive data.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Schema(description = "Payment method information in response (masked for security)")
public class PaymentMethodResponse {

    @Schema(description = "Payment method type", example = "CREDIT_CARD")
    @JsonProperty("type")
    private String type;

    @Schema(description = "Masked card number", example = "****-****-****-1111")
    @JsonProperty("maskedCardNumber")
    private String maskedCardNumber;

    @Schema(description = "Card brand", example = "VISA")
    @JsonProperty("cardBrand")
    private String cardBrand;

    @Schema(description = "Card expiry month", example = "12")
    @JsonProperty("expiryMonth")
    private String expiryMonth;

    @Schema(description = "Card expiry year", example = "2025")
    @JsonProperty("expiryYear")
    private String expiryYear;

    @Schema(description = "Cardholder name", example = "John Doe")
    @JsonProperty("cardholderName")
    private String cardholderName;

    @Schema(description = "Payment token for future use", example = "pm_1234567890abcdef")
    @JsonProperty("token")
    private String token;

    @Schema(description = "Masked bank account number", example = "****7890")
    @JsonProperty("maskedAccountNumber")
    private String maskedAccountNumber;

    @Schema(description = "Bank routing number", example = "123456789")
    @JsonProperty("routingNumber")
    private String routingNumber;

    @Schema(description = "Bank account type", example = "CHECKING")
    @JsonProperty("accountType")
    private String accountType;

    @Schema(description = "Bank name", example = "Wells Fargo")
    @JsonProperty("bankName")
    private String bankName;

    @Schema(description = "Account holder name", example = "John Doe")
    @JsonProperty("accountHolderName")
    private String accountHolderName;

    // Default constructor
    public PaymentMethodResponse() {
    }

    // Constructor for credit card
    public PaymentMethodResponse(String type, String maskedCardNumber, String cardBrand, 
                                String expiryMonth, String expiryYear, String cardholderName) {
        this.type = type;
        this.maskedCardNumber = maskedCardNumber;
        this.cardBrand = cardBrand;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.cardholderName = cardholderName;
    }

    // Constructor for bank account - using different parameter order to avoid constructor conflict
    public PaymentMethodResponse(String type, String maskedAccountNumber, String routingNumber, 
                                String accountType, String bankName, String accountHolderName, boolean isBankAccount) {
        this.type = type;
        this.maskedAccountNumber = maskedAccountNumber;
        this.routingNumber = routingNumber;
        this.accountType = accountType;
        this.bankName = bankName;
        this.accountHolderName = accountHolderName;
    }

    // Static factory methods
    public static PaymentMethodResponse fromCreditCard(String cardNumber, String cardBrand, 
                                                      String expiryMonth, String expiryYear, 
                                                      String cardholderName) {
        return new PaymentMethodResponse(
            "CREDIT_CARD",
            maskCardNumber(cardNumber),
            cardBrand,
            expiryMonth,
            expiryYear,
            cardholderName
        );
    }

    public static PaymentMethodResponse fromToken(String token) {
        PaymentMethodResponse response = new PaymentMethodResponse();
        response.setType("TOKEN");
        response.setToken(token);
        return response;
    }

    public static PaymentMethodResponse fromBankAccount(String accountNumber, String routingNumber, 
                                                       String accountType, String bankName, 
                                                       String accountHolderName) {
        return new PaymentMethodResponse(
            "BANK_ACCOUNT",
            maskAccountNumber(accountNumber),
            routingNumber,
            accountType,
            bankName,
            accountHolderName,
            true
        );
    }

    // Utility methods for masking sensitive data
    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****-****-****-****";
        }
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }

    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = maskedCardNumber;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
    }

    public String getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(String expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public String getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(String expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMaskedAccountNumber() {
        return maskedAccountNumber;
    }

    public void setMaskedAccountNumber(String maskedAccountNumber) {
        this.maskedAccountNumber = maskedAccountNumber;
    }

    public String getRoutingNumber() {
        return routingNumber;
    }

    public void setRoutingNumber(String routingNumber) {
        this.routingNumber = routingNumber;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    @Override
    public String toString() {
        return "PaymentMethodResponse{" +
                "type='" + type + '\'' +
                ", maskedCardNumber='" + maskedCardNumber + '\'' +
                ", cardBrand='" + cardBrand + '\'' +
                ", expiryMonth='" + expiryMonth + '\'' +
                ", expiryYear='" + expiryYear + '\'' +
                ", cardholderName='" + cardholderName + '\'' +
                ", token='" + token + '\'' +
                ", maskedAccountNumber='" + maskedAccountNumber + '\'' +
                ", routingNumber='" + routingNumber + '\'' +
                ", accountType='" + accountType + '\'' +
                ", bankName='" + bankName + '\'' +
                ", accountHolderName='" + accountHolderName + '\'' +
                '}';
    }
}
