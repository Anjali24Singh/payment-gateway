package com.talentica.paymentgateway.repository;

import com.talentica.paymentgateway.entity.Webhook;
import com.talentica.paymentgateway.entity.WebhookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Webhook entity.
 * Provides data access methods for webhook delivery management.
 */
@Repository
public interface WebhookRepository extends JpaRepository<Webhook, UUID> {

    /**
     * Find webhook by webhook ID.
     */
    Optional<Webhook> findByWebhookId(String webhookId);

    /**
     * Find webhooks by event type.
     */
    List<Webhook> findByEventType(String eventType);

    /**
     * Find webhooks by event ID.
     */
    List<Webhook> findByEventId(String eventId);

    /**
     * Find webhooks by status.
     */
    List<Webhook> findByStatus(WebhookStatus status);

    /**
     * Find webhooks by endpoint URL.
     */
    List<Webhook> findByEndpointUrl(String endpointUrl);

    /**
     * Find webhooks by correlation ID.
     */
    List<Webhook> findByCorrelationId(String correlationId);

    /**
     * Find pending webhooks.
     */
    List<Webhook> findByStatusOrderByScheduledAt(WebhookStatus status);

    /**
     * Find webhooks ready for retry.
     */
    @Query(value = "SELECT * FROM webhooks w WHERE w.next_attempt_at <= CURRENT_TIMESTAMP AND " +
           "w.attempts < w.max_attempts AND w.status IN (CAST('FAILED' AS webhook_status), CAST('RETRYING' AS webhook_status))", nativeQuery = true)
    List<Webhook> findWebhooksReadyForRetry();

    /**
     * Find failed webhooks with max attempts reached.
     */
    @Query(value = "SELECT * FROM webhooks w WHERE w.attempts >= w.max_attempts AND w.status = CAST('FAILED' AS webhook_status)", nativeQuery = true)
    List<Webhook> findFailedWebhooksWithMaxAttempts();

    /**
     * Find webhooks scheduled for delivery.
     */
    @Query(value = "SELECT * FROM webhooks w WHERE w.scheduled_at <= CURRENT_TIMESTAMP AND w.status = CAST('PENDING' AS webhook_status)", nativeQuery = true)
    List<Webhook> findWebhooksScheduledForDelivery();

    /**
     * Find webhooks within date range.
     */
    @Query("SELECT w FROM Webhook w WHERE w.createdAt BETWEEN :startDate AND :endDate")
    List<Webhook> findWebhooksBetween(@Param("startDate") ZonedDateTime startDate,
                                     @Param("endDate") ZonedDateTime endDate);

    /**
     * Find webhooks with filters and pagination.
     */
    @Query("SELECT w FROM Webhook w WHERE " +
           "(:eventType IS NULL OR w.eventType = :eventType) AND " +
           "(:status IS NULL OR w.status = :status) AND " +
           "(:endpointUrl IS NULL OR w.endpointUrl = :endpointUrl) AND " +
           "(:startDate IS NULL OR w.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR w.createdAt <= :endDate) AND " +
           "(:correlationId IS NULL OR w.correlationId = :correlationId)")
    Page<Webhook> findWebhooksWithFilters(@Param("eventType") String eventType,
                                         @Param("status") WebhookStatus status,
                                         @Param("endpointUrl") String endpointUrl,
                                         @Param("startDate") ZonedDateTime startDate,
                                         @Param("endDate") ZonedDateTime endDate,
                                         @Param("correlationId") String correlationId,
                                         Pageable pageable);

    /**
     * Count webhooks by status.
     */
    long countByStatus(WebhookStatus status);

    /**
     * Count webhooks by event type.
     */
    long countByEventType(String eventType);

    /**
     * Count webhooks created today.
     */
    @Query("SELECT COUNT(w) FROM Webhook w WHERE " +
           "DATE_TRUNC('day', w.createdAt) = DATE_TRUNC('day', CURRENT_TIMESTAMP)")
    long countWebhooksCreatedToday();

    /**
     * Count successful webhook deliveries today.
     */
    @Query(value = "SELECT COUNT(w) FROM webhooks w WHERE " +
           "DATE_TRUNC('day', w.delivered_at) = DATE_TRUNC('day', CURRENT_TIMESTAMP) AND w.status = CAST('DELIVERED' AS webhook_status)", nativeQuery = true)
    long countSuccessfulDeliveriesToday();

    /**
     * Find webhooks by response status code.
     */
    List<Webhook> findByResponseStatusCode(Integer responseStatusCode);

