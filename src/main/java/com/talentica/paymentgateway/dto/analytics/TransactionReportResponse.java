package com.talentica.paymentgateway.dto.analytics;

import com.talentica.paymentgateway.entity.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for transaction reporting responses with data and metadata.
 * Provides comprehensive transaction report with aggregations and export metadata.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Schema(description = "Transaction reporting response with data and analytics")
public class TransactionReportResponse {
    
    @Schema(description = "Transaction data matching the filter criteria")
    private List<Transaction> transactions;
    
    @Schema(description = "Report metadata including totals and statistics")
    private ReportMetadata metadata;
    
    @Schema(description = "Aggregated statistics")
    private TransactionAggregations aggregations;
    
    @Schema(description = "Export information")
    private ExportInfo exportInfo;
    
    @Schema(description = "Time series data for charts")
    private List<TimeSeriesData> timeSeries;
    
    // Constructors
    public TransactionReportResponse() {}
    
    public TransactionReportResponse(List<Transaction> transactions, ReportMetadata metadata) {
        this.transactions = transactions;
        this.metadata = metadata;
    }
    
    // Getters and Setters
    public List<Transaction> getTransactions() {
        return transactions;
    }
    
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
    
    public ReportMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(ReportMetadata metadata) {
        this.metadata = metadata;
    }
    
    public TransactionAggregations getAggregations() {
        return aggregations;
    }
    
    public void setAggregations(TransactionAggregations aggregations) {
        this.aggregations = aggregations;
    }
    
    public ExportInfo getExportInfo() {
        return exportInfo;
    }
    
    public void setExportInfo(ExportInfo exportInfo) {
        this.exportInfo = exportInfo;
    }
    
    public List<TimeSeriesData> getTimeSeries() {
        return timeSeries;
    }
    
    public void setTimeSeries(List<TimeSeriesData> timeSeries) {
        this.timeSeries = timeSeries;
    }
    
    /**
     * Report metadata including pagination and summary statistics.
     */
    @Schema(description = "Report metadata and summary information")
    public static class ReportMetadata {
        private int totalRecords;
        private int currentPage;
        private int pageSize;
        private int totalPages;
        private ZonedDateTime reportGeneratedAt;
        private ZonedDateTime periodStart;
        private ZonedDateTime periodEnd;
        
        // Constructors
        public ReportMetadata() {}
        
        public ReportMetadata(int totalRecords, int currentPage, int pageSize, int totalPages) {
            this.totalRecords = totalRecords;
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalPages = totalPages;
            this.reportGeneratedAt = ZonedDateTime.now();
        }
        
        // Getters and Setters
        public int getTotalRecords() {
            return totalRecords;
        }
        
        public void setTotalRecords(int totalRecords) {
            this.totalRecords = totalRecords;
        }
        
        public int getCurrentPage() {
            return currentPage;
        }
        
        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }
        
        public int getPageSize() {
            return pageSize;
        }
        
        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
        
        public int getTotalPages() {
            return totalPages;
        }
        
        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
        
        public ZonedDateTime getReportGeneratedAt() {
            return reportGeneratedAt;
        }
        
        public void setReportGeneratedAt(ZonedDateTime reportGeneratedAt) {
            this.reportGeneratedAt = reportGeneratedAt;
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
    }
    
    /**
     * Transaction aggregations and statistics.
     */
    @Schema(description = "Aggregated transaction statistics")
    public static class TransactionAggregations {
        private long totalTransactions;
        private long successfulTransactions;
        private long failedTransactions;
        private BigDecimal totalVolume;
        private BigDecimal averageAmount;
        private BigDecimal medianAmount;
        private Map<String, Long> statusBreakdown;
        private Map<String, Long> typeBreakdown;
        private Map<String, BigDecimal> currencyBreakdown;
        
        // Constructors
        public TransactionAggregations() {}
        
        // Getters and Setters
        public long getTotalTransactions() {
            return totalTransactions;
        }
        
        public void setTotalTransactions(long totalTransactions) {
            this.totalTransactions = totalTransactions;
        }
        
        public long getSuccessfulTransactions() {
            return successfulTransactions;
        }
        
