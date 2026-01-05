package com.talentica.paymentgateway.repository;

import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.entity.Order;
import com.talentica.paymentgateway.entity.PaymentStatus;
import com.talentica.paymentgateway.entity.Transaction;
import com.talentica.paymentgateway.entity.TransactionType;
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
 * Repository interface for Transaction entity.
 * Provides data access methods for transaction processing and reporting.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Find transaction by transaction ID.
     */
    Optional<Transaction> findByTransactionId(String transactionId);

    /**
     * Find transaction by transaction ID with payment method eagerly loaded.
     */
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.paymentMethod WHERE t.transactionId = :transactionId")
    Optional<Transaction> findByTransactionIdWithPaymentMethod(@Param("transactionId") String transactionId);

    /**
     * Find transaction by Authorize.Net transaction ID.
     */
    Optional<Transaction> findByAuthnetTransactionId(String authnetTransactionId);

    /**
     * Find transaction by idempotency key.
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find transactions by customer.
     */
    List<Transaction> findByCustomer(Customer customer);

    /**
     * Find transactions by customer ID.
     */
    List<Transaction> findByCustomerId(UUID customerId);

    /**
     * Find transactions by order.
     */
    List<Transaction> findByOrder(Order order);

    /**
     * Find transactions by order ID.
     */
    List<Transaction> findByOrderId(UUID orderId);

    /**
     * Find transactions by status.
     */
    List<Transaction> findByStatus(PaymentStatus status);

    /**
     * Find transactions by type.
     */
    List<Transaction> findByTransactionType(TransactionType transactionType);

    /**
     * Find transactions by correlation ID.
     */
    List<Transaction> findByCorrelationId(String correlationId);

    /**
     * Find child transactions by parent transaction.
     */
    List<Transaction> findByParentTransaction(Transaction parentTransaction);

    /**
     * Find transactions within date range.
     */
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findTransactionsBetween(@Param("startDate") ZonedDateTime startDate,
                                            @Param("endDate") ZonedDateTime endDate);

    /**
     * Find successful transactions (authorized, captured, or settled).
     */
    @Query("SELECT t FROM Transaction t WHERE t.status IN ('AUTHORIZED', 'CAPTURED', 'SETTLED')")
    List<Transaction> findSuccessfulTransactions();

    /**
     * Find failed transactions.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status IN ('FAILED', 'VOIDED', 'CANCELLED')")
    List<Transaction> findFailedTransactions();

    /**
     * Find transactions with filters and pagination.
     */
    @Query(value = "SELECT * FROM transactions t WHERE " +
           "(:customerId IS NULL OR t.customer_id = :customerId) AND " +
           "(:orderId IS NULL OR t.order_id = :orderId) AND " +
           "(:status IS NULL OR t.status = CAST(:status AS payment_status)) AND " +
           "(:transactionType IS NULL OR t.transaction_type = CAST(:transactionType AS transaction_type)) AND " +
           "(:startDate IS NULL OR t.created_at >= :startDate) AND " +
           "(:endDate IS NULL OR t.created_at <= :endDate) AND " +
           "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR t.amount <= :maxAmount)", 
           nativeQuery = true)
    Page<Transaction> findTransactionsWithFilters(@Param("customerId") UUID customerId,
                                                 @Param("orderId") UUID orderId,
                                                 @Param("status") String status,
                                                 @Param("transactionType") String transactionType,
                                                 @Param("startDate") ZonedDateTime startDate,
                                                 @Param("endDate") ZonedDateTime endDate,
                                                 @Param("minAmount") BigDecimal minAmount,
                                                 @Param("maxAmount") BigDecimal maxAmount,
                                                 Pageable pageable);

    /**
     * Calculate total transaction amount by status.
     */
    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE status = CAST(:status AS payment_status)", nativeQuery = true)
    BigDecimal sumAmountByStatus(@Param("status") String status);

    /**
     * Calculate total transaction amount for customer.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.customer.id = :customerId AND t.status IN ('CAPTURED', 'SETTLED')")
    BigDecimal sumSuccessfulAmountByCustomer(@Param("customerId") UUID customerId);

    /**
     * Calculate daily transaction volume.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE " +
           "DATE_TRUNC('day', t.createdAt) = DATE_TRUNC('day', CURRENT_TIMESTAMP) AND t.status IN ('CAPTURED', 'SETTLED')")
    BigDecimal calculateDailyVolume();

    /**
     * Calculate monthly transaction volume.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE " +
           "EXTRACT(YEAR FROM t.createdAt) = EXTRACT(YEAR FROM CURRENT_DATE) AND " +
           "EXTRACT(MONTH FROM t.createdAt) = EXTRACT(MONTH FROM CURRENT_DATE) AND " +
           "t.status IN ('CAPTURED', 'SETTLED')")
    BigDecimal calculateMonthlyVolume();

    /**
     * Count transactions by status.
     */
    long countByStatus(PaymentStatus status);

    /**
     * Count transactions by type.
     */
    long countByTransactionType(TransactionType transactionType);

    /**
     * Count transactions created today.
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE " +
           "DATE_TRUNC('day', t.createdAt) = DATE_TRUNC('day', CURRENT_TIMESTAMP)")
    long countTransactionsCreatedToday();

    /**
     * Count successful transactions today.
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE " +
           "DATE_TRUNC('day', t.createdAt) = DATE_TRUNC('day', CURRENT_TIMESTAMP) AND t.status IN ('CAPTURED', 'SETTLED')")
    long countSuccessfulTransactionsToday();

    /**
     * Find transactions requiring settlement (captured but not settled).
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'CAPTURED' AND t.transactionType IN ('PURCHASE', 'CAPTURE')")
    List<Transaction> findTransactionsForSettlement();

    /**
     * Find transactions that can be voided.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'AUTHORIZED' AND t.transactionType IN ('AUTHORIZE', 'PURCHASE')")
    List<Transaction> findVoidableTransactions();

    /**
     * Find transactions that can be refunded.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'SETTLED' AND t.transactionType IN ('PURCHASE', 'CAPTURE')")
    List<Transaction> findRefundableTransactions();

    /**
     * Find transactions for a specific time period with aggregation.
     */
    @Query("SELECT DATE(t.createdAt) as transactionDate, " +
           "COUNT(t) as transactionCount, " +
           "COALESCE(SUM(t.amount), 0) as totalAmount " +
           "FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(t.createdAt) ORDER BY transactionDate")
    List<Object[]> getTransactionSummaryByDate(@Param("startDate") ZonedDateTime startDate,
                                              @Param("endDate") ZonedDateTime endDate);

    /**
     * Find top customers by transaction volume.
     */
    @Query("SELECT t.customer.id, " +
           "COUNT(t) as transactionCount, " +
           "COALESCE(SUM(t.amount), 0) as totalAmount " +
           "FROM Transaction t WHERE t.status IN ('CAPTURED', 'SETTLED') " +
           "GROUP BY t.customer.id ORDER BY totalAmount DESC")
    List<Object[]> getTopCustomersByVolume(Pageable pageable);

    /**
     * Find average transaction amount by type.
     */
    @Query("SELECT t.transactionType, AVG(t.amount) FROM Transaction t GROUP BY t.transactionType")
    List<Object[]> getAverageAmountByType();

    /**
     * Find transactions by payment method.
     */
    @Query("SELECT t FROM Transaction t WHERE t.paymentMethod.id = :paymentMethodId")
    List<Transaction> findByPaymentMethodId(@Param("paymentMethodId") UUID paymentMethodId);

    /**
     * Find pending transactions older than specified hours.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.createdAt < :cutoffTime")
    List<Transaction> findPendingTransactionsOlderThan(@Param("cutoffTime") ZonedDateTime cutoffTime);

    /**
     * Find duplicate transactions by amount and customer within time window.
     */
    @Query("SELECT t FROM Transaction t WHERE " +
           "t.customer.id = :customerId AND " +
           "t.amount = :amount AND " +
           "t.createdAt BETWEEN :startTime AND :endTime AND " +
           "t.status != 'FAILED'")
    List<Transaction> findPotentialDuplicates(@Param("customerId") UUID customerId,
                                            @Param("amount") BigDecimal amount,
                                            @Param("startTime") ZonedDateTime startTime,
                                            @Param("endTime") ZonedDateTime endTime);

    /**
     * Get transaction statistics for dashboard.
     */
    @Query("SELECT " +
           "COUNT(*) as totalCount, " +
           "COUNT(CASE WHEN t.status IN ('CAPTURED', 'SETTLED') THEN 1 END) as successCount, " +
           "COUNT(CASE WHEN t.status IN ('FAILED', 'VOIDED', 'CANCELLED') THEN 1 END) as failedCount, " +
           "COALESCE(SUM(CASE WHEN t.status IN ('CAPTURED', 'SETTLED') THEN t.amount ELSE 0 END), 0) as successVolume, " +
           "COALESCE(AVG(t.amount), 0) as averageAmount " +
           "FROM Transaction t WHERE t.createdAt >= :startDate")
    Object[] getTransactionStatistics(@Param("startDate") ZonedDateTime startDate);

    /**
     * Find transactions by response code.
     */
    List<Transaction> findByAuthnetResponseCode(String responseCode);

    /**
     * Find transactions with specific AVS result.
     */
    List<Transaction> findByAuthnetAvsResult(String avsResult);

    /**
     * Find transactions with specific CVV result.
     */
    List<Transaction> findByAuthnetCvvResult(String cvvResult);

    /**
     * Update transaction status.
     */
    @Query("UPDATE Transaction t SET t.status = :status, t.processedAt = :processedAt WHERE t.id = :transactionId")
    void updateTransactionStatus(@Param("transactionId") UUID transactionId,
                                @Param("status") PaymentStatus status,
                                @Param("processedAt") ZonedDateTime processedAt);
}
