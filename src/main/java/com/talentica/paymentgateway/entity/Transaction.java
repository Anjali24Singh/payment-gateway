package com.talentica.paymentgateway.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.talentica.paymentgateway.validation.ValidAmount;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a payment transaction.
 * Transactions capture all payment processing details including Authorize.Net responses.
 */
@Entity
@Table(name = "transactions",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "transactionId"),
           @UniqueConstraint(columnNames = "idempotencyKey")
       },
       indexes = {
           @Index(name = "idx_transactions_order_id", columnList = "order_id"),
           @Index(name = "idx_transactions_customer_id", columnList = "customer_id"),
           @Index(name = "idx_transactions_transaction_id", columnList = "transactionId"),
           @Index(name = "idx_transactions_authnet_id", columnList = "authnetTransactionId"),
           @Index(name = "idx_transactions_parent_id", columnList = "parent_transaction_id"),
           @Index(name = "idx_transactions_idempotency_key", columnList = "idempotencyKey"),
           @Index(name = "idx_transactions_correlation_id", columnList = "correlationId"),
           @Index(name = "idx_transactions_status", columnList = "status"),
           @Index(name = "idx_transactions_type", columnList = "transactionType"),
           @Index(name = "idx_transactions_created_at", columnList = "createdAt")
       })
public class Transaction extends BaseEntity {

