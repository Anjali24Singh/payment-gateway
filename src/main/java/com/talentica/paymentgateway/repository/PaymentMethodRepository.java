package com.talentica.paymentgateway.repository;

import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for PaymentMethod entity.
 * Provides data access methods for payment method management.
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    /**
     * Find payment method by token.
     */
    Optional<PaymentMethod> findByPaymentToken(String paymentToken);

    /**
     * Find payment method by payment method ID (alias for payment token).
     */
    default Optional<PaymentMethod> findByPaymentMethodId(String paymentMethodId) {
        return findByPaymentToken(paymentMethodId);
    }

    /**
     * Check if payment token exists.
     */
    boolean existsByPaymentToken(String paymentToken);

    /**
     * Find payment methods by customer.
     */
    List<PaymentMethod> findByCustomer(Customer customer);

    /**
     * Find payment methods by customer ID.
     */
    List<PaymentMethod> findByCustomerId(UUID customerId);

    /**
     * Find active payment methods by customer.
     */
    List<PaymentMethod> findByCustomerAndIsActiveTrue(Customer customer);

    /**
     * Find active payment methods by customer ID.
     */
    List<PaymentMethod> findByCustomerIdAndIsActiveTrue(UUID customerId);

    /**
     * Find default payment method for customer.
     */
    Optional<PaymentMethod> findByCustomerAndIsDefaultTrueAndIsActiveTrue(Customer customer);

    /**
     * Find default payment method by customer ID.
     */
    Optional<PaymentMethod> findByCustomerIdAndIsDefaultTrueAndIsActiveTrue(UUID customerId);

    /**
     * Find payment methods by type.
     */
    List<PaymentMethod> findByPaymentType(String paymentType);

    /**
     * Find payment methods by card brand.
     */
    List<PaymentMethod> findByCardBrand(String cardBrand);

    /**
     * Find payment methods by card last four digits.
     */
    List<PaymentMethod> findByCardLastFour(String cardLastFour);

    /**
     * Find payment methods expiring soon.
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "pm.cardExpiryYear = EXTRACT(YEAR FROM CURRENT_DATE) AND " +
           "pm.cardExpiryMonth <= EXTRACT(MONTH FROM CURRENT_DATE) + :monthsAhead AND " +
           "pm.isActive = true")
    List<PaymentMethod> findExpiringSoon(@Param("monthsAhead") int monthsAhead);

    /**
     * Find expired payment methods.
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE " +
           "(pm.cardExpiryYear < EXTRACT(YEAR FROM CURRENT_DATE) OR " +
           "(pm.cardExpiryYear = EXTRACT(YEAR FROM CURRENT_DATE) AND pm.cardExpiryMonth < EXTRACT(MONTH FROM CURRENT_DATE))) AND " +
           "pm.isActive = true")
    List<PaymentMethod> findExpiredPaymentMethods();

    /**
     * Find payment methods by cardholder name.
     */
    List<PaymentMethod> findByCardholderNameContainingIgnoreCase(String cardholderName);

    /**
     * Find all active payment methods.
     */
    List<PaymentMethod> findByIsActiveTrue();

    /**
     * Find all default payment methods.
     */
    List<PaymentMethod> findByIsDefaultTrue();

    /**
     * Count payment methods by customer.
     */
    long countByCustomerId(UUID customerId);

    /**
     * Count active payment methods by customer.
     */
    long countByCustomerIdAndIsActiveTrue(UUID customerId);

    /**
     * Count payment methods by type.
     */
    long countByPaymentType(String paymentType);

    /**
     * Count payment methods by card brand.
     */
    long countByCardBrand(String cardBrand);

    /**
     * Find payment methods created within date range.
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.createdAt BETWEEN :startDate AND :endDate")
    List<PaymentMethod> findPaymentMethodsCreatedBetween(@Param("startDate") ZonedDateTime startDate,
                                                        @Param("endDate") ZonedDateTime endDate);

    /**
     * Find payment methods by customer and type.
     */
    List<PaymentMethod> findByCustomerAndPaymentType(Customer customer, String paymentType);

    /**
     * Find payment methods with transactions.
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE SIZE(pm.transactions) > 0")
    List<PaymentMethod> findPaymentMethodsWithTransactions();

    /**
     * Find unused payment methods.
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE SIZE(pm.transactions) = 0")
    List<PaymentMethod> findUnusedPaymentMethods();

    /**
     * Find payment methods used in the last N days.
     */
    @Query("SELECT DISTINCT pm FROM PaymentMethod pm JOIN pm.transactions t " +
           "WHERE t.createdAt >= :cutoffDate")
    List<PaymentMethod> findRecentlyUsedPaymentMethods(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Find payment methods not used since specified date.
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.id NOT IN " +
           "(SELECT DISTINCT t.paymentMethod.id FROM Transaction t WHERE t.createdAt >= :cutoffDate AND t.paymentMethod IS NOT NULL)")
    List<PaymentMethod> findPaymentMethodsNotUsedSince(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Set default payment method for customer.
     */
    @Query("UPDATE PaymentMethod pm SET pm.isDefault = CASE WHEN pm.id = :paymentMethodId THEN true ELSE false END " +
           "WHERE pm.customer.id = :customerId")
    void setDefaultPaymentMethod(@Param("customerId") UUID customerId, @Param("paymentMethodId") UUID paymentMethodId);

    /**
     * Deactivate payment method.
     */
    @Query("UPDATE PaymentMethod pm SET pm.isActive = false WHERE pm.id = :paymentMethodId")
    void deactivatePaymentMethod(@Param("paymentMethodId") UUID paymentMethodId);

    /**
     * Activate payment method.
     */
    @Query("UPDATE PaymentMethod pm SET pm.isActive = true WHERE pm.id = :paymentMethodId")
    void activatePaymentMethod(@Param("paymentMethodId") UUID paymentMethodId);

    /**
     * Remove default flag from all payment methods for customer.
     */
    @Query("UPDATE PaymentMethod pm SET pm.isDefault = false WHERE pm.customer.id = :customerId")
    void clearDefaultPaymentMethods(@Param("customerId") UUID customerId);

    /**
     * Find payment methods by expiry year.
     */
    List<PaymentMethod> findByCardExpiryYear(Integer cardExpiryYear);

    /**
     * Find payment methods by expiry month and year.
     */
    List<PaymentMethod> findByCardExpiryMonthAndCardExpiryYear(Integer cardExpiryMonth, Integer cardExpiryYear);

    /**
     * Find customers with multiple payment methods.
     */
    @Query("SELECT pm.customer.id, COUNT(pm) FROM PaymentMethod pm WHERE pm.isActive = true " +
           "GROUP BY pm.customer.id HAVING COUNT(pm) > 1")
    List<Object[]> findCustomersWithMultiplePaymentMethods();

    /**
     * Find most used payment method types.
     */
    @Query("SELECT pm.paymentType, COUNT(t) as usageCount FROM PaymentMethod pm " +
           "LEFT JOIN pm.transactions t GROUP BY pm.paymentType ORDER BY usageCount DESC")
    List<Object[]> getPaymentMethodUsageStatistics();

    /**
     * Find most used card brands.
     */
    @Query("SELECT pm.cardBrand, COUNT(t) as usageCount FROM PaymentMethod pm " +
           "LEFT JOIN pm.transactions t WHERE pm.cardBrand IS NOT NULL " +
           "GROUP BY pm.cardBrand ORDER BY usageCount DESC")
    List<Object[]> getCardBrandUsageStatistics();

    /**
     * Find payment methods that should be cleaned up (inactive and unused).
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.isActive = false AND " +
           "SIZE(pm.transactions) = 0 AND pm.createdAt < :cutoffDate")
    List<PaymentMethod> findPaymentMethodsForCleanup(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Get payment method statistics by customer.
     */
    @Query("SELECT " +
           "COUNT(*) as totalMethods, " +
           "COUNT(CASE WHEN pm.isActive = true THEN 1 END) as activeMethods, " +
           "COUNT(CASE WHEN pm.isDefault = true THEN 1 END) as defaultMethods " +
           "FROM PaymentMethod pm WHERE pm.customer.id = :customerId")
    Object[] getPaymentMethodStatisticsByCustomer(@Param("customerId") UUID customerId);
}
