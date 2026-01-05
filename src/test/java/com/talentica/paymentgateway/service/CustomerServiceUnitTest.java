package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceUnitTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setId(UUID.randomUUID());
        testCustomer.setCustomerReference("CUST_001");
        testCustomer.setEmail("test@example.com");
        testCustomer.setFirstName("Test");
        testCustomer.setLastName("Customer");
        testCustomer.setPhone("1234567890");
        testCustomer.setIsActive(true);
        testCustomer.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
        testCustomer.setUpdatedAt(ZonedDateTime.now().toLocalDateTime());
        testCustomer.setAuthorizeNetCustomerProfileId("12345");
    }

    @Test
    void createCustomer_WithValidCustomer_ShouldReturnSavedCustomer() {
        when(customerRepository.save(testCustomer)).thenReturn(testCustomer);

        Customer result = customerService.createCustomer(testCustomer);

        assertNotNull(result);
        assertEquals(testCustomer.getId(), result.getId());
        assertEquals(testCustomer.getEmail(), result.getEmail());
        verify(customerRepository).save(testCustomer);
    }

    @Test
    void createCustomer_WithNullCustomer_ShouldHandleGracefully() {
        assertThrows(NullPointerException.class, () -> {
            customerService.createCustomer(null);
        });

        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateCustomer_WithValidCustomer_ShouldReturnUpdatedCustomer() {
        testCustomer.setFirstName("Updated");
        when(customerRepository.save(testCustomer)).thenReturn(testCustomer);

        Customer result = customerService.updateCustomer(testCustomer);

        assertNotNull(result);
        assertEquals("Updated", result.getFirstName());
        verify(customerRepository).save(testCustomer);
    }

    @Test
    void findById_WithValidId_ShouldReturnCustomer() {
        UUID customerId = testCustomer.getId();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));

        Customer result = customerService.findById(customerId);

        assertNotNull(result);
        assertEquals(testCustomer.getId(), result.getId());
        verify(customerRepository).findById(customerId);
    }

    @Test
    void findById_WithNonExistentId_ShouldReturnNull() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        Customer result = customerService.findById(customerId);

        assertNull(result);
        verify(customerRepository).findById(customerId);
    }

    @Test
    void findByCustomerId_WithValidCustomerId_ShouldReturnCustomer() {
        String customerId = "CUST_001";
        when(customerRepository.findByCustomerId(customerId)).thenReturn(Optional.of(testCustomer));

        Customer result = customerService.findByCustomerId(customerId);

        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
        verify(customerRepository).findByCustomerId(customerId);
    }

    @Test
    void findByCustomerId_WithNonExistentCustomerId_ShouldReturnNull() {
        String customerId = "NONEXISTENT";
        when(customerRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        Customer result = customerService.findByCustomerId(customerId);

        assertNull(result);
        verify(customerRepository).findByCustomerId(customerId);
    }

    @Test
    void findByEmail_WithValidEmail_ShouldReturnCustomer() {
        String email = "test@example.com";
        when(customerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testCustomer));

        Customer result = customerService.findByEmail(email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        verify(customerRepository).findByEmailIgnoreCase(email);
    }

    @Test
    void findByEmail_WithNonExistentEmail_ShouldReturnNull() {
        String email = "nonexistent@example.com";
        when(customerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());

        Customer result = customerService.findByEmail(email);

        assertNull(result);
        verify(customerRepository).findByEmailIgnoreCase(email);
    }

    @Test
    void findByEmail_WithCaseInsensitiveEmail_ShouldReturnCustomer() {
        String email = "TEST@EXAMPLE.COM";
        when(customerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testCustomer));

        Customer result = customerService.findByEmail(email);

        assertNotNull(result);
        verify(customerRepository).findByEmailIgnoreCase(email);
    }

    @Test
    void findByAuthorizeNetCustomerProfileId_WithValidId_ShouldReturnCustomer() {
        String profileId = "12345";
        when(customerRepository.findByAuthorizeNetCustomerProfileId(profileId)).thenReturn(Optional.of(testCustomer));

        Customer result = customerService.findByAuthorizeNetCustomerProfileId(profileId);

        assertNotNull(result);
        assertEquals(profileId, result.getAuthorizeNetCustomerProfileId());
        verify(customerRepository).findByAuthorizeNetCustomerProfileId(profileId);
    }

    @Test
    void findByAuthorizeNetCustomerProfileId_WithNonExistentId_ShouldReturnNull() {
        String profileId = "NONEXISTENT";
        when(customerRepository.findByAuthorizeNetCustomerProfileId(profileId)).thenReturn(Optional.empty());

        Customer result = customerService.findByAuthorizeNetCustomerProfileId(profileId);

        assertNull(result);
        verify(customerRepository).findByAuthorizeNetCustomerProfileId(profileId);
    }

    @Test
    void existsByEmail_WithExistingEmail_ShouldReturnTrue() {
        String email = "test@example.com";
        when(customerRepository.existsByEmailIgnoreCase(email)).thenReturn(true);

        boolean result = customerService.existsByEmail(email);

        assertTrue(result);
        verify(customerRepository).existsByEmailIgnoreCase(email);
    }

    @Test
    void existsByEmail_WithNonExistentEmail_ShouldReturnFalse() {
        String email = "nonexistent@example.com";
        when(customerRepository.existsByEmailIgnoreCase(email)).thenReturn(false);

        boolean result = customerService.existsByEmail(email);

        assertFalse(result);
        verify(customerRepository).existsByEmailIgnoreCase(email);
    }

    @Test
    void existsByCustomerId_WithExistingCustomerId_ShouldReturnTrue() {
        String customerId = "CUST_001";
        when(customerRepository.existsByCustomerReference(customerId)).thenReturn(true);

        boolean result = customerService.existsByCustomerId(customerId);

        assertTrue(result);
        verify(customerRepository).existsByCustomerReference(customerId);
    }

    @Test
    void existsByCustomerId_WithNonExistentCustomerId_ShouldReturnFalse() {
        String customerId = "NONEXISTENT";
        when(customerRepository.existsByCustomerReference(customerId)).thenReturn(false);

        boolean result = customerService.existsByCustomerId(customerId);

        assertFalse(result);
        verify(customerRepository).existsByCustomerReference(customerId);
    }

    @Test
    void findAllActiveCustomers_ShouldReturnActiveCustomers() {
        List<Customer> activeCustomers = Arrays.asList(testCustomer);
        when(customerRepository.findByIsActiveTrue()).thenReturn(activeCustomers);

        List<Customer> result = customerService.findAllActiveCustomers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCustomer.getId(), result.get(0).getId());
        verify(customerRepository).findByIsActiveTrue();
    }

    @Test
    void findAllActiveCustomers_WithNoActiveCustomers_ShouldReturnEmptyList() {
        when(customerRepository.findByIsActiveTrue()).thenReturn(Arrays.asList());

        List<Customer> result = customerService.findAllActiveCustomers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(customerRepository).findByIsActiveTrue();
    }

    @Test
    void searchCustomers_WithValidParameters_ShouldReturnPagedResults() {
        String searchTerm = "test";
        Boolean isActive = true;
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        
        List<Customer> customers = Arrays.asList(testCustomer);
        Page<Customer> customerPage = new PageImpl<>(customers, pageable, 1);
        
        when(customerRepository.findCustomersWithFilters(searchTerm, isActive, userId, pageable))
                .thenReturn(customerPage);

        Page<Customer> result = customerService.searchCustomers(searchTerm, isActive, userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testCustomer.getId(), result.getContent().get(0).getId());
        verify(customerRepository).findCustomersWithFilters(searchTerm, isActive, userId, pageable);
    }

    @Test
    void searchCustomers_WithNullSearchTerm_ShouldHandleGracefully() {
        Boolean isActive = true;
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        
        List<Customer> customers = Arrays.asList(testCustomer);
        Page<Customer> customerPage = new PageImpl<>(customers, pageable, 1);
        
        when(customerRepository.findCustomersWithFilters(null, isActive, userId, pageable))
                .thenReturn(customerPage);

        Page<Customer> result = customerService.searchCustomers(null, isActive, userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(customerRepository).findCustomersWithFilters(null, isActive, userId, pageable);
    }

    @Test
    void searchCustomers_WithNullFilters_ShouldHandleGracefully() {
        Pageable pageable = PageRequest.of(0, 10);
        
        List<Customer> customers = Arrays.asList(testCustomer);
        Page<Customer> customerPage = new PageImpl<>(customers, pageable, 1);
        
        when(customerRepository.findCustomersWithFilters(null, null, null, pageable))
                .thenReturn(customerPage);

        Page<Customer> result = customerService.searchCustomers(null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(customerRepository).findCustomersWithFilters(null, null, null, pageable);
    }

    @Test
    void deleteCustomer_WithValidId_ShouldDeleteCustomer() {
        UUID customerId = testCustomer.getId();

        customerService.deleteCustomer(customerId);

        verify(customerRepository).deleteById(customerId);
    }

    @Test
    void deleteCustomer_WithNullId_ShouldHandleGracefully() {
        doThrow(new IllegalArgumentException("ID cannot be null")).when(customerRepository).deleteById(any());

        assertThrows(IllegalArgumentException.class, () -> {
            customerService.deleteCustomer(null);
        });

        verify(customerRepository).deleteById(any());
    }

    @Test
    void updateCustomerStatus_WithValidParameters_ShouldUpdateStatus() {
        UUID customerId = testCustomer.getId();
        boolean isActive = false;

        customerService.updateCustomerStatus(customerId, isActive);

        verify(customerRepository).updateActiveStatus(customerId, isActive);
    }

    @Test
    void updateCustomerStatus_WithNullId_ShouldHandleGracefully() {
        boolean isActive = false;
        doThrow(new IllegalArgumentException("ID cannot be null")).when(customerRepository).updateActiveStatus(null, isActive);

        assertThrows(IllegalArgumentException.class, () -> {
            customerService.updateCustomerStatus(null, isActive);
        });

        verify(customerRepository).updateActiveStatus(null, isActive);
    }

    @Test
    void getCustomerCount_ShouldReturnCount() {
        long expectedCount = 5L;
        when(customerRepository.count()).thenReturn(expectedCount);

        long result = customerService.getCustomerCount();

        assertEquals(expectedCount, result);
        verify(customerRepository).count();
    }

    @Test
    void getActiveCustomerCount_ShouldReturnActiveCount() {
        long expectedCount = 3L;
        when(customerRepository.countByIsActiveTrue()).thenReturn(expectedCount);

        long result = customerService.getActiveCustomerCount();

        assertEquals(expectedCount, result);
        verify(customerRepository).countByIsActiveTrue();
    }

    @Test
    void findCustomersWithPaymentMethods_ShouldReturnCustomersWithPaymentMethods() {
        List<Customer> customersWithPaymentMethods = Arrays.asList(testCustomer);
        when(customerRepository.findCustomersWithPaymentMethods()).thenReturn(customersWithPaymentMethods);

        List<Customer> result = customerService.findCustomersWithPaymentMethods();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCustomer.getId(), result.get(0).getId());
        verify(customerRepository).findCustomersWithPaymentMethods();
    }

    @Test
    void findCustomersWithPaymentMethods_WithNoResults_ShouldReturnEmptyList() {
        when(customerRepository.findCustomersWithPaymentMethods()).thenReturn(Arrays.asList());

        List<Customer> result = customerService.findCustomersWithPaymentMethods();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(customerRepository).findCustomersWithPaymentMethods();
    }

    @Test
    void findCustomersWithoutPaymentMethods_ShouldReturnCustomersWithoutPaymentMethods() {
        List<Customer> customersWithoutPaymentMethods = Arrays.asList(testCustomer);
        when(customerRepository.findCustomersWithoutPaymentMethods()).thenReturn(customersWithoutPaymentMethods);

        List<Customer> result = customerService.findCustomersWithoutPaymentMethods();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCustomer.getId(), result.get(0).getId());
        verify(customerRepository).findCustomersWithoutPaymentMethods();
    }

    @Test
    void findCustomersWithoutPaymentMethods_WithNoResults_ShouldReturnEmptyList() {
        when(customerRepository.findCustomersWithoutPaymentMethods()).thenReturn(Arrays.asList());

        List<Customer> result = customerService.findCustomersWithoutPaymentMethods();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(customerRepository).findCustomersWithoutPaymentMethods();
    }

    @Test
    void findCustomersWithActiveSubscriptions_ShouldReturnCustomersWithActiveSubscriptions() {
        List<Customer> customersWithActiveSubscriptions = Arrays.asList(testCustomer);
        when(customerRepository.findCustomersWithActiveSubscriptions()).thenReturn(customersWithActiveSubscriptions);

        List<Customer> result = customerService.findCustomersWithActiveSubscriptions();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCustomer.getId(), result.get(0).getId());
        verify(customerRepository).findCustomersWithActiveSubscriptions();
    }

    @Test
    void findCustomersWithActiveSubscriptions_WithNoResults_ShouldReturnEmptyList() {
        when(customerRepository.findCustomersWithActiveSubscriptions()).thenReturn(Arrays.asList());

        List<Customer> result = customerService.findCustomersWithActiveSubscriptions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(customerRepository).findCustomersWithActiveSubscriptions();
    }

    @Test
    void createCustomer_WithDuplicateEmail_ShouldHandleRepositoryConstraint() {
        when(customerRepository.save(testCustomer)).thenThrow(new RuntimeException("Duplicate email"));

        assertThrows(RuntimeException.class, () -> {
            customerService.createCustomer(testCustomer);
        });

        verify(customerRepository).save(testCustomer);
    }

    @Test
    void updateCustomer_WithInvalidData_ShouldHandleRepositoryException() {
        testCustomer.setEmail(null);
        when(customerRepository.save(testCustomer)).thenThrow(new RuntimeException("Invalid data"));

        assertThrows(RuntimeException.class, () -> {
            customerService.updateCustomer(testCustomer);
        });

        verify(customerRepository).save(testCustomer);
    }

    @Test
    void searchCustomers_WithEmptyResults_ShouldReturnEmptyPage() {
        String searchTerm = "nonexistent";
        Boolean isActive = true;
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        
        Page<Customer> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);
        
        when(customerRepository.findCustomersWithFilters(searchTerm, isActive, userId, pageable))
                .thenReturn(emptyPage);

        Page<Customer> result = customerService.searchCustomers(searchTerm, isActive, userId, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(customerRepository).findCustomersWithFilters(searchTerm, isActive, userId, pageable);
    }
}
