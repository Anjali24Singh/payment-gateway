package com.talentica.paymentgateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentica.paymentgateway.config.SandboxConfig;
import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.entity.PaymentMethod;
import com.talentica.paymentgateway.repository.CustomerRepository;
import com.talentica.paymentgateway.repository.PaymentMethodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SandboxController.
 * Tests all sandbox endpoints without Spring context.
 */
@ExtendWith(MockitoExtension.class)
class SandboxControllerUnitTest {

    @Mock
    private SandboxConfig sandboxConfig;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private SandboxConfig.TestCards testCards;

    @InjectMocks
    private SandboxController sandboxController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sandboxController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetSandboxInfo_ReturnsSandboxInformation() throws Exception {
        // Given
        when(sandboxConfig.getDefaultApiKey()).thenReturn("test_api_key_123");
        when(sandboxConfig.getTestCards()).thenReturn(testCards);

        // When & Then
        mockMvc.perform(get("/api/v1/sandbox/info"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.description").value("Sandbox environment for Payment Gateway API testing"))
                .andExpect(jsonPath("$.data.defaultApiKey").value("test_api_key_123"))
                .andExpect(jsonPath("$.data.testScenarios").isArray())
                .andExpect(jsonPath("$.data.apiEndpoints").isArray());

        verify(sandboxConfig).getDefaultApiKey();
        verify(sandboxConfig).getTestCards();
    }

    @Test
    void testGetTestCards_ReturnsTestCardInformation() throws Exception {
        // Given
        when(sandboxConfig.getTestCards()).thenReturn(testCards);
        when(testCards.getSuccessCard()).thenReturn("4111111111111111");
        when(testCards.getDeclineCard()).thenReturn("4000000000000002");
        when(testCards.getErrorCard()).thenReturn("4000000000000119");
        when(testCards.getExpiredCard()).thenReturn("4000000000000069");
        when(testCards.getCvvFailCard()).thenReturn("4000000000000127");

        // When & Then
        mockMvc.perform(get("/api/v1/sandbox/test-cards"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success.number").value("4111111111111111"))
                .andExpect(jsonPath("$.data.success.expiry").value("12/28"))
                .andExpect(jsonPath("$.data.success.cvv").value("123"))
                .andExpect(jsonPath("$.data.success.description").value("Successful payment - transaction will be approved"))
                .andExpect(jsonPath("$.data.decline.number").value("4000000000000002"))
                .andExpect(jsonPath("$.data.decline.description").value("Declined payment - insufficient funds"))
                .andExpect(jsonPath("$.data.error.number").value("4000000000000119"))
                .andExpect(jsonPath("$.data.expired.number").value("4000000000000069"))
                .andExpect(jsonPath("$.data.cvv_fail.number").value("4000000000000127"));

        verify(sandboxConfig, atLeastOnce()).getTestCards();
        verify(testCards).getSuccessCard();
        verify(testCards).getDeclineCard();
        verify(testCards).getErrorCard();
        verify(testCards).getExpiredCard();
        verify(testCards).getCvvFailCard();
    }

    @Test
    void testGenerateTestData_WithDefaultParameters_ReturnsTestTransactions() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/sandbox/generate-test-data"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.generated_count").value(10))
                .andExpect(jsonPath("$.data.transaction_type").value("PURCHASE"))
                .andExpect(jsonPath("$.data.transactions").isArray())
                .andExpect(jsonPath("$.data.transactions[0].transaction_id").exists())
                .andExpect(jsonPath("$.data.transactions[0].amount").exists())
                .andExpect(jsonPath("$.data.transactions[0].currency").value("USD"))
                .andExpect(jsonPath("$.data.transactions[0].status").exists())
                .andExpect(jsonPath("$.data.transactions[0].type").value("PURCHASE"));
    }

    @Test
    void testGenerateTestData_WithCustomParameters_ReturnsCustomTestTransactions() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/sandbox/generate-test-data")
                .param("count", "5")
                .param("transactionType", "REFUND"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.generated_count").value(5))
                .andExpect(jsonPath("$.data.transaction_type").value("REFUND"))
                .andExpect(jsonPath("$.data.transactions").isArray())
                .andExpect(jsonPath("$.data.transactions[0].type").value("REFUND"));
    }

    @Test
    void testResetSandbox_ReturnsSuccessMessage() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/sandbox/reset"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("reset_complete"))
                .andExpect(jsonPath("$.data.message").value("Sandbox environment has been reset to initial state"))
                .andExpect(jsonPath("$.data.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateTestPaymentMethod_WithValidCustomer_ReturnsSuccess() throws Exception {
        // Given
        Customer customer = new Customer();
        customer.setCustomerReference("test_customer_123");
        customer.setEmail("test@example.com");

        PaymentMethod savedPaymentMethod = new PaymentMethod();
        savedPaymentMethod.setPaymentToken("pm_test_test_customer_123_123456789");
        savedPaymentMethod.setCustomer(customer);

        when(customerRepository.findByCustomerId("test_customer_123")).thenReturn(Optional.of(customer));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(savedPaymentMethod);

        // When & Then
        mockMvc.perform(post("/api/v1/sandbox/create-test-payment-method")
                .param("customerId", "test_customer_123")
                .param("tokenPrefix", "pm_test_"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.paymentMethodId").value("pm_test_test_customer_123_123456789"))
                .andExpect(jsonPath("$.customerId").value("test_customer_123"))
                .andExpect(jsonPath("$.message").value("Test payment method created successfully"));

        verify(customerRepository).findByCustomerId("test_customer_123");
        verify(paymentMethodRepository).save(any(PaymentMethod.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateTestPaymentMethod_WithInvalidCustomer_ReturnsNotFound() throws Exception {
        // Given
        when(customerRepository.findByCustomerId("invalid_customer")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/v1/sandbox/create-test-payment-method")
                .param("customerId", "invalid_customer"))
                .andExpect(status().isNotFound());

        verify(customerRepository).findByCustomerId("invalid_customer");
        verify(paymentMethodRepository, never()).save(any(PaymentMethod.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateTestPaymentMethod_WithRepositoryException_ReturnsInternalServerError() throws Exception {
        // Given
        Customer customer = new Customer();
        customer.setCustomerReference("test_customer_123");

        when(customerRepository.findByCustomerId("test_customer_123")).thenReturn(Optional.of(customer));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(post("/api/v1/sandbox/create-test-payment-method")
                .param("customerId", "test_customer_123"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Database error"));

        verify(customerRepository).findByCustomerId("test_customer_123");
        verify(paymentMethodRepository).save(any(PaymentMethod.class));
    }

    @Test
    void testSandboxInfo_ContainsTestScenarios() throws Exception {
        // Given
        when(sandboxConfig.getDefaultApiKey()).thenReturn("test_key");
        when(sandboxConfig.getTestCards()).thenReturn(testCards);

        // When & Then
        mockMvc.perform(get("/api/v1/sandbox/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.testScenarios[0].name").value("successful_payment"))
                .andExpect(jsonPath("$.data.testScenarios[0].description").value("Test successful payment processing"))
                .andExpect(jsonPath("$.data.testScenarios[1].name").value("declined_payment"))
                .andExpect(jsonPath("$.data.testScenarios[2].name").value("processing_error"));
    }

    @Test
    void testSandboxInfo_ContainsApiEndpoints() throws Exception {
        // Given
        when(sandboxConfig.getDefaultApiKey()).thenReturn("test_key");
        when(sandboxConfig.getTestCards()).thenReturn(testCards);

        // When & Then
        mockMvc.perform(get("/api/v1/sandbox/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiEndpoints[0].method").value("POST"))
                .andExpect(jsonPath("$.data.apiEndpoints[0].path").value("/api/v1/payments/purchase"))
                .andExpect(jsonPath("$.data.apiEndpoints[0].description").value("Process purchase transaction"))
                .andExpect(jsonPath("$.data.apiEndpoints[1].method").value("POST"))
                .andExpect(jsonPath("$.data.apiEndpoints[1].path").value("/api/v1/payments/authorize"));
    }

    @Test
    void testGenerateTestData_CreatesValidTransactionStructure() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/sandbox/generate-test-data")
                .param("count", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactions[0].transaction_id").value(org.hamcrest.Matchers.startsWith("test_")))
                .andExpect(jsonPath("$.data.transactions[0].amount").isNumber())
                .andExpect(jsonPath("$.data.transactions[0].currency").value("USD"))
                .andExpect(jsonPath("$.data.transactions[0].status").value(org.hamcrest.Matchers.oneOf("COMPLETED", "PENDING", "FAILED", "CANCELLED", "REFUNDED")))
                .andExpect(jsonPath("$.data.transactions[0].created_at").exists())
                .andExpect(jsonPath("$.data.transactions[0].card_last_four").exists())
                .andExpect(jsonPath("$.data.transactions[0].merchant_id").value(org.hamcrest.Matchers.startsWith("test_merchant_")));
    }
}
