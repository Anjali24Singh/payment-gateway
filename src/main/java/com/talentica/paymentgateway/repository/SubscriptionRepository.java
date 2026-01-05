package com.talentica.paymentgateway.repository;

import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.entity.Subscription;
import com.talentica.paymentgateway.entity.SubscriptionPlan;
import com.talentica.paymentgateway.entity.SubscriptionStatus;
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
 * Repository interface for Subscription entity.
 * Provides data access methods for subscription management and billing.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /**
     * Find subscription by subscription ID.
     */
    Optional<Subscription> findBySubscriptionId(String subscriptionId);

    /**
     * Find subscription by customer ID and idempotency key.
     * Optimized query with better indexing strategy.
     */
    @Query(value = "SELECT s.* FROM subscriptions s JOIN customers c ON s.customer_id = c.id WHERE c.customer_reference = :customerId AND s.metadata @> '{\"idempotencyKey\": \"' || :idempotencyKey || '\"}' LIMIT 1", nativeQuery = true)
    Optional<Subscription> findByCustomerIdAndIdempotencyKey(@Param("customerId") String customerId, @Param("idempotencyKey") String idempotencyKey);

    /**
     * Check if subscription ID exists.
     */
    boolean existsBySubscriptionId(String subscriptionId);

    /**
     * Find subscriptions by customer.
     */
    List<Subscription> findByCustomer(Customer customer);

    /**
     * Find subscriptions by customer with pagination.
     */
    Page<Subscription> findByCustomer(Customer customer, Pageable pageable);

    /**
     * Find subscriptions by customer ID.
     */
    List<Subscription> findByCustomerId(UUID customerId);

    /**
     * Find subscriptions by plan.
     */
    List<Subscription> findByPlan(SubscriptionPlan plan);

    /**
     * Find subscriptions by plan ID.
     */
    List<Subscription> findByPlanId(UUID planId);

    /**
     * Find subscriptions by status.
     */
    List<Subscription> findByStatus(SubscriptionStatus status);

    /**
     * Find active subscriptions.
     */
    List<Subscription> findByStatusIn(List<SubscriptionStatus> statuses);

    /**
     * Find subscriptions due for billing.
     */
    @Query("SELECT s FROM Subscription s WHERE s.nextBillingDate <= :currentDate AND s.status = 'ACTIVE'")
    List<Subscription> findSubscriptionsDueForBilling(@Param("currentDate") ZonedDateTime currentDate);

    /**
     * Find subscriptions ending trial period.
     */
    @Query("SELECT s FROM Subscription s WHERE s.trialEnd <= :currentDate AND s.status = 'ACTIVE'")
    List<Subscription> findSubscriptionsEndingTrial(@Param("currentDate") ZonedDateTime currentDate);

    /**
     * Find subscriptions in trial period.
     */
    @Query("SELECT s FROM Subscription s WHERE s.trialStart <= :currentDate AND s.trialEnd > :currentDate")
    List<Subscription> findSubscriptionsInTrial(@Param("currentDate") ZonedDateTime currentDate);

    /**
     * Find expired subscriptions.
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.currentPeriodEnd < :currentDate")
    List<Subscription> findExpiredSubscriptions(@Param("currentDate") ZonedDateTime currentDate);

    /**
     * Find subscriptions with filters and pagination.
     */
    @Query("SELECT s FROM Subscription s WHERE " +
           "(:customerId IS NULL OR s.customer.id = :customerId) AND " +
           "(:planId IS NULL OR s.plan.id = :planId) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:startDate IS NULL OR s.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR s.createdAt <= :endDate)")
    Page<Subscription> findSubscriptionsWithFilters(@Param("customerId") UUID customerId,
                                                   @Param("planId") UUID planId,
                                                   @Param("status") SubscriptionStatus status,
                                                   @Param("startDate") ZonedDateTime startDate,
                                                   @Param("endDate") ZonedDateTime endDate,
                                                   Pageable pageable);

    /**
     * Count subscriptions by status.
     */
    @Query(value = "SELECT COUNT(*) FROM subscriptions WHERE status = :status", nativeQuery = true)
    long countByStatus(@Param("status") String status);

    /**
     * Count active subscriptions.
     */
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE'")
    long countActiveSubscriptions();

    /**
     * Count subscriptions created today.
     */
    @Query("SELECT COUNT(s) FROM Subscription s WHERE " +
           "DATE_TRUNC('day', s.createdAt) = DATE_TRUNC('day', CURRENT_TIMESTAMP)")
    long countSubscriptionsCreatedToday();

    /**
     * Count subscriptions by plan.
     */
    long countByPlanId(UUID planId);

    /**
     * Count subscriptions by customer.
     */
    long countByCustomerId(UUID customerId);

    /**
     * Calculate monthly recurring revenue (MRR).
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Subscription s JOIN s.plan p WHERE s.status = 'ACTIVE'")
    BigDecimal calculateMonthlyRecurringRevenue();

    /**
     * Calculate MRR by plan.
     */
    @Query("SELECT p.planCode, COALESCE(SUM(p.amount), 0) FROM Subscription s JOIN s.plan p " +
           "WHERE s.status = 'ACTIVE' GROUP BY p.planCode")
    List<Object[]> calculateMRRByPlan();

    /**
     * Find top customers by subscription value.
     */
    @Query("SELECT s.customer.id, COUNT(s), COALESCE(SUM(p.amount), 0) FROM Subscription s JOIN s.plan p " +
           "WHERE s.status = 'ACTIVE' GROUP BY s.customer.id ORDER BY SUM(p.amount) DESC")
    List<Object[]> getTopCustomersBySubscriptionValue(Pageable pageable);

    /**
     * Find subscriptions with upcoming renewals.
     */
    @Query("SELECT s FROM Subscription s WHERE s.nextBillingDate BETWEEN :startDate AND :endDate AND s.status = 'ACTIVE'")
    List<Subscription> findUpcomingRenewals(@Param("startDate") ZonedDateTime startDate,
                                          @Param("endDate") ZonedDateTime endDate);

    /**
     * Find past due subscriptions.
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'PAST_DUE'")
    List<Subscription> findPastDueSubscriptions();

    /**
     * Find subscriptions to cancel (past due for too long).
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'PAST_DUE' AND s.nextBillingDate < :cutoffDate")
    List<Subscription> findSubscriptionsToCancel(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Find subscriptions cancelled within date range.
     */
    @Query("SELECT s FROM Subscription s WHERE s.cancelledAt BETWEEN :startDate AND :endDate")
    List<Subscription> findCancelledSubscriptionsBetween(@Param("startDate") ZonedDateTime startDate,
                                                        @Param("endDate") ZonedDateTime endDate);

    /**
     * Get subscription statistics.
     */
    @Query("SELECT " +
           "COUNT(*) as totalSubscriptions, " +
           "COUNT(CASE WHEN s.status = 'ACTIVE' THEN 1 END) as activeSubscriptions, " +
           "COUNT(CASE WHEN s.status = 'CANCELLED' THEN 1 END) as cancelledSubscriptions, " +
           "COUNT(CASE WHEN s.status = 'PAST_DUE' THEN 1 END) as pastDueSubscriptions, " +
           "COUNT(CASE WHEN s.trialStart <= CURRENT_TIMESTAMP AND s.trialEnd > CURRENT_TIMESTAMP THEN 1 END) as trialSubscriptions " +
           "FROM Subscription s WHERE s.createdAt >= :startDate")
    Object[] getSubscriptionStatistics(@Param("startDate") ZonedDateTime startDate);

    /**
     * Get churn rate data.
     */
    @Query("SELECT DATE(s.cancelledAt) as cancelDate, COUNT(s) as cancellationCount " +
           "FROM Subscription s WHERE s.cancelledAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(s.cancelledAt) ORDER BY cancelDate")
    List<Object[]> getChurnRateData(@Param("startDate") ZonedDateTime startDate,
                                   @Param("endDate") ZonedDateTime endDate);

    /**
     * Find subscriptions by payment method.
     */
    @Query("SELECT s FROM Subscription s WHERE s.paymentMethod.id = :paymentMethodId")
    List<Subscription> findByPaymentMethodId(@Param("paymentMethodId") UUID paymentMethodId);

    /**
     * Find customers with multiple active subscriptions.
     */
    @Query("SELECT s.customer.id, COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE' " +
           "GROUP BY s.customer.id HAVING COUNT(s) > 1")
    List<Object[]> findCustomersWithMultipleActiveSubscriptions();

    /**
     * Find subscriptions with failed payments.
     */
    @Query("SELECT DISTINCT s FROM Subscription s JOIN s.invoices i WHERE i.status = 'FAILED'")
    List<Subscription> findSubscriptionsWithFailedPayments();

    /**
     * Update subscription status.
     */
    @Query("UPDATE Subscription s SET s.status = :status WHERE s.id = :subscriptionId")
    void updateSubscriptionStatus(@Param("subscriptionId") UUID subscriptionId, @Param("status") SubscriptionStatus status);

    /**
     * Update next billing date.
     */
    @Query("UPDATE Subscription s SET s.nextBillingDate = :nextBillingDate WHERE s.id = :subscriptionId")
    void updateNextBillingDate(@Param("subscriptionId") UUID subscriptionId, @Param("nextBillingDate") ZonedDateTime nextBillingDate);

    /**
     * Cancel subscription.
     */
    @Query("UPDATE Subscription s SET s.status = 'CANCELLED', s.cancelledAt = :cancelledAt, " +
           "s.cancellationReason = :reason WHERE s.id = :subscriptionId")
    void cancelSubscription(@Param("subscriptionId") UUID subscriptionId,
                           @Param("cancelledAt") ZonedDateTime cancelledAt,
                           @Param("reason") String reason);

    /**
     * Find subscription growth data.
     */
    @Query("SELECT DATE(s.createdAt) as signupDate, COUNT(s) as newSubscriptions " +
           "FROM Subscription s WHERE s.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(s.createdAt) ORDER BY signupDate")
    List<Object[]> getSubscriptionGrowthData(@Param("startDate") ZonedDateTime startDate,
                                           @Param("endDate") ZonedDateTime endDate);

    /**
     * Find average subscription lifetime.
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (COALESCE(cancelled_at, CURRENT_TIMESTAMP) - created_at)) / 86400.0) " +
           "FROM subscriptions WHERE created_at >= :startDate", nativeQuery = true)
    Double getAverageSubscriptionLifetime(@Param("startDate") ZonedDateTime startDate);

    /**
     * Find subscription renewal rate.
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN s.status = 'ACTIVE' AND s.createdAt < :oneMonthAgo THEN 1 END) as renewedSubscriptions, " +
           "COUNT(CASE WHEN s.createdAt < :oneMonthAgo THEN 1 END) as totalEligibleSubscriptions " +
           "FROM Subscription s")
    Object[] getSubscriptionRenewalRate(@Param("oneMonthAgo") ZonedDateTime oneMonthAgo);

    /**
     * Find trial conversion rate.
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN s.trialEnd IS NOT NULL AND s.status = 'ACTIVE' AND s.trialEnd < CURRENT_TIMESTAMP THEN 1 END) as convertedTrials, " +
           "COUNT(CASE WHEN s.trialEnd IS NOT NULL AND s.trialEnd < CURRENT_TIMESTAMP THEN 1 END) as totalExpiredTrials " +
           "FROM Subscription s WHERE s.createdAt >= :startDate")
    Object[] getTrialConversionRate(@Param("startDate") ZonedDateTime startDate);

    /**
     * Count subscriptions by plan and status.
     */
    @Query(value = "SELECT COUNT(*) FROM subscriptions WHERE plan_id = :#{#plan.id} AND status = :status", nativeQuery = true)
    long countByPlanAndStatus(@Param("plan") SubscriptionPlan plan, @Param("status") String status);

    /**
     * Count all subscriptions by plan.
     */
    long countByPlan(SubscriptionPlan plan);

    /**
     * Find subscriptions with scheduled cancellation.
     */
    @Query(value = "SELECT s.* FROM subscriptions s WHERE CAST(s.metadata->>'scheduledCancellation' AS timestamp) <= :currentDate", nativeQuery = true)
    List<Subscription> findSubscriptionsWithScheduledCancellation(@Param("currentDate") ZonedDateTime currentDate);

    /**
     * Find subscriptions with scheduled plan change.
     */
    @Query(value = "SELECT s.* FROM subscriptions s WHERE CAST(s.metadata->>'planChangeDate' AS timestamp) <= :currentDate", nativeQuery = true)
    List<Subscription> findSubscriptionsWithScheduledPlanChange(@Param("currentDate") ZonedDateTime currentDate);

    /**
     * Count subscriptions created between dates.
     */
    long countByCreatedAtBetween(ZonedDateTime startDate, ZonedDateTime endDate);

    /**
     * Count subscriptions by status and cancelled between dates.
     */
    @Query(value = "SELECT COUNT(*) FROM subscriptions WHERE status = :status AND cancelled_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    long countByStatusAndCancelledAtBetween(@Param("status") String status, @Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);

    /**
     * Calculate active monthly recurring revenue.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Subscription s JOIN s.plan p WHERE s.status = 'ACTIVE' AND p.intervalUnit = 'MONTH'")
    BigDecimal calculateActiveMonthlyRevenue();

    /**
     * Count active subscriptions at a specific date.
     */
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE' AND s.createdAt <= :date AND (s.cancelledAt IS NULL OR s.cancelledAt > :date)")
    long countActiveAtDate(@Param("date") ZonedDateTime date);
}
