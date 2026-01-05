package com.talentica.paymentgateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.talentica.paymentgateway.dto.subscription.ARBSubscriptionRequest;
import com.talentica.paymentgateway.dto.subscription.ARBSubscriptionResponse;
import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.entity.PaymentMethod;
import com.talentica.paymentgateway.entity.SubscriptionPlan;
import com.talentica.paymentgateway.exception.GlobalExceptionHandler;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import com.talentica.paymentgateway.repository.CustomerRepository;
import com.talentica.paymentgateway.repository.PaymentMethodRepository;
import com.talentica.paymentgateway.repository.SubscriptionPlanRepository;
import com.talentica.paymentgateway.service.AuthorizeNetARBService;
import net.authorize.api.contract.v1.ARBGetSubscriptionResponse;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ARBSubscriptionControllerUnitTest {

    @Mock
    private AuthorizeNetARBService arbService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SubscriptionPlanRepository planRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @InjectMocks
    private ARBSubscriptionController arbSubscriptionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(arbSubscriptionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should list ARB subscriptions successfully")
    void listARBSubscriptions_ShouldReturnPlaceholderResponse() throws Exception {
        // When & Then
        mockMvc.perform(get("/arb-subscriptions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.subscriptions").isArray());
    }

    @Test
    @DisplayName("Should create ARB subscription successfully")
    void createARBSubscription_WithValidRequest_ShouldReturnCreated() throws Exception {
        // Given
        ARBSubscriptionRequest request = createValidARBSubscriptionRequest();
        Customer customer = createCustomer();
        SubscriptionPlan plan = createSubscriptionPlan();
        PaymentMethod paymentMethod = createPaymentMethod();
        String arbSubscriptionId = "ARB_12345";

        when(customerRepository.findByCustomerId(request.getCustomerId())).thenReturn(Optional.of(customer));
        when(planRepository.findByPlanCode(request.getPlanCode())).thenReturn(Optional.of(plan));
        when(paymentMethodRepository.findByPaymentMethodId(request.getPaymentMethodId())).thenReturn(Optional.of(paymentMethod));
        when(arbService.createARBSubscription(customer, plan, paymentMethod)).thenReturn(arbSubscriptionId);

        // When & Then
        mockMvc.perform(post("/arb-subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arbSubscriptionId").value(arbSubscriptionId))
                .andExpect(jsonPath("$.customerId").value(customer.getCustomerId()))
                .andExpect(jsonPath("$.customerName").value(customer.getFirstName() + " " + customer.getLastName()))
                .andExpect(jsonPath("$.customerEmail").value(customer.getEmail()))
                .andExpect(jsonPath("$.planCode").value(plan.getPlanCode()))
                .andExpect(jsonPath("$.planName").value(plan.getName()))
                .andExpect(jsonPath("$.planAmount").value(plan.getAmount()))
                .andExpect(jsonPath("$.currency").value(plan.getCurrency()))
                .andExpect(jsonPath("$.intervalUnit").value(plan.getIntervalUnit().toString()))
                .andExpect(jsonPath("$.intervalCount").value(plan.getIntervalCount()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").exists());

        verify(customerRepository).findByCustomerId(request.getCustomerId());
        verify(planRepository).findByPlanCode(request.getPlanCode());
        verify(paymentMethodRepository).findByPaymentMethodId(request.getPaymentMethodId());
        verify(arbService).createARBSubscription(customer, plan, paymentMethod);
    }

    @Test
    @DisplayName("Should handle customer not found during ARB subscription creation")
    void createARBSubscription_WithInvalidCustomer_ShouldThrowException() throws Exception {
        // Given
        ARBSubscriptionRequest request = createValidARBSubscriptionRequest();

        when(customerRepository.findByCustomerId(request.getCustomerId())).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/arb-subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        verify(customerRepository).findByCustomerId(request.getCustomerId());
        verify(planRepository, never()).findByPlanCode(anyString());
        verify(paymentMethodRepository, never()).findByPaymentMethodId(anyString());
        verify(arbService, never()).createARBSubscription(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle plan not found during ARB subscription creation")
    void createARBSubscription_WithInvalidPlan_ShouldThrowException() throws Exception {
        // Given
        ARBSubscriptionRequest request = createValidARBSubscriptionRequest();
        Customer customer = createCustomer();

        when(customerRepository.findByCustomerId(request.getCustomerId())).thenReturn(Optional.of(customer));
        when(planRepository.findByPlanCode(request.getPlanCode())).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/arb-subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        verify(customerRepository).findByCustomerId(request.getCustomerId());
        verify(planRepository).findByPlanCode(request.getPlanCode());
        verify(paymentMethodRepository, never()).findByPaymentMethodId(anyString());
        verify(arbService, never()).createARBSubscription(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle payment method not found during ARB subscription creation")
    void createARBSubscription_WithInvalidPaymentMethod_ShouldThrowException() throws Exception {
        // Given
        ARBSubscriptionRequest request = createValidARBSubscriptionRequest();
        Customer customer = createCustomer();
        SubscriptionPlan plan = createSubscriptionPlan();

        when(customerRepository.findByCustomerId(request.getCustomerId())).thenReturn(Optional.of(customer));
        when(planRepository.findByPlanCode(request.getPlanCode())).thenReturn(Optional.of(plan));
        when(paymentMethodRepository.findByPaymentMethodId(request.getPaymentMethodId())).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/arb-subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        verify(customerRepository).findByCustomerId(request.getCustomerId());
        verify(planRepository).findByPlanCode(request.getPlanCode());
        verify(paymentMethodRepository).findByPaymentMethodId(request.getPaymentMethodId());
        verify(arbService, never()).createARBSubscription(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle ARB service exception during subscription creation")
    void createARBSubscription_WithServiceException_ShouldThrowException() throws Exception {
        // Given
        ARBSubscriptionRequest request = createValidARBSubscriptionRequest();
        Customer customer = createCustomer();
        SubscriptionPlan plan = createSubscriptionPlan();
        PaymentMethod paymentMethod = createPaymentMethod();

        when(customerRepository.findByCustomerId(request.getCustomerId())).thenReturn(Optional.of(customer));
        when(planRepository.findByPlanCode(request.getPlanCode())).thenReturn(Optional.of(plan));
        when(paymentMethodRepository.findByPaymentMethodId(request.getPaymentMethodId())).thenReturn(Optional.of(paymentMethod));
        when(arbService.createARBSubscription(customer, plan, paymentMethod))
                .thenThrow(new RuntimeException("ARB service error"));

        // When & Then
        mockMvc.perform(post("/arb-subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        verify(arbService).createARBSubscription(customer, plan, paymentMethod);
    }

    @Test
    @DisplayName("Should get ARB subscription successfully")
    void getARBSubscription_WithValidId_ShouldReturnSubscription() throws Exception {
        // Given
        String arbSubscriptionId = "ARB_12345";
        ARBGetSubscriptionResponse response = mock(ARBGetSubscriptionResponse.class);

        when(arbService.getARBSubscription(arbSubscriptionId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/arb-subscriptions/{arbSubscriptionId}", arbSubscriptionId))
                .andExpect(status().isOk());

        verify(arbService).getARBSubscription(arbSubscriptionId);
    }

    @Test
    @DisplayName("Should handle ARB service exception during subscription retrieval")
    void getARBSubscription_WithServiceException_ShouldThrowException() throws Exception {
        // Given
        String arbSubscriptionId = "ARB_12345";

        when(arbService.getARBSubscription(arbSubscriptionId))
                .thenThrow(new RuntimeException("ARB service error"));

        // When & Then
        mockMvc.perform(get("/arb-subscriptions/{arbSubscriptionId}", arbSubscriptionId))
                .andExpect(status().is4xxClientError());

        verify(arbService).getARBSubscription(arbSubscriptionId);
    }

    @Test
    @DisplayName("Should cancel ARB subscription successfully")
    void cancelARBSubscription_WithValidId_ShouldReturnSuccess() throws Exception {
        // Given
        String arbSubscriptionId = "ARB_12345";

        when(arbService.cancelARBSubscription(arbSubscriptionId)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/arb-subscriptions/{arbSubscriptionId}", arbSubscriptionId))
                .andExpect(status().isOk())
                .andExpect(content().string("ARB subscription cancelled successfully"));

        verify(arbService).cancelARBSubscription(arbSubscriptionId);
    }

    @Test
    @DisplayName("Should handle failed ARB subscription cancellation")
    void cancelARBSubscription_WithFailedCancellation_ShouldThrowException() throws Exception {
        // Given
        String arbSubscriptionId = "ARB_12345";

        when(arbService.cancelARBSubscription(arbSubscriptionId)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/arb-subscriptions/{arbSubscriptionId}", arbSubscriptionId))
                .andExpect(status().is4xxClientError());

        verify(arbService).cancelARBSubscription(arbSubscriptionId);
    }

    @Test
    @DisplayName("Should handle ARB service exception during subscription cancellation")
    void cancelARBSubscription_WithServiceException_ShouldThrowException() throws Exception {
        // Given
        String arbSubscriptionId = "ARB_12345";

        when(arbService.cancelARBSubscription(arbSubscriptionId))
                .thenThrow(new RuntimeException("ARB service error"));

        // When & Then
        mockMvc.perform(delete("/arb-subscriptions/{arbSubscriptionId}", arbSubscriptionId))
                .andExpect(status().is4xxClientError());

        verify(arbService).cancelARBSubscription(arbSubscriptionId);
    }

    @Test
    @DisplayName("Should handle PaymentProcessingException during subscription creation")
    void createARBSubscription_WithPaymentProcessingException_ShouldPropagateException() throws Exception {
        // Given
        ARBSubscriptionRequest request = createValidARBSubscriptionRequest();
        Customer customer = createCustomer();
        SubscriptionPlan plan = createSubscriptionPlan();
        PaymentMethod paymentMethod = createPaymentMethod();

        when(customerRepository.findByCustomerId(request.getCustomerId())).thenReturn(Optional.of(customer));
        when(planRepository.findByPlanCode(request.getPlanCode())).thenReturn(Optional.of(plan));
        when(paymentMethodRepository.findByPaymentMethodId(request.getPaymentMethodId())).thenReturn(Optional.of(paymentMethod));
        when(arbService.createARBSubscription(customer, plan, paymentMethod))
                .thenThrow(new PaymentProcessingException("ARB creation failed", "ARB_ERROR"));

        // When & Then
        mockMvc.perform(post("/arb-subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        verify(arbService).createARBSubscription(customer, plan, paymentMethod);
    }

    @Test
    @DisplayName("Should validate request body for ARB subscription creation")
    void createARBSubscription_WithInvalidRequestBody_ShouldReturnBadRequest() throws Exception {
        // Given - empty request body
        String invalidJson = "{}";

        // When & Then
        mockMvc.perform(post("/arb-subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().is4xxClientError());

        verify(customerRepository, never()).findByCustomerId(anyString());
        verify(planRepository, never()).findByPlanCode(anyString());
        verify(paymentMethodRepository, never()).findByPaymentMethodId(anyString());
        verify(arbService, never()).createARBSubscription(any(), any(), any());
    }

    // Helper methods
    private ARBSubscriptionRequest createValidARBSubscriptionRequest() {
        ARBSubscriptionRequest request = new ARBSubscriptionRequest();
        request.setCustomerId("CUST_12345");
        request.setPlanCode("BASIC_PLAN");
        request.setPaymentMethodId("PM_12345");
        return request;
    }

    private Customer createCustomer() {
        Customer customer = new Customer();
        customer.setCustomerReference("CUST_12345");
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        customer.setPhone("555-1234");
        return customer;
    }

    private SubscriptionPlan createSubscriptionPlan() {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setPlanCode("BASIC_PLAN");
        plan.setName("Basic Plan");
        plan.setDescription("Basic subscription plan");
        plan.setAmount(new BigDecimal("29.99"));
        plan.setCurrency("USD");
        plan.setIntervalUnit("MONTH");
        plan.setIntervalCount(1);
        plan.setIsActive(true);
        return plan;
    }

    private PaymentMethod createPaymentMethod() {
        PaymentMethod paymentMethod = new PaymentMethod();
        // PaymentMethod uses UUID id, not paymentMethodId
        // PaymentMethod doesn't have setType method
        // PaymentMethod doesn't have setLastFourDigits method
        // PaymentMethod doesn't have setExpiryMonth method
        // PaymentMethod doesn't have setExpiryYear method
        // PaymentMethod doesn't have setActive method
        return paymentMethod;
    }
}
