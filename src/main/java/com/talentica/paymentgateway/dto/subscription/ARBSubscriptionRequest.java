package com.talentica.paymentgateway.dto.subscription;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating Authorize.Net ARB subscriptions.
 */
@Schema(description = "Request to create an Authorize.Net ARB subscription")
public class ARBSubscriptionRequest {

    @NotBlank(message = "Customer ID is required")
    @Schema(description = "Customer identifier", example = "CUST001", required = true)
    private String customerId;

    @NotBlank(message = "Plan code is required")
    @Schema(description = "Subscription plan code", example = "SUCCESS_PLAN", required = true)
    private String planCode;

    @NotBlank(message = "Payment method ID is required")
    @Schema(description = "Payment method identifier", example = "pm_cust001_test_12345", required = true)
    private String paymentMethodId;

    @Schema(description = "Custom subscription name (optional)", example = "Premium Monthly Subscription")
    private String subscriptionName;

    // Constructors
    public ARBSubscriptionRequest() {}

    public ARBSubscriptionRequest(String customerId, String planCode, String paymentMethodId) {
        this.customerId = customerId;
        this.planCode = planCode;
        this.paymentMethodId = paymentMethodId;
    }

    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    @Override
    public String toString() {
        return "ARBSubscriptionRequest{" +
                "customerId='" + customerId + '\'' +
                ", planCode='" + planCode + '\'' +
                ", paymentMethodId='" + paymentMethodId + '\'' +
                ", subscriptionName='" + subscriptionName + '\'' +
                '}';
    }
}
