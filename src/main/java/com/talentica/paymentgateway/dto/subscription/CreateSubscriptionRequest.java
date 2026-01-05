package com.talentica.paymentgateway.dto.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Request DTO for creating a new subscription.
 * Contains all required information to start a subscription for a customer.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Schema(description = "Request to create a new subscription")
public class CreateSubscriptionRequest {

    @NotBlank(message = "Customer ID is required")
    @Size(max = 100, message = "Customer ID must not exceed 100 characters")
    @Schema(description = "Customer identifier", example = "cust_12345", required = true)
    @JsonProperty("customerId")
    private String customerId;

    @NotBlank(message = "Plan code is required")
    @Size(max = 100, message = "Plan code must not exceed 100 characters")
    @Schema(description = "Subscription plan code", example = "premium_monthly", required = true)
    @JsonProperty("planCode")
    private String planCode;

    @NotBlank(message = "Payment method ID is required")
    @Size(max = 100, message = "Payment method ID must not exceed 100 characters")
    @Schema(description = "Payment method identifier", example = "pm_12345", required = true)
    @JsonProperty("paymentMethodId")
    private String paymentMethodId;

    @Schema(description = "Subscription start date (defaults to now)", example = "2024-01-01T00:00:00Z")
    @JsonProperty("startDate")
    private ZonedDateTime startDate;

    @Schema(description = "Whether to start trial period immediately", example = "true")
    @JsonProperty("startTrial")
    private Boolean startTrial = true;

    @Schema(description = "Billing cycle anchor date (if different from start date)", example = "2024-01-01T00:00:00Z")
    @JsonProperty("billingCycleAnchor")
    private ZonedDateTime billingCycleAnchor;

    @Schema(description = "Whether to prorate the first payment", example = "true")
    @JsonProperty("prorated")
    private Boolean prorated = true;

    @Size(max = 255, message = "Idempotency key must not exceed 255 characters")
    @Schema(description = "Idempotency key to prevent duplicate subscriptions")
    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    @Schema(description = "Additional metadata for the subscription")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Description or reason for the subscription", example = "Premium plan subscription")
    @JsonProperty("description")
    private String description;

    // Default constructor
    public CreateSubscriptionRequest() {
    }

    // Constructor with required fields
    public CreateSubscriptionRequest(String customerId, String planCode, String paymentMethodId) {
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

    public ZonedDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(ZonedDateTime startDate) {
        this.startDate = startDate;
    }

    public Boolean getStartTrial() {
        return startTrial;
    }

    public void setStartTrial(Boolean startTrial) {
        this.startTrial = startTrial;
    }

    public ZonedDateTime getBillingCycleAnchor() {
        return billingCycleAnchor;
    }

    public void setBillingCycleAnchor(ZonedDateTime billingCycleAnchor) {
        this.billingCycleAnchor = billingCycleAnchor;
    }

    public Boolean getProrated() {
        return prorated;
    }

    public void setProrated(Boolean prorated) {
        this.prorated = prorated;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "CreateSubscriptionRequest{" +
                "customerId='" + customerId + '\'' +
                ", planCode='" + planCode + '\'' +
                ", paymentMethodId='" + paymentMethodId + '\'' +
                ", startDate=" + startDate +
                ", startTrial=" + startTrial +
                ", billingCycleAnchor=" + billingCycleAnchor +
                ", prorated=" + prorated +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
