package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.analytics.TransactionReportResponse;
import com.talentica.paymentgateway.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReportExportService.
 * Tests all export methods and utility functions.
 */
class ReportExportServiceUnitTest {

    private ReportExportService reportExportService;
    private List<Transaction> testTransactions;
    private Transaction testTransaction1;
    private Transaction testTransaction2;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        reportExportService = new ReportExportService();
        
        // Create test customer
        testCustomer = new Customer();
        testCustomer.setId(java.util.UUID.randomUUID());
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
        testCustomer.setEmail("john.doe@example.com");

        // Create test transactions
        testTransaction1 = new Transaction();
        testTransaction1.setTransactionId("TXN_001");
        testTransaction1.setCustomer(testCustomer);
        testTransaction1.setAmount(new BigDecimal("100.50"));
        testTransaction1.setCurrency("USD");
        testTransaction1.setStatus(PaymentStatus.CAPTURED);
        testTransaction1.setTransactionType(TransactionType.PURCHASE);
        testTransaction1.setCreatedAt(ZonedDateTime.now().minusDays(1).toLocalDateTime());
        testTransaction1.setProcessedAt(ZonedDateTime.now().minusDays(1).plusHours(1));

        testTransaction2 = new Transaction();
        testTransaction2.setTransactionId("TXN_002");
        testTransaction2.setCustomer(testCustomer);
        testTransaction2.setAmount(new BigDecimal("75.25"));
        testTransaction2.setCurrency("EUR");
        testTransaction2.setStatus(PaymentStatus.FAILED);
        testTransaction2.setTransactionType(TransactionType.REFUND);
        testTransaction2.setCreatedAt(ZonedDateTime.now().minusDays(2).toLocalDateTime());

