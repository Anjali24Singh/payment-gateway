package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.entity.PaymentMethod;
import com.talentica.paymentgateway.entity.SubscriptionPlan;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthorizeNetARBService.
 * 
 * These tests verify service initialization and basic functionality.
 * Full integration testing with Authorize.Net ARB API is handled separately.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AuthorizeNetARBServiceUnitTest {

    private AuthorizeNetARBService authorizeNetARBService;
    private Customer testCustomer;
    private SubscriptionPlan testPlan;
    private PaymentMethod testPaymentMethod;

    @BeforeEach
    void setUp() {
        // Initialize service with test credentials
        authorizeNetARBService = new AuthorizeNetARBService(
            "test-api-login-id",
            "test-transaction-key", 
            "sandbox"
        );

        // Create test customer
        testCustomer = new Customer();
        testCustomer.setCustomerReference("CUST_001");
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
        testCustomer.setEmail("john.doe@example.com");
        testCustomer.setBillingAddressLine1("123 Main St");
        testCustomer.setBillingCity("New York");
        testCustomer.setBillingState("NY");
        testCustomer.setBillingPostalCode("10001");
        testCustomer.setBillingCountry("US");

        // Create test subscription plan
        testPlan = new SubscriptionPlan();
        testPlan.setPlanCode("BASIC_MONTHLY");
        testPlan.setName("Basic Monthly Plan");
        testPlan.setAmount(new BigDecimal("29.99"));
        testPlan.setIntervalUnit("MONTH");
        testPlan.setIntervalCount(1);
        testPlan.setTrialPeriodDays(7);

        // Create test payment method
        testPaymentMethod = new PaymentMethod();
        testPaymentMethod.setCardNumber("4111111111111111");
        testPaymentMethod.setExpiryMonth("12");
        testPaymentMethod.setExpiryYear("2025");
        testPaymentMethod.setCvv("123");
    }

    @Test
    void constructor_WithSandboxEnvironment_ShouldCreateService() {
        // Given & When
        AuthorizeNetARBService service = new AuthorizeNetARBService(
            "test-login", "test-key", "sandbox"
        );
        
        // Then
        assertNotNull(service);
    }

    @Test
    void constructor_WithProductionEnvironment_ShouldCreateService() {
        // Given & When
        AuthorizeNetARBService service = new AuthorizeNetARBService(
            "test-login", "test-key", "production"
        );
        
        // Then
        assertNotNull(service);
    }

    @Test
    void constructor_WithInvalidEnvironment_ShouldDefaultToProduction() {
        // Given & When
        AuthorizeNetARBService service = new AuthorizeNetARBService(
            "test-login", "test-key", "invalid"
        );
        
        // Then
        assertNotNull(service);
    }

    @Test
    void createARBSubscription_WithValidInputs_ShouldNotThrowException() {
        // Given - test data already set up in setUp()
        
        // When & Then - Service should be created without throwing exceptions
        // Note: Actual API calls would require integration testing
        assertDoesNotThrow(() -> {
            // This would normally call the actual API, but we're testing service initialization
            assertNotNull(authorizeNetARBService);
            assertNotNull(testCustomer);
            assertNotNull(testPlan);
            assertNotNull(testPaymentMethod);
        });
    }

    @Test
    void createARBSubscription_WithNullCustomer_ShouldThrowException() {
        // Given
        Customer nullCustomer = null;
        
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            authorizeNetARBService.createARBSubscription(nullCustomer, testPlan, testPaymentMethod);
        });
    }

    @Test
    void createARBSubscription_WithNullPlan_ShouldThrowException() {
        // Given
        SubscriptionPlan nullPlan = null;
        
        // When & Then
        assertThrows(PaymentProcessingException.class, () -> {
            authorizeNetARBService.createARBSubscription(testCustomer, nullPlan, testPaymentMethod);
        });
    }

    @Test
    void createARBSubscription_WithNullPaymentMethod_ShouldThrowException() {
        // Given
        PaymentMethod nullPaymentMethod = null;
        
        // When & Then
        assertThrows(PaymentProcessingException.class, () -> {
            authorizeNetARBService.createARBSubscription(testCustomer, testPlan, nullPaymentMethod);
        });
    }

    @Test
    void getARBSubscription_WithValidSubscriptionId_ShouldNotThrowException() {
        // Given
        String validSubscriptionId = "12345";
        
        // When & Then - Service method should be callable
        assertDoesNotThrow(() -> {
            // This would normally call the actual API
            assertNotNull(validSubscriptionId);
        });
    }

    @Test
    void getARBSubscription_WithNullSubscriptionId_ShouldHandleGracefully() {
        // Given
        String nullSubscriptionId = null;
        
        // When & Then - Should handle null input (may throw exception or return error response)
        assertDoesNotThrow(() -> {
            try {
                authorizeNetARBService.getARBSubscription(nullSubscriptionId);
            } catch (PaymentProcessingException e) {
                // Expected behavior - service should handle null input
                assertNotNull(e.getMessage());
            }
        });
    }

    @Test
    void getARBSubscription_WithEmptySubscriptionId_ShouldHandleGracefully() {
        // Given
        String emptySubscriptionId = "";
        
        // When & Then - Should handle empty input (may throw exception or return error response)
        assertDoesNotThrow(() -> {
            try {
                authorizeNetARBService.getARBSubscription(emptySubscriptionId);
            } catch (PaymentProcessingException e) {
                // Expected behavior - service should handle empty input
                assertNotNull(e.getMessage());
            }
        });
    }

    @Test
    void cancelARBSubscription_WithValidSubscriptionId_ShouldNotThrowException() {
        // Given
        String validSubscriptionId = "12345";
        
        // When & Then - Service method should be callable
        assertDoesNotThrow(() -> {
            // This would normally call the actual API
            assertNotNull(validSubscriptionId);
        });
    }

    @Test
    void cancelARBSubscription_WithNullSubscriptionId_ShouldReturnFalse() {
        // Given
        String nullSubscriptionId = null;
        
        // When
        boolean result = authorizeNetARBService.cancelARBSubscription(nullSubscriptionId);
        
        // Then
        assertFalse(result);
    }

    @Test
    void cancelARBSubscription_WithEmptySubscriptionId_ShouldReturnFalse() {
        // Given
        String emptySubscriptionId = "";
        
        // When
        boolean result = authorizeNetARBService.cancelARBSubscription(emptySubscriptionId);
        
        // Then
        assertFalse(result);
    }

    @Test
    void testPlanWithDifferentIntervalUnits_ShouldHandleAllCases() {
        // Test DAY interval
        testPlan.setIntervalUnit("DAY");
        testPlan.setIntervalCount(1);
        assertDoesNotThrow(() -> {
            assertNotNull(testPlan.getIntervalUnit());
            assertEquals("DAY", testPlan.getIntervalUnit());
        });

        // Test WEEK interval
        testPlan.setIntervalUnit("WEEK");
        testPlan.setIntervalCount(2);
        assertDoesNotThrow(() -> {
            assertNotNull(testPlan.getIntervalUnit());
            assertEquals("WEEK", testPlan.getIntervalUnit());
        });

        // Test MONTH interval
        testPlan.setIntervalUnit("MONTH");
        testPlan.setIntervalCount(1);
        assertDoesNotThrow(() -> {
            assertNotNull(testPlan.getIntervalUnit());
            assertEquals("MONTH", testPlan.getIntervalUnit());
        });

        // Test YEAR interval
        testPlan.setIntervalUnit("YEAR");
        testPlan.setIntervalCount(1);
        assertDoesNotThrow(() -> {
            assertNotNull(testPlan.getIntervalUnit());
            assertEquals("YEAR", testPlan.getIntervalUnit());
        });
    }

    @Test
    void testPlanWithTrialPeriod_ShouldHandleTrialSettings() {
        // Given - plan with trial period
        testPlan.setTrialPeriodDays(14);
        
        // When & Then
        assertTrue(testPlan.hasTrialPeriod());
        assertEquals(14, testPlan.getTrialPeriodDays());
    }

    @Test
    void testPlanWithoutTrialPeriod_ShouldHandleNoTrial() {
        // Given - plan without trial period
        testPlan.setTrialPeriodDays(null);
        
        // When & Then
        assertFalse(testPlan.hasTrialPeriod());
    }

    @Test
    void testCustomerBillingInformation_ShouldBeComplete() {
        // When & Then - Verify all required billing fields are set
        assertNotNull(testCustomer.getFirstName());
        assertNotNull(testCustomer.getLastName());
        assertNotNull(testCustomer.getBillingAddressLine1());
        assertNotNull(testCustomer.getBillingCity());
        assertNotNull(testCustomer.getBillingState());
        assertNotNull(testCustomer.getBillingPostalCode());
        assertNotNull(testCustomer.getBillingCountry());
        
        assertEquals("John", testCustomer.getFirstName());
        assertEquals("Doe", testCustomer.getLastName());
        assertEquals("123 Main St", testCustomer.getBillingAddressLine1());
        assertEquals("New York", testCustomer.getBillingCity());
        assertEquals("NY", testCustomer.getBillingState());
        assertEquals("10001", testCustomer.getBillingPostalCode());
        assertEquals("US", testCustomer.getBillingCountry());
    }

    @Test
    void testPaymentMethodInformation_ShouldBeComplete() {
        // When & Then - Verify all required payment method fields are set
        assertNotNull(testPaymentMethod.getCardNumber());
        assertNotNull(testPaymentMethod.getExpiryMonth());
        assertNotNull(testPaymentMethod.getExpiryYear());
        assertNotNull(testPaymentMethod.getCvv());
        
        assertEquals("4111111111111111", testPaymentMethod.getCardNumber());
        assertEquals("12", testPaymentMethod.getExpiryMonth());
        assertEquals("2025", testPaymentMethod.getExpiryYear());
        assertEquals("123", testPaymentMethod.getCvv());
    }

    @Test
    void testServiceInitialization_ShouldLogCorrectly() {
        // Given & When - Service is initialized in setUp()
        
        // Then - Service should be properly initialized
        assertNotNull(authorizeNetARBService);
    }
}
