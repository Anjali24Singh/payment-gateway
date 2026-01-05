package com.talentica.paymentgateway.repository;

import com.talentica.paymentgateway.entity.ApiKey;
import com.talentica.paymentgateway.entity.User;
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
 * Repository interface for ApiKey entity.
 * Provides data access methods for API key management and authentication.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    /**
     * Find API key by key hash.
     */
    Optional<ApiKey> findByKeyHash(String keyHash);

    /**
     * Find API key by key prefix.
     */
    List<ApiKey> findByKeyPrefix(String keyPrefix);

    /**
     * Check if key hash exists.
     */
    boolean existsByKeyHash(String keyHash);

    /**
     * Find API keys by user.
     */
    List<ApiKey> findByUser(User user);

    /**
     * Find API keys by user ID.
     */
    List<ApiKey> findByUserId(UUID userId);

    /**
     * Find active API keys by user.
     */
    List<ApiKey> findByUserAndIsActiveTrue(User user);

    /**
     * Find active API keys by user ID.
     */
    List<ApiKey> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Find all active API keys.
     */
    List<ApiKey> findByIsActiveTrue();

    /**
     * Find API keys by name (case-insensitive partial match).
     */
    List<ApiKey> findByKeyNameContainingIgnoreCase(String keyName);

    /**
     * Find expired API keys.
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.expiresAt IS NOT NULL AND ak.expiresAt < CURRENT_TIMESTAMP")
    List<ApiKey> findExpiredApiKeys();

    /**
     * Find API keys expiring soon.
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.expiresAt IS NOT NULL AND " +
           "ak.expiresAt BETWEEN CURRENT_TIMESTAMP AND :futureDate AND ak.isActive = true")
    List<ApiKey> findApiKeysExpiringSoon(@Param("futureDate") ZonedDateTime futureDate);

    /**
     * Find API keys that never expire.
     */
    List<ApiKey> findByExpiresAtIsNull();

    /**
     * Find API keys with specific permission.
     */
    @Query(value = "SELECT * FROM api_keys ak WHERE :permission = ANY(ak.permissions)", nativeQuery = true)
    List<ApiKey> findByPermission(@Param("permission") String permission);

    /**
     * Find API keys with any of the specified permissions.
     */
    @Query(value = "SELECT * FROM api_keys ak WHERE ak.permissions && CAST(:permissions AS text[])", nativeQuery = true)
    List<ApiKey> findByAnyPermission(@Param("permissions") String[] permissions);

    /**
     * Find API keys by rate limit range.
     */
    List<ApiKey> findByRateLimitPerHourBetween(Integer minLimit, Integer maxLimit);

    /**
     * Find API keys with high rate limits.
     */
    List<ApiKey> findByRateLimitPerHourGreaterThan(Integer limit);

    /**
     * Find API keys with filters and pagination.
     */
    @Query("SELECT ak FROM ApiKey ak WHERE " +
           "(:userId IS NULL OR ak.user.id = :userId) AND " +
           "(:isActive IS NULL OR ak.isActive = :isActive) AND " +
           "(:isExpired IS NULL OR " +
           "(:isExpired = true AND ak.expiresAt IS NOT NULL AND ak.expiresAt < CURRENT_TIMESTAMP) OR " +
           "(:isExpired = false AND (ak.expiresAt IS NULL OR ak.expiresAt >= CURRENT_TIMESTAMP))) AND " +
           "(:searchTerm IS NULL OR " +
           "LOWER(ak.keyName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ak.keyPrefix) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<ApiKey> findApiKeysWithFilters(@Param("userId") UUID userId,
                                       @Param("isActive") Boolean isActive,
                                       @Param("isExpired") Boolean isExpired,
                                       @Param("searchTerm") String searchTerm,
                                       Pageable pageable);

    /**
     * Find API keys created within date range.
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.createdAt BETWEEN :startDate AND :endDate")
    List<ApiKey> findApiKeysCreatedBetween(@Param("startDate") ZonedDateTime startDate,
                                          @Param("endDate") ZonedDateTime endDate);

    /**
     * Find API keys last used within date range.
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.lastUsedAt BETWEEN :startDate AND :endDate")
    List<ApiKey> findApiKeysUsedBetween(@Param("startDate") ZonedDateTime startDate,
                                       @Param("endDate") ZonedDateTime endDate);

    /**
     * Find unused API keys.
     */
    List<ApiKey> findByLastUsedAtIsNull();

    /**
     * Find API keys not used since specified date.
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.lastUsedAt IS NULL OR ak.lastUsedAt < :cutoffDate")
    List<ApiKey> findApiKeysNotUsedSince(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Find recently used API keys.
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.lastUsedAt >= :cutoffDate")
    List<ApiKey> findRecentlyUsedApiKeys(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Count API keys by user.
     */
    long countByUserId(UUID userId);

    /**
     * Count active API keys by user.
     */
    long countByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Count expired API keys.
     */
    @Query("SELECT COUNT(ak) FROM ApiKey ak WHERE ak.expiresAt IS NOT NULL AND ak.expiresAt < CURRENT_TIMESTAMP")
    long countExpiredApiKeys();

    /**
     * Count API keys created today.
     */
    @Query("SELECT COUNT(ak) FROM ApiKey ak WHERE " +
           "DATE_TRUNC('day', ak.createdAt) = DATE_TRUNC('day', CURRENT_TIMESTAMP)")
    long countApiKeysCreatedToday();

    /**
     * Get API key usage statistics.
     */
    @Query("SELECT " +
           "COUNT(*) as totalApiKeys, " +
           "COUNT(CASE WHEN ak.isActive = true THEN 1 END) as activeApiKeys, " +
           "COUNT(CASE WHEN ak.expiresAt IS NOT NULL AND ak.expiresAt < CURRENT_TIMESTAMP THEN 1 END) as expiredApiKeys, " +
           "COUNT(CASE WHEN ak.lastUsedAt IS NULL THEN 1 END) as unusedApiKeys, " +
           "COUNT(CASE WHEN ak.lastUsedAt >= :recentCutoff THEN 1 END) as recentlyUsedApiKeys, " +
           "COALESCE(AVG(ak.rateLimitPerHour), 0) as averageRateLimit " +
           "FROM ApiKey ak WHERE ak.createdAt >= :startDate")
    Object[] getApiKeyStatistics(@Param("startDate") ZonedDateTime startDate,
                                 @Param("recentCutoff") ZonedDateTime recentCutoff);

    /**
     * Find most used API keys.
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.lastUsedAt IS NOT NULL ORDER BY ak.lastUsedAt DESC")
    List<ApiKey> findMostRecentlyUsedApiKeys(Pageable pageable);

    /**
     * Find API keys by user with usage information.
     */
    @Query("SELECT ak, " +
           "CASE WHEN ak.lastUsedAt IS NULL THEN 'Never' " +
           "WHEN ak.lastUsedAt >= :recentCutoff THEN 'Recent' " +
           "ELSE 'Old' END as usageStatus " +
           "FROM ApiKey ak WHERE ak.user.id = :userId")
    List<Object[]> findApiKeysByUserWithUsageStatus(@Param("userId") UUID userId,
                                                   @Param("recentCutoff") ZonedDateTime recentCutoff);

    /**
     * Update last used timestamp.
     */
    @Query("UPDATE ApiKey ak SET ak.lastUsedAt = :lastUsedAt WHERE ak.id = :apiKeyId")
    void updateLastUsedAt(@Param("apiKeyId") UUID apiKeyId, @Param("lastUsedAt") ZonedDateTime lastUsedAt);

    /**
     * Deactivate API key.
     */
    @Query("UPDATE ApiKey ak SET ak.isActive = false WHERE ak.id = :apiKeyId")
    void deactivateApiKey(@Param("apiKeyId") UUID apiKeyId);

    /**
     * Activate API key.
     */
    @Query("UPDATE ApiKey ak SET ak.isActive = true WHERE ak.id = :apiKeyId")
    void activateApiKey(@Param("apiKeyId") UUID apiKeyId);

    /**
     * Update API key rate limit.
     */
    @Query("UPDATE ApiKey ak SET ak.rateLimitPerHour = :rateLimit WHERE ak.id = :apiKeyId")
    void updateRateLimit(@Param("apiKeyId") UUID apiKeyId, @Param("rateLimit") Integer rateLimit);

    /**
     * Update API key expiration.
     */
    @Query("UPDATE ApiKey ak SET ak.expiresAt = :expiresAt WHERE ak.id = :apiKeyId")
    void updateExpiration(@Param("apiKeyId") UUID apiKeyId, @Param("expiresAt") ZonedDateTime expiresAt);

    /**
     * Find API keys for cleanup (inactive and old).
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.isActive = false AND ak.createdAt < :cutoffDate")
    List<ApiKey> findApiKeysForCleanup(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Find top users by API key count.
     */
    @Query("SELECT ak.user.id, ak.user.username, COUNT(ak) as apiKeyCount " +
           "FROM ApiKey ak GROUP BY ak.user.id, ak.user.username ORDER BY apiKeyCount DESC")
    List<Object[]> findTopUsersByApiKeyCount(Pageable pageable);

    /**
     * Find permission usage statistics.
     */
    @Query(value = "SELECT permission, COUNT(*) as usageCount " +
           "FROM api_keys ak, unnest(ak.permissions) as permission " +
           "GROUP BY permission ORDER BY usageCount DESC", nativeQuery = true)
    List<Object[]> getPermissionUsageStatistics();

    /**
     * Find rate limit distribution.
     */
    @Query("SELECT ak.rateLimitPerHour, COUNT(ak) as count " +
           "FROM ApiKey ak GROUP BY ak.rateLimitPerHour ORDER BY ak.rateLimitPerHour")
    List<Object[]> getRateLimitDistribution();

    /**
     * Find API keys that should be rotated (old and active).
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.isActive = true AND ak.createdAt < :rotationCutoff")
    List<ApiKey> findApiKeysForRotation(@Param("rotationCutoff") ZonedDateTime rotationCutoff);
}
