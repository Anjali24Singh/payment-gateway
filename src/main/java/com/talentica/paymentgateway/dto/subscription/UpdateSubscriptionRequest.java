package com.talentica.paymentgateway.dto.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Request DTO for updating an existing subscription.
 * Supports plan changes, payment method updates, and metadata modifications.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Schema(description = "Request to update an existing subscription")
public class UpdateSubscriptionRequest {

    @Size(max = 100, message = "Plan code must not exceed 100 characters")
    @Schema(description = "New plan code for subscription upgrade/downgrade", example = "premium_yearly")
    @JsonProperty("planCode")
    private String planCode;

    @Size(max = 100, message = "Payment method ID must not exceed 100 characters")
    @Schema(description = "New payment method identifier", example = "pm_67890")
    @JsonProperty("paymentMethodId")
    private String paymentMethodId;

    @Schema(description = "New billing cycle anchor date", example = "2024-02-01T00:00:00Z")
    @JsonProperty("billingCycleAnchor")
    private ZonedDateTime billingCycleAnchor;

    @Schema(description = "Whether to prorate plan changes", example = "true")
    @JsonProperty("prorated")
    private Boolean prorated = true;

    @Schema(description = "When to apply the plan change", example = "IMMEDIATE", 
            allowableValues = {"IMMEDIATE", "END_OF_PERIOD"})
    @JsonProperty("changeOption")
    private String changeOption = "IMMEDIATE";

    @Schema(description = "Additional metadata for the subscription")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Reason for the subscription change", example = "Customer upgrade to premium plan")
    @JsonProperty("changeReason")
    private String changeReason;

    @Schema(description = "Whether to send notification about the change", example = "true")
    @JsonProperty("notifyCustomer")
    private Boolean notifyCustomer = true;

    // Default constructor
    public UpdateSubscriptionRequest() {
    }

    // Getters and Setters
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

    public String getChangeOption() {
        return changeOption;
    }

    public void setChangeOption(String changeOption) {
        this.changeOption = changeOption;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public Boolean getNotifyCustomer() {
        return notifyCustomer;
    }

    public void setNotifyCustomer(Boolean notifyCustomer) {
        this.notifyCustomer = notifyCustomer;
    }

    public boolean hasChanges() {
        return planCode != null || paymentMethodId != null || billingCycleAnchor != null || 
               (metadata != null && !metadata.isEmpty());
    }

    public boolean isPlanChange() {
        return planCode != null;
    }

    public boolean isPaymentMethodChange() {
        return paymentMethodId != null;
    }

    @Override
    public String toString() {
        return "UpdateSubscriptionRequest{" +
                "planCode='" + planCode + '\'' +
                ", paymentMethodId='" + paymentMethodId + '\'' +
                ", billingCycleAnchor=" + billingCycleAnchor +
                ", prorated=" + prorated +
                ", changeOption='" + changeOption + '\'' +
                ", changeReason='" + changeReason + '\'' +
                ", notifyCustomer=" + notifyCustomer +
                '}';
    }
}
