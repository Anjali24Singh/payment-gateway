package com.talentica.paymentgateway.dto.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.talentica.paymentgateway.validation.ValidAmount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO for creating a new subscription plan.
 * Contains plan pricing, billing cycle, and feature configuration.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Schema(description = "Request to create a new subscription plan")
public class CreatePlanRequest {

    @NotBlank(message = "Plan code is required")
    @Size(max = 100, message = "Plan code must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Plan code can only contain letters, numbers, underscores, and hyphens")
    @Schema(description = "Unique plan identifier", example = "premium_monthly", required = true)
    @JsonProperty("planCode")
    private String planCode;

    @NotBlank(message = "Plan name is required")
    @Size(max = 255, message = "Plan name must not exceed 255 characters")
    @Schema(description = "Plan display name", example = "Premium Monthly Plan", required = true)
    @JsonProperty("name")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Plan description", example = "Premium features with monthly billing")
    @JsonProperty("description")
    private String description;

    @NotNull(message = "Amount is required")
    @ValidAmount
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Schema(description = "Plan price", example = "29.99", required = true)
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
    @Schema(description = "Currency code", example = "USD", required = true)
    @JsonProperty("currency")
    private String currency = "USD";

    @NotBlank(message = "Interval unit is required")
    @Pattern(regexp = "^(DAY|WEEK|MONTH|YEAR)$", message = "Interval unit must be DAY, WEEK, MONTH, or YEAR")
    @Schema(description = "Billing interval unit", example = "MONTH", required = true,
            allowableValues = {"DAY", "WEEK", "MONTH", "YEAR"})
    @JsonProperty("intervalUnit")
    private String intervalUnit = "MONTH";

    @NotNull(message = "Interval count is required")
    @Min(value = 1, message = "Interval count must be at least 1")
    @Max(value = 365, message = "Interval count must not exceed 365")
    @Schema(description = "Number of interval units between billings", example = "1", required = true)
    @JsonProperty("intervalCount")
    private Integer intervalCount = 1;

    @Min(value = 0, message = "Trial period days must be non-negative")
    @Max(value = 365, message = "Trial period days must not exceed 365")
    @Schema(description = "Trial period length in days", example = "14")
    @JsonProperty("trialPeriodDays")
    private Integer trialPeriodDays = 0;

    @Schema(description = "Whether the plan is active", example = "true")
    @JsonProperty("isActive")
    private Boolean isActive = true;

    @Schema(description = "Additional plan metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Plan features list")
    @JsonProperty("features")
    private java.util.List<String> features;

    @Schema(description = "Setup fee for the plan", example = "0.00")
    @JsonProperty("setupFee")
    private BigDecimal setupFee = BigDecimal.ZERO;

    @Min(value = 0, message = "Max subscribers must be non-negative")
    @Schema(description = "Maximum number of subscribers allowed (0 for unlimited)", example = "1000")
    @JsonProperty("maxSubscribers")
    private Integer maxSubscribers = 0;

    @Schema(description = "Plan category or type", example = "Premium")
    @JsonProperty("category")
    private String category;

    @Schema(description = "Plan display order", example = "1")
    @JsonProperty("sortOrder")
    private Integer sortOrder = 0;

    // Default constructor
    public CreatePlanRequest() {
    }

    // Constructor with required fields
    public CreatePlanRequest(String planCode, String name, BigDecimal amount, String intervalUnit, Integer intervalCount) {
        this.planCode = planCode;
        this.name = name;
        this.amount = amount;
        this.intervalUnit = intervalUnit;
        this.intervalCount = intervalCount;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public java.util.List<String> getFeatures() {
        return features;
    }

    public void setFeatures(java.util.List<String> features) {
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

    public boolean hasTrialPeriod() {
        return trialPeriodDays != null && trialPeriodDays > 0;
    }

    public boolean hasSetupFee() {
        return setupFee != null && setupFee.compareTo(BigDecimal.ZERO) > 0;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getFormattedInterval() {
        if (intervalCount == 1) {
            return "Every " + intervalUnit.toLowerCase();
        } else {
            return "Every " + intervalCount + " " + intervalUnit.toLowerCase() + "s";
        }
    }

    @Override
    public String toString() {
        return "CreatePlanRequest{" +
                "planCode='" + planCode + '\'' +
                ", name='" + name + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", intervalUnit='" + intervalUnit + '\'' +
                ", intervalCount=" + intervalCount +
                ", trialPeriodDays=" + trialPeriodDays +
                ", isActive=" + isActive +
                '}';
    }
}
