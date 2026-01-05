package com.talentica.paymentgateway.dto.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.talentica.paymentgateway.entity.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Response DTO for subscription operations.
 * Contains subscription details and current state information.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Schema(description = "Subscription response containing current subscription state")
public class SubscriptionResponse {

    @Schema(description = "Subscription identifier", example = "sub_12345")
    @JsonProperty("subscriptionId")
    private String subscriptionId;

    @Schema(description = "Customer identifier", example = "cust_12345")
    @JsonProperty("customerId")
    private String customerId;

    @Schema(description = "Customer name", example = "John Doe")
    @JsonProperty("customerName")
    private String customerName;

    @Schema(description = "Customer email", example = "john.doe@example.com")
    @JsonProperty("customerEmail")
    private String customerEmail;

    @Schema(description = "Plan code", example = "premium_monthly")
    @JsonProperty("planCode")
    private String planCode;

    @Schema(description = "Plan name", example = "Premium Monthly Plan")
    @JsonProperty("planName")
    private String planName;

    @Schema(description = "Plan amount", example = "29.99")
    @JsonProperty("planAmount")
    private BigDecimal planAmount;

    @Schema(description = "Plan currency", example = "USD")
    @JsonProperty("currency")
    private String currency;

    @Schema(description = "Billing interval", example = "MONTH")
    @JsonProperty("intervalUnit")
    private String intervalUnit;

    @Schema(description = "Billing interval count", example = "1")
    @JsonProperty("intervalCount")
    private Integer intervalCount;

    @Schema(description = "Current subscription status", example = "ACTIVE")
    @JsonProperty("status")
    private SubscriptionStatus status;

    @Schema(description = "Subscription creation date", example = "2024-01-01T00:00:00Z")
    @JsonProperty("createdAt")
    private ZonedDateTime createdAt;

    @Schema(description = "Current billing period start", example = "2024-01-01T00:00:00Z")
    @JsonProperty("currentPeriodStart")
    private ZonedDateTime currentPeriodStart;

    @Schema(description = "Current billing period end", example = "2024-02-01T00:00:00Z")
    @JsonProperty("currentPeriodEnd")
    private ZonedDateTime currentPeriodEnd;

    @Schema(description = "Trial period start date", example = "2024-01-01T00:00:00Z")
    @JsonProperty("trialStart")
    private ZonedDateTime trialStart;

    @Schema(description = "Trial period end date", example = "2024-01-15T00:00:00Z")
    @JsonProperty("trialEnd")
    private ZonedDateTime trialEnd;

    @Schema(description = "Next billing date", example = "2024-02-01T00:00:00Z")
    @JsonProperty("nextBillingDate")
    private ZonedDateTime nextBillingDate;

    @Schema(description = "Billing cycle anchor date", example = "2024-01-01T00:00:00Z")
    @JsonProperty("billingCycleAnchor")
    private ZonedDateTime billingCycleAnchor;

    @Schema(description = "Cancellation date (if cancelled)", example = "2024-01-15T00:00:00Z")
    @JsonProperty("cancelledAt")
    private ZonedDateTime cancelledAt;

    @Schema(description = "Cancellation reason", example = "Customer requested cancellation")
    @JsonProperty("cancellationReason")
    private String cancellationReason;

    @Schema(description = "Payment method identifier", example = "pm_12345")
    @JsonProperty("paymentMethodId")
    private String paymentMethodId;

    @Schema(description = "Payment method type", example = "CREDIT_CARD")
    @JsonProperty("paymentMethodType")
    private String paymentMethodType;

    @Schema(description = "Last 4 digits of payment method", example = "1234")
    @JsonProperty("paymentMethodLast4")
    private String paymentMethodLast4;

    @Schema(description = "Days until next billing", example = "15")
    @JsonProperty("daysUntilNextBilling")
    private Integer daysUntilNextBilling;

    @Schema(description = "Whether subscription is in trial period", example = "false")
    @JsonProperty("inTrialPeriod")
    private Boolean inTrialPeriod;

    @Schema(description = "Whether trial has expired", example = "true")
    @JsonProperty("trialExpired")
    private Boolean trialExpired;

    @Schema(description = "Latest invoice ID", example = "inv_12345")
    @JsonProperty("latestInvoiceId")
    private String latestInvoiceId;

