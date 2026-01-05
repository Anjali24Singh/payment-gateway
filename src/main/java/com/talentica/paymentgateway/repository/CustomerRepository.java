package com.talentica.paymentgateway.repository;

import com.talentica.paymentgateway.entity.Customer;
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
 * Repository interface for Customer entity.
 * Provides data access methods for customer management and queries.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Find customer by email (case-insensitive).
     */
    Optional<Customer> findByEmailIgnoreCase(String email);

    /**
     * Find customer by customer reference.
     */
    Optional<Customer> findByCustomerReference(String customerReference);

    /**
     * Find customer by customer ID (alias for customer reference).
     */
    default Optional<Customer> findByCustomerId(String customerId) {
        return findByCustomerReference(customerId);
    }

    /**
     * Check if customer exists by email.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Check if customer reference exists.
     */
    boolean existsByCustomerReference(String customerReference);

    /**
     * Find all active customers.
     */
    List<Customer> findByIsActiveTrue();

    /**
     * Find customers by user.
     */
    List<Customer> findByUser(User user);

    /**
     * Find customers by user ID.
     */
    List<Customer> findByUserId(UUID userId);

    /**
     * Find customers by partial name or email match.
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.company) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.customerReference) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Customer> findBySearchTerm(@Param("searchTerm") String searchTerm);

    /**
     * Find customers with filters and pagination.
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.company) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.customerReference) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:isActive IS NULL OR c.isActive = :isActive) AND " +
           "(:userId IS NULL OR c.user.id = :userId)")
    Page<Customer> findCustomersWithFilters(@Param("searchTerm") String searchTerm,
                                          @Param("isActive") Boolean isActive,
                                          @Param("userId") UUID userId,
                                          Pageable pageable);

    /**
     * Find customers by billing country.
     */
    List<Customer> findByBillingCountry(String country);

    /**
     * Find customers by shipping country.
     */
    List<Customer> findByShippingCountry(String country);

    /**
     * Find customers with complete billing address.
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "c.billingAddressLine1 IS NOT NULL AND " +
           "c.billingCity IS NOT NULL AND " +
           "c.billingState IS NOT NULL AND " +
           "c.billingPostalCode IS NOT NULL")
    List<Customer> findCustomersWithCompleteBillingAddress();

    /**
     * Find customers with incomplete address information.
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "c.billingAddressLine1 IS NULL OR " +
           "c.billingCity IS NULL OR " +
           "c.billingState IS NULL OR " +
           "c.billingPostalCode IS NULL")
    List<Customer> findCustomersWithIncompleteBillingAddress();

    /**
     * Find customers created within a date range.
     */
    @Query("SELECT c FROM Customer c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    List<Customer> findCustomersCreatedBetween(@Param("startDate") ZonedDateTime startDate,
                                             @Param("endDate") ZonedDateTime endDate);

    /**
     * Count active customers.
     */
    long countByIsActiveTrue();

    /**
     * Count customers created today.
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE " +
           "DATE_TRUNC('day', c.createdAt) = DATE_TRUNC('day', CURRENT_TIMESTAMP)")
    long countCustomersCreatedToday();

    /**
     * Count customers by billing country.
     */
    long countByBillingCountry(String country);

    /**
     * Find customers with payment methods.
     */
    @Query("SELECT c FROM Customer c WHERE SIZE(c.paymentMethods) > 0")
    List<Customer> findCustomersWithPaymentMethods();

    /**
     * Find customers without payment methods.
     */
    @Query("SELECT c FROM Customer c WHERE SIZE(c.paymentMethods) = 0")
    List<Customer> findCustomersWithoutPaymentMethods();

    /**
     * Find customers with active subscriptions.
     */
    @Query("SELECT DISTINCT c FROM Customer c JOIN c.subscriptions s WHERE s.status = 'ACTIVE'")
    List<Customer> findCustomersWithActiveSubscriptions();

    /**
     * Find customers with orders.
     */
    @Query("SELECT c FROM Customer c WHERE SIZE(c.orders) > 0")
    List<Customer> findCustomersWithOrders();

    /**
     * Find top customers by order count.
     */
    @Query("SELECT c FROM Customer c LEFT JOIN c.orders o " +
           "GROUP BY c.id ORDER BY COUNT(o) DESC")
    List<Customer> findTopCustomersByOrderCount(Pageable pageable);

    /**
     * Find top customers by transaction volume.
     */
    @Query("SELECT c FROM Customer c LEFT JOIN c.transactions t " +
           "WHERE t.status IN ('CAPTURED', 'SETTLED') " +
           "GROUP BY c.id ORDER BY COALESCE(SUM(t.amount), 0) DESC")
    List<Customer> findTopCustomersByTransactionVolume(Pageable pageable);

    /**
     * Find customers by phone number.
     */
    List<Customer> findByPhoneContaining(String phone);

    /**
     * Find customers by company name.
     */
    List<Customer> findByCompanyContainingIgnoreCase(String company);

    /**
     * Find customers in a specific city.
     */
    List<Customer> findByBillingCityIgnoreCase(String city);

    /**
     * Find customers in a specific state.
     */
    List<Customer> findByBillingStateIgnoreCase(String state);

    /**
     * Find customers by postal code.
     */
    List<Customer> findByBillingPostalCode(String postalCode);

    /**
     * Update customer active status.
     */
    @Query("UPDATE Customer c SET c.isActive = :isActive WHERE c.id = :customerId")
    void updateActiveStatus(@Param("customerId") UUID customerId, @Param("isActive") Boolean isActive);

    /**
     * Find customers with transactions in date range.
     */
    @Query("SELECT DISTINCT c FROM Customer c JOIN c.transactions t " +
           "WHERE t.createdAt BETWEEN :startDate AND :endDate")
    List<Customer> findCustomersWithTransactionsBetween(@Param("startDate") ZonedDateTime startDate,
                                                       @Param("endDate") ZonedDateTime endDate);

    /**
     * Find customer by Authorize.Net customer profile ID.
     */
    Optional<Customer> findByAuthorizeNetCustomerProfileId(String authorizeNetCustomerProfileId);
}