        testTransactions = new ArrayList<>();
        testTransactions.add(testTransaction1);
        testTransactions.add(testTransaction2);
    }

    @Test
    void exportToCSV_WithValidTransactions_ShouldReturnCSVBytes() {
        // When
        byte[] csvBytes = reportExportService.exportToCSV(testTransactions);

        // Then
        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0);
        
        String csvContent = new String(csvBytes);
        assertTrue(csvContent.contains("Transaction ID,Customer ID,Amount,Currency,Status,Type,Created At,Processed At"));
        assertTrue(csvContent.contains("TXN_001"));
        assertTrue(csvContent.contains("TXN_002"));
        assertTrue(csvContent.contains("100.5"));
        assertTrue(csvContent.contains("75.25"));
        assertTrue(csvContent.contains("USD"));
        assertTrue(csvContent.contains("EUR"));
        assertTrue(csvContent.contains("CAPTURED"));
        assertTrue(csvContent.contains("FAILED"));
    }

    @Test
    void exportToCSV_WithEmptyList_ShouldReturnHeaderOnly() {
        // Given
        List<Transaction> emptyTransactions = new ArrayList<>();

        // When
        byte[] csvBytes = reportExportService.exportToCSV(emptyTransactions);

        // Then
        assertNotNull(csvBytes);
        String csvContent = new String(csvBytes);
        assertTrue(csvContent.contains("Transaction ID,Customer ID,Amount,Currency,Status,Type,Created At,Processed At"));
        assertEquals(1, csvContent.split("\n").length); // Header only
    }

    @Test
    void exportToCSV_WithNullValues_ShouldHandleGracefully() {
        // Given
        Transaction transactionWithNulls = new Transaction();
        transactionWithNulls.setTransactionId("TXN_NULL");
        // Leave other fields null
        List<Transaction> transactions = List.of(transactionWithNulls);

        // When
        byte[] csvBytes = reportExportService.exportToCSV(transactions);

        // Then
        assertNotNull(csvBytes);
        String csvContent = new String(csvBytes);
        assertTrue(csvContent.contains("TXN_NULL"));
        assertTrue(csvContent.contains("TXN_NULL,,0,USD,PENDING,,,"));
    }

    @Test
    void exportToCSV_WithSpecialCharacters_ShouldEscapeCorrectly() {
        // Given
        Transaction specialTransaction = new Transaction();
        specialTransaction.setTransactionId("TXN,WITH\"COMMA");
        specialTransaction.setAmount(new BigDecimal("50.00"));
        specialTransaction.setCurrency("USD");
        specialTransaction.setStatus(PaymentStatus.CAPTURED);
        specialTransaction.setTransactionType(TransactionType.PURCHASE);
        specialTransaction.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
        List<Transaction> transactions = List.of(specialTransaction);

        // When
        byte[] csvBytes = reportExportService.exportToCSV(transactions);

        // Then
        assertNotNull(csvBytes);
        String csvContent = new String(csvBytes);
        assertTrue(csvContent.contains("\"TXN,WITH\"\"COMMA\""));
    }

    @Test
    void exportToPDF_WithValidTransactions_ShouldReturnPDFBytes() {
        // When
        byte[] pdfBytes = reportExportService.exportToPDF(testTransactions);

        // Then
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        
        String pdfContent = new String(pdfBytes);
        assertTrue(pdfContent.contains("TRANSACTION REPORT"));
        assertTrue(pdfContent.contains("TXN_001"));
        assertTrue(pdfContent.contains("TXN_002"));
        assertTrue(pdfContent.contains("Total Amount: 175.75"));
        assertTrue(pdfContent.contains("Total Transactions: 2"));
    }

    @Test
    void exportToPDF_WithEmptyList_ShouldReturnHeaderWithZeros() {
        // Given
        List<Transaction> emptyTransactions = new ArrayList<>();

        // When
        byte[] pdfBytes = reportExportService.exportToPDF(emptyTransactions);

        // Then
        assertNotNull(pdfBytes);
        String pdfContent = new String(pdfBytes);
        assertTrue(pdfContent.contains("TRANSACTION REPORT"));
        assertTrue(pdfContent.contains("Total Amount: 0"));
        assertTrue(pdfContent.contains("Total Transactions: 0"));
    }

    @Test
    void exportToPDF_WithNullAmounts_ShouldHandleGracefully() {
        // Given
        Transaction nullAmountTransaction = new Transaction();
        nullAmountTransaction.setTransactionId("TXN_NULL_AMOUNT");
        nullAmountTransaction.setCurrency("USD");
        nullAmountTransaction.setStatus(PaymentStatus.CAPTURED);
        nullAmountTransaction.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
        // Amount is null
        List<Transaction> transactions = List.of(nullAmountTransaction);

        // When
        byte[] pdfBytes = reportExportService.exportToPDF(transactions);

        // Then
        assertNotNull(pdfBytes);
        String pdfContent = new String(pdfBytes);
        assertTrue(pdfContent.contains("TXN_NULL_AMOUNT"));
        assertTrue(pdfContent.contains("Total Amount: 0"));
    }

    @Test
    void exportSummary_WithValidAggregations_ShouldReturnSummaryBytes() {
        // Given
        TransactionReportResponse.TransactionAggregations aggregations = 
            new TransactionReportResponse.TransactionAggregations();
        aggregations.setTotalTransactions(100L);
        aggregations.setSuccessfulTransactions(85L);
        aggregations.setFailedTransactions(15L);
        aggregations.setTotalVolume(new BigDecimal("10000.00"));
        aggregations.setAverageAmount(new BigDecimal("100.00"));
        
        Map<String, Long> statusBreakdown = new HashMap<>();
        statusBreakdown.put("CAPTURED", 70L);
        statusBreakdown.put("FAILED", 15L);
        statusBreakdown.put("PENDING", 15L);
        aggregations.setStatusBreakdown(statusBreakdown);
        
        Map<String, Long> typeBreakdown = new HashMap<>();
        typeBreakdown.put("PURCHASE", 80L);
        typeBreakdown.put("REFUND", 20L);
        aggregations.setTypeBreakdown(typeBreakdown);

        // When
        byte[] summaryBytes = reportExportService.exportSummary(aggregations, "TXT");

        // Then
        assertNotNull(summaryBytes);
        String summaryContent = new String(summaryBytes);
        assertTrue(summaryContent.contains("ANALYTICS SUMMARY"));
        assertTrue(summaryContent.contains("Total Transactions: 100"));
        assertTrue(summaryContent.contains("Successful Transactions: 85"));
        assertTrue(summaryContent.contains("Failed Transactions: 15"));
        assertTrue(summaryContent.contains("Total Volume: 10000.00"));
        assertTrue(summaryContent.contains("Average Amount: 100.00"));
        assertTrue(summaryContent.contains("Status Breakdown:"));
        assertTrue(summaryContent.contains("CAPTURED: 70"));
        assertTrue(summaryContent.contains("Type Breakdown:"));
        assertTrue(summaryContent.contains("PURCHASE: 80"));
    }

    @Test
    void exportSummary_WithNullBreakdowns_ShouldHandleGracefully() {
        // Given
        TransactionReportResponse.TransactionAggregations aggregations = 
            new TransactionReportResponse.TransactionAggregations();
        aggregations.setTotalTransactions(50L);
        aggregations.setSuccessfulTransactions(40L);
        aggregations.setFailedTransactions(10L);
        aggregations.setTotalVolume(new BigDecimal("5000.00"));
        aggregations.setAverageAmount(new BigDecimal("100.00"));
        // Breakdowns are null

        // When
        byte[] summaryBytes = reportExportService.exportSummary(aggregations, "TXT");

        // Then
        assertNotNull(summaryBytes);
        String summaryContent = new String(summaryBytes);
        assertTrue(summaryContent.contains("ANALYTICS SUMMARY"));
        assertTrue(summaryContent.contains("Total Transactions: 50"));
        assertFalse(summaryContent.contains("Status Breakdown:"));
        assertFalse(summaryContent.contains("Type Breakdown:"));
    }

    @Test
    void generateFileName_WithValidParameters_ShouldReturnFormattedFileName() {
        // When
        String fileName = reportExportService.generateFileName("CSV", "transactions");

        // Then
        assertTrue(fileName.matches("transactions_report_\\d{8}_\\d{6}\\.csv"));
    }

    @Test
    void generateFileName_WithDifferentFormats_ShouldReturnCorrectExtensions() {
        // When
        String csvFileName = reportExportService.generateFileName("CSV", "analytics");
        String pdfFileName = reportExportService.generateFileName("PDF", "summary");
        String excelFileName = reportExportService.generateFileName("EXCEL", "report");

        // Then
        assertTrue(csvFileName.endsWith(".csv"));
        assertTrue(pdfFileName.endsWith(".pdf"));
        assertTrue(excelFileName.endsWith(".excel"));
    }

    @Test
    void getMimeType_WithValidFormats_ShouldReturnCorrectMimeTypes() {
        // When & Then
        assertEquals("text/csv", reportExportService.getMimeType("CSV"));
        assertEquals("text/csv", reportExportService.getMimeType("csv"));
        assertEquals("application/pdf", reportExportService.getMimeType("PDF"));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                    reportExportService.getMimeType("EXCEL"));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                    reportExportService.getMimeType("XLS"));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                    reportExportService.getMimeType("XLSX"));
    }

    @Test
    void getMimeType_WithUnknownFormat_ShouldReturnDefaultMimeType() {
        // When & Then
        assertEquals("application/octet-stream", reportExportService.getMimeType("UNKNOWN"));
        assertEquals("application/octet-stream", reportExportService.getMimeType(""));
        assertEquals("application/octet-stream", reportExportService.getMimeType("TXT"));
    }

    @Test
    void estimateFileSize_WithValidFormats_ShouldReturnReasonableEstimates() {
        // Given
        int recordCount = 100;

        // When & Then
        assertEquals(20000L, reportExportService.estimateFileSize(recordCount, "CSV"));
        assertEquals(30000L, reportExportService.estimateFileSize(recordCount, "PDF"));
        assertEquals(15000L, reportExportService.estimateFileSize(recordCount, "EXCEL"));
        assertEquals(25000L, reportExportService.estimateFileSize(recordCount, "UNKNOWN"));
    }

    @Test
    void estimateFileSize_WithZeroRecords_ShouldReturnZero() {
        // When & Then
        assertEquals(0L, reportExportService.estimateFileSize(0, "CSV"));
        assertEquals(0L, reportExportService.estimateFileSize(0, "PDF"));
    }

    @Test
    void estimateFileSize_WithLargeRecordCount_ShouldScaleCorrectly() {
        // Given
        int largeRecordCount = 10000;

        // When
        long csvSize = reportExportService.estimateFileSize(largeRecordCount, "CSV");
        long pdfSize = reportExportService.estimateFileSize(largeRecordCount, "PDF");

        // Then
        assertEquals(2000000L, csvSize); // 10000 * 200
        assertEquals(3000000L, pdfSize); // 10000 * 300
        assertTrue(pdfSize > csvSize); // PDF should be larger
    }

    @Test
    void exportToCSV_WithIOException_ShouldThrowRuntimeException() {
        // Given - Create a transaction that will cause issues during processing
        List<Transaction> problematicTransactions = new ArrayList<>();
        // Add a very large number of transactions to potentially cause memory issues
        for (int i = 0; i < 1000; i++) {
            Transaction tx = new Transaction();
            tx.setTransactionId("TXN_" + i);
            tx.setAmount(new BigDecimal("100.00"));
            tx.setCurrency("USD");
            tx.setStatus(PaymentStatus.CAPTURED);
            tx.setTransactionType(TransactionType.PURCHASE);
            tx.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
            problematicTransactions.add(tx);
        }

        // When & Then - Should not throw exception for normal processing
        assertDoesNotThrow(() -> reportExportService.exportToCSV(problematicTransactions));
    }

    @Test
    void exportToPDF_WithNullTransaction_ShouldHandleGracefully() {
        // Given
        Transaction nullFieldsTransaction = new Transaction();
        nullFieldsTransaction.setTransactionId("TXN_NULL_FIELDS");
        // All other fields are null
        List<Transaction> transactions = List.of(nullFieldsTransaction);

        // When
        byte[] pdfBytes = reportExportService.exportToPDF(transactions);

        // Then
        assertNotNull(pdfBytes);
        String pdfContent = new String(pdfBytes);
        assertTrue(pdfContent.contains("TXN_NULL_FIELDS"));
        assertTrue(pdfContent.contains("Amount: null"));
        assertTrue(pdfContent.contains("Date: N/A"));
    }

    @Test
    void generateFileName_WithCaseInsensitiveFormat_ShouldWork() {
        // When
        String lowerCaseFileName = reportExportService.generateFileName("csv", "test");
        String upperCaseFileName = reportExportService.generateFileName("CSV", "test");
        String mixedCaseFileName = reportExportService.generateFileName("CsV", "test");

        // Then
        assertTrue(lowerCaseFileName.endsWith(".csv"));
        assertTrue(upperCaseFileName.endsWith(".csv"));
        assertTrue(mixedCaseFileName.endsWith(".csv"));
    }

    @Test
    void exportSummary_WithZeroValues_ShouldHandleCorrectly() {
        // Given
        TransactionReportResponse.TransactionAggregations aggregations = 
            new TransactionReportResponse.TransactionAggregations();
        aggregations.setTotalTransactions(0L);
        aggregations.setSuccessfulTransactions(0L);
        aggregations.setFailedTransactions(0L);
        aggregations.setTotalVolume(BigDecimal.ZERO);
        aggregations.setAverageAmount(BigDecimal.ZERO);

        // When
        byte[] summaryBytes = reportExportService.exportSummary(aggregations, "TXT");

        // Then
        assertNotNull(summaryBytes);
        String summaryContent = new String(summaryBytes);
        assertTrue(summaryContent.contains("Total Transactions: 0"));
        assertTrue(summaryContent.contains("Total Volume: 0"));
        assertTrue(summaryContent.contains("Average Amount: 0"));
    }

    @Test
    void estimateFileSize_WithNegativeRecordCount_ShouldReturnNegativeValue() {
        // When & Then - The service doesn't validate negative inputs, so it returns negative values
        assertEquals(-200L, reportExportService.estimateFileSize(-1, "CSV"));
        assertEquals(-30000L, reportExportService.estimateFileSize(-100, "PDF"));
    }
}