    /**
     * Find webhooks with successful response codes.
     */
    @Query("SELECT w FROM Webhook w WHERE w.responseStatusCode BETWEEN 200 AND 299")
    List<Webhook> findWebhooksWithSuccessfulResponse();

    /**
     * Find webhooks with error response codes.
     */
    @Query("SELECT w FROM Webhook w WHERE w.responseStatusCode >= 400")
    List<Webhook> findWebhooksWithErrorResponse();

    /**
     * Find webhooks delivered within date range.
     */
    @Query("SELECT w FROM Webhook w WHERE w.deliveredAt BETWEEN :startDate AND :endDate")
    List<Webhook> findWebhooksDeliveredBetween(@Param("startDate") ZonedDateTime startDate,
                                              @Param("endDate") ZonedDateTime endDate);

    /**
     * Get webhook statistics.
     */
    @Query("SELECT " +
           "COUNT(*) as totalWebhooks, " +
           "COUNT(CASE WHEN w.status = 'DELIVERED' THEN 1 END) as deliveredWebhooks, " +
           "COUNT(CASE WHEN w.status = 'FAILED' THEN 1 END) as failedWebhooks, " +
           "COUNT(CASE WHEN w.status = 'PENDING' THEN 1 END) as pendingWebhooks, " +
           "COUNT(CASE WHEN w.status = 'RETRYING' THEN 1 END) as retryingWebhooks, " +
           "COALESCE(AVG(w.attempts), 0) as averageAttempts " +
           "FROM Webhook w WHERE w.createdAt >= :startDate")
    Object[] getWebhookStatistics(@Param("startDate") ZonedDateTime startDate);

    /**
     * Get webhook delivery summary by date.
     */
    @Query("SELECT DATE(w.createdAt) as webhookDate, " +
           "COUNT(w) as totalWebhooks, " +
           "COUNT(CASE WHEN w.status = 'DELIVERED' THEN 1 END) as deliveredCount, " +
           "COUNT(CASE WHEN w.status = 'FAILED' THEN 1 END) as failedCount " +
           "FROM Webhook w WHERE w.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(w.createdAt) ORDER BY webhookDate")
    List<Object[]> getWebhookDeliverySummaryByDate(@Param("startDate") ZonedDateTime startDate,
                                                  @Param("endDate") ZonedDateTime endDate);

    /**
     * Find webhook delivery rates by endpoint.
     */
    @Query("SELECT w.endpointUrl, " +
           "COUNT(w) as totalWebhooks, " +
           "COUNT(CASE WHEN w.status = 'DELIVERED' THEN 1 END) as deliveredCount, " +
           "(COUNT(CASE WHEN w.status = 'DELIVERED' THEN 1 END) * 100.0 / COUNT(w)) as deliveryRate " +
           "FROM Webhook w WHERE w.createdAt >= :startDate " +
           "GROUP BY w.endpointUrl " +
           "HAVING COUNT(w) > 0 " +
           "ORDER BY deliveryRate DESC")
    List<Object[]> getDeliveryRatesByEndpoint(@Param("startDate") ZonedDateTime startDate);

    /**
     * Find webhook delivery rates by event type.
     */
    @Query("SELECT w.eventType, " +
           "COUNT(w) as totalWebhooks, " +
           "COUNT(CASE WHEN w.status = 'DELIVERED' THEN 1 END) as deliveredCount, " +
           "(COUNT(CASE WHEN w.status = 'DELIVERED' THEN 1 END) * 100.0 / COUNT(w)) as deliveryRate " +
           "FROM Webhook w WHERE w.createdAt >= :startDate " +
           "GROUP BY w.eventType " +
           "ORDER BY deliveryRate DESC")
    List<Object[]> getDeliveryRatesByEventType(@Param("startDate") ZonedDateTime startDate);

    /**
     * Find webhooks with high retry count.
     */
    @Query("SELECT w FROM Webhook w WHERE w.attempts >= :minAttempts")
    List<Webhook> findWebhooksWithHighRetryCount(@Param("minAttempts") Integer minAttempts);

    /**
     * Find oldest pending webhooks.
     */
    @Query("SELECT w FROM Webhook w WHERE w.status = 'PENDING' ORDER BY w.scheduledAt ASC")
    List<Webhook> findOldestPendingWebhooks(Pageable pageable);

    /**
     * Find webhooks by HTTP method.
     */
    List<Webhook> findByHttpMethod(String httpMethod);

