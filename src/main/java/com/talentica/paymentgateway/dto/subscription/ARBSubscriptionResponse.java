package com.talentica.paymentgateway.dto.subscription;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Response DTO for Authorize.Net ARB subscription operations.
 */
@Schema(description = "Response for ARB subscription operations")
public class ARBSubscriptionResponse {

    @Schema(description = "Authorize.Net ARB subscription ID", example = "7654321")
    private String arbSubscriptionId;

    @Schema(description = "Customer identifier", example = "CUST001")
    private String customerId;

    @Schema(description = "Customer name", example = "John Doe-Updated")
    private String customerName;

    @Schema(description = "Customer email", example = "john.doe.updated@example.com")
    private String customerEmail;

    @Schema(description = "Subscription plan code", example = "SUCCESS_PLAN")
    private String planCode;

    @Schema(description = "Subscription plan name", example = "Success Monthly Plan")
    private String planName;

    @Schema(description = "Plan amount", example = "39.99")
    private BigDecimal planAmount;

    @Schema(description = "Currency", example = "USD")
    private String currency;

    @Schema(description = "Billing interval unit", example = "MONTH")
    private String intervalUnit;

    @Schema(description = "Billing interval count", example = "1")
    private Integer intervalCount;

    @Schema(description = "Subscription status", example = "ACTIVE")
    private String status;

    @Schema(description = "Response message", example = "ARB subscription created successfully")
    private String message;

    // Constructors
    public ARBSubscriptionResponse() {}

    // Getters and Setters
    public String getArbSubscriptionId() {
        return arbSubscriptionId;
    }

    public void setArbSubscriptionId(String arbSubscriptionId) {
        this.arbSubscriptionId = arbSubscriptionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public BigDecimal getPlanAmount() {
        return planAmount;
    }

    public void setPlanAmount(BigDecimal planAmount) {
        this.planAmount = planAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getIntervalUnit() {
        return intervalUnit;
    }

    public void setIntervalUnit(String intervalUnit) {
        this.intervalUnit = intervalUnit;
    }

    public Integer getIntervalCount() {
        return intervalCount;
    }

    public void setIntervalCount(Integer intervalCount) {
        this.intervalCount = intervalCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ARBSubscriptionResponse{" +
                "arbSubscriptionId='" + arbSubscriptionId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", customerName='" + customerName + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", planCode='" + planCode + '\'' +
                ", planName='" + planName + '\'' +
                ", planAmount=" + planAmount +
                ", currency='" + currency + '\'' +
                ", intervalUnit='" + intervalUnit + '\'' +
                ", intervalCount=" + intervalCount +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
