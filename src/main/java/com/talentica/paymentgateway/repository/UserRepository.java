package com.talentica.paymentgateway.repository;

import com.talentica.paymentgateway.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity.
 * Provides data access methods for user management and authentication.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by username (case-insensitive).
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Find user by email (case-insensitive).
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Check if username exists (case-insensitive).
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Check if email exists (case-insensitive).
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find all active users.
     */
    List<User> findByIsActiveTrue();

    /**
     * Find all verified users.
     */
    List<User> findByIsVerifiedTrue();

    /**
     * Find active and verified users.
     */
    List<User> findByIsActiveTrueAndIsVerifiedTrue();

    /**
     * Find users by partial name match (first name or last name).
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> findByNameContaining(@Param("name") String name);

    /**
     * Find users who haven't logged in since the specified date.
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt IS NULL OR u.lastLoginAt < :date")
    List<User> findUsersNotLoggedInSince(@Param("date") ZonedDateTime date);

    /**
     * Find users created within a date range.
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findUsersCreatedBetween(@Param("startDate") ZonedDateTime startDate, 
                                      @Param("endDate") ZonedDateTime endDate);

    /**
     * Find users with paginated results and search functionality.
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:isActive IS NULL OR u.isActive = :isActive) AND " +
           "(:isVerified IS NULL OR u.isVerified = :isVerified)")
    Page<User> findUsersWithFilters(@Param("searchTerm") String searchTerm,
                                   @Param("isActive") Boolean isActive,
                                   @Param("isVerified") Boolean isVerified,
                                   Pageable pageable);

    /**
     * Count active users.
     */
    long countByIsActiveTrue();

    /**
     * Count verified users.
     */
    long countByIsVerifiedTrue();

    /**
     * Count users created today.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE " +
           "DATE_TRUNC('day', u.createdAt) = DATE_TRUNC('day', CURRENT_TIMESTAMP)")
    long countUsersCreatedToday();

    /**
     * Count users who logged in within the last N days.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginAt >= :date")
    long countUsersLoggedInSince(@Param("date") ZonedDateTime date);

    /**
     * Update last login timestamp for a user.
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") UUID userId, @Param("loginTime") ZonedDateTime loginTime);

    /**
     * Find users with expired accounts (not logged in for more than specified days).
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
           "(u.lastLoginAt IS NULL OR u.lastLoginAt < :cutoffDate)")
    List<User> findExpiredUsers(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Find top users by number of API keys.
     */
    @Query("SELECT u FROM User u LEFT JOIN u.apiKeys ak " +
           "GROUP BY u.id ORDER BY COUNT(ak) DESC")
    List<User> findTopUsersByApiKeyCount(Pageable pageable);

    /**
     * Find users with multiple customers.
     */
    @Query("SELECT u FROM User u WHERE SIZE(u.customers) > 1")
    List<User> findUsersWithMultipleCustomers();

    /**
     * Soft delete user (mark as inactive).
     */
    @Query("UPDATE User u SET u.isActive = false WHERE u.id = :userId")
    void softDeleteUser(@Param("userId") UUID userId);

    /**
     * Activate user account.
     */
    @Query("UPDATE User u SET u.isActive = true WHERE u.id = :userId")
    void activateUser(@Param("userId") UUID userId);

    /**
     * Verify user account.
     */
    @Query("UPDATE User u SET u.isVerified = true WHERE u.id = :userId")
    void verifyUser(@Param("userId") UUID userId);
}
