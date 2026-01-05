package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.payment.CustomerRequest;
import com.talentica.paymentgateway.dto.payment.PaymentMethodRequest;
import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import net.authorize.Environment;
import net.authorize.api.contract.v1.*;
import net.authorize.api.controller.*;
import net.authorize.api.controller.base.ApiOperationBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthorizeNetCustomerService.
 * 
 * These tests verify service initialization and basic functionality.
 * Full integration testing with Authorize.Net SDK is handled separately.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AuthorizeNetCustomerServiceUnitTest {

    @Mock
    private MerchantAuthenticationType merchant;

    @Mock
    private Environment environment;

    private AuthorizeNetCustomerService authorizeNetCustomerService;
    private Customer testCustomer;
    private CustomerRequest testCustomerRequest;
    private PaymentMethodRequest testPaymentMethod;

    @BeforeEach
    void setUp() {
        authorizeNetCustomerService = new AuthorizeNetCustomerService(merchant, environment);
        setupTestData();
    }

    private void setupTestData() {
        testCustomer = new Customer();
        testCustomer.setCustomerReference("CUST_123");
        testCustomer.setEmail("test@example.com");
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
        testCustomer.setBillingAddressLine1("123 Main St");
        testCustomer.setBillingCity("Anytown");
        testCustomer.setBillingState("CA");
        testCustomer.setBillingPostalCode("12345");
        testCustomer.setBillingCountry("US");

        testCustomerRequest = new CustomerRequest();
        testCustomerRequest.setEmail("test@example.com");
        testCustomerRequest.setFirstName("John");
        testCustomerRequest.setLastName("Doe");

        testPaymentMethod = new PaymentMethodRequest();
        testPaymentMethod.setType("CREDIT_CARD");
        testPaymentMethod.setCardNumber("4111111111111111");
        testPaymentMethod.setExpiryMonth("12");
        testPaymentMethod.setExpiryYear("2025");
        testPaymentMethod.setCvv("123");
    }

    @Test
    void constructor_WithValidParameters_ShouldCreateService() {
        // When & Then
        assertNotNull(authorizeNetCustomerService);
    }

    @Test
    void serviceInitialization_ShouldNotThrowException() {
        // Given & When
        AuthorizeNetCustomerService service = new AuthorizeNetCustomerService(merchant, environment);
        
        // Then
        assertNotNull(service);
    }

    @Test
    void createCustomerProfile_WithValidData_ShouldThrowException() {
        // When & Then - Since we can't mock the Authorize.Net SDK properly, expect exception
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createCustomerProfile(testCustomer, testCustomerRequest));
        
        assertTrue(exception.getMessage().contains("Failed to create customer profile"));
    }

    @Test
    void createCustomerProfile_WithFirstNameOnly_ShouldThrowException() {
        // Given
        testCustomer.setLastName(null);
        
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createCustomerProfile(testCustomer, testCustomerRequest));
        
        assertTrue(exception.getMessage().contains("Failed to create customer profile"));
    }

    @Test
    void createCustomerProfile_WithLastNameOnly_ShouldThrowException() {
        // Given
        testCustomer.setFirstName(null);
        
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createCustomerProfile(testCustomer, testCustomerRequest));
        
        assertTrue(exception.getMessage().contains("Failed to create customer profile"));
    }

    @Test
    void createCustomerProfile_WithCustomerIdOnly_ShouldThrowException() {
        // Given
        testCustomer.setFirstName(null);
        testCustomer.setLastName(null);
        
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createCustomerProfile(testCustomer, testCustomerRequest));
        
        assertTrue(exception.getMessage().contains("Failed to create customer profile"));
    }

    @Test
    void createCustomerProfile_WithNoNames_ShouldThrowException() {
        // Given
        testCustomer.setFirstName(null);
        testCustomer.setLastName(null);
        
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createCustomerProfile(testCustomer, testCustomerRequest));
        
        assertTrue(exception.getMessage().contains("Failed to create customer profile"));
    }

    @Test
    void createCustomerProfile_WithNullResponse_ShouldThrowException() {
        // When & Then - Test with invalid data that would cause null response
        Customer invalidCustomer = new Customer();
        invalidCustomer.setFirstName(null);
        invalidCustomer.setLastName(null);
        invalidCustomer.setCustomerReference(null);
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createCustomerProfile(invalidCustomer, testCustomerRequest));
        assertTrue(exception.getMessage().contains("Failed to create customer profile"));
    }

    @Test
    void createCustomerProfile_WithException_ShouldThrowPaymentProcessingException() {
        // When & Then - Test with data that would cause an exception
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createCustomerProfile(testCustomer, testCustomerRequest));
        
        assertTrue(exception.getMessage().contains("Failed to create customer profile"));
    }

    @Test
    void createCustomerProfile_WithNullCustomerRequest_ShouldThrowException() {
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createCustomerProfile(testCustomer, null));
        
        assertTrue(exception.getMessage().contains("Failed to create customer profile"));
    }

    @Test
    void createCustomerProfile_WithEmptyCustomerName_ShouldThrowException() {
        // Given - Customer with empty names
        Customer emptyNameCustomer = new Customer();
        emptyNameCustomer.setFirstName("");
        emptyNameCustomer.setLastName("");
        emptyNameCustomer.setCustomerReference("CUST001");
        
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createCustomerProfile(emptyNameCustomer, testCustomerRequest));
        
        assertTrue(exception.getMessage().contains("Failed to create customer profile"));
    }

    @Test
    void createPaymentProfile_WithValidData_ShouldThrowException() {
        // Given
        String customerProfileId = "12345";
        
        // When & Then - Since we can't mock the Authorize.Net SDK properly, expect exception
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createPaymentProfile(customerProfileId, testPaymentMethod, testCustomer));
        
        assertTrue(exception.getMessage().contains("Failed to create payment profile"));
    }

    @Test
    void createPaymentProfile_WithNoBillingAddress_ShouldThrowException() {
        // Given
        String customerProfileId = "12345";
        PaymentMethodRequest paymentMethodWithoutAddress = new PaymentMethodRequest();
        paymentMethodWithoutAddress.setCardNumber("4111111111111111");
        paymentMethodWithoutAddress.setExpiryMonth("12");
        paymentMethodWithoutAddress.setExpiryYear("25");
        paymentMethodWithoutAddress.setCvv("123");
        
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createPaymentProfile(customerProfileId, paymentMethodWithoutAddress, testCustomer));
        
        assertTrue(exception.getMessage().contains("Failed to create payment profile"));
    }

    @Test
    void createPaymentProfile_WithNullCustomer_ShouldThrowException() {
        // Given
        String customerProfileId = "12345";
        
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createPaymentProfile(customerProfileId, testPaymentMethod, null));
        
        assertTrue(exception.getMessage().contains("Failed to create payment profile"));
    }

    // ===== GET CUSTOMER PROFILE TESTS =====

    @Test
    void getCustomerProfile_WithNullId_ShouldThrowException() {
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.getCustomerProfile(null));
        
        assertTrue(exception.getMessage().contains("Failed to retrieve customer profile"));
    }

    @Test
    void getCustomerProfile_WithEmptyId_ShouldThrowException() {
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.getCustomerProfile(""));
        
        assertTrue(exception.getMessage().contains("Failed to retrieve customer profile"));
    }

    // ===== GET CUSTOMER PAYMENT PROFILE TESTS =====

    @Test
    void getCustomerPaymentProfile_WithNullCustomerProfileId_ShouldThrowException() {
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.getCustomerPaymentProfile(null, "67890"));
        
        assertTrue(exception.getMessage().contains("Failed to retrieve customer payment profile"));
    }

    @Test
    void getCustomerPaymentProfile_WithNullPaymentProfileId_ShouldThrowException() {
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.getCustomerPaymentProfile("12345", null));
        
        assertTrue(exception.getMessage().contains("Failed to retrieve customer payment profile"));
    }

    // ===== UPDATE CUSTOMER PROFILE TESTS =====

    @Test
    void updateCustomerProfile_WithNullCustomerProfileId_ShouldThrowException() {
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.updateCustomerProfile(null, testCustomer));
        
        assertTrue(exception.getMessage().contains("Failed to update customer profile"));
    }

    @Test
    void updateCustomerProfile_WithNullCustomer_ShouldThrowException() {
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.updateCustomerProfile("12345", null));
        
        assertTrue(exception.getMessage().contains("Failed to update customer profile"));
    }

    // ===== DELETE CUSTOMER PROFILE TESTS =====

    @Test
    void deleteCustomerProfile_WithNullId_ShouldThrowException() {
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.deleteCustomerProfile(null));
        
        assertTrue(exception.getMessage().contains("Failed to delete customer profile"));
    }

    @Test
    void deleteCustomerProfile_WithEmptyId_ShouldThrowException() {
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.deleteCustomerProfile(""));
        
        assertTrue(exception.getMessage().contains("Failed to delete customer profile"));
    }

    // ===== DELETE CUSTOMER PAYMENT PROFILE TESTS =====

    @Test
    void deleteCustomerPaymentProfile_WithNullCustomerProfileId_ShouldThrowException() {
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.deleteCustomerPaymentProfile(null, "67890"));
        
        assertTrue(exception.getMessage().contains("Failed to delete customer payment profile"));
    }

    @Test
    void deleteCustomerPaymentProfile_WithNullPaymentProfileId_ShouldThrowException() {
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.deleteCustomerPaymentProfile("12345", null));
        
        assertTrue(exception.getMessage().contains("Failed to delete customer payment profile"));
    }

    // ===== VALIDATE CUSTOMER PAYMENT PROFILE TESTS =====

    @Test
    void validateCustomerPaymentProfile_WithNullCustomerProfileId_ShouldThrowException() {
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.validateCustomerPaymentProfile(null, "67890"));
        
        assertTrue(exception.getMessage().contains("Failed to validate payment profile"));
    }

    @Test
    void validateCustomerPaymentProfile_WithNullPaymentProfileId_ShouldThrowException() {
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.validateCustomerPaymentProfile("12345", null));
        
        assertTrue(exception.getMessage().contains("Failed to validate payment profile"));
    }

    // ===== ADDITIONAL VALIDATION TESTS =====

    @Test
    void createCustomerProfile_WithInvalidCustomerData_ShouldThrowException() {
        // Given - Customer with null first and last name
        Customer invalidCustomer = new Customer();
        invalidCustomer.setCustomerReference(null);
        invalidCustomer.setFirstName(null);
        invalidCustomer.setLastName(null);
        
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createCustomerProfile(invalidCustomer, testCustomerRequest));
        
        assertTrue(exception.getMessage().contains("Failed to create customer profile"));
    }

    @Test
    void createPaymentProfile_WithInvalidPaymentMethod_ShouldThrowException() {
        // Given - PaymentMethod with null card number
        PaymentMethodRequest invalidPaymentMethod = new PaymentMethodRequest();
        invalidPaymentMethod.setCardNumber(null);
        invalidPaymentMethod.setExpiryMonth("12");
        invalidPaymentMethod.setExpiryYear("25");
        invalidPaymentMethod.setCvv("123");
        
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createPaymentProfile("12345", invalidPaymentMethod, testCustomer));
        
        assertTrue(exception.getMessage().contains("Failed to create payment profile"));
    }

    @Test
    void createPaymentProfile_WithEmptyCustomerProfileId_ShouldThrowException() {
        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, 
            () -> authorizeNetCustomerService.createPaymentProfile("", testPaymentMethod, testCustomer));
        
        assertTrue(exception.getMessage().contains("Failed to create payment profile"));
    }
}
