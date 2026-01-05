package com.talentica.paymentgateway.repository;

import com.talentica.paymentgateway.entity.ApiKey;
import com.talentica.paymentgateway.entity.AuditLog;
import com.talentica.paymentgateway.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.net.InetAddress;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for AuditLog entity.
 * Provides data access methods for audit trail management and compliance reporting.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs by entity type and ID.
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);

    /**
     * Find audit logs by entity type.
     */
    List<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType);

    /**
     * Find audit logs by action.
     */
    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);

    /**
     * Find audit logs by user.
     */
    List<AuditLog> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find audit logs by user ID.
     */
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find audit logs by API key.
     */
    List<AuditLog> findByApiKeyOrderByCreatedAtDesc(ApiKey apiKey);

    /**
     * Find audit logs by API key ID.
     */
    List<AuditLog> findByApiKeyIdOrderByCreatedAtDesc(UUID apiKeyId);

    /**
     * Find audit logs by correlation ID.
     */
    List<AuditLog> findByCorrelationIdOrderByCreatedAtDesc(String correlationId);

    /**
     * Find audit logs by IP address.
     */
    List<AuditLog> findByIpAddressOrderByCreatedAtDesc(InetAddress ipAddress);

    /**
     * Find audit logs within date range.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AuditLog> findAuditLogsBetween(@Param("startDate") ZonedDateTime startDate,
                                       @Param("endDate") ZonedDateTime endDate);

    /**
     * Find audit logs with filters and pagination.
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:entityId IS NULL OR a.entityId = :entityId) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:userId IS NULL OR a.user.id = :userId) AND " +
           "(:apiKeyId IS NULL OR a.apiKey.id = :apiKeyId) AND " +
           "(:correlationId IS NULL OR a.correlationId = :correlationId) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findAuditLogsWithFilters(@Param("entityType") String entityType,
                                           @Param("entityId") UUID entityId,
                                           @Param("action") String action,
                                           @Param("userId") UUID userId,
                                           @Param("apiKeyId") UUID apiKeyId,
                                           @Param("correlationId") String correlationId,
                                           @Param("startDate") ZonedDateTime startDate,
                                           @Param("endDate") ZonedDateTime endDate,
                                           Pageable pageable);

    /**
     * Find creation audit logs.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action IN ('CREATE', 'CREATED') ORDER BY a.createdAt DESC")
    List<AuditLog> findCreationAuditLogs();

    /**
     * Find update audit logs.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action IN ('UPDATE', 'UPDATED') ORDER BY a.createdAt DESC")
    List<AuditLog> findUpdateAuditLogs();

    /**
     * Find deletion audit logs.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action IN ('DELETE', 'DELETED') ORDER BY a.createdAt DESC")
    List<AuditLog> findDeletionAuditLogs();

    /**
     * Find audit logs with changes.
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(a.oldValues IS NOT NULL AND SIZE(a.oldValues) > 0) OR " +
           "(a.newValues IS NOT NULL AND SIZE(a.newValues) > 0) " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findAuditLogsWithChanges();

    /**
     * Find system audit logs (no user or API key).
     */
    @Query("SELECT a FROM AuditLog a WHERE a.user IS NULL AND a.apiKey IS NULL ORDER BY a.createdAt DESC")
    List<AuditLog> findSystemAuditLogs();

    /**
     * Find user audit logs (initiated by users).
     */
    @Query("SELECT a FROM AuditLog a WHERE a.user IS NOT NULL ORDER BY a.createdAt DESC")
    List<AuditLog> findUserAuditLogs();

    /**
     * Find API audit logs (initiated by API keys).
     */
    @Query("SELECT a FROM AuditLog a WHERE a.apiKey IS NOT NULL ORDER BY a.createdAt DESC")
    List<AuditLog> findApiAuditLogs();

    /**
     * Count audit logs by entity type.
     */
    long countByEntityType(String entityType);

    /**
     * Count audit logs by action.
     */
    long countByAction(String action);

    /**
     * Count audit logs by user.
     */
    long countByUserId(UUID userId);

    /**
     * Count audit logs created today.
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE " +
           "DATE_TRUNC('day', a.createdAt) = DATE_TRUNC('day', CURRENT_TIMESTAMP)")
    long countAuditLogsCreatedToday();

    /**
     * Get audit log statistics.
     */
    @Query("SELECT " +
           "COUNT(*) as totalLogs, " +
           "COUNT(CASE WHEN a.user IS NOT NULL THEN 1 END) as userInitiatedLogs, " +
           "COUNT(CASE WHEN a.apiKey IS NOT NULL THEN 1 END) as apiInitiatedLogs, " +
           "COUNT(CASE WHEN a.user IS NULL AND a.apiKey IS NULL THEN 1 END) as systemLogs, " +
           "COUNT(DISTINCT a.entityType) as distinctEntityTypes, " +
           "COUNT(DISTINCT a.action) as distinctActions " +
           "FROM AuditLog a WHERE a.createdAt >= :startDate")
    Object[] getAuditLogStatistics(@Param("startDate") ZonedDateTime startDate);

    /**
     * Get audit activity by date.
     */
    @Query("SELECT DATE(a.createdAt) as auditDate, COUNT(a) as logCount " +
           "FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(a.createdAt) ORDER BY auditDate")
    List<Object[]> getAuditActivityByDate(@Param("startDate") ZonedDateTime startDate,
                                         @Param("endDate") ZonedDateTime endDate);

    /**
     * Get most active users by audit log count.
     */
    @Query("SELECT a.user.id, a.user.username, COUNT(a) as logCount " +
           "FROM AuditLog a WHERE a.user IS NOT NULL AND a.createdAt >= :startDate " +
           "GROUP BY a.user.id, a.user.username ORDER BY logCount DESC")
    List<Object[]> getMostActiveUsers(@Param("startDate") ZonedDateTime startDate, Pageable pageable);

    /**
     * Get most active API keys by audit log count.
     */
    @Query("SELECT a.apiKey.id, a.apiKey.keyName, COUNT(a) as logCount " +
           "FROM AuditLog a WHERE a.apiKey IS NOT NULL AND a.createdAt >= :startDate " +
           "GROUP BY a.apiKey.id, a.apiKey.keyName ORDER BY logCount DESC")
    List<Object[]> getMostActiveApiKeys(@Param("startDate") ZonedDateTime startDate, Pageable pageable);

    /**
     * Get entity type activity summary.
     */
    @Query("SELECT a.entityType, a.action, COUNT(a) as count " +
           "FROM AuditLog a WHERE a.createdAt >= :startDate " +
           "GROUP BY a.entityType, a.action ORDER BY a.entityType, count DESC")
    List<Object[]> getEntityTypeActivitySummary(@Param("startDate") ZonedDateTime startDate);

    /**
     * Find audit logs by IP address pattern.
     */
    @Query(value = "SELECT * FROM audit_logs a WHERE CAST(a.ip_address AS TEXT) LIKE :ipPattern ORDER BY a.created_at DESC", nativeQuery = true)
    List<AuditLog> findByIpAddressPattern(@Param("ipPattern") String ipPattern);

    /**
     * Find audit logs by user agent pattern.
     */
    @Query("SELECT a FROM AuditLog a WHERE LOWER(a.userAgent) LIKE LOWER(:userAgentPattern) ORDER BY a.createdAt DESC")
    List<AuditLog> findByUserAgentPattern(@Param("userAgentPattern") String userAgentPattern);

    /**
     * Find security-related audit logs.
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "a.action IN ('LOGIN', 'LOGOUT', 'LOGIN_FAILED', 'PASSWORD_CHANGE', 'API_KEY_CREATED', 'API_KEY_DELETED') " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findSecurityAuditLogs();

    /**
     * Find failed login attempts.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action = 'LOGIN_FAILED' ORDER BY a.createdAt DESC")
    List<AuditLog> findFailedLoginAttempts();

    /**
     * Find failed login attempts by IP address.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action = 'LOGIN_FAILED' AND a.ipAddress = :ipAddress ORDER BY a.createdAt DESC")
    List<AuditLog> findFailedLoginAttemptsByIp(@Param("ipAddress") InetAddress ipAddress);

    /**
     * Find recent failed login attempts by IP.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action = 'LOGIN_FAILED' AND a.ipAddress = :ipAddress " +
           "AND a.createdAt >= :cutoffTime ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentFailedLoginAttemptsByIp(@Param("ipAddress") InetAddress ipAddress,
                                                    @Param("cutoffTime") ZonedDateTime cutoffTime);

    /**
     * Count failed login attempts by IP within time window.
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = 'LOGIN_FAILED' AND a.ipAddress = :ipAddress " +
           "AND a.createdAt >= :cutoffTime")
    long countFailedLoginAttemptsByIp(@Param("ipAddress") InetAddress ipAddress,
                                     @Param("cutoffTime") ZonedDateTime cutoffTime);

    /**
     * Find suspicious activity patterns.
     */
    @Query("SELECT a.ipAddress, COUNT(a) as attemptCount " +
           "FROM AuditLog a WHERE a.action = 'LOGIN_FAILED' AND a.createdAt >= :cutoffTime " +
           "GROUP BY a.ipAddress HAVING COUNT(a) >= :threshold ORDER BY attemptCount DESC")
    List<Object[]> findSuspiciousActivityPatterns(@Param("cutoffTime") ZonedDateTime cutoffTime,
                                                 @Param("threshold") long threshold);

    /**
     * Find audit trail for specific entity.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId " +
           "ORDER BY a.createdAt ASC")
    List<AuditLog> findEntityAuditTrail(@Param("entityType") String entityType, @Param("entityId") UUID entityId);

    /**
     * Find compliance audit logs (specific actions).
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action IN :complianceActions ORDER BY a.createdAt DESC")
    List<AuditLog> findComplianceAuditLogs(@Param("complianceActions") List<String> complianceActions);

    /**
     * Delete old audit logs.
     */
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :cutoffDate")
    void deleteOldAuditLogs(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Archive old audit logs (mark for archival).
     */
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt < :cutoffDate ORDER BY a.createdAt")
    List<AuditLog> findAuditLogsForArchival(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Get audit log retention statistics.
     */
    @Query("SELECT " +
           "COUNT(*) as totalLogs, " +
           "COUNT(CASE WHEN a.createdAt >= :recentCutoff THEN 1 END) as recentLogs, " +
           "COUNT(CASE WHEN a.createdAt < :archiveCutoff THEN 1 END) as logsForArchival, " +
           "MIN(a.createdAt) as oldestLog, " +
           "MAX(a.createdAt) as newestLog " +
           "FROM AuditLog a")
    Object[] getAuditLogRetentionStatistics(@Param("recentCutoff") ZonedDateTime recentCutoff,
                                           @Param("archiveCutoff") ZonedDateTime archiveCutoff);
}
