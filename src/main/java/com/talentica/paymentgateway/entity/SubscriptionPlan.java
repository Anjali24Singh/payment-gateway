package com.talentica.paymentgateway.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.talentica.paymentgateway.validation.ValidAmount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a subscription plan template.
 * Plans define the pricing, billing cycle, and terms for recurring subscriptions.
 */
@Entity
@Table(name = "subscription_plans",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "planCode")
       },
       indexes = {
           @Index(name = "idx_subscription_plans_code", columnList = "planCode"),
           @Index(name = "idx_subscription_plans_active", columnList = "isActive")
       })
public class SubscriptionPlan extends BaseEntity {

    @NotBlank(message = "Plan code is required")
    @Size(max = 100, message = "Plan code must not exceed 100 characters")
    @Column(name = "plan_code", nullable = false, unique = true, length = 100)
    private String planCode;

    @NotBlank(message = "Plan name is required")
    @Size(max = 255, message = "Plan name must not exceed 255 characters")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Amount is required")
    @ValidAmount
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Size(max = 3, message = "Currency must be 3 characters")
    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @NotBlank(message = "Interval unit is required")
    @Size(max = 20, message = "Interval unit must not exceed 20 characters")
    @Pattern(regexp = "DAY|WEEK|MONTH|YEAR", message = "Interval unit must be DAY, WEEK, MONTH, or YEAR")
    @Column(name = "interval_unit", nullable = false, length = 20)
    private String intervalUnit = "MONTH";

    @NotNull(message = "Interval count is required")
    @Min(value = 1, message = "Interval count must be at least 1")
    @Column(name = "interval_count", nullable = false)
    private Integer intervalCount = 1;

    @Min(value = 0, message = "Trial period days must be non-negative")
    @Column(name = "trial_period_days")
    private Integer trialPeriodDays = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "setup_fee", precision = 12, scale = 2)
    private BigDecimal setupFee = BigDecimal.ZERO;

    // Relationships
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Subscription> subscriptions = new ArrayList<>();

    // Constructors
    public SubscriptionPlan() {
        super();
    }

    public SubscriptionPlan(String planCode, String name, BigDecimal amount) {
        this();
        this.planCode = planCode;
        this.name = name;
        this.amount = amount;
    }

    public SubscriptionPlan(String planCode, String name, BigDecimal amount, String intervalUnit, Integer intervalCount) {
        this(planCode, name, amount);
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

    public BigDecimal getSetupFee() {
        return setupFee;
    }

    public void setSetupFee(BigDecimal setupFee) {
        this.setupFee = setupFee;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    // Utility methods
    public String getFormattedInterval() {
        if (intervalCount == 1) {
            return "Every " + intervalUnit.toLowerCase();
        } else {
            return "Every " + intervalCount + " " + intervalUnit.toLowerCase() + "s";
        }
    }

    public String getFormattedPrice() {
        return String.format("$%.2f", amount);
    }

    public String getDisplayName() {
        return name + " (" + getFormattedPrice() + " " + getFormattedInterval() + ")";
    }

    public boolean hasTrialPeriod() {
        return trialPeriodDays != null && trialPeriodDays > 0;
    }

    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    public long getActiveSubscriptionsCount() {
        return subscriptions.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .count();
    }

    public BigDecimal getTotalMonthlyRevenue() {
        BigDecimal monthlyAmount = calculateMonthlyAmount();
        return monthlyAmount.multiply(BigDecimal.valueOf(getActiveSubscriptionsCount()));
    }

    private BigDecimal calculateMonthlyAmount() {
        switch (intervalUnit.toUpperCase()) {
            case "DAY":
                return amount.multiply(BigDecimal.valueOf(30.44 / intervalCount)); // Average days per month
            case "WEEK":
                return amount.multiply(BigDecimal.valueOf(4.33 / intervalCount)); // Average weeks per month
            case "MONTH":
                return amount.divide(BigDecimal.valueOf(intervalCount), 2, java.math.RoundingMode.HALF_UP);
            case "YEAR":
                return amount.divide(BigDecimal.valueOf(intervalCount * 12), 2, java.math.RoundingMode.HALF_UP);
            default:
                return amount;
        }
    }
}
