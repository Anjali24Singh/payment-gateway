package com.talentica.paymentgateway.repository;

import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.entity.Subscription;
import com.talentica.paymentgateway.entity.SubscriptionInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SubscriptionInvoice entity.
 * Provides data access methods for subscription invoice management and billing.
 */
@Repository
public interface SubscriptionInvoiceRepository extends JpaRepository<SubscriptionInvoice, UUID> {

    /**
     * Find invoice by invoice number.
     */
    Optional<SubscriptionInvoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Check if invoice number exists.
     */
    boolean existsByInvoiceNumber(String invoiceNumber);

    /**
     * Find invoices by subscription.
     */
    List<SubscriptionInvoice> findBySubscription(Subscription subscription);

    /**
     * Find invoices by subscription ID.
     */
    List<SubscriptionInvoice> findBySubscriptionId(UUID subscriptionId);

    /**
     * Find invoices by customer.
     */
    List<SubscriptionInvoice> findByCustomer(Customer customer);

    /**
     * Find invoices by customer ID.
     */
    List<SubscriptionInvoice> findByCustomerId(UUID customerId);

    /**
     * Find invoices by status.
     */
    List<SubscriptionInvoice> findByStatus(String status);

    /**
     * Find pending invoices.
     */
    @Query("SELECT si FROM SubscriptionInvoice si WHERE si.status = 'PENDING'")
    List<SubscriptionInvoice> findByStatus_Pending();

    /**
     * Find paid invoices.
     */
    @Query("SELECT si FROM SubscriptionInvoice si WHERE si.status = 'PAID'")
    List<SubscriptionInvoice> findByStatus_Paid();

    /**
     * Find failed invoices.
     */
    @Query("SELECT si FROM SubscriptionInvoice si WHERE si.status = 'FAILED'")
    List<SubscriptionInvoice> findByStatus_Failed();

    /**
     * Find overdue invoices.
     */
    @Query("SELECT i FROM SubscriptionInvoice i WHERE i.dueDate < CURRENT_TIMESTAMP AND i.status != 'PAID'")
    List<SubscriptionInvoice> findOverdueInvoices();

    /**
     * Find invoices due soon.
     */
    @Query("SELECT i FROM SubscriptionInvoice i WHERE i.dueDate BETWEEN CURRENT_TIMESTAMP AND :futureDate AND i.status = 'PENDING'")
    List<SubscriptionInvoice> findInvoicesDueSoon(@Param("futureDate") ZonedDateTime futureDate);

    /**
     * Find invoices due for retry.
     */
    @Query("SELECT i FROM SubscriptionInvoice i WHERE i.nextPaymentAttempt <= CURRENT_TIMESTAMP AND " +
           "i.paymentAttempts < 5 AND i.status IN ('FAILED', 'PENDING')")
    List<SubscriptionInvoice> findInvoicesDueForRetry();

    /**
     * Find invoices due for retry with date parameter.
     */
    @Query("SELECT i FROM SubscriptionInvoice i WHERE i.nextPaymentAttempt <= :currentDate AND " +
           "i.paymentAttempts < 5 AND i.status IN ('FAILED', 'PENDING')")
    List<SubscriptionInvoice> findInvoicesDueForRetry(@Param("currentDate") ZonedDateTime currentDate);

    /**
     * Find invoices by subscription and billing period.
     */
    @Query("SELECT i FROM SubscriptionInvoice i WHERE i.subscription = :subscription AND " +
           "i.periodStart = :periodStart AND i.periodEnd = :periodEnd")
    List<SubscriptionInvoice> findBySubscriptionAndPeriodStartAndPeriodEnd(
        @Param("subscription") Subscription subscription,
        @Param("periodStart") ZonedDateTime periodStart,
        @Param("periodEnd") ZonedDateTime periodEnd);

    /**
     * Find invoices within date range.
     */
    @Query("SELECT i FROM SubscriptionInvoice i WHERE i.createdAt BETWEEN :startDate AND :endDate")
    List<SubscriptionInvoice> findInvoicesBetween(@Param("startDate") ZonedDateTime startDate,
                                                 @Param("endDate") ZonedDateTime endDate);

    /**
     * Find invoices by billing period.
     */
    @Query("SELECT i FROM SubscriptionInvoice i WHERE i.periodStart = :periodStart AND i.periodEnd = :periodEnd")
    List<SubscriptionInvoice> findByBillingPeriod(@Param("periodStart") ZonedDateTime periodStart,
                                                 @Param("periodEnd") ZonedDateTime periodEnd);

