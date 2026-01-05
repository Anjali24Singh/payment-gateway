package com.talentica.paymentgateway.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.talentica.paymentgateway.validation.ValidAmount;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing an invoice for a subscription billing period.
 * Invoices track the billing details, payment attempts, and transaction references.
 */
@Entity
@Table(name = "subscription_invoices",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "invoiceNumber")
       },
       indexes = {
           @Index(name = "idx_subscription_invoices_subscription_id", columnList = "subscription_id"),
           @Index(name = "idx_subscription_invoices_customer_id", columnList = "customer_id"),
           @Index(name = "idx_subscription_invoices_status", columnList = "status"),
           @Index(name = "idx_subscription_invoices_due_date", columnList = "dueDate")
       })
public class SubscriptionInvoice extends BaseEntity {

    @NotBlank(message = "Invoice number is required")
    @Size(max = 100, message = "Invoice number must not exceed 100 characters")
    @Column(name = "invoice_number", nullable = false, unique = true, length = 100)
    private String invoiceNumber;

    @NotNull(message = "Subscription is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull(message = "Amount is required")
    @ValidAmount
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Size(max = 3, message = "Currency must be 3 characters")
    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Size(max = 50, message = "Status must not exceed 50 characters")
    @Column(name = "status", length = 50)
    private String status = "PENDING";

    @NotNull(message = "Period start is required")
    @Column(name = "period_start", nullable = false)
    private ZonedDateTime periodStart;

    @NotNull(message = "Period end is required")
    @Column(name = "period_end", nullable = false)
    private ZonedDateTime periodEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(name = "paid_at")
    private ZonedDateTime paidAt;

    @Column(name = "due_date")
    private ZonedDateTime dueDate;

    @Min(value = 0, message = "Payment attempts must be non-negative")
    @Column(name = "payment_attempts")
    private Integer paymentAttempts = 0;

    @Column(name = "next_payment_attempt")
    private ZonedDateTime nextPaymentAttempt;

    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private Map<String, Object> metadata = new HashMap<>();

    // Constructors
    public SubscriptionInvoice() {
        super();
    }

    public SubscriptionInvoice(String invoiceNumber, Subscription subscription, Customer customer, 
                             BigDecimal amount, ZonedDateTime periodStart, ZonedDateTime periodEnd) {
        this();
        this.invoiceNumber = invoiceNumber;
        this.subscription = subscription;
        this.customer = customer;
        this.amount = amount;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.dueDate = ZonedDateTime.now().plusDays(30); // Default 30 days due date
    }

    // Getters and Setters
    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ZonedDateTime getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(ZonedDateTime periodStart) {
        this.periodStart = periodStart;
    }

    public ZonedDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(ZonedDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public ZonedDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(ZonedDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public ZonedDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(ZonedDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getPaymentAttempts() {
        return paymentAttempts;
    }

    public void setPaymentAttempts(Integer paymentAttempts) {
        this.paymentAttempts = paymentAttempts;
    }

    public ZonedDateTime getNextPaymentAttempt() {
        return nextPaymentAttempt;
    }

    public void setNextPaymentAttempt(ZonedDateTime nextPaymentAttempt) {
        this.nextPaymentAttempt = nextPaymentAttempt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Utility methods
    public boolean isPaid() {
        return "PAID".equals(status);
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    public boolean isOverdue() {
        return dueDate != null && ZonedDateTime.now().isAfter(dueDate) && !isPaid();
    }

    public boolean isDue() {
        return dueDate != null && !ZonedDateTime.now().isBefore(dueDate.minusDays(1)) && !isPaid();
    }

    public void markAsPaid(Transaction transaction) {
        this.status = "PAID";
        this.paidAt = ZonedDateTime.now();
        this.transaction = transaction;
        this.nextPaymentAttempt = null;
    }

    public void markAsFailed() {
        this.status = "FAILED";
        this.paymentAttempts++;
        scheduleNextPaymentAttempt();
    }

    public void markAsProcessing() {
        this.status = "PROCESSING";
        this.paymentAttempts++;
    }

    public void markAsCancelled() {
        this.status = "CANCELLED";
        this.nextPaymentAttempt = null;
    }

    public void scheduleNextPaymentAttempt() {
        if (paymentAttempts < 5) { // Max 5 attempts
            // Exponential backoff: 1 day, 3 days, 7 days, 14 days, 30 days
            int[] retryDays = {1, 3, 7, 14, 30};
            int dayIndex = Math.min(paymentAttempts - 1, retryDays.length - 1);
            this.nextPaymentAttempt = ZonedDateTime.now().plusDays(retryDays[dayIndex]);
        } else {
            this.nextPaymentAttempt = null; // No more retries
        }
    }

    public boolean canRetryPayment() {
        return paymentAttempts < 5 && 
               nextPaymentAttempt != null && 
               ZonedDateTime.now().isAfter(nextPaymentAttempt);
    }

    public int getDaysUntilDue() {
        if (dueDate == null || isPaid()) {
            return -1;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(ZonedDateTime.now(), dueDate);
    }

    public int getDaysOverdue() {
        if (dueDate == null || !isOverdue()) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(dueDate, ZonedDateTime.now());
    }

    public String getBillingPeriodDescription() {
        if (periodStart != null && periodEnd != null) {
            return String.format("Billing period: %s to %s", 
                    periodStart.toLocalDate(), 
                    periodEnd.toLocalDate());
        }
        return "Unknown billing period";
    }

    public String getFormattedAmount() {
        return String.format("$%.2f %s", amount, currency);
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

    public void reset() {
        this.status = "PENDING";
        this.paymentAttempts = 0;
        this.nextPaymentAttempt = null;
        this.paidAt = null;
        this.transaction = null;
    }
}
