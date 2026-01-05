package com.talentica.paymentgateway.dto.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for subscription plan operations.
 * Contains plan configuration and usage statistics.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Schema(description = "Subscription plan details and statistics")
public class PlanResponse {

    @Schema(description = "Plan identifier", example = "premium_monthly")
    @JsonProperty("planCode")
    private String planCode;

    @Schema(description = "Plan display name", example = "Premium Monthly Plan")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Plan description", example = "Premium features with monthly billing")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Plan price", example = "29.99")
    @JsonProperty("amount")
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "USD")
    @JsonProperty("currency")
    private String currency;

    @Schema(description = "Billing interval unit", example = "MONTH")
    @JsonProperty("intervalUnit")
    private String intervalUnit;

    @Schema(description = "Number of interval units between billings", example = "1")
    @JsonProperty("intervalCount")
    private Integer intervalCount;

    @Schema(description = "Trial period length in days", example = "14")
    @JsonProperty("trialPeriodDays")
    private Integer trialPeriodDays;

    @Schema(description = "Whether the plan is active", example = "true")
    @JsonProperty("isActive")
    private Boolean isActive;

    @Schema(description = "Plan creation date", example = "2024-01-01T00:00:00Z")
    @JsonProperty("createdAt")
    private ZonedDateTime createdAt;

    @Schema(description = "Plan last update date", example = "2024-01-01T12:00:00Z")
    @JsonProperty("updatedAt")
    private ZonedDateTime updatedAt;

    @Schema(description = "Additional plan metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Plan features list")
    @JsonProperty("features")
    private List<String> features;

    @Schema(description = "Setup fee for the plan", example = "0.00")
    @JsonProperty("setupFee")
    private BigDecimal setupFee;

    @Schema(description = "Maximum number of subscribers allowed", example = "1000")
    @JsonProperty("maxSubscribers")
    private Integer maxSubscribers;

    @Schema(description = "Plan category or type", example = "Premium")
    @JsonProperty("category")
    private String category;

    @Schema(description = "Plan display order", example = "1")
    @JsonProperty("sortOrder")
    private Integer sortOrder;

    // Usage Statistics
    @Schema(description = "Number of active subscriptions", example = "125")
    @JsonProperty("activeSubscriptions")
    private Long activeSubscriptions;

    @Schema(description = "Total number of subscriptions ever created", example = "200")
    @JsonProperty("totalSubscriptions")
    private Long totalSubscriptions;

    @Schema(description = "Monthly recurring revenue from this plan", example = "3749.75")
    @JsonProperty("monthlyRecurringRevenue")
    private BigDecimal monthlyRecurringRevenue;

    @Schema(description = "Formatted billing interval", example = "Every month")
    @JsonProperty("formattedInterval")
    private String formattedInterval;

    @Schema(description = "Formatted price", example = "$29.99")
    @JsonProperty("formattedPrice")
    private String formattedPrice;

    @Schema(description = "Display name with price and interval", example = "Premium Monthly Plan ($29.99 every month)")
    @JsonProperty("displayName")
    private String displayName;

    @Schema(description = "Whether plan has trial period", example = "true")
    @JsonProperty("hasTrialPeriod")
    private Boolean hasTrialPeriod;

    @Schema(description = "Whether plan has setup fee", example = "false")
    @JsonProperty("hasSetupFee")
    private Boolean hasSetupFee;

    @Schema(description = "Whether plan is at subscriber limit", example = "false")
    @JsonProperty("atSubscriberLimit")
    private Boolean atSubscriberLimit;

    // Default constructor
    public PlanResponse() {
    }

    // Getters and Setters
    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public Integer getTrialPeriodDays() {
        return trialPeriodDays;
    }

    public void setTrialPeriodDays(Integer trialPeriodDays) {
        this.trialPeriodDays = trialPeriodDays;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public BigDecimal getSetupFee() {
        return setupFee;
    }

    public void setSetupFee(BigDecimal setupFee) {
        this.setupFee = setupFee;
    }

    public Integer getMaxSubscribers() {
        return maxSubscribers;
    }

    public void setMaxSubscribers(Integer maxSubscribers) {
        this.maxSubscribers = maxSubscribers;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getActiveSubscriptions() {
        return activeSubscriptions;
    }

    public void setActiveSubscriptions(Long activeSubscriptions) {
        this.activeSubscriptions = activeSubscriptions;
    }

    public Long getTotalSubscriptions() {
        return totalSubscriptions;
    }

    public void setTotalSubscriptions(Long totalSubscriptions) {
        this.totalSubscriptions = totalSubscriptions;
    }

    public BigDecimal getMonthlyRecurringRevenue() {
        return monthlyRecurringRevenue;
    }

    public void setMonthlyRecurringRevenue(BigDecimal monthlyRecurringRevenue) {
        this.monthlyRecurringRevenue = monthlyRecurringRevenue;
    }

    public String getFormattedInterval() {
        return formattedInterval;
    }

    public void setFormattedInterval(String formattedInterval) {
        this.formattedInterval = formattedInterval;
    }

    public String getFormattedPrice() {
        return formattedPrice;
    }

    public void setFormattedPrice(String formattedPrice) {
        this.formattedPrice = formattedPrice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getHasTrialPeriod() {
        return hasTrialPeriod;
    }

    public void setHasTrialPeriod(Boolean hasTrialPeriod) {
        this.hasTrialPeriod = hasTrialPeriod;
    }

    public Boolean getHasSetupFee() {
        return hasSetupFee;
    }

    public void setHasSetupFee(Boolean hasSetupFee) {
        this.hasSetupFee = hasSetupFee;
    }

    public Boolean getAtSubscriberLimit() {
        return atSubscriberLimit;
    }

    public void setAtSubscriberLimit(Boolean atSubscriberLimit) {
        this.atSubscriberLimit = atSubscriberLimit;
    }

    @Override
    public String toString() {
        return "PlanResponse{" +
                "planCode='" + planCode + '\'' +
                ", name='" + name + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", intervalUnit='" + intervalUnit + '\'' +
                ", intervalCount=" + intervalCount +
                ", activeSubscriptions=" + activeSubscriptions +
                ", isActive=" + isActive +
                '}';
    }
}