    /**
     * Find invoices with filters and pagination.
     */
    @Query("SELECT i FROM SubscriptionInvoice i WHERE " +
           "(:customerId IS NULL OR i.customer.id = :customerId) AND " +
           "(:subscriptionId IS NULL OR i.subscription.id = :subscriptionId) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:startDate IS NULL OR i.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR i.createdAt <= :endDate) AND " +
           "(:minAmount IS NULL OR i.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR i.amount <= :maxAmount) AND " +
           "(:isOverdue IS NULL OR " +
           "(:isOverdue = true AND i.dueDate < CURRENT_TIMESTAMP AND i.status != 'PAID') OR " +
           "(:isOverdue = false AND (i.dueDate >= CURRENT_TIMESTAMP OR i.status = 'PAID')))")
    Page<SubscriptionInvoice> findInvoicesWithFilters(@Param("customerId") UUID customerId,
                                                     @Param("subscriptionId") UUID subscriptionId,
                                                     @Param("status") String status,
                                                     @Param("startDate") ZonedDateTime startDate,
                                                     @Param("endDate") ZonedDateTime endDate,
                                                     @Param("minAmount") BigDecimal minAmount,
                                                     @Param("maxAmount") BigDecimal maxAmount,
                                                     @Param("isOverdue") Boolean isOverdue,
                                                     Pageable pageable);

    /**
     * Calculate total invoice amount by status.
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM SubscriptionInvoice i WHERE i.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") String status);

    /**
     * Calculate total paid amount.
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM SubscriptionInvoice i WHERE i.status = 'PAID'")
    BigDecimal calculateTotalPaidAmount();

    /**
     * Calculate total outstanding amount.
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM SubscriptionInvoice i WHERE i.status != 'PAID'")
    BigDecimal calculateTotalOutstandingAmount();

    /**
     * Calculate monthly billing volume.
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM SubscriptionInvoice i WHERE " +
           "EXTRACT(YEAR FROM i.createdAt) = EXTRACT(YEAR FROM CURRENT_DATE) AND " +
           "EXTRACT(MONTH FROM i.createdAt) = EXTRACT(MONTH FROM CURRENT_DATE)")
    BigDecimal calculateMonthlyBillingVolume();

    /**
     * Count invoices by status.
     */
    long countByStatus(String status);

    /**
     * Count overdue invoices.
     */
    @Query("SELECT COUNT(i) FROM SubscriptionInvoice i WHERE i.dueDate < CURRENT_TIMESTAMP AND i.status != 'PAID'")
    long countOverdueInvoices();

    /**
     * Count invoices created today.
     */
    @Query("SELECT COUNT(i) FROM SubscriptionInvoice i WHERE " +
           "DATE_TRUNC('day', i.createdAt) = DATE_TRUNC('day', CURRENT_TIMESTAMP)")
    long countInvoicesCreatedToday();

    /**
     * Find latest invoice for subscription.
     */
    @Query("SELECT i FROM SubscriptionInvoice i WHERE i.subscription.id = :subscriptionId ORDER BY i.createdAt DESC")
    Optional<SubscriptionInvoice> findLatestInvoiceForSubscription(@Param("subscriptionId") UUID subscriptionId);

    /**
     * Find unpaid invoices for customer.
     */
    @Query("SELECT i FROM SubscriptionInvoice i WHERE i.customer.id = :customerId AND i.status != 'PAID' ORDER BY i.dueDate")
    List<SubscriptionInvoice> findUnpaidInvoicesForCustomer(@Param("customerId") UUID customerId);

    /**
     * Find invoices with maximum payment attempts reached.
     */
    @Query("SELECT i FROM SubscriptionInvoice i WHERE i.paymentAttempts >= 5 AND i.status != 'PAID'")
    List<SubscriptionInvoice> findInvoicesWithMaxAttemptsReached();

    /**
     * Get invoice statistics.
     */
    @Query("SELECT " +
           "COUNT(*) as totalInvoices, " +
           "COUNT(CASE WHEN i.status = 'PAID' THEN 1 END) as paidInvoices, " +
           "COUNT(CASE WHEN i.status = 'PENDING' THEN 1 END) as pendingInvoices, " +
           "COUNT(CASE WHEN i.status = 'FAILED' THEN 1 END) as failedInvoices, " +
           "COUNT(CASE WHEN i.dueDate < CURRENT_TIMESTAMP AND i.status != 'PAID' THEN 1 END) as overdueInvoices, " +
           "COALESCE(SUM(i.amount), 0) as totalAmount, " +
           "COALESCE(SUM(CASE WHEN i.status = 'PAID' THEN i.amount ELSE 0 END), 0) as paidAmount, " +
           "COALESCE(AVG(i.amount), 0) as averageAmount " +
           "FROM SubscriptionInvoice i WHERE i.createdAt >= :startDate")
    Object[] getInvoiceStatistics(@Param("startDate") ZonedDateTime startDate);