    @NotBlank(message = "Transaction ID is required")
    @Size(max = 100, message = "Transaction ID must not exceed 100 characters")
    @Column(name = "transaction_id", nullable = false, unique = true, length = 100)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @NotNull(message = "Amount is required")
    @ValidAmount
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Size(max = 3, message = "Currency must be 3 characters")
    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "payment_status")
    private PaymentStatus status = PaymentStatus.PENDING;

    // Authorize.Net specific fields
    @Size(max = 100, message = "Authorize.Net transaction ID must not exceed 100 characters")
    @Column(name = "authnet_transaction_id", length = 100)
    private String authnetTransactionId;

    @Size(max = 20, message = "Authorize.Net auth code must not exceed 20 characters")
    @Column(name = "authnet_auth_code", length = 20)
    private String authnetAuthCode;

    @Size(max = 10, message = "Authorize.Net AVS result must not exceed 10 characters")
    @Column(name = "authnet_avs_result", length = 10)
    private String authnetAvsResult;

    @Size(max = 10, message = "Authorize.Net CVV result must not exceed 10 characters")
    @Column(name = "authnet_cvv_result", length = 10)
    private String authnetCvvResult;

    @Size(max = 10, message = "Authorize.Net response code must not exceed 10 characters")
    @Column(name = "authnet_response_code", length = 10)
    private String authnetResponseCode;

    @Size(max = 255, message = "Authorize.Net response reason must not exceed 255 characters")
    @Column(name = "authnet_response_reason", length = 255)
    private String authnetResponseReason;

    // Parent transaction for captures, voids, refunds
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_transaction_id")
    private Transaction parentTransaction;

    // Request and response data as JSON
    @Convert(converter = MapToJsonConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_data", columnDefinition = "JSONB")
    private Map<String, Object> requestData = new HashMap<>();

    @Convert(converter = MapToJsonConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_data", columnDefinition = "JSONB")
    private Map<String, Object> responseData = new HashMap<>();

    // Idempotency key for preventing duplicate transactions
    @Size(max = 255, message = "Idempotency key must not exceed 255 characters")
    @Column(name = "idempotency_key", unique = true, length = 255)
    private String idempotencyKey;

    // Correlation ID for tracing
    @Size(max = 100, message = "Correlation ID must not exceed 100 characters")
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "processed_at")
    private ZonedDateTime processedAt;

    // Relationships
    @OneToMany(mappedBy = "parentTransaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> childTransactions = new ArrayList<>();

    // Constructors
    public Transaction() {
        super();
    }

    public Transaction(String transactionId, Customer customer, TransactionType transactionType, BigDecimal amount) {
        this();
        this.transactionId = transactionId;
        this.customer = customer;
        this.transactionType = transactionType;
        this.amount = amount;
    }

    // Getters and Setters
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
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

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getAuthnetTransactionId() {
        return authnetTransactionId;
    }

    public void setAuthnetTransactionId(String authnetTransactionId) {
        this.authnetTransactionId = authnetTransactionId;
    }

    public String getAuthnetAuthCode() {
        return authnetAuthCode;
    }

    public void setAuthnetAuthCode(String authnetAuthCode) {
        this.authnetAuthCode = authnetAuthCode;
    }

    public String getAuthnetAvsResult() {
        return authnetAvsResult;
    }

    public void setAuthnetAvsResult(String authnetAvsResult) {
        this.authnetAvsResult = authnetAvsResult;
    }

    public String getAuthnetCvvResult() {
        return authnetCvvResult;
    }

    public void setAuthnetCvvResult(String authnetCvvResult) {
        this.authnetCvvResult = authnetCvvResult;
    }

    public String getAuthnetResponseCode() {
        return authnetResponseCode;
    }

    public void setAuthnetResponseCode(String authnetResponseCode) {
        this.authnetResponseCode = authnetResponseCode;
    }

    public String getAuthnetResponseReason() {
        return authnetResponseReason;
    }

    public void setAuthnetResponseReason(String authnetResponseReason) {
        this.authnetResponseReason = authnetResponseReason;
    }

    public Transaction getParentTransaction() {
        return parentTransaction;
    }

    public void setParentTransaction(Transaction parentTransaction) {
        this.parentTransaction = parentTransaction;
    }

    public Map<String, Object> getRequestData() {
        return requestData;
    }

    public void setRequestData(Map<String, Object> requestData) {
        this.requestData = requestData;
    }

    public Map<String, Object> getResponseData() {
        return responseData;
    }

    public void setResponseData(Map<String, Object> responseData) {
        this.responseData = responseData;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public ZonedDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(ZonedDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public List<Transaction> getChildTransactions() {
        return childTransactions;
    }

    public void setChildTransactions(List<Transaction> childTransactions) {
        this.childTransactions = childTransactions;
    }

    // Utility methods
    public boolean isSuccessful() {
        return status == PaymentStatus.AUTHORIZED || 
               status == PaymentStatus.CAPTURED || 
               status == PaymentStatus.SETTLED;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED || 
               status == PaymentStatus.VOIDED || 
               status == PaymentStatus.CANCELLED;
    }

    public boolean canBeVoided() {
        return status == PaymentStatus.AUTHORIZED && 
               (transactionType == TransactionType.AUTHORIZE || transactionType == TransactionType.PURCHASE);
    }

    public boolean canBeCaptured() {
        return status == PaymentStatus.AUTHORIZED && transactionType == TransactionType.AUTHORIZE;
    }

    public boolean canBeRefunded() {
        return status == PaymentStatus.SETTLED && 
               (transactionType == TransactionType.PURCHASE || transactionType == TransactionType.CAPTURE);
    }

    public void addChildTransaction(Transaction childTransaction) {
        childTransactions.add(childTransaction);
        childTransaction.setParentTransaction(this);
    }

    public void removeChildTransaction(Transaction childTransaction) {
        childTransactions.remove(childTransaction);
        childTransaction.setParentTransaction(null);
    }

    public BigDecimal getRefundedAmount() {
        return childTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.REFUND || 
                           t.getTransactionType() == TransactionType.PARTIAL_REFUND)
                .filter(t -> t.getStatus() == PaymentStatus.SETTLED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getAvailableRefundAmount() {
        if (!canBeRefunded()) {
            return BigDecimal.ZERO;
        }
        return amount.subtract(getRefundedAmount());
    }

    public void markAsProcessed() {
        this.processedAt = ZonedDateTime.now();
    }

    public void addRequestData(String key, Object value) {
        if (requestData == null) {
            requestData = new HashMap<>();
        }
        requestData.put(key, value);
    }

    public void addResponseData(String key, Object value) {
        if (responseData == null) {
            responseData = new HashMap<>();
        }
        responseData.put(key, value);
    }
}
