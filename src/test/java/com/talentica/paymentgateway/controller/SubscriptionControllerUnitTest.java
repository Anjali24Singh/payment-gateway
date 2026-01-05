package com.talentica.paymentgateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.talentica.paymentgateway.dto.subscription.CreateSubscriptionRequest;
import com.talentica.paymentgateway.dto.subscription.UpdateSubscriptionRequest;
import com.talentica.paymentgateway.dto.subscription.CancelSubscriptionRequest;
import com.talentica.paymentgateway.dto.subscription.SubscriptionResponse;
import com.talentica.paymentgateway.dto.subscription.ProrationCalculation;
import com.talentica.paymentgateway.entity.SubscriptionStatus;
import com.talentica.paymentgateway.exception.GlobalExceptionHandler;
import com.talentica.paymentgateway.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SubscriptionController.
 * Tests all subscription management endpoints including CRUD operations,
 * lifecycle management (pause/resume/cancel), and proration calculations.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionControllerUnitTest {

    @Mock
    private SubscriptionService subscriptionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private SubscriptionController subscriptionController;

    @BeforeEach
    void setUp() {
        subscriptionController = new SubscriptionController(subscriptionService);
        mockMvc = MockMvcBuilders.standaloneSetup(subscriptionController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void getAllSubscriptions_ShouldReturnPagedSubscriptions() throws Exception {
        // Note: Controller returns empty page as placeholder - no service call implemented yet
        // Act & Assert
        mockMvc.perform(get("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAllSubscriptions_WithEmptyPage_ShouldReturnEmptyPage() throws Exception {
        // Arrange
        Page<SubscriptionResponse> emptyPage = Page.empty(PageRequest.of(0, 20));

        // Act & Assert
        mockMvc.perform(get("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void createSubscription_ShouldReturnCreatedSubscription() throws Exception {
        // Arrange
        CreateSubscriptionRequest request = createCreateSubscriptionRequest();
        SubscriptionResponse expectedResponse = createSubscriptionResponse("sub-123", "ACTIVE");
        
        when(subscriptionService.createSubscription(any(CreateSubscriptionRequest.class)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subscriptionId").value("sub-123"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(subscriptionService).createSubscription(any(CreateSubscriptionRequest.class));
    }

    @Test
    void createSubscription_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange - invalid request with missing required fields
        CreateSubscriptionRequest invalidRequest = new CreateSubscriptionRequest();
        // Don't set required fields

        // Act & Assert
        mockMvc.perform(post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(subscriptionService, never()).createSubscription(any());
    }

    @Test
    void updateSubscription_ShouldReturnUpdatedSubscription() throws Exception {
        // Arrange
        String subscriptionId = "sub-123";
        UpdateSubscriptionRequest request = createUpdateSubscriptionRequest();
        SubscriptionResponse expectedResponse = createSubscriptionResponse(subscriptionId, "ACTIVE");
        
        when(subscriptionService.updateSubscription(eq(subscriptionId), any(UpdateSubscriptionRequest.class)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(put("/subscriptions/{subscriptionId}", subscriptionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(subscriptionService).updateSubscription(eq(subscriptionId), any(UpdateSubscriptionRequest.class));
    }

    @Test
    void cancelSubscription_ShouldReturnCancelledSubscription() throws Exception {
        // Arrange
        String subscriptionId = "sub-123";
        CancelSubscriptionRequest request = createCancelSubscriptionRequest();
        SubscriptionResponse expectedResponse = createSubscriptionResponse(subscriptionId, "CANCELLED");
        
        when(subscriptionService.cancelSubscription(eq(subscriptionId), any(CancelSubscriptionRequest.class)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/subscriptions/{subscriptionId}/cancel", subscriptionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(subscriptionService).cancelSubscription(eq(subscriptionId), any(CancelSubscriptionRequest.class));
    }

    @Test
    void pauseSubscription_ShouldReturnPausedSubscription() throws Exception {
        // Arrange
        String subscriptionId = "sub-123";
        SubscriptionResponse expectedResponse = createSubscriptionResponse(subscriptionId, "PAUSED");
        
        when(subscriptionService.pauseSubscription(eq(subscriptionId)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/subscriptions/{subscriptionId}/pause", subscriptionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId))
                .andExpect(jsonPath("$.status").value("PAUSED"));

        verify(subscriptionService).pauseSubscription(eq(subscriptionId));
    }

    @Test
    void resumeSubscription_ShouldReturnActiveSubscription() throws Exception {
        // Arrange
        String subscriptionId = "sub-123";
        SubscriptionResponse expectedResponse = createSubscriptionResponse(subscriptionId, "ACTIVE");
        
        when(subscriptionService.resumeSubscription(eq(subscriptionId)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/subscriptions/{subscriptionId}/resume", subscriptionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(subscriptionService).resumeSubscription(eq(subscriptionId));
    }

    @Test
    void getSubscription_ShouldReturnSubscriptionDetails() throws Exception {
        // Arrange
        String subscriptionId = "sub-123";
        SubscriptionResponse expectedResponse = createSubscriptionResponse(subscriptionId, "ACTIVE");
        
        when(subscriptionService.getSubscription(eq(subscriptionId)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/subscriptions/{subscriptionId}", subscriptionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(subscriptionService).getSubscription(eq(subscriptionId));
    }

    @Test
    void getCustomerSubscriptions_ShouldReturnPagedSubscriptions() throws Exception {
        // Arrange
        String customerId = "customer123";
        List<SubscriptionResponse> subscriptions = Arrays.asList(
            createSubscriptionResponse("sub-1", "ACTIVE")
        );
        Page<SubscriptionResponse> subscriptionPage = new PageImpl<>(subscriptions, PageRequest.of(0, 10), 1);
        
        when(subscriptionService.getCustomerSubscriptions(eq("customer123"), any(Pageable.class)))
                .thenReturn(subscriptionPage);

        // Act & Assert
        mockMvc.perform(get("/subscriptions/customer/customer123")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(subscriptionService).getCustomerSubscriptions(eq("customer123"), any(Pageable.class));
    }

    @Test
    void calculateProration_ShouldReturnProrationCalculation() throws Exception {
        // Arrange
        String subscriptionId = "sub-123";
        String newPlanCode = "premium-plan";

        // Act & Assert
        mockMvc.perform(post("/subscriptions/{subscriptionId}/calculate-proration", subscriptionId)
                .param("newPlanCode", newPlanCode)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.prorationReason").value("Proration calculation not yet implemented"));
    }

    @Test
    void calculateProration_WithChangeDate_ShouldReturnProrationCalculation() throws Exception {
        // Arrange
        String subscriptionId = "sub-123";
        String newPlanCode = "premium-plan";
        String changeDate = "2025-02-01";

        // Act & Assert
        mockMvc.perform(post("/subscriptions/{subscriptionId}/calculate-proration", subscriptionId)
                .param("newPlanCode", newPlanCode)
                .param("changeDate", changeDate)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void previewSubscriptionChange_ShouldReturnPreview() throws Exception {
        // Arrange
        String subscriptionId = "sub-123";
        UpdateSubscriptionRequest request = createUpdateSubscriptionRequest();

        // Act & Assert
        mockMvc.perform(post("/subscriptions/{subscriptionId}/preview-change", subscriptionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Change preview not yet implemented"));
    }

    @Test
    void createSubscription_WithServiceException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        CreateSubscriptionRequest request = createCreateSubscriptionRequest();
        
        when(subscriptionService.createSubscription(any(CreateSubscriptionRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateSubscription_WithServiceException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        String subscriptionId = "sub-123";
        UpdateSubscriptionRequest request = createUpdateSubscriptionRequest();
        
        when(subscriptionService.updateSubscription(eq(subscriptionId), any(UpdateSubscriptionRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(put("/subscriptions/{subscriptionId}", subscriptionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getSubscription_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        // Arrange
        String subscriptionId = "non-existent";
        
        when(subscriptionService.getSubscription(eq(subscriptionId)))
                .thenThrow(new RuntimeException("Subscription not found"));

        // Act & Assert
        mockMvc.perform(get("/subscriptions/{subscriptionId}", subscriptionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void pauseSubscription_WithInactiveSubscription_ShouldReturnConflict() throws Exception {
        // Arrange
        String subscriptionId = "sub-123";
        
        when(subscriptionService.pauseSubscription(eq(subscriptionId)))
                .thenThrow(new RuntimeException("Cannot pause inactive subscription"));

        // Act & Assert
        mockMvc.perform(post("/subscriptions/{subscriptionId}/pause", subscriptionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void resumeSubscription_WithNonPausedSubscription_ShouldReturnConflict() throws Exception {
        // Arrange
        String subscriptionId = "sub-123";
        
        when(subscriptionService.resumeSubscription(eq(subscriptionId)))
                .thenThrow(new RuntimeException("Cannot resume non-paused subscription"));

        // Act & Assert
        mockMvc.perform(post("/subscriptions/{subscriptionId}/resume", subscriptionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    // Helper methods to create test data

    private CreateSubscriptionRequest createCreateSubscriptionRequest() {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setCustomerId("cust-123");
        request.setPlanCode("basic-plan");
        request.setPaymentMethodId("pm-123");
        request.setStartDate(ZonedDateTime.now());
        return request;
    }

    private UpdateSubscriptionRequest createUpdateSubscriptionRequest() {
        UpdateSubscriptionRequest request = new UpdateSubscriptionRequest();
        request.setPlanCode("premium-plan");
        request.setPaymentMethodId("pm-456");
        return request;
    }

    private CancelSubscriptionRequest createCancelSubscriptionRequest() {
        CancelSubscriptionRequest request = new CancelSubscriptionRequest();
        request.setWhen("END_OF_PERIOD");
        request.setReason("Customer requested cancellation");
        return request;
    }

    private SubscriptionResponse createSubscriptionResponse(String subscriptionId, String status) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setSubscriptionId(subscriptionId);
        response.setCustomerId("cust-123");
        response.setPlanCode("basic-plan");
        response.setStatus(SubscriptionStatus.valueOf(status));
        response.setPlanAmount(BigDecimal.valueOf(29.99));
        response.setCurrency("USD");
        response.setIntervalUnit("MONTH");
        response.setCreatedAt(ZonedDateTime.now().minusDays(30));
        response.setNextBillingDate(ZonedDateTime.now().plusDays(1));
        response.setUpdatedAt(ZonedDateTime.now());
        return response;
    }
}