    /**
     * Get billing summary by date.
     */
    @Query("SELECT DATE(i.createdAt) as billingDate, " +
           "COUNT(i) as invoiceCount, " +
           "COALESCE(SUM(i.amount), 0) as totalAmount, " +
           "COUNT(CASE WHEN i.status = 'PAID' THEN 1 END) as paidCount " +
           "FROM SubscriptionInvoice i WHERE i.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(i.createdAt) ORDER BY billingDate")
    List<Object[]> getBillingSummaryByDate(@Param("startDate") ZonedDateTime startDate,
                                          @Param("endDate") ZonedDateTime endDate);

    /**
     * Find top customers by invoice volume.
     */
    @Query("SELECT i.customer.id, " +
           "COUNT(i) as invoiceCount, " +
           "COALESCE(SUM(i.amount), 0) as totalAmount " +
           "FROM SubscriptionInvoice i WHERE i.status = 'PAID' " +
           "GROUP BY i.customer.id ORDER BY totalAmount DESC")
    List<Object[]> getTopCustomersByInvoiceVolume(Pageable pageable);

    /**
     * Find invoices by currency.
     */
    List<SubscriptionInvoice> findByCurrency(String currency);

    /**
     * Find invoices by amount range.
     */
    List<SubscriptionInvoice> findByAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);

    /**
     * Find invoices paid within date range.
     */
    @Query("SELECT i FROM SubscriptionInvoice i WHERE i.paidAt BETWEEN :startDate AND :endDate")
    List<SubscriptionInvoice> findInvoicesPaidBetween(@Param("startDate") ZonedDateTime startDate,
                                                     @Param("endDate") ZonedDateTime endDate);

    /**
     * Update invoice status.
     */
    @Query("UPDATE SubscriptionInvoice i SET i.status = :status WHERE i.id = :invoiceId")
    void updateInvoiceStatus(@Param("invoiceId") UUID invoiceId, @Param("status") String status);

    /**
     * Mark invoice as paid.
     */
    @Query("UPDATE SubscriptionInvoice i SET i.status = 'PAID', i.paidAt = :paidAt WHERE i.id = :invoiceId")
    void markInvoiceAsPaid(@Param("invoiceId") UUID invoiceId, @Param("paidAt") ZonedDateTime paidAt);

    /**
     * Increment payment attempts.
     */
    @Query("UPDATE SubscriptionInvoice i SET i.paymentAttempts = i.paymentAttempts + 1 WHERE i.id = :invoiceId")
    void incrementPaymentAttempts(@Param("invoiceId") UUID invoiceId);

    /**
     * Update next payment attempt time.
     */
    @Query("UPDATE SubscriptionInvoice i SET i.nextPaymentAttempt = :nextAttempt WHERE i.id = :invoiceId")
    void updateNextPaymentAttempt(@Param("invoiceId") UUID invoiceId, @Param("nextAttempt") ZonedDateTime nextAttempt);

    /**
     * Find collection success rate.
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN i.status = 'PAID' THEN 1 END) as successfulCollections, " +
           "COUNT(*) as totalInvoices, " +
           "(COUNT(CASE WHEN i.status = 'PAID' THEN 1 END) * 100.0 / COUNT(*)) as successRate " +
           "FROM SubscriptionInvoice i WHERE i.createdAt >= :startDate")
    Object[] getCollectionSuccessRate(@Param("startDate") ZonedDateTime startDate);

    /**
     * Find average days to payment.
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (paid_at - created_at)) / 86400.0) " +
           "FROM subscription_invoices WHERE status = 'PAID' AND created_at >= :startDate", nativeQuery = true)
    Double getAverageDaysToPayment(@Param("startDate") ZonedDateTime startDate);

    /**
     * Find invoices by metadata key-value.
     */
    @Query(value = "SELECT i.* FROM subscription_invoices i WHERE i.metadata->>:jsonPath = :value", nativeQuery = true)
    List<SubscriptionInvoice> findByMetadata(@Param("jsonPath") String jsonPath, @Param("value") String value);
}
