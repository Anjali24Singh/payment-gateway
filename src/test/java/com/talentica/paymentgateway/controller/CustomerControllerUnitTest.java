package com.talentica.paymentgateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.talentica.paymentgateway.dto.customer.*;
import com.talentica.paymentgateway.dto.payment.PaymentMethodRequest;
import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.service.AuthorizeNetCustomerService;
import com.talentica.paymentgateway.service.CustomerService;
import net.authorize.api.contract.v1.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerControllerUnitTest {

    @Mock
    private AuthorizeNetCustomerService authorizeNetCustomerService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should create customer profile successfully")
    void createCustomerProfile_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        CustomerProfileRequest request = createValidCustomerProfileRequest();
        Customer customer = createTestCustomer();
        String customerProfileId = "12345";

        when(customerService.findByCustomerId(anyString())).thenReturn(null);
        when(customerService.createCustomer(any(Customer.class))).thenReturn(customer);
        when(authorizeNetCustomerService.createCustomerProfile(any(Customer.class), isNull())).thenReturn(customerProfileId);
        when(customerService.updateCustomer(any(Customer.class))).thenReturn(customer);

        // When & Then
        mockMvc.perform(post("/api/customers/profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.customerProfileId").value(customerProfileId));

        verify(customerService).findByCustomerId(request.getCustomerId());
        verify(customerService).createCustomer(any(Customer.class));
        verify(authorizeNetCustomerService).createCustomerProfile(any(Customer.class), isNull());
        verify(customerService).updateCustomer(any(Customer.class));
    }

    @Test
    @DisplayName("Should create customer profile with existing customer")
    void createCustomerProfile_WithExistingCustomer_ShouldReturnSuccess() throws Exception {
        // Given
        CustomerProfileRequest request = createValidCustomerProfileRequest();
        Customer existingCustomer = createTestCustomer();
        String customerProfileId = "12345";

        when(customerService.findByCustomerId(anyString())).thenReturn(existingCustomer);
        when(authorizeNetCustomerService.createCustomerProfile(any(Customer.class), isNull())).thenReturn(customerProfileId);
        when(customerService.updateCustomer(any(Customer.class))).thenReturn(existingCustomer);

        // When & Then
        mockMvc.perform(post("/api/customers/profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(customerService).findByCustomerId(request.getCustomerId());
        verify(customerService, never()).createCustomer(any(Customer.class));
        verify(authorizeNetCustomerService).createCustomerProfile(any(Customer.class), isNull());
    }

    @Test
    @DisplayName("Should handle create customer profile error")
    void createCustomerProfile_WithServiceException_ShouldReturnError() throws Exception {
        // Given
        CustomerProfileRequest request = createValidCustomerProfileRequest();

        when(customerService.findByCustomerId(anyString())).thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(post("/api/customers/profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Should get customer profile successfully")
    void getCustomerProfile_WithValidId_ShouldReturnProfile() throws Exception {
        // Given
        String customerProfileId = "12345";
        CustomerProfileMaskedType profile = createMockCustomerProfile();

        when(authorizeNetCustomerService.getCustomerProfile(customerProfileId)).thenReturn(profile);

        // When & Then
        mockMvc.perform(get("/api/customers/profiles/{customerProfileId}", customerProfileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.profile").exists());

        verify(authorizeNetCustomerService).getCustomerProfile(customerProfileId);
    }

    @Test
    @DisplayName("Should handle get customer profile error")
    void getCustomerProfile_WithServiceException_ShouldReturnError() throws Exception {
        // Given
        String customerProfileId = "12345";

        when(authorizeNetCustomerService.getCustomerProfile(customerProfileId))
                .thenThrow(new RuntimeException("Profile not found"));

        // When & Then
        mockMvc.perform(get("/api/customers/profiles/{customerProfileId}", customerProfileId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Should update customer profile successfully")
    void updateCustomerProfile_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        String customerProfileId = "12345";
        CustomerProfileRequest request = createValidCustomerProfileRequest();
        Customer customer = createTestCustomer();

        when(customerService.findByAuthorizeNetCustomerProfileId(customerProfileId)).thenReturn(customer);
        when(authorizeNetCustomerService.updateCustomerProfile(eq(customerProfileId), any(Customer.class))).thenReturn(true);
        when(customerService.updateCustomer(any(Customer.class))).thenReturn(customer);

        // When & Then
        mockMvc.perform(put("/api/customers/profiles/{customerProfileId}", customerProfileId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.customerProfileId").value(customerProfileId));

        verify(customerService).findByAuthorizeNetCustomerProfileId(customerProfileId);
        verify(authorizeNetCustomerService).updateCustomerProfile(eq(customerProfileId), any(Customer.class));
        verify(customerService).updateCustomer(any(Customer.class));
    }

    @Test
    @DisplayName("Should handle update customer profile with customer not found")
    void updateCustomerProfile_WithCustomerNotFound_ShouldReturnNotFound() throws Exception {
        // Given
        String customerProfileId = "12345";
        CustomerProfileRequest request = createValidCustomerProfileRequest();

        when(customerService.findByAuthorizeNetCustomerProfileId(customerProfileId)).thenReturn(null);
        when(customerService.findByCustomerId(request.getCustomerId())).thenReturn(null);

        // When & Then
        mockMvc.perform(put("/api/customers/profiles/{customerProfileId}", customerProfileId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Customer not found"));
    }

    @Test
    @DisplayName("Should delete customer profile successfully")
    void deleteCustomerProfile_WithValidId_ShouldReturnSuccess() throws Exception {
        // Given
        String customerProfileId = "12345";
        Customer customer = createTestCustomer();

        when(authorizeNetCustomerService.deleteCustomerProfile(customerProfileId)).thenReturn(true);
        when(customerService.findByAuthorizeNetCustomerProfileId(customerProfileId)).thenReturn(customer);
        when(customerService.updateCustomer(any(Customer.class))).thenReturn(customer);

        // When & Then
        mockMvc.perform(delete("/api/customers/profiles/{customerProfileId}", customerProfileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.customerProfileId").value(customerProfileId));

        verify(authorizeNetCustomerService).deleteCustomerProfile(customerProfileId);
        verify(customerService).findByAuthorizeNetCustomerProfileId(customerProfileId);
        verify(customerService).updateCustomer(any(Customer.class));
    }

    @Test
    @DisplayName("Should create payment profile successfully")
    void createPaymentProfile_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        String customerProfileId = "12345";
        String paymentProfileId = "67890";
        PaymentProfileRequest request = createValidPaymentProfileRequest();
        Customer customer = createTestCustomer();

        when(customerService.findByAuthorizeNetCustomerProfileId(customerProfileId)).thenReturn(customer);
        when(authorizeNetCustomerService.createPaymentProfile(eq(customerProfileId), any(PaymentMethodRequest.class), eq(customer)))
                .thenReturn(paymentProfileId);

        // When & Then
        mockMvc.perform(post("/api/customers/profiles/{customerProfileId}/payment-profiles", customerProfileId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.paymentProfileId").value(paymentProfileId));

        verify(customerService).findByAuthorizeNetCustomerProfileId(customerProfileId);
        verify(authorizeNetCustomerService).createPaymentProfile(eq(customerProfileId), any(PaymentMethodRequest.class), eq(customer));
    }

    @Test
    @DisplayName("Should handle create payment profile with customer not found")
    void createPaymentProfile_WithCustomerNotFound_ShouldReturnNotFound() throws Exception {
        // Given
        String customerProfileId = "12345";
        PaymentProfileRequest request = createValidPaymentProfileRequest();

        when(customerService.findByAuthorizeNetCustomerProfileId(customerProfileId)).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/api/customers/profiles/{customerProfileId}/payment-profiles", customerProfileId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Customer profile not found"));
    }

    @Test
    @DisplayName("Should get payment profile successfully")
    void getPaymentProfile_WithValidIds_ShouldReturnProfile() throws Exception {
        // Given
        String customerProfileId = "12345";
        String paymentProfileId = "67890";
        CustomerPaymentProfileMaskedType paymentProfile = createMockPaymentProfile();

        when(authorizeNetCustomerService.getCustomerPaymentProfile(customerProfileId, paymentProfileId))
                .thenReturn(paymentProfile);

        // When & Then
        mockMvc.perform(get("/api/customers/profiles/{customerProfileId}/payment-profiles/{paymentProfileId}", 
                customerProfileId, paymentProfileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.paymentProfile").exists());

        verify(authorizeNetCustomerService).getCustomerPaymentProfile(customerProfileId, paymentProfileId);
    }

    @Test
    @DisplayName("Should update payment profile successfully")
    void updatePaymentProfile_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        String customerProfileId = "12345";
        String paymentProfileId = "67890";
        PaymentProfileRequest request = createValidPaymentProfileRequest();
        Customer customer = createTestCustomer();

        when(customerService.findByAuthorizeNetCustomerProfileId(customerProfileId)).thenReturn(customer);
        when(authorizeNetCustomerService.updateCustomerPaymentProfile(
                eq(customerProfileId), eq(paymentProfileId), any(PaymentMethodRequest.class), eq(customer)))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(put("/api/customers/profiles/{customerProfileId}/payment-profiles/{paymentProfileId}", 
                customerProfileId, paymentProfileId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.paymentProfileId").value(paymentProfileId));

        verify(customerService).findByAuthorizeNetCustomerProfileId(customerProfileId);
        verify(authorizeNetCustomerService).updateCustomerPaymentProfile(
                eq(customerProfileId), eq(paymentProfileId), any(PaymentMethodRequest.class), eq(customer));
    }

    @Test
    @DisplayName("Should delete payment profile successfully")
    void deletePaymentProfile_WithValidIds_ShouldReturnSuccess() throws Exception {
        // Given
        String customerProfileId = "12345";
        String paymentProfileId = "67890";

        when(authorizeNetCustomerService.deleteCustomerPaymentProfile(customerProfileId, paymentProfileId))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/customers/profiles/{customerProfileId}/payment-profiles/{paymentProfileId}", 
                customerProfileId, paymentProfileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.paymentProfileId").value(paymentProfileId));

        verify(authorizeNetCustomerService).deleteCustomerPaymentProfile(customerProfileId, paymentProfileId);
    }

    @Test
    @DisplayName("Should validate payment profile successfully")
    void validatePaymentProfile_WithValidIds_ShouldReturnValidation() throws Exception {
        // Given
        String customerProfileId = "12345";
        String paymentProfileId = "67890";

        when(authorizeNetCustomerService.validateCustomerPaymentProfile(customerProfileId, paymentProfileId))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/customers/profiles/{customerProfileId}/payment-profiles/{paymentProfileId}/validate", 
                customerProfileId, paymentProfileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.valid").value(true));

        verify(authorizeNetCustomerService).validateCustomerPaymentProfile(customerProfileId, paymentProfileId);
    }

    @Test
    @DisplayName("Should handle validate payment profile error")
    void validatePaymentProfile_WithServiceException_ShouldReturnError() throws Exception {
        // Given
        String customerProfileId = "12345";
        String paymentProfileId = "67890";

        when(authorizeNetCustomerService.validateCustomerPaymentProfile(customerProfileId, paymentProfileId))
                .thenThrow(new RuntimeException("Validation failed"));

        // When & Then
        mockMvc.perform(post("/api/customers/profiles/{customerProfileId}/payment-profiles/{paymentProfileId}/validate", 
                customerProfileId, paymentProfileId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    // Helper methods
    private CustomerProfileRequest createValidCustomerProfileRequest() {
        CustomerProfileRequest request = new CustomerProfileRequest();
        request.setCustomerId("CUST123");
        request.setEmail("test@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setBillingAddressLine1("123 Main St");
        request.setBillingCity("Anytown");
        request.setBillingState("CA");
        request.setBillingPostalCode("12345");
        request.setBillingCountry("US");
        return request;
    }

    private PaymentProfileRequest createValidPaymentProfileRequest() {
        PaymentProfileRequest request = new PaymentProfileRequest();
        request.setCardNumber("4111111111111111");
        request.setExpiryMonth("12");
        request.setExpiryYear("2025");
        request.setCvv("123");
        return request;
    }

    private Customer createTestCustomer() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCustomerReference("CUST123");
        customer.setEmail("test@example.com");
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setAuthorizeNetCustomerProfileId("12345");
        return customer;
    }

    private CustomerProfileMaskedType createMockCustomerProfile() {
        CustomerProfileMaskedType profile = new CustomerProfileMaskedType();
        profile.setCustomerProfileId("12345");
        profile.setMerchantCustomerId("CUST123");
        profile.setEmail("test@example.com");
        profile.setDescription("Test Customer");
        
        List<CustomerPaymentProfileMaskedType> paymentProfiles = new ArrayList<>();
        CustomerPaymentProfileMaskedType paymentProfile = createMockPaymentProfile();
        paymentProfiles.add(paymentProfile);
        // CustomerProfileMaskedType doesn't have setPaymentProfiles method
        
        return profile;
    }

    private CustomerPaymentProfileMaskedType createMockPaymentProfile() {
        CustomerPaymentProfileMaskedType paymentProfile = new CustomerPaymentProfileMaskedType();
        paymentProfile.setCustomerPaymentProfileId("67890");
        
        PaymentMaskedType payment = new PaymentMaskedType();
        CreditCardMaskedType creditCard = new CreditCardMaskedType();
        creditCard.setCardNumber("XXXX1111");
        creditCard.setExpirationDate("XXXX");
        creditCard.setCardType("Visa");
        payment.setCreditCard(creditCard);
        paymentProfile.setPayment(payment);
        
        return paymentProfile;
    }
}
