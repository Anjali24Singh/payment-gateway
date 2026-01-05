package com.talentica.paymentgateway.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.talentica.paymentgateway.util.SubscriptionStatusConverter;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity representing an active subscription for a customer.
 * Subscriptions track the complete billing lifecycle including trials, renewals, and cancellations.
 */
@Entity
@Table(name = "subscriptions",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "subscriptionId")
       },
       indexes = {
           @Index(name = "idx_subscriptions_customer_id", columnList = "customer_id"),
           @Index(name = "idx_subscriptions_plan_id", columnList = "plan_id"),
           @Index(name = "idx_subscriptions_subscription_id", columnList = "subscriptionId"),
           @Index(name = "idx_subscriptions_status", columnList = "status"),
           @Index(name = "idx_subscriptions_next_billing", columnList = "nextBillingDate")
       })
public class Subscription extends BaseEntity {

    @NotBlank(message = "Subscription ID is required")
    @Size(max = 100, message = "Subscription ID must not exceed 100 characters")
    @Column(name = "subscription_id", nullable = false, unique = true, length = 100)
    private String subscriptionId;

    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull(message = "Plan is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @NotNull(message = "Payment method is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Convert(converter = SubscriptionStatusConverter.class)
    @Column(name = "status")
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    @Column(name = "current_period_start")
    private ZonedDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private ZonedDateTime currentPeriodEnd;

    @Column(name = "trial_start")
    private ZonedDateTime trialStart;

    @Column(name = "trial_end")
    private ZonedDateTime trialEnd;

    @Column(name = "next_billing_date")
    private ZonedDateTime nextBillingDate;

    @Column(name = "billing_cycle_anchor")
    private ZonedDateTime billingCycleAnchor;

    @Column(name = "cancelled_at")
    private ZonedDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata = new HashMap<>();

    // Relationships
    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SubscriptionInvoice> invoices = new ArrayList<>();

    // Constructors
    public Subscription() {
        super();
    }

    public Subscription(String subscriptionId, Customer customer, SubscriptionPlan plan, PaymentMethod paymentMethod) {
        this();
        this.subscriptionId = subscriptionId;
        this.customer = customer;
        this.plan = plan;
        this.paymentMethod = paymentMethod;
    }

    // Getters and Setters
    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<SubscriptionInvoice> getInvoices() {
        return invoices;
    }

    public void setInvoices(List<SubscriptionInvoice> invoices) {
        this.invoices = invoices;
    }

    // Utility methods
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public boolean isCancelled() {
        return status == SubscriptionStatus.CANCELLED;
    }

    public boolean isInTrial() {
        if (trialStart == null || trialEnd == null) {
            return false;
        }
        ZonedDateTime now = ZonedDateTime.now();
        return !now.isBefore(trialStart) && now.isBefore(trialEnd);
    }

    public boolean hasTrialExpired() {
        if (trialEnd == null) {
            return false;
        }
        return ZonedDateTime.now().isAfter(trialEnd);
    }

    public boolean isDue() {
        return nextBillingDate != null && ZonedDateTime.now().isAfter(nextBillingDate);
    }

    public boolean isPastDue() {
        return status == SubscriptionStatus.PAST_DUE;
    }

    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
        if (currentPeriodStart == null) {
            this.currentPeriodStart = ZonedDateTime.now();
        }
        calculateNextBillingCycle();
    }

    public void pause() {
        this.status = SubscriptionStatus.PAUSED;
    }

    public void cancel(String reason) {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = ZonedDateTime.now();
        this.cancellationReason = reason;
        this.nextBillingDate = null;
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.nextBillingDate = null;
    }

    public void markAsPastDue() {
        this.status = SubscriptionStatus.PAST_DUE;
    }

    public void calculateNextBillingCycle() {
        if (plan == null) {
            return;
        }

        ZonedDateTime startDate = currentPeriodStart != null ? currentPeriodStart : ZonedDateTime.now();
        
        // Calculate next period based on plan interval
        switch (plan.getIntervalUnit().toUpperCase()) {
            case "DAY":
                this.currentPeriodEnd = startDate.plusDays(plan.getIntervalCount());
                break;
            case "WEEK":
                this.currentPeriodEnd = startDate.plusWeeks(plan.getIntervalCount());
                break;
            case "MONTH":
                this.currentPeriodEnd = startDate.plusMonths(plan.getIntervalCount());
                break;
            case "YEAR":
                this.currentPeriodEnd = startDate.plusYears(plan.getIntervalCount());
                break;
            default:
                this.currentPeriodEnd = startDate.plusMonths(1); // Default to monthly
        }

        this.nextBillingDate = this.currentPeriodEnd;
    }

    public void startTrial() {
        if (plan != null && plan.getTrialPeriodDays() != null && plan.getTrialPeriodDays() > 0) {
            this.trialStart = ZonedDateTime.now();
            this.trialEnd = this.trialStart.plusDays(plan.getTrialPeriodDays());
            this.nextBillingDate = this.trialEnd;
        }
    }

    public void advanceBillingCycle() {
        if (currentPeriodEnd != null) {
            this.currentPeriodStart = currentPeriodEnd;
            calculateNextBillingCycle();
        }
    }

    public void addInvoice(SubscriptionInvoice invoice) {
        invoices.add(invoice);
        invoice.setSubscription(this);
    }

    public void removeInvoice(SubscriptionInvoice invoice) {
        invoices.remove(invoice);
        invoice.setSubscription(null);
    }

    public SubscriptionInvoice getLatestInvoice() {
        return invoices.stream()
                .max((i1, i2) -> i1.getCreatedAt().compareTo(i2.getCreatedAt()))
                .orElse(null);
    }

    public List<SubscriptionInvoice> getUnpaidInvoices() {
        return invoices.stream()
                .filter(i -> !"PAID".equals(i.getStatus()))
                .toList();
    }

    public int getDaysUntilNextBilling() {
        if (nextBillingDate == null) {
            return -1;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(ZonedDateTime.now(), nextBillingDate);
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
}