    @Schema(description = "Number of unpaid invoices", example = "0")
    @JsonProperty("unpaidInvoicesCount")
    private Integer unpaidInvoicesCount;

    @Schema(description = "Additional subscription metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Whether subscription has changes pending", example = "false")
    @JsonProperty("hasPendingChanges")
    private Boolean hasPendingChanges;

    @Schema(description = "When pending changes will take effect", example = "2024-02-01T00:00:00Z")
    @JsonProperty("pendingChangesDate")
    private ZonedDateTime pendingChangesDate;

    @Schema(description = "Last update timestamp", example = "2024-01-01T12:00:00Z")
    @JsonProperty("updatedAt")
    private ZonedDateTime updatedAt;

    // Default constructor
    public SubscriptionResponse() {
    }

    // Getters and Setters
    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
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

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public void setCurrentPeriodStart(ZonedDateTime currentPeriodStart) {
        this.currentPeriodStart = currentPeriodStart;
    }

    public ZonedDateTime getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(ZonedDateTime currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public ZonedDateTime getTrialStart() {
        return trialStart;
    }

    public void setTrialStart(ZonedDateTime trialStart) {
        this.trialStart = trialStart;
    }

    public ZonedDateTime getTrialEnd() {
        return trialEnd;
    }

    public void setTrialEnd(ZonedDateTime trialEnd) {
        this.trialEnd = trialEnd;
    }

    public ZonedDateTime getNextBillingDate() {
        return nextBillingDate;
    }

    public void setNextBillingDate(ZonedDateTime nextBillingDate) {
        this.nextBillingDate = nextBillingDate;
    }

    public ZonedDateTime getBillingCycleAnchor() {
        return billingCycleAnchor;
    }

    public void setBillingCycleAnchor(ZonedDateTime billingCycleAnchor) {
        this.billingCycleAnchor = billingCycleAnchor;
    }

    public ZonedDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(ZonedDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getPaymentMethodType() {
        return paymentMethodType;
    }

    public void setPaymentMethodType(String paymentMethodType) {
        this.paymentMethodType = paymentMethodType;
    }

    public String getPaymentMethodLast4() {
        return paymentMethodLast4;
    }

    public void setPaymentMethodLast4(String paymentMethodLast4) {
        this.paymentMethodLast4 = paymentMethodLast4;
    }

    public Integer getDaysUntilNextBilling() {
        return daysUntilNextBilling;
    }

    public void setDaysUntilNextBilling(Integer daysUntilNextBilling) {
        this.daysUntilNextBilling = daysUntilNextBilling;
    }

    public Boolean getInTrialPeriod() {
        return inTrialPeriod;
    }

    public void setInTrialPeriod(Boolean inTrialPeriod) {
        this.inTrialPeriod = inTrialPeriod;
    }

    public Boolean getTrialExpired() {
        return trialExpired;
    }

    public void setTrialExpired(Boolean trialExpired) {
        this.trialExpired = trialExpired;
    }

    public String getLatestInvoiceId() {
        return latestInvoiceId;
    }

    public void setLatestInvoiceId(String latestInvoiceId) {
        this.latestInvoiceId = latestInvoiceId;
    }

    public Integer getUnpaidInvoicesCount() {
        return unpaidInvoicesCount;
    }

    public void setUnpaidInvoicesCount(Integer unpaidInvoicesCount) {
        this.unpaidInvoicesCount = unpaidInvoicesCount;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Boolean getHasPendingChanges() {
        return hasPendingChanges;
    }

    public void setHasPendingChanges(Boolean hasPendingChanges) {
        this.hasPendingChanges = hasPendingChanges;
    }

    public ZonedDateTime getPendingChangesDate() {
        return pendingChangesDate;
    }

    public void setPendingChangesDate(ZonedDateTime pendingChangesDate) {
        this.pendingChangesDate = pendingChangesDate;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "SubscriptionResponse{" +
                "subscriptionId='" + subscriptionId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", planCode='" + planCode + '\'' +
                ", status=" + status +
                ", planAmount=" + planAmount +
                ", currency='" + currency + '\'' +
                ", nextBillingDate=" + nextBillingDate +
                ", inTrialPeriod=" + inTrialPeriod +
                '}';
    }
}