    /**
     * Find average delivery time for successful webhooks.
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (delivered_at - scheduled_at))) " +
           "FROM webhooks WHERE status = 'DELIVERED' AND created_at >= :startDate", nativeQuery = true)
    Double getAverageDeliveryTime(@Param("startDate") ZonedDateTime startDate);

    /**
     * Update webhook status.
     */
    @Query(value = "UPDATE webhooks SET status = CAST(:status AS webhook_status) WHERE id = :webhookId", nativeQuery = true)
    void updateWebhookStatus(@Param("webhookId") UUID webhookId, @Param("status") String status);

    /**
     * Update webhook delivery information.
     */
    @Query(value = "UPDATE webhooks SET status = CAST('DELIVERED' AS webhook_status), delivered_at = :deliveredAt, " +
           "response_status_code = :statusCode WHERE id = :webhookId", nativeQuery = true)
    void markWebhookAsDelivered(@Param("webhookId") UUID webhookId,
                               @Param("deliveredAt") ZonedDateTime deliveredAt,
                               @Param("statusCode") Integer statusCode);

    /**
     * Update webhook failure information.
     */
    @Query(value = "UPDATE webhooks SET attempts = attempts + 1, error_message = :errorMessage WHERE id = :webhookId", nativeQuery = true)
    void markWebhookAsFailed(@Param("webhookId") UUID webhookId, @Param("errorMessage") String errorMessage);

    /**
     * Update next attempt time.
     */
    @Query(value = "UPDATE webhooks SET next_attempt_at = :nextAttemptAt WHERE id = :webhookId", nativeQuery = true)
    void updateNextAttemptTime(@Param("webhookId") UUID webhookId, @Param("nextAttemptAt") ZonedDateTime nextAttemptAt);

    /**
     * Delete old delivered webhooks.
     */
    @Query(value = "DELETE FROM webhooks WHERE status = CAST('DELIVERED' AS webhook_status) AND delivered_at < :cutoffDate", nativeQuery = true)
    void deleteOldDeliveredWebhooks(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Delete old failed webhooks with max attempts.
     */
    @Query(value = "DELETE FROM webhooks WHERE status = CAST('FAILED' AS webhook_status) AND attempts >= max_attempts AND created_at < :cutoffDate", nativeQuery = true)
    void deleteOldFailedWebhooks(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Find webhooks for cleanup (old and processed).
     */
    @Query("SELECT w FROM Webhook w WHERE " +
           "((w.status = 'DELIVERED' AND w.deliveredAt < :deliveredCutoff) OR " +
           "(w.status = 'FAILED' AND w.attempts >= w.maxAttempts AND w.createdAt < :failedCutoff))")
    List<Webhook> findWebhooksForCleanup(@Param("deliveredCutoff") ZonedDateTime deliveredCutoff,
                                        @Param("failedCutoff") ZonedDateTime failedCutoff);

    /**
     * Get webhook performance metrics.
     */
    @Query(value = "SELECT " +
           "endpoint_url, " +
           "COUNT(*) as totalRequests, " +
           "COUNT(CASE WHEN status = 'DELIVERED' THEN 1 END) as successfulRequests, " +
           "COALESCE(AVG(attempts), 0) as averageAttempts, " +
           "COALESCE(AVG(CASE WHEN status = 'DELIVERED' THEN EXTRACT(EPOCH FROM (delivered_at - scheduled_at)) END), 0) as averageDeliveryTime " +
           "FROM webhooks WHERE created_at >= :startDate " +
           "GROUP BY endpoint_url " +
           "ORDER BY successfulRequests DESC", nativeQuery = true)
    List<Object[]> getWebhookPerformanceMetrics(@Param("startDate") ZonedDateTime startDate);

    /**
     * Find webhooks by event ID, event type, and created after date (for duplicate detection).
     */
    @Query("SELECT w FROM Webhook w WHERE w.eventId = :eventId AND w.eventType = :eventType AND w.createdAt >= :createdAfter")
    List<Webhook> findByEventIdAndEventTypeAndCreatedAtAfter(@Param("eventId") String eventId,
                                                            @Param("eventType") String eventType,
                                                            @Param("createdAfter") ZonedDateTime createdAfter);

    /**
     * Count webhooks created since a specific date.
     */
    @Query("SELECT COUNT(w) FROM Webhook w WHERE w.createdAt >= :startDate")
    long countWebhooksCreatedSince(@Param("startDate") ZonedDateTime startDate);

    /**
     * Count webhooks by status and created after date.
     */
    @Query("SELECT COUNT(w) FROM Webhook w WHERE w.status = :status AND w.createdAt >= :startDate")
    long countByStatusAndCreatedAtAfter(@Param("status") WebhookStatus status, @Param("startDate") ZonedDateTime startDate);
}
