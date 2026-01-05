package com.talentica.paymentgateway.repository;

import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.entity.Order;
import com.talentica.paymentgateway.entity.PaymentStatus;
import com.talentica.paymentgateway.entity.User;
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
 * Repository interface for Order entity.
 * Provides data access methods for order management and reporting.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find order by order number.
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Check if order number exists.
     */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * Find orders by customer.
     */
    List<Order> findByCustomer(Customer customer);

    /**
     * Find orders by customer ID.
     */
    List<Order> findByCustomerId(UUID customerId);

    /**
     * Find orders by user.
     */
    List<Order> findByUser(User user);

    /**
     * Find orders by user ID.
     */
    List<Order> findByUserId(UUID userId);

    /**
     * Find orders by status.
     */
    List<Order> findByStatus(String status);

    /**
     * Find orders by payment status.
     */
    List<Order> findByPaymentStatus(PaymentStatus paymentStatus);

    /**
     * Find orders within date range.
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findOrdersBetween(@Param("startDate") ZonedDateTime startDate,
                                 @Param("endDate") ZonedDateTime endDate);

    /**
     * Find orders with total amount greater than specified value.
     */
    List<Order> findByTotalAmountGreaterThan(BigDecimal amount);

    /**
     * Find orders with total amount between range.
     */
    List<Order> findByTotalAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);

    /**
     * Find orders with filters and pagination.
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(:customerId IS NULL OR o.customer.id = :customerId) AND " +
           "(:userId IS NULL OR o.user.id = :userId) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) AND " +
           "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR o.createdAt <= :endDate) AND " +
           "(:minAmount IS NULL OR o.totalAmount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR o.totalAmount <= :maxAmount) AND " +
           "(:searchTerm IS NULL OR " +
           "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Order> findOrdersWithFilters(@Param("customerId") UUID customerId,
                                     @Param("userId") UUID userId,
                                     @Param("status") String status,
                                     @Param("paymentStatus") PaymentStatus paymentStatus,
                                     @Param("startDate") ZonedDateTime startDate,
                                     @Param("endDate") ZonedDateTime endDate,
                                     @Param("minAmount") BigDecimal minAmount,
                                     @Param("maxAmount") BigDecimal maxAmount,
                                     @Param("searchTerm") String searchTerm,
                                     Pageable pageable);

    /**
     * Find pending orders.
     */
    List<Order> findByStatusAndPaymentStatus(String status, PaymentStatus paymentStatus);

    /**
     * Find fully paid orders.
     */
    @Query("SELECT o FROM Order o WHERE o.paymentStatus IN ('CAPTURED', 'SETTLED')")
    List<Order> findFullyPaidOrders();

    /**
     * Find unpaid orders.
     */
    @Query("SELECT o FROM Order o WHERE o.paymentStatus = 'PENDING' OR o.paymentStatus = 'FAILED'")
    List<Order> findUnpaidOrders();

    /**
     * Find orders requiring payment.
     */
    @Query("SELECT o FROM Order o WHERE o.paymentStatus = 'PENDING' AND o.totalAmount > 0")
    List<Order> findOrdersRequiringPayment();

    /**
     * Calculate total order value by status.
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") String status);

    /**
     * Calculate total order value by payment status.
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = :paymentStatus")
    BigDecimal sumTotalAmountByPaymentStatus(@Param("paymentStatus") PaymentStatus paymentStatus);

    /**
     * Calculate daily order value.
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE " +
           "DATE_TRUNC('day', o.createdAt) = DATE_TRUNC('day', CURRENT_TIMESTAMP)")
    BigDecimal calculateDailyOrderValue();

    /**
     * Calculate monthly order value.
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE " +
           "EXTRACT(YEAR FROM o.createdAt) = EXTRACT(YEAR FROM CURRENT_DATE) AND " +
           "EXTRACT(MONTH FROM o.createdAt) = EXTRACT(MONTH FROM CURRENT_DATE)")
    BigDecimal calculateMonthlyOrderValue();

    /**
     * Count orders by status.
     */
    long countByStatus(String status);

    /**
     * Count orders by payment status.
     */
    long countByPaymentStatus(PaymentStatus paymentStatus);

    /**
     * Count orders created today.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE " +
           "DATE_TRUNC('day', o.createdAt) = DATE_TRUNC('day', CURRENT_TIMESTAMP)")
    long countOrdersCreatedToday();

    /**
     * Count orders for customer.
     */
    long countByCustomerId(UUID customerId);

    /**
     * Find orders with specific currency.
     */
    List<Order> findByCurrency(String currency);

    /**
     * Find orders with tax amount greater than zero.
     */
    List<Order> findByTaxAmountGreaterThan(BigDecimal amount);

    /**
     * Find orders with shipping amount greater than zero.
     */
    List<Order> findByShippingAmountGreaterThan(BigDecimal amount);

    /**
     * Find orders with discount applied.
     */
    List<Order> findByDiscountAmountGreaterThan(BigDecimal amount);

    /**
     * Get order summary by date.
     */
    @Query("SELECT DATE(o.createdAt) as orderDate, " +
           "COUNT(o) as orderCount, " +
           "COALESCE(SUM(o.totalAmount), 0) as totalValue, " +
           "COALESCE(AVG(o.totalAmount), 0) as averageValue " +
           "FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(o.createdAt) ORDER BY orderDate")
    List<Object[]> getOrderSummaryByDate(@Param("startDate") ZonedDateTime startDate,
                                        @Param("endDate") ZonedDateTime endDate);

    /**
     * Find top customers by order value.
     */
    @Query("SELECT o.customer.id, " +
           "COUNT(o) as orderCount, " +
           "COALESCE(SUM(o.totalAmount), 0) as totalValue " +
           "FROM Order o " +
           "GROUP BY o.customer.id ORDER BY totalValue DESC")
    List<Object[]> getTopCustomersByOrderValue(Pageable pageable);

    /**
     * Find average order value by customer.
     */
    @Query("SELECT o.customer.id, AVG(o.totalAmount) FROM Order o GROUP BY o.customer.id")
    List<Object[]> getAverageOrderValueByCustomer();

    /**
     * Find orders with incomplete payment.
     */
    @Query("SELECT o FROM Order o WHERE " +
           "o.totalAmount > (SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.order.id = o.id AND t.status IN ('CAPTURED', 'SETTLED') " +
           "AND t.transactionType IN ('PURCHASE', 'CAPTURE'))")
    List<Order> findOrdersWithIncompletePayment();

    /**
     * Find orders older than specified days with pending payment.
     */
    @Query("SELECT o FROM Order o WHERE o.paymentStatus = 'PENDING' AND o.createdAt < :cutoffDate")
    List<Order> findPendingOrdersOlderThan(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Get order statistics.
     */
    @Query("SELECT " +
           "COUNT(*) as totalOrders, " +
           "COUNT(CASE WHEN o.paymentStatus IN ('CAPTURED', 'SETTLED') THEN 1 END) as paidOrders, " +
           "COUNT(CASE WHEN o.paymentStatus = 'PENDING' THEN 1 END) as pendingOrders, " +
           "COALESCE(SUM(o.totalAmount), 0) as totalValue, " +
           "COALESCE(AVG(o.totalAmount), 0) as averageValue, " +
           "COALESCE(MAX(o.totalAmount), 0) as maxValue " +
           "FROM Order o WHERE o.createdAt >= :startDate")
    Object[] getOrderStatistics(@Param("startDate") ZonedDateTime startDate);

    /**
     * Find orders by metadata key-value.
     */
    @Query("SELECT o FROM Order o WHERE JSON_EXTRACT(o.metadata, :jsonPath) = :value")
    List<Order> findByMetadata(@Param("jsonPath") String jsonPath, @Param("value") String value);

    /**
     * Update order status.
     */
    @Query("UPDATE Order o SET o.status = :status WHERE o.id = :orderId")
    void updateOrderStatus(@Param("orderId") UUID orderId, @Param("status") String status);

    /**
     * Update order payment status.
     */
    @Query("UPDATE Order o SET o.paymentStatus = :paymentStatus WHERE o.id = :orderId")
    void updateOrderPaymentStatus(@Param("orderId") UUID orderId, @Param("paymentStatus") PaymentStatus paymentStatus);

    /**
     * Find recent orders for customer.
     */
    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByCustomer(@Param("customerId") UUID customerId, Pageable pageable);

    /**
     * Search orders by description or notes.
     */
    @Query("SELECT o FROM Order o WHERE " +
           "LOWER(o.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.notes) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Order> searchOrdersByDescription(@Param("searchTerm") String searchTerm);
}
