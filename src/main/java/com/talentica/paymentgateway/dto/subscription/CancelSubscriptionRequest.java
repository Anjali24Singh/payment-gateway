package com.talentica.paymentgateway.dto.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.ZonedDateTime;

/**
 * Request DTO for cancelling a subscription.
 * Supports immediate cancellation or end-of-period cancellation.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Schema(description = "Request to cancel a subscription")
public class CancelSubscriptionRequest {

    @Schema(description = "Reason for cancellation", example = "Customer requested cancellation")
    @JsonProperty("reason")
    private String reason;

    @Schema(description = "When to cancel the subscription", example = "END_OF_PERIOD", 
            allowableValues = {"IMMEDIATE", "END_OF_PERIOD"})
    @JsonProperty("when")
    private String when = "END_OF_PERIOD";

    @Schema(description = "Specific date to cancel subscription (if different from when option)", 
            example = "2024-12-31T23:59:59Z")
    @JsonProperty("cancelAt")
    private ZonedDateTime cancelAt;

    @Schema(description = "Whether to refund prorated amount for current period", example = "false")
    @JsonProperty("refundProrated")
    private Boolean refundProrated = false;

    @Schema(description = "Whether to send cancellation notification to customer", example = "true")
    @JsonProperty("notifyCustomer")
    private Boolean notifyCustomer = true;

    @Size(max = 500, message = "Additional notes must not exceed 500 characters")
    @Schema(description = "Additional notes about the cancellation")
    @JsonProperty("notes")
    private String notes;

    // Default constructor
    public CancelSubscriptionRequest() {
    }

    // Constructor with reason
    public CancelSubscriptionRequest(String reason) {
        this.reason = reason;
    }

    // Getters and Setters
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getWhen() {
        return when;
    }

    public void setWhen(String when) {
        this.when = when;
    }

    public ZonedDateTime getCancelAt() {
        return cancelAt;
    }

    public void setCancelAt(ZonedDateTime cancelAt) {
        this.cancelAt = cancelAt;
    }

    public Boolean getRefundProrated() {
        return refundProrated;
    }

    public void setRefundProrated(Boolean refundProrated) {
        this.refundProrated = refundProrated;
    }

    public Boolean getNotifyCustomer() {
        return notifyCustomer;
    }

    public void setNotifyCustomer(Boolean notifyCustomer) {
        this.notifyCustomer = notifyCustomer;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isImmediateCancellation() {
        return "IMMEDIATE".equals(when);
    }

    public boolean isEndOfPeriodCancellation() {
        return "END_OF_PERIOD".equals(when);
    }

    @Override
    public String toString() {
        return "CancelSubscriptionRequest{" +
                "reason='" + reason + '\'' +
                ", when='" + when + '\'' +
                ", cancelAt=" + cancelAt +
                ", refundProrated=" + refundProrated +
                ", notifyCustomer=" + notifyCustomer +
                ", notes='" + notes + '\'' +
                '}';
    }
}
