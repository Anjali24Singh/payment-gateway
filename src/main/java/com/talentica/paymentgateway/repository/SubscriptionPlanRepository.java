package com.talentica.paymentgateway.repository;

import com.talentica.paymentgateway.entity.SubscriptionPlan;
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
 * Repository interface for SubscriptionPlan entity.
 * Provides data access methods for subscription plan management.
 */
@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    /**
     * Find subscription plan by plan code.
     */
    Optional<SubscriptionPlan> findByPlanCode(String planCode);

    /**
     * Check if plan code exists.
     */
    boolean existsByPlanCode(String planCode);

    /**
     * Find all active plans.
     */
    List<SubscriptionPlan> findByIsActiveTrue();

    /**
     * Find plans by interval unit.
     */
    List<SubscriptionPlan> findByIntervalUnit(String intervalUnit);

    /**
     * Find plans by interval unit and count.
     */
    List<SubscriptionPlan> findByIntervalUnitAndIntervalCount(String intervalUnit, Integer intervalCount);

    /**
     * Find plans by currency.
     */
    List<SubscriptionPlan> findByCurrency(String currency);

    /**
     * Find plans with trial period.
     */
    List<SubscriptionPlan> findByTrialPeriodDaysGreaterThan(Integer days);

    /**
     * Find plans without trial period.
     */
    List<SubscriptionPlan> findByTrialPeriodDaysOrTrialPeriodDaysIsNull(Integer days);

    /**
     * Find plans by amount range.
     */
    List<SubscriptionPlan> findByAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);

    /**
     * Find plans by amount greater than.
     */
    List<SubscriptionPlan> findByAmountGreaterThan(BigDecimal amount);

    /**
     * Find plans by amount less than.
     */
    List<SubscriptionPlan> findByAmountLessThan(BigDecimal amount);

    /**
     * Find plans with filters and pagination.
     */
    @Query("SELECT p FROM SubscriptionPlan p WHERE " +
           "(:isActive IS NULL OR p.isActive = :isActive) AND " +
           "(:intervalUnit IS NULL OR p.intervalUnit = :intervalUnit) AND " +
           "(:currency IS NULL OR p.currency = :currency) AND " +
           "(:minAmount IS NULL OR p.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR p.amount <= :maxAmount) AND " +
           "(:hasTrialPeriod IS NULL OR " +
           "(:hasTrialPeriod = true AND p.trialPeriodDays > 0) OR " +
           "(:hasTrialPeriod = false AND (p.trialPeriodDays IS NULL OR p.trialPeriodDays = 0))) AND " +
           "(:searchTerm IS NULL OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.planCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<SubscriptionPlan> findPlansWithFilters(@Param("isActive") Boolean isActive,
                                               @Param("intervalUnit") String intervalUnit,
                                               @Param("currency") String currency,
                                               @Param("minAmount") BigDecimal minAmount,
                                               @Param("maxAmount") BigDecimal maxAmount,
                                               @Param("hasTrialPeriod") Boolean hasTrialPeriod,
                                               @Param("searchTerm") String searchTerm,
                                               Pageable pageable);

    /**
     * Find plans by name (case-insensitive partial match).
     */
    List<SubscriptionPlan> findByNameContainingIgnoreCase(String name);

    /**
     * Find plans by description (case-insensitive partial match).
     */
    List<SubscriptionPlan> findByDescriptionContainingIgnoreCase(String description);

    /**
     * Count active plans.
     */
    long countByIsActiveTrue();

    /**
     * Count plans by interval unit.
     */
    long countByIntervalUnit(String intervalUnit);

    /**
     * Count plans by currency.
     */
    long countByCurrency(String currency);

    /**
     * Count plans with trial period.
     */
    long countByTrialPeriodDaysGreaterThan(Integer days);

    /**
     * Find most popular plans by subscription count.
     */
    @Query("SELECT p, COUNT(s) as subscriptionCount FROM SubscriptionPlan p " +
           "LEFT JOIN p.subscriptions s WHERE s.status = 'ACTIVE' " +
           "GROUP BY p.id ORDER BY subscriptionCount DESC")
    List<Object[]> findMostPopularPlans(Pageable pageable);

    /**
     * Find top revenue generating plans.
     */
    @Query("SELECT p, COUNT(s) as subscriptionCount, COALESCE(SUM(p.amount), 0) as totalRevenue " +
           "FROM SubscriptionPlan p LEFT JOIN p.subscriptions s WHERE s.status = 'ACTIVE' " +
           "GROUP BY p.id ORDER BY totalRevenue DESC")
    List<Object[]> findTopRevenueGeneratingPlans(Pageable pageable);

    /**
     * Find plans created within date range.
     */
    @Query("SELECT p FROM SubscriptionPlan p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<SubscriptionPlan> findPlansCreatedBetween(@Param("startDate") ZonedDateTime startDate,
                                                  @Param("endDate") ZonedDateTime endDate);

    /**
     * Find plans with active subscriptions.
     */
    @Query("SELECT DISTINCT p FROM SubscriptionPlan p JOIN p.subscriptions s WHERE s.status = 'ACTIVE'")
    List<SubscriptionPlan> findPlansWithActiveSubscriptions();

    /**
     * Find plans without subscriptions.
     */
    @Query("SELECT p FROM SubscriptionPlan p WHERE SIZE(p.subscriptions) = 0")
    List<SubscriptionPlan> findPlansWithoutSubscriptions();

    /**
     * Find unused plans older than specified date.
     */
    @Query("SELECT p FROM SubscriptionPlan p WHERE SIZE(p.subscriptions) = 0 AND p.createdAt < :cutoffDate")
    List<SubscriptionPlan> findUnusedPlansOlderThan(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Get plan statistics.
     */
    @Query("SELECT " +
           "COUNT(*) as totalPlans, " +
           "COUNT(CASE WHEN p.isActive = true THEN 1 END) as activePlans, " +
           "COUNT(CASE WHEN p.trialPeriodDays > 0 THEN 1 END) as plansWithTrial, " +
           "COALESCE(AVG(p.amount), 0) as averageAmount, " +
           "COALESCE(MIN(p.amount), 0) as minAmount, " +
           "COALESCE(MAX(p.amount), 0) as maxAmount " +
           "FROM SubscriptionPlan p WHERE p.createdAt >= :startDate")
    Object[] getPlanStatistics(@Param("startDate") ZonedDateTime startDate);

    /**
     * Find plans by interval (monthly, yearly, etc.).
     */
    @Query("SELECT p FROM SubscriptionPlan p WHERE " +
           "(p.intervalUnit = 'MONTH' AND p.intervalCount = 1) OR " +
           "(p.intervalUnit = 'YEAR' AND p.intervalCount = 1)")
    List<SubscriptionPlan> findStandardIntervalPlans();

    /**
     * Find custom interval plans.
     */
    @Query("SELECT p FROM SubscriptionPlan p WHERE " +
           "NOT ((p.intervalUnit = 'MONTH' AND p.intervalCount = 1) OR " +
           "(p.intervalUnit = 'YEAR' AND p.intervalCount = 1))")
    List<SubscriptionPlan> findCustomIntervalPlans();

    /**
     * Find plans by metadata key-value.
     */
    @Query(value = "SELECT p.* FROM subscription_plans p WHERE p.metadata->>:jsonPath = :value", nativeQuery = true)
    List<SubscriptionPlan> findByMetadata(@Param("jsonPath") String jsonPath, @Param("value") String value);

    /**
     * Activate plan.
     */
    @Query("UPDATE SubscriptionPlan p SET p.isActive = true WHERE p.id = :planId")
    void activatePlan(@Param("planId") UUID planId);

    /**
     * Deactivate plan.
     */
    @Query("UPDATE SubscriptionPlan p SET p.isActive = false WHERE p.id = :planId")
    void deactivatePlan(@Param("planId") UUID planId);

    /**
     * Update plan amount.
     */
    @Query("UPDATE SubscriptionPlan p SET p.amount = :amount WHERE p.id = :planId")
    void updatePlanAmount(@Param("planId") UUID planId, @Param("amount") BigDecimal amount);

    /**
     * Get subscription conversion rates by plan.
     */
    @Query("SELECT p.planCode, " +
           "COUNT(CASE WHEN s.trialEnd IS NOT NULL THEN 1 END) as trialSignups, " +
           "COUNT(CASE WHEN s.trialEnd IS NOT NULL AND s.status = 'ACTIVE' AND s.trialEnd < CURRENT_TIMESTAMP THEN 1 END) as trialConversions, " +
           "COUNT(CASE WHEN s.status = 'ACTIVE' THEN 1 END) as activeSubscriptions " +
           "FROM SubscriptionPlan p LEFT JOIN p.subscriptions s " +
           "GROUP BY p.planCode")
    List<Object[]> getConversionRatesByPlan();

    /**
     * Find plans with highest churn rate.
     */
    @Query("SELECT p.planCode, " +
           "COUNT(s) as totalSubscriptions, " +
           "COUNT(CASE WHEN s.status = 'CANCELLED' THEN 1 END) as cancelledSubscriptions, " +
           "(COUNT(CASE WHEN s.status = 'CANCELLED' THEN 1 END) * 100.0 / COUNT(s)) as churnRate " +
           "FROM SubscriptionPlan p LEFT JOIN p.subscriptions s " +
           "WHERE s.createdAt >= :startDate " +
           "GROUP BY p.planCode " +
           "HAVING COUNT(s) > 0 " +
           "ORDER BY churnRate DESC")
    List<Object[]> getChurnRatesByPlan(@Param("startDate") ZonedDateTime startDate);

    /**
     * Find similar plans by amount range.
     */
    @Query("SELECT p FROM SubscriptionPlan p WHERE p.id != :planId AND " +
           "p.amount BETWEEN (:amount * 0.8) AND (:amount * 1.2) AND " +
           "p.currency = :currency AND p.isActive = true")
    List<SubscriptionPlan> findSimilarPlans(@Param("planId") UUID planId, 
                                           @Param("amount") BigDecimal amount, 
                                           @Param("currency") String currency);
}
