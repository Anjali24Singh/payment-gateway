package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.analytics.AnalyticsDashboardRequest;
import com.talentica.paymentgateway.dto.metrics.DashboardMetrics;
import com.talentica.paymentgateway.dto.metrics.TransactionMetrics;
import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.entity.PaymentStatus;
import com.talentica.paymentgateway.entity.Transaction;
import com.talentica.paymentgateway.entity.TransactionType;
import com.talentica.paymentgateway.repository.CustomerRepository;
import com.talentica.paymentgateway.repository.SubscriptionRepository;
import com.talentica.paymentgateway.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AnalyticsService.
 * Tests core analytics and reporting functionality.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CustomerRepository customerRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
            transactionRepository, 
            subscriptionRepository, 
            customerRepository
        );
    }

    @Test
    void shouldGenerateDashboardMetrics() {
        // Given
        ZonedDateTime startDate = ZonedDateTime.now().minusDays(30);
        ZonedDateTime endDate = ZonedDateTime.now();
        
        AnalyticsDashboardRequest request = new AnalyticsDashboardRequest(startDate, endDate);

        // Mock transaction statistics
        Object[] transactionStats = {100L, 85L, 15L, new BigDecimal("50000.00"), new BigDecimal("588.24")};
        when(transactionRepository.getTransactionStatistics(any(ZonedDateTime.class)))
            .thenReturn(transactionStats);

        // Mock subscription data
        when(subscriptionRepository.countByCreatedAtBetween(any(), any())).thenReturn(25L);
        when(subscriptionRepository.countByStatusAndCancelledAtBetween(any(), any(), any())).thenReturn(3L);
        when(subscriptionRepository.countByStatus(any())).thenReturn(150L);
        when(subscriptionRepository.calculateActiveMonthlyRevenue()).thenReturn(new BigDecimal("15000.00"));
        when(subscriptionRepository.countActiveAtDate(any())).thenReturn(140L);

        // Mock revenue data
        when(transactionRepository.sumAmountByStatus("SETTLED")).thenReturn(new BigDecimal("75000.00"));
        when(transactionRepository.sumAmountByStatus("REFUNDED")).thenReturn(new BigDecimal("2500.00"));

        // Mock customer count
        when(customerRepository.count()).thenReturn(500L);

        // When
        DashboardMetrics metrics = analyticsService.generateDashboardMetrics(request);

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalCustomers()).isEqualTo(500L);
        assertThat(metrics.getTransactionMetrics()).isNotNull();
        assertThat(metrics.getSubscriptionMetrics()).isNotNull();
        assertThat(metrics.getRevenueMetrics()).isNotNull();
        
        // Verify transaction metrics
        TransactionMetrics txMetrics = metrics.getTransactionMetrics();
        assertThat(txMetrics.getSuccessfulTransactions()).isEqualTo(85L);
        assertThat(txMetrics.getFailedTransactions()).isEqualTo(15L);
        assertThat(txMetrics.getSuccessfulAmount()).isEqualTo(new BigDecimal("50000.00"));
        assertThat(txMetrics.getAverageTransactionAmount()).isNotNull();
    }

    @Test
    void shouldGenerateTransactionMetrics() {
        // Given
        ZonedDateTime startDate = ZonedDateTime.now().minusDays(7);
        ZonedDateTime endDate = ZonedDateTime.now();

        Object[] stats = {50L, 42L, 8L, new BigDecimal("25000.00"), new BigDecimal("595.24")};
        when(transactionRepository.getTransactionStatistics(startDate)).thenReturn(stats);

        // When
        TransactionMetrics metrics = analyticsService.generateTransactionMetrics(startDate, endDate);

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.getSuccessfulTransactions()).isEqualTo(42L);
        assertThat(metrics.getFailedTransactions()).isEqualTo(8L);
        assertThat(metrics.getSuccessfulAmount()).isEqualTo(new BigDecimal("25000.00"));
        assertThat(metrics.getAverageTransactionAmount()).isNotNull();
        assertThat(metrics.getTotalTransactions()).isEqualTo(50L);
        assertThat(metrics.getSuccessRate()).isNotNull();
    }

    @Test
    void shouldCalculateRevenueMetrics() {
        // Given
        ZonedDateTime startDate = ZonedDateTime.now().minusMonths(1);
        ZonedDateTime endDate = ZonedDateTime.now();

        when(transactionRepository.sumAmountByStatus("SETTLED"))
            .thenReturn(new BigDecimal("100000.00"));
        when(transactionRepository.sumAmountByStatus("REFUNDED"))
            .thenReturn(new BigDecimal("5000.00"));
        when(subscriptionRepository.calculateActiveMonthlyRevenue())
            .thenReturn(new BigDecimal("20000.00"));

        // When
        var revenueMetrics = analyticsService.generateRevenueMetrics(startDate, endDate);

        // Then
        assertThat(revenueMetrics).isNotNull();
        assertThat(revenueMetrics.getTotalRevenue()).isEqualTo(new BigDecimal("100000.00"));
        assertThat(revenueMetrics.getRecurringRevenue()).isEqualTo(new BigDecimal("20000.00"));
        assertThat(revenueMetrics.getMonthlyRecurringRevenue()).isEqualTo(new BigDecimal("20000.00"));
    }
}
