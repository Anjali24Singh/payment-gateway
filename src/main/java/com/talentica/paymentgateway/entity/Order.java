package com.talentica.paymentgateway.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.talentica.paymentgateway.validation.ValidAmount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity representing an order that can contain multiple line items and associated transactions.
 * Orders track the complete purchase lifecycle and financial details.
 */
@Entity
@Table(name = "orders",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "orderNumber")
       },
       indexes = {
           @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
           @Index(name = "idx_orders_user_id", columnList = "user_id"),
           @Index(name = "idx_orders_number", columnList = "orderNumber"),
           @Index(name = "idx_orders_status", columnList = "status"),
           @Index(name = "idx_orders_payment_status", columnList = "paymentStatus"),
           @Index(name = "idx_orders_created_at", columnList = "createdAt")
       })
public class Order extends BaseEntity {

    @NotBlank(message = "Order number is required")
    @Size(max = 100, message = "Order number must not exceed 100 characters")
    @Column(name = "order_number", nullable = false, unique = true, length = 100)
    private String orderNumber;

    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Size(max = 3, message = "Currency must be 3 characters")
    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @NotNull(message = "Subtotal amount is required")
    @ValidAmount
    @DecimalMin(value = "0.0", inclusive = true, message = "Subtotal amount must be non-negative")
    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @ValidAmount
    @DecimalMin(value = "0.0", inclusive = true, message = "Tax amount must be non-negative")
    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @ValidAmount
    @DecimalMin(value = "0.0", inclusive = true, message = "Shipping amount must be non-negative")
    @Column(name = "shipping_amount", precision = 12, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @ValidAmount
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount amount must be non-negative")
    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @NotNull(message = "Total amount is required")
    @ValidAmount
    @DecimalMin(value = "0.0", inclusive = true, message = "Total amount must be non-negative")
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Size(max = 50, message = "Status must not exceed 50 characters")
    @Column(name = "status", length = 50)
    private String status = "PENDING";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private Map<String, Object> metadata = new HashMap<>();

    // Relationships
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    // Constructors
    public Order() {
        super();
    }

    public Order(String orderNumber, Customer customer, BigDecimal subtotalAmount) {
        this();
        this.orderNumber = orderNumber;
        this.customer = customer;
        this.subtotalAmount = subtotalAmount;
        calculateTotalAmount();
    }

    // Getters and Setters
    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
        calculateTotalAmount();
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
        calculateTotalAmount();
    }

    public BigDecimal getShippingAmount() {
        return shippingAmount;
    }

    public void setShippingAmount(BigDecimal shippingAmount) {
        this.shippingAmount = shippingAmount;
        calculateTotalAmount();
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
        calculateTotalAmount();
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    // Utility methods
    private void calculateTotalAmount() {
        if (subtotalAmount != null) {
            BigDecimal tax = taxAmount != null ? taxAmount : BigDecimal.ZERO;
            BigDecimal shipping = shippingAmount != null ? shippingAmount : BigDecimal.ZERO;
            BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
            
            this.totalAmount = subtotalAmount.add(tax).add(shipping).subtract(discount);
        }
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setOrder(this);
    }

    public void removeTransaction(Transaction transaction) {
        transactions.remove(transaction);
        transaction.setOrder(null);
    }

    public BigDecimal getPaidAmount() {
        return transactions.stream()
                .filter(t -> t.getStatus() == PaymentStatus.CAPTURED || t.getStatus() == PaymentStatus.SETTLED)
                .filter(t -> t.getTransactionType() == TransactionType.PURCHASE || t.getTransactionType() == TransactionType.CAPTURE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getRefundedAmount() {
        return transactions.stream()
                .filter(t -> t.getStatus() == PaymentStatus.SETTLED)
                .filter(t -> t.getTransactionType() == TransactionType.REFUND || t.getTransactionType() == TransactionType.PARTIAL_REFUND)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getOutstandingAmount() {
        return totalAmount.subtract(getPaidAmount()).add(getRefundedAmount());
    }

    public boolean isFullyPaid() {
        return getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0;
    }

    public boolean isPartiallyPaid() {
        BigDecimal paidAmount = getPaidAmount();
        return paidAmount.compareTo(BigDecimal.ZERO) > 0 && paidAmount.compareTo(totalAmount) < 0;
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

/**
 * JPA Converter for Map to JSON conversion
 */
@Converter
class MapToJsonConverter implements AttributeConverter<Map<String, Object>, String> {
    
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
            new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Error converting map to JSON", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(dbData, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to map", e);
        }
    }
}
