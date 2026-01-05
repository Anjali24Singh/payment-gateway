package com.talentica.paymentgateway.dto.analytics;

import com.talentica.paymentgateway.entity.PaymentStatus;
import com.talentica.paymentgateway.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * DTO for transaction reporting requests with comprehensive filtering options.
 * Supports date ranges, amounts, statuses, and export formats.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Schema(description = "Transaction reporting request with filtering and export options")
public class TransactionReportRequest {
    
    @Schema(description = "Start date for the report period")
    private ZonedDateTime startDate;
    
    @Schema(description = "End date for the report period")
    private ZonedDateTime endDate;
    
    @Schema(description = "Filter by customer ID")
    private UUID customerId;
    
    @Schema(description = "Filter by order ID")
    private UUID orderId;
    
    @Schema(description = "Filter by payment status")
    private PaymentStatus status;
    
    @Schema(description = "Filter by transaction type")
    private TransactionType transactionType;
    
    @Schema(description = "Minimum transaction amount")
    private BigDecimal minAmount;
    
    @Schema(description = "Maximum transaction amount")
    private BigDecimal maxAmount;
    
    @Schema(description = "Currency filter (e.g., USD, EUR)")
    private String currency;
    
    @Schema(description = "Payment method type filter")
    private String paymentMethodType;
    
    @Schema(description = "Include successful transactions only")
    private Boolean successfulOnly;
    
    @Schema(description = "Include failed transactions only")
    private Boolean failedOnly;
    
    @Schema(description = "Group results by time period (DAY, WEEK, MONTH)")
    private String groupBy;
    
    @Schema(description = "Export format (JSON, CSV, PDF)", allowableValues = {"JSON", "CSV", "PDF"})
    private String exportFormat = "JSON";
    
    @Schema(description = "Page number for pagination")
    @Min(value = 0, message = "Page number must be non-negative")
    private Integer page = 0;
    
    @Schema(description = "Page size for pagination")
    @Min(value = 1, message = "Page size must be positive")
    @Max(value = 1000, message = "Page size must not exceed 1000")
    private Integer size = 50;
    
    @Schema(description = "Sort field")
    private String sortBy = "createdAt";
    
    @Schema(description = "Sort direction (ASC, DESC)")
    private String sortDirection = "DESC";
    
    @Schema(description = "Include metadata in the response")
    private Boolean includeMetadata = true;
    
    @Schema(description = "Include aggregation statistics")
    private Boolean includeAggregations = false;
    
    // Constructors
    public TransactionReportRequest() {}
    
    // Getters and Setters
    public ZonedDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(ZonedDateTime startDate) {
        this.startDate = startDate;
    }
    
    public ZonedDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(ZonedDateTime endDate) {
        this.endDate = endDate;
    }
    
    public UUID getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
    
    public TransactionType getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }
    
    public BigDecimal getMinAmount() {
        return minAmount;
    }
    
    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }
    
    public BigDecimal getMaxAmount() {
        return maxAmount;
    }
    
    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getPaymentMethodType() {
        return paymentMethodType;
    }
    
    public void setPaymentMethodType(String paymentMethodType) {
        this.paymentMethodType = paymentMethodType;
    }
    
    public Boolean getSuccessfulOnly() {
        return successfulOnly;
    }
    
    public void setSuccessfulOnly(Boolean successfulOnly) {
        this.successfulOnly = successfulOnly;
    }
    
    public Boolean getFailedOnly() {
        return failedOnly;
    }
    
    public void setFailedOnly(Boolean failedOnly) {
        this.failedOnly = failedOnly;
    }
    
    public String getGroupBy() {
        return groupBy;
    }
    
    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }
    
    public String getExportFormat() {
        return exportFormat;
    }
    
    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }
    
    public Integer getPage() {
        return page;
    }
    
    public void setPage(Integer page) {
        this.page = page;
    }
    
    public Integer getSize() {
        return size;
    }
    
    public void setSize(Integer size) {
        this.size = size;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
    
    public String getSortDirection() {
        return sortDirection;
    }
    
    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
    
    public Boolean getIncludeMetadata() {
        return includeMetadata;
    }
    
    public void setIncludeMetadata(Boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }
    
    public Boolean getIncludeAggregations() {
        return includeAggregations;
    }
    
    public void setIncludeAggregations(Boolean includeAggregations) {
        this.includeAggregations = includeAggregations;
    }
}