        public void setSuccessfulTransactions(long successfulTransactions) {
            this.successfulTransactions = successfulTransactions;
        }
        
        public long getFailedTransactions() {
            return failedTransactions;
        }
        
        public void setFailedTransactions(long failedTransactions) {
            this.failedTransactions = failedTransactions;
        }
        
        public BigDecimal getTotalVolume() {
            return totalVolume;
        }
        
        public void setTotalVolume(BigDecimal totalVolume) {
            this.totalVolume = totalVolume;
        }
        
        public BigDecimal getAverageAmount() {
            return averageAmount;
        }
        
        public void setAverageAmount(BigDecimal averageAmount) {
            this.averageAmount = averageAmount;
        }
        
        public BigDecimal getMedianAmount() {
            return medianAmount;
        }
        
        public void setMedianAmount(BigDecimal medianAmount) {
            this.medianAmount = medianAmount;
        }
        
        public Map<String, Long> getStatusBreakdown() {
            return statusBreakdown;
        }
        
        public void setStatusBreakdown(Map<String, Long> statusBreakdown) {
            this.statusBreakdown = statusBreakdown;
        }
        
        public Map<String, Long> getTypeBreakdown() {
            return typeBreakdown;
        }
        
        public void setTypeBreakdown(Map<String, Long> typeBreakdown) {
            this.typeBreakdown = typeBreakdown;
        }
        
        public Map<String, BigDecimal> getCurrencyBreakdown() {
            return currencyBreakdown;
        }
        
        public void setCurrencyBreakdown(Map<String, BigDecimal> currencyBreakdown) {
            this.currencyBreakdown = currencyBreakdown;
        }
    }
    
    /**
     * Export information and download links.
     */
    @Schema(description = "Export information and file details")
    public static class ExportInfo {
        private String exportFormat;
        private String downloadUrl;
        private String fileName;
        private long fileSizeBytes;
        private ZonedDateTime expiresAt;
        
        // Constructors
        public ExportInfo() {}
        
        public ExportInfo(String exportFormat, String fileName) {
            this.exportFormat = exportFormat;
            this.fileName = fileName;
        }
        
        // Getters and Setters
        public String getExportFormat() {
            return exportFormat;
        }
        
        public void setExportFormat(String exportFormat) {
            this.exportFormat = exportFormat;
        }
        
        public String getDownloadUrl() {
            return downloadUrl;
        }
        
        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        
        public long getFileSizeBytes() {
            return fileSizeBytes;
        }
        
        public void setFileSizeBytes(long fileSizeBytes) {
            this.fileSizeBytes = fileSizeBytes;
        }
        
        public ZonedDateTime getExpiresAt() {
            return expiresAt;
        }
        
        public void setExpiresAt(ZonedDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
    
    /**
     * Time series data for charts and visualization.
     */
    @Schema(description = "Time series data point for charting")
    public static class TimeSeriesData {
        private ZonedDateTime timestamp;
        private long transactionCount;
        private BigDecimal volume;
        private BigDecimal averageAmount;
        private String period;
        
        // Constructors
        public TimeSeriesData() {}
        
        public TimeSeriesData(ZonedDateTime timestamp, long transactionCount, BigDecimal volume) {
            this.timestamp = timestamp;
            this.transactionCount = transactionCount;
            this.volume = volume;
        }
        
        // Getters and Setters
        public ZonedDateTime getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(ZonedDateTime timestamp) {
            this.timestamp = timestamp;
        }
        
        public long getTransactionCount() {
            return transactionCount;
        }
        
        public void setTransactionCount(long transactionCount) {
            this.transactionCount = transactionCount;
        }
        
        public BigDecimal getVolume() {
            return volume;
        }
        
        public void setVolume(BigDecimal volume) {
            this.volume = volume;
        }
        
        public BigDecimal getAverageAmount() {
            return averageAmount;
        }
        
        public void setAverageAmount(BigDecimal averageAmount) {
            this.averageAmount = averageAmount;
        }
        
        public String getPeriod() {
            return period;
        }
        
        public void setPeriod(String period) {
            this.period = period;
        }
    }
}
