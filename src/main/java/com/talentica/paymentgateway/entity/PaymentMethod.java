package com.talentica.paymentgateway.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a tokenized payment method for a customer.
 * Payment methods store tokenized payment information for secure reuse.
 */
@Entity
@Table(name = "payment_methods",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "paymentToken")
       },
       indexes = {
           @Index(name = "idx_payment_methods_customer_id", columnList = "customer_id"),
           @Index(name = "idx_payment_methods_token", columnList = "paymentToken"),
           @Index(name = "idx_payment_methods_active", columnList = "isActive")
       })
public class PaymentMethod extends BaseEntity {

    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotBlank(message = "Payment token is required")
    @Size(max = 255, message = "Payment token must not exceed 255 characters")
    @Column(name = "payment_token", nullable = false, unique = true, length = 255)
    private String paymentToken;

    @NotBlank(message = "Payment type is required")
    @Size(max = 50, message = "Payment type must not exceed 50 characters")
    @Column(name = "payment_type", nullable = false, length = 50)
    private String paymentType = "CREDIT_CARD";

    @Size(max = 4, message = "Card last four must not exceed 4 characters")
    @Pattern(regexp = "\\d{4}", message = "Card last four must be 4 digits")
    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Size(max = 50, message = "Card brand must not exceed 50 characters")
    @Column(name = "card_brand", length = 50)
    private String cardBrand;

    @Min(value = 1, message = "Card expiry month must be between 1 and 12")
    @Max(value = 12, message = "Card expiry month must be between 1 and 12")
    @Column(name = "card_expiry_month")
    private Integer cardExpiryMonth;

    @Min(value = 2024, message = "Card expiry year must be current year or later")
    @Column(name = "card_expiry_year")
    private Integer cardExpiryYear;

    @Size(max = 255, message = "Cardholder name must not exceed 255 characters")
    @Column(name = "cardholder_name", length = 255)
    private String cardholderName;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Credit card fields for processing (encrypted/tokenized in real implementation)
    @Column(name = "card_number", length = 255)
    private String cardNumber;

    @Column(name = "expiry_month", length = 2)
    private String expiryMonth;

    @Column(name = "expiry_year", length = 4)
    private String expiryYear;

    @Column(name = "cvv", length = 4)
    private String cvv;

    // Relationships
    @OneToMany(mappedBy = "paymentMethod", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "paymentMethod", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Subscription> subscriptions = new ArrayList<>();

    // Constructors
    public PaymentMethod() {
        super();
    }

    public PaymentMethod(Customer customer, String paymentToken, String paymentType) {
        this();
        this.customer = customer;
        this.paymentToken = paymentToken;
        this.paymentType = paymentType;
    }

    // Getters and Setters
    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getPaymentToken() {
        return paymentToken;
    }

    public void setPaymentToken(String paymentToken) {
        this.paymentToken = paymentToken;
    }

    public String getPaymentMethodId() {
        return paymentToken;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getType() {
        return paymentType;
    }

    public String getCardLastFour() {
        return cardLastFour;
    }

    public void setCardLastFour(String cardLastFour) {
        this.cardLastFour = cardLastFour;
    }

    public String getLast4() {
        return cardLastFour;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
    }

    public Integer getCardExpiryMonth() {
        return cardExpiryMonth;
    }

    public void setCardExpiryMonth(Integer cardExpiryMonth) {
        this.cardExpiryMonth = cardExpiryMonth;
    }

    public Integer getCardExpiryYear() {
        return cardExpiryYear;
    }

    public void setCardExpiryYear(Integer cardExpiryYear) {
        this.cardExpiryYear = cardExpiryYear;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
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

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    // Utility methods
    public String getMaskedCardNumber() {
        if (cardLastFour != null) {
            return "**** **** **** " + cardLastFour;
        }
        return null;
    }

    public String getExpiryDateFormatted() {
        if (cardExpiryMonth != null && cardExpiryYear != null) {
            return String.format("%02d/%d", cardExpiryMonth, cardExpiryYear % 100);
        }
        return null;
    }

    public boolean isExpired() {
        if (cardExpiryMonth == null || cardExpiryYear == null) {
            return false;
        }
        
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate expiry = java.time.LocalDate.of(cardExpiryYear, cardExpiryMonth, 1)
                .plusMonths(1).minusDays(1); // Last day of expiry month
        
        return now.isAfter(expiry);
    }

    public String getDisplayName() {
        StringBuilder displayName = new StringBuilder();
        
        if (cardBrand != null) {
            displayName.append(cardBrand);
        } else {
            displayName.append(paymentType);
        }
        
        if (cardLastFour != null) {
            displayName.append(" ending in ").append(cardLastFour);
        }
        
        return displayName.toString();
    }
}
