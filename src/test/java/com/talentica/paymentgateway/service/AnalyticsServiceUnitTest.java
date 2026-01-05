package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.analytics.*;
import com.talentica.paymentgateway.dto.metrics.*;
import com.talentica.paymentgateway.entity.*;
import com.talentica.paymentgateway.repository.*;
import com.talentica.paymentgateway.util.CorrelationIdUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceUnitTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private ZonedDateTime startDate;
    private ZonedDateTime endDate;
    private List<Transaction> testTransactions;
    private Transaction successfulTransaction;
    private Transaction failedTransaction;

    @BeforeEach
    void setUp() {
        startDate = ZonedDateTime.now().minusDays(30);
        endDate = ZonedDateTime.now();
        
        successfulTransaction = createTransaction("txn-1", PaymentStatus.SETTLED, TransactionType.PURCHASE, new BigDecimal("100.00"));
        failedTransaction = createTransaction("txn-2", PaymentStatus.FAILED, TransactionType.PURCHASE, new BigDecimal("50.00"));
        testTransactions = Arrays.asList(successfulTransaction, failedTransaction);
    }

    @Test
    void generateTransactionReport_WithBasicRequest_ShouldReturnReport() {
        // Given
        TransactionReportRequest request = createBasicReportRequest();
        Page<Transaction> transactionPage = new PageImpl<>(testTransactions, PageRequest.of(0, 10), 2);
        
        when(transactionRepository.findTransactionsWithFilters(
            any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(transactionPage);

        try (MockedStatic<CorrelationIdUtil> mockedUtil = mockStatic(CorrelationIdUtil.class)) {
            mockedUtil.when(CorrelationIdUtil::getOrGenerate).thenReturn("test-correlation-id");

            // When
            TransactionReportResponse response = analyticsService.generateTransactionReport(request);

            // Then
            assertNotNull(response);
            assertEquals(2, response.getTransactions().size());
            verify(transactionRepository).findTransactionsWithFilters(
                any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class));
        }
    }

    @Test
    void generateTransactionReport_WithMetadata_ShouldIncludeMetadata() {
        // Given
        TransactionReportRequest request = createBasicReportRequest();
        request.setIncludeMetadata(true);
        Page<Transaction> transactionPage = new PageImpl<>(testTransactions, PageRequest.of(0, 10), 2);
        
        when(transactionRepository.findTransactionsWithFilters(
            any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(transactionPage);

        try (MockedStatic<CorrelationIdUtil> mockedUtil = mockStatic(CorrelationIdUtil.class)) {
            mockedUtil.when(CorrelationIdUtil::getOrGenerate).thenReturn("test-correlation-id");

            // When
            TransactionReportResponse response = analyticsService.generateTransactionReport(request);

            // Then
            assertNotNull(response.getMetadata());
            assertEquals(2, response.getMetadata().getTotalRecords());
            assertEquals(0, response.getMetadata().getCurrentPage());
            assertEquals(10, response.getMetadata().getPageSize());
            assertEquals(1, response.getMetadata().getTotalPages());
        }
    }

    @Test
    void generateTransactionReport_WithAggregations_ShouldIncludeAggregations() {
        // Given
        TransactionReportRequest request = createBasicReportRequest();
        request.setIncludeAggregations(true);
        Page<Transaction> transactionPage = new PageImpl<>(testTransactions, PageRequest.of(0, 10), 2);
        
        when(transactionRepository.findTransactionsWithFilters(
            any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(transactionPage);

        try (MockedStatic<CorrelationIdUtil> mockedUtil = mockStatic(CorrelationIdUtil.class)) {
            mockedUtil.when(CorrelationIdUtil::getOrGenerate).thenReturn("test-correlation-id");

            // When
            TransactionReportResponse response = analyticsService.generateTransactionReport(request);

            // Then
            assertNotNull(response.getAggregations());
            assertEquals(2, response.getAggregations().getTotalTransactions());
            assertEquals(1, response.getAggregations().getSuccessfulTransactions());
            assertEquals(1, response.getAggregations().getFailedTransactions());
            assertEquals(new BigDecimal("100.00"), response.getAggregations().getTotalVolume());
        }
    }

    @Test
    void generateTransactionReport_WithTimeSeries_ShouldIncludeTimeSeries() {
        // Given
        TransactionReportRequest request = createBasicReportRequest();
        request.setGroupBy("DAY");
        Page<Transaction> transactionPage = new PageImpl<>(testTransactions, PageRequest.of(0, 10), 2);
        List<Object[]> timeSeriesData = Arrays.asList(
            new Object[]{ZonedDateTime.now(), 5L, new BigDecimal("500.00")},
            new Object[]{ZonedDateTime.now().minusDays(1), 3L, new BigDecimal("300.00")}
        );
        
        when(transactionRepository.findTransactionsWithFilters(
            any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(transactionPage);
        when(transactionRepository.getTransactionSummaryByDate(any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(timeSeriesData);

        try (MockedStatic<CorrelationIdUtil> mockedUtil = mockStatic(CorrelationIdUtil.class)) {
            mockedUtil.when(CorrelationIdUtil::getOrGenerate).thenReturn("test-correlation-id");

            // When
            TransactionReportResponse response = analyticsService.generateTransactionReport(request);

            // Then
            assertNotNull(response.getTimeSeries());
            assertEquals(2, response.getTimeSeries().size());
            assertEquals("DAY", response.getTimeSeries().get(0).getPeriod());
        }
    }

    @Test
    void generateTransactionReport_WithExportFormat_ShouldIncludeExportInfo() {
        // Given
        TransactionReportRequest request = createBasicReportRequest();
        request.setExportFormat("CSV");
        Page<Transaction> transactionPage = new PageImpl<>(testTransactions, PageRequest.of(0, 10), 2);
        
        when(transactionRepository.findTransactionsWithFilters(
            any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(transactionPage);

        try (MockedStatic<CorrelationIdUtil> mockedUtil = mockStatic(CorrelationIdUtil.class)) {
            mockedUtil.when(CorrelationIdUtil::getOrGenerate).thenReturn("test-correlation-id");

            // When
            TransactionReportResponse response = analyticsService.generateTransactionReport(request);

            // Then
            assertNotNull(response.getExportInfo());
            assertEquals("CSV", response.getExportInfo().getExportFormat());
            assertTrue(response.getExportInfo().getFileName().endsWith(".csv"));
            assertNotNull(response.getExportInfo().getDownloadUrl());
        }
    }

    @Test
    void generateDashboardMetrics_ShouldReturnComprehensiveMetrics() {
        // Given
        AnalyticsDashboardRequest request = new AnalyticsDashboardRequest();
        request.setStartDate(startDate);
        request.setEndDate(endDate);

        Object[] transactionStats = {10L, 8L, 2L, new BigDecimal("800.00"), new BigDecimal("100.00")};
        
        when(transactionRepository.getTransactionStatistics(any(ZonedDateTime.class)))
            .thenReturn(transactionStats);
        when(subscriptionRepository.countByCreatedAtBetween(any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(5L);
        when(subscriptionRepository.countByStatusAndCancelledAtBetween(anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(2L);
        when(subscriptionRepository.countByStatus(anyString()))
            .thenReturn(15L);
        when(subscriptionRepository.calculateActiveMonthlyRevenue())
            .thenReturn(new BigDecimal("1500.00"));
        when(subscriptionRepository.countActiveAtDate(any(ZonedDateTime.class)))
            .thenReturn(20L);
        when(transactionRepository.sumAmountByStatus(PaymentStatus.SETTLED.name()))
            .thenReturn(new BigDecimal("10000.00"));
        when(transactionRepository.sumAmountByStatus(PaymentStatus.REFUNDED.name()))
            .thenReturn(new BigDecimal("500.00"));
        when(customerRepository.count())
            .thenReturn(100L);

        try (MockedStatic<CorrelationIdUtil> mockedUtil = mockStatic(CorrelationIdUtil.class)) {
            mockedUtil.when(CorrelationIdUtil::getOrGenerate).thenReturn("test-correlation-id");

            // When
            DashboardMetrics metrics = analyticsService.generateDashboardMetrics(request);

            // Then
            assertNotNull(metrics);
            assertNotNull(metrics.getTransactionMetrics());
            assertNotNull(metrics.getSubscriptionMetrics());
            assertNotNull(metrics.getRevenueMetrics());
            assertEquals(100L, metrics.getTotalCustomers());
        }
    }

    @Test
    void generateTransactionMetrics_WithValidData_ShouldReturnMetrics() {
        // Given
        Object[] stats = {10L, 8L, 2L, new BigDecimal("800.00"), new BigDecimal("100.00")};
        when(transactionRepository.getTransactionStatistics(any(ZonedDateTime.class)))
            .thenReturn(stats);

        // When
        TransactionMetrics metrics = analyticsService.generateTransactionMetrics(startDate, endDate);

        // Then
        assertNotNull(metrics);
        assertEquals(8L, metrics.getSuccessfulTransactions());
        assertEquals(2L, metrics.getFailedTransactions());
        assertEquals(new BigDecimal("800.00"), metrics.getSuccessfulAmount());
        assertNotNull(metrics.getAverageTransactionAmount());
        assertEquals(10L, metrics.getTotalTransactions());
    }

    @Test
    void generateTransactionMetrics_WithNullStats_ShouldReturnZeroMetrics() {
        // Given
        when(transactionRepository.getTransactionStatistics(any(ZonedDateTime.class)))
            .thenReturn(null);

        // When
        TransactionMetrics metrics = analyticsService.generateTransactionMetrics(startDate, endDate);

        // Then
        assertNotNull(metrics);
        assertEquals(0L, metrics.getSuccessfulTransactions());
        assertEquals(0L, metrics.getFailedTransactions());
        assertEquals(BigDecimal.ZERO, metrics.getSuccessfulAmount());
    }

    @Test
    void generateSubscriptionMetrics_ShouldReturnMetrics() {
        // Given
        lenient().when(subscriptionRepository.countByCreatedAtBetween(any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(5L);
        lenient().when(subscriptionRepository.countByStatusAndCancelledAtBetween(anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(2L);
        lenient().when(subscriptionRepository.countByStatus(anyString()))
            .thenReturn(15L);
        lenient().when(subscriptionRepository.calculateActiveMonthlyRevenue())
            .thenReturn(new BigDecimal("1500.00"));
        lenient().when(subscriptionRepository.countActiveAtDate(any(ZonedDateTime.class)))
            .thenReturn(20L);

        // When
        SubscriptionMetrics metrics = analyticsService.generateSubscriptionMetrics(startDate, endDate);

        // Then
        assertNotNull(metrics);
        assertNotNull(metrics.getActiveSubscriptions());
        assertNotNull(metrics.getCancelledSubscriptions());
        assertNotNull(metrics.getChurnRate());
    }

    @Test
    void generateRevenueMetrics_ShouldReturnMetrics() {
        // Given
        when(transactionRepository.sumAmountByStatus(PaymentStatus.SETTLED.name()))
            .thenReturn(new BigDecimal("10000.00"));
        when(transactionRepository.sumAmountByStatus(PaymentStatus.REFUNDED.name()))
            .thenReturn(new BigDecimal("500.00"));
        when(subscriptionRepository.calculateActiveMonthlyRevenue())
            .thenReturn(new BigDecimal("1500.00"));

        // When
        RevenueMetrics metrics = analyticsService.generateRevenueMetrics(startDate, endDate);

        // Then
        assertNotNull(metrics);
        assertEquals(new BigDecimal("10000.00"), metrics.getTotalRevenue());
        assertEquals(new BigDecimal("1500.00"), metrics.getRecurringRevenue());
        assertEquals(new BigDecimal("1500.00"), metrics.getMonthlyRecurringRevenue());
    }

    @Test
    void generateRevenueMetrics_WithNullValues_ShouldHandleGracefully() {
        // Given
        when(transactionRepository.sumAmountByStatus(PaymentStatus.SETTLED.name()))
            .thenReturn(null);
        when(transactionRepository.sumAmountByStatus(PaymentStatus.REFUNDED.name()))
            .thenReturn(null);
        when(subscriptionRepository.calculateActiveMonthlyRevenue())
            .thenReturn(null);

        // When
        RevenueMetrics metrics = analyticsService.generateRevenueMetrics(startDate, endDate);

        // Then
        assertNotNull(metrics);
        assertEquals(BigDecimal.ZERO, metrics.getTotalRevenue());
        assertEquals(BigDecimal.ZERO, metrics.getRecurringRevenue());
        assertEquals(BigDecimal.ZERO, metrics.getMonthlyRecurringRevenue());
    }

    @Test
    void analyzeFailedPayments_ShouldReturnAnalysis() {
        // Given
        List<Transaction> failedTransactions = Arrays.asList(
            createFailedTransactionWithErrorCode("txn-1", "2", new BigDecimal("100.00")),
            createFailedTransactionWithErrorCode("txn-2", "3", new BigDecimal("200.00"))
        );
        
        when(transactionRepository.findTransactionsBetween(any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(failedTransactions);
        when(transactionRepository.countTransactionsCreatedToday())
            .thenReturn(10L);

        try (MockedStatic<CorrelationIdUtil> mockedUtil = mockStatic(CorrelationIdUtil.class)) {
            mockedUtil.when(CorrelationIdUtil::getOrGenerate).thenReturn("test-correlation-id");

            // When
            FailedPaymentAnalysis analysis = analyticsService.analyzeFailedPayments(startDate, endDate);

            // Then
            assertNotNull(analysis);
            assertEquals(2, analysis.getTotalFailedPayments());
            assertEquals(new BigDecimal("300.00"), analysis.getTotalFailedAmount());
            assertEquals(20.0, analysis.getFailureRate(), 0.01); // 2/10 * 100
            assertNotNull(analysis.getErrorCodeBreakdown());
            assertNotNull(analysis.getPaymentMethodBreakdown());
            assertNotNull(analysis.getRiskIndicators());
            assertNotNull(analysis.getRecommendations());
        }
    }

    @Test
    void generateComplianceReport_ShouldReturnReport() {
        // Given
        String reportType = "ANNUAL";
        List<Transaction> transactions = Arrays.asList(successfulTransaction, failedTransaction);
        
        when(transactionRepository.findTransactionsBetween(any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(transactions);

        try (MockedStatic<CorrelationIdUtil> mockedUtil = mockStatic(CorrelationIdUtil.class)) {
            mockedUtil.when(CorrelationIdUtil::getOrGenerate).thenReturn("test-correlation-id");

            // When
            ComplianceReport report = analyticsService.generateComplianceReport(reportType, startDate, endDate);

            // Then
            assertNotNull(report);
            assertEquals(reportType, report.getReportType());
            assertEquals(startDate, report.getPeriodStart());
            assertEquals(endDate, report.getPeriodEnd());
            assertNotNull(report.getComplianceStatus());
            assertNotNull(report.getTransactionAudit());
            assertNotNull(report.getSecurityMetrics());
            assertNotNull(report.getDataPrivacy());
            assertNotNull(report.getRiskAssessments());
            assertNotNull(report.getRecommendations());
        }
    }

    @Test
    void generateTransactionReport_WithFilters_ShouldApplyFilters() {
        // Given
        TransactionReportRequest request = createBasicReportRequest();
        request.setCustomerId(UUID.randomUUID());
        request.setStatus(PaymentStatus.SETTLED);
        request.setTransactionType(TransactionType.PURCHASE);
        request.setMinAmount(new BigDecimal("50.00"));
        request.setMaxAmount(new BigDecimal("500.00"));
        
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(successfulTransaction), PageRequest.of(0, 10), 1);
        
        when(transactionRepository.findTransactionsWithFilters(
            eq(request.getCustomerId()),
            eq(request.getOrderId()),
            eq(request.getStatus().name()),
            eq(request.getTransactionType().name()),
            eq(request.getStartDate()),
            eq(request.getEndDate()),
            eq(request.getMinAmount()),
            eq(request.getMaxAmount()),
            any(Pageable.class)))
            .thenReturn(transactionPage);

        try (MockedStatic<CorrelationIdUtil> mockedUtil = mockStatic(CorrelationIdUtil.class)) {
            mockedUtil.when(CorrelationIdUtil::getOrGenerate).thenReturn("test-correlation-id");

            // When
            TransactionReportResponse response = analyticsService.generateTransactionReport(request);

            // Then
            assertNotNull(response);
            assertEquals(1, response.getTransactions().size());
            verify(transactionRepository).findTransactionsWithFilters(
                eq(request.getCustomerId()),
                eq(request.getOrderId()),
                eq(request.getStatus().name()),
                eq(request.getTransactionType().name()),
                eq(request.getStartDate()),
                eq(request.getEndDate()),
                eq(request.getMinAmount()),
                eq(request.getMaxAmount()),
                any(Pageable.class));
        }
    }

    @Test
    void generateTransactionReport_WithSorting_ShouldApplySorting() {
        // Given
        TransactionReportRequest request = createBasicReportRequest();
        request.setSortBy("createdAt");
        request.setSortDirection("DESC");
        
        Page<Transaction> transactionPage = new PageImpl<>(testTransactions, PageRequest.of(0, 10), 2);
        
        when(transactionRepository.findTransactionsWithFilters(
            any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(transactionPage);

        try (MockedStatic<CorrelationIdUtil> mockedUtil = mockStatic(CorrelationIdUtil.class)) {
            mockedUtil.when(CorrelationIdUtil::getOrGenerate).thenReturn("test-correlation-id");

            // When
            TransactionReportResponse response = analyticsService.generateTransactionReport(request);

            // Then
            assertNotNull(response);
            verify(transactionRepository).findTransactionsWithFilters(
                any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class));
        }
    }

    @Test
    void analyzeFailedPayments_WithHighFraudScore_ShouldIncludeRecommendations() {
        // Given
        List<Transaction> suspiciousTransactions = Arrays.asList(
            createSuspiciousTransaction("txn-1", new BigDecimal("15000.00")), // High amount
            createSuspiciousTransaction("txn-2", new BigDecimal("12000.00"))  // High amount
        );
        
        when(transactionRepository.findTransactionsBetween(any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(suspiciousTransactions);
        when(transactionRepository.countTransactionsCreatedToday())
            .thenReturn(4L);

        try (MockedStatic<CorrelationIdUtil> mockedUtil = mockStatic(CorrelationIdUtil.class)) {
            mockedUtil.when(CorrelationIdUtil::getOrGenerate).thenReturn("test-correlation-id");

            // When
            FailedPaymentAnalysis analysis = analyticsService.analyzeFailedPayments(startDate, endDate);

            // Then
            assertNotNull(analysis);
            assertEquals(50.0, analysis.getFailureRate(), 0.01); // 2/4 * 100
            assertTrue(analysis.getRiskIndicators().getFraudScore() > 50);
            assertTrue(analysis.getRecommendations().stream()
                .anyMatch(rec -> rec.contains("High fraud score detected")));
        }
    }

    // Helper methods for creating test data

    private TransactionReportRequest createBasicReportRequest() {
        TransactionReportRequest request = new TransactionReportRequest();
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPage(0);
        request.setSize(10);
        request.setSortBy("createdAt");
        request.setSortDirection("ASC");
        request.setExportFormat("JSON");
        return request;
    }

    private Transaction createTransaction(String id, PaymentStatus status, TransactionType type, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(id);
        transaction.setStatus(status);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setCurrency("USD");
        transaction.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
        transaction.setResponseData(new HashMap<>());
        
        // Add customer and payment method for analysis
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setEmail("test@example.com");
        transaction.setCustomer(customer);
        
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setPaymentType("CREDIT_CARD");
        transaction.setPaymentMethod(paymentMethod);
        
        return transaction;
    }

    private Transaction createFailedTransactionWithErrorCode(String id, String errorCode, BigDecimal amount) {
        Transaction transaction = createTransaction(id, PaymentStatus.FAILED, TransactionType.PURCHASE, amount);
        transaction.setAuthnetResponseCode(errorCode);
        return transaction;
    }

    private Transaction createSuspiciousTransaction(String id, BigDecimal amount) {
        Transaction transaction = createTransaction(id, PaymentStatus.FAILED, TransactionType.PURCHASE, amount);
        // Set time to unusual hours (3 AM)
        transaction.setCreatedAt(ZonedDateTime.now().withHour(3).withMinute(0).withSecond(0).toLocalDateTime());
        return transaction;
    }
}
