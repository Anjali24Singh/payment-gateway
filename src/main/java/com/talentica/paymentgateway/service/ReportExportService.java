package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.analytics.TransactionReportResponse;
import com.talentica.paymentgateway.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting reports in various formats (CSV, PDF, Excel).
 * Handles data transformation and file generation for analytics exports.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class ReportExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Export transaction data to CSV format.
     */
    public byte[] exportToCSV(List<Transaction> transactions) {
        log.info("Exporting {} transactions to CSV", transactions.size());
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(outputStream)) {
            
            // Write CSV header
            writer.println("Transaction ID,Customer ID,Amount,Currency,Status,Type,Created At,Processed At");
            
            // Write transaction data
            for (Transaction transaction : transactions) {
                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                    escapeCsvValue(transaction.getTransactionId()),
                    transaction.getCustomer() != null ? transaction.getCustomer().getId().toString() : "",
                    transaction.getAmount() != null ? transaction.getAmount().toString() : "0",
                    transaction.getCurrency() != null ? transaction.getCurrency() : "USD",
                    transaction.getStatus() != null ? transaction.getStatus().toString() : "",
                    transaction.getTransactionType() != null ? transaction.getTransactionType().toString() : "",
                    transaction.getCreatedAt() != null ? transaction.getCreatedAt().format(DATE_FORMATTER) : "",
                    transaction.getProcessedAt() != null ? transaction.getProcessedAt().format(DATE_FORMATTER) : ""
                ));
            }
            
            writer.flush();
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            log.error("Error exporting transactions to CSV", e);
            throw new RuntimeException("Failed to export CSV", e);
        }
    }

    /**
     * Export transaction data to PDF format.
     */
    public byte[] exportToPDF(List<Transaction> transactions) {
        log.info("Exporting {} transactions to PDF", transactions.size());
        
        // In a real implementation, you would use a PDF library like iText or PDFBox
        // For now, return a simple text-based representation
        StringBuilder pdfContent = new StringBuilder();
        pdfContent.append("TRANSACTION REPORT\n");
        pdfContent.append("=================\n\n");
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            pdfContent.append(String.format("Transaction: %s\n", transaction.getTransactionId()));
            pdfContent.append(String.format("Amount: %s %s\n", 
                transaction.getAmount(), transaction.getCurrency()));
            pdfContent.append(String.format("Status: %s\n", transaction.getStatus()));
            pdfContent.append(String.format("Date: %s\n", 
                transaction.getCreatedAt() != null ? transaction.getCreatedAt().format(DATE_FORMATTER) : "N/A"));
            pdfContent.append("\n");
            
            if (transaction.getAmount() != null) {
                totalAmount = totalAmount.add(transaction.getAmount());
            }
        }
        
        pdfContent.append(String.format("Total Amount: %s\n", totalAmount));
        pdfContent.append(String.format("Total Transactions: %d\n", transactions.size()));
        
        return pdfContent.toString().getBytes();
    }

    /**
     * Export analytics summary to various formats.
     */
    public byte[] exportSummary(TransactionReportResponse.TransactionAggregations aggregations, String format) {
        log.info("Exporting aggregations summary to {}", format);
        
        StringBuilder content = new StringBuilder();
        content.append("ANALYTICS SUMMARY\n");
        content.append("================\n\n");
        content.append(String.format("Total Transactions: %d\n", aggregations.getTotalTransactions()));
        content.append(String.format("Successful Transactions: %d\n", aggregations.getSuccessfulTransactions()));
        content.append(String.format("Failed Transactions: %d\n", aggregations.getFailedTransactions()));
        content.append(String.format("Total Volume: %s\n", aggregations.getTotalVolume()));
        content.append(String.format("Average Amount: %s\n", aggregations.getAverageAmount()));
        
        if (aggregations.getStatusBreakdown() != null) {
            content.append("\nStatus Breakdown:\n");
            aggregations.getStatusBreakdown().forEach((status, count) -> 
                content.append(String.format("  %s: %d\n", status, count)));
        }
        
        if (aggregations.getTypeBreakdown() != null) {
            content.append("\nType Breakdown:\n");
            aggregations.getTypeBreakdown().forEach((type, count) -> 
                content.append(String.format("  %s: %d\n", type, count)));
        }
        
        return content.toString().getBytes();
    }

    /**
     * Generate export filename based on type and current timestamp.
     */
    public String generateFileName(String exportType, String reportType) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now());
        return String.format("%s_report_%s.%s", reportType, timestamp, exportType.toLowerCase());
    }

    /**
     * Get MIME type for export format.
     */
    public String getMimeType(String exportFormat) {
        switch (exportFormat.toUpperCase()) {
            case "CSV":
                return "text/csv";
            case "PDF":
                return "application/pdf";
            case "EXCEL":
            case "XLS":
            case "XLSX":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * Estimate file size based on data and format.
     */
    public long estimateFileSize(int recordCount, String format) {
        switch (format.toUpperCase()) {
            case "CSV":
                return recordCount * 200L; // ~200 bytes per row estimate
            case "PDF":
                return recordCount * 300L; // ~300 bytes per row estimate
            case "EXCEL":
                return recordCount * 150L; // ~150 bytes per row estimate
            default:
                return recordCount * 250L; // Default estimate
        }
    }

    // Helper methods

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        
        // Escape CSV special characters
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
}
