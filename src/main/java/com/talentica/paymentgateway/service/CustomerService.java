package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing Customer entities and business logic.
 */
@Slf4j
@Service
@Transactional
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Create a new customer.
     */
    public Customer createCustomer(Customer customer) {
        log.info("Creating new customer with email: {}", customer.getEmail());
        return customerRepository.save(customer);
    }

    /**
     * Update an existing customer.
     */
    public Customer updateCustomer(Customer customer) {
        log.info("Updating customer with ID: {}", customer.getId());
        return customerRepository.save(customer);
    }

    /**
     * Find customer by ID.
     */
    public Customer findById(UUID id) {
        return customerRepository.findById(id).orElse(null);
    }

    /**
     * Find customer by customer ID (customer reference).
     */
    public Customer findByCustomerId(String customerId) {
        return customerRepository.findByCustomerId(customerId).orElse(null);
    }

    /**
     * Find customer by email.
     */
    public Customer findByEmail(String email) {
        return customerRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    /**
     * Find customer by Authorize.Net customer profile ID.
     */
    public Customer findByAuthorizeNetCustomerProfileId(String authorizeNetCustomerProfileId) {
        return customerRepository.findByAuthorizeNetCustomerProfileId(authorizeNetCustomerProfileId).orElse(null);
    }

    /**
     * Check if customer exists by email.
     */
    public boolean existsByEmail(String email) {
        return customerRepository.existsByEmailIgnoreCase(email);
    }

    /**
     * Check if customer exists by customer reference.
     */
    public boolean existsByCustomerId(String customerId) {
        return customerRepository.existsByCustomerReference(customerId);
    }

    /**
     * Get all active customers.
     */
    public List<Customer> findAllActiveCustomers() {
        return customerRepository.findByIsActiveTrue();
    }

    /**
     * Search customers with filters and pagination.
     */
    public Page<Customer> searchCustomers(String searchTerm, Boolean isActive, UUID userId, Pageable pageable) {
        return customerRepository.findCustomersWithFilters(searchTerm, isActive, userId, pageable);
    }

    /**
     * Delete customer by ID.
     */
    public void deleteCustomer(UUID customerId) {
        log.info("Deleting customer with ID: {}", customerId);
        customerRepository.deleteById(customerId);
    }

    /**
     * Activate or deactivate customer.
     */
    public void updateCustomerStatus(UUID customerId, boolean isActive) {
        log.info("Updating customer status - ID: {}, Active: {}", customerId, isActive);
        customerRepository.updateActiveStatus(customerId, isActive);
    }

    /**
     * Get customer count.
     */
    public long getCustomerCount() {
        return customerRepository.count();
    }

    /**
     * Get active customer count.
     */
    public long getActiveCustomerCount() {
        return customerRepository.countByIsActiveTrue();
    }

    /**
     * Find customers with payment methods.
     */
    public List<Customer> findCustomersWithPaymentMethods() {
        return customerRepository.findCustomersWithPaymentMethods();
    }

    /**
     * Find customers without payment methods.
     */
    public List<Customer> findCustomersWithoutPaymentMethods() {
        return customerRepository.findCustomersWithoutPaymentMethods();
    }

    /**
     * Find customers with active subscriptions.
     */
    public List<Customer> findCustomersWithActiveSubscriptions() {
        return customerRepository.findCustomersWithActiveSubscriptions();
    }
}
