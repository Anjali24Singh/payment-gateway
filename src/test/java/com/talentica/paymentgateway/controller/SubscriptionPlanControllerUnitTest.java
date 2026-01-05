package com.talentica.paymentgateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.talentica.paymentgateway.dto.subscription.CreatePlanRequest;
import com.talentica.paymentgateway.dto.subscription.PlanResponse;
import com.talentica.paymentgateway.exception.GlobalExceptionHandler;
import com.talentica.paymentgateway.service.SubscriptionPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import com.fasterxml.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class SubscriptionPlanControllerUnitTest {

    @Mock
    private SubscriptionPlanService planService;

    @InjectMocks
    private SubscriptionPlanController subscriptionPlanController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModule(new com.fasterxml.jackson.datatype.jdk8.Jdk8Module())
                .build();
        // Configure to handle Page serialization issues
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        
        mockMvc = MockMvcBuilders.standaloneSetup(subscriptionPlanController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(mapper))
                .build();
        objectMapper = mapper;
    }

    @Test
    @DisplayName("Should create subscription plan successfully")
    void createPlan_WithValidRequest_ShouldReturnCreated() throws Exception {
        // Given
        CreatePlanRequest request = createValidPlanRequest();
        PlanResponse response = createPlanResponse();

        when(planService.createPlan(any(CreatePlanRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/subscription-plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.planCode").value(response.getPlanCode()))
                .andExpect(jsonPath("$.name").value(response.getName()));

        verify(planService).createPlan(any(CreatePlanRequest.class));
    }

    @Test
    @DisplayName("Should update subscription plan successfully")
    void updatePlan_WithValidRequest_ShouldReturnOk() throws Exception {
        // Given
        String planCode = "BASIC_PLAN";
        CreatePlanRequest request = createValidPlanRequest();
        PlanResponse response = createPlanResponse();

        when(planService.updatePlan(eq(planCode), any(CreatePlanRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/subscription-plans/{planCode}", planCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.planCode").value(response.getPlanCode()));

        verify(planService).updatePlan(eq(planCode), any(CreatePlanRequest.class));
    }

    @Test
    @DisplayName("Should get subscription plan successfully")
    void getPlan_WithValidPlanCode_ShouldReturnPlan() throws Exception {
        // Given
        String planCode = "BASIC_PLAN";
        PlanResponse response = createPlanResponse();

        when(planService.getPlan(planCode)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/subscription-plans/{planCode}", planCode))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.planCode").value(response.getPlanCode()))
                .andExpect(jsonPath("$.name").value(response.getName()));

        verify(planService).getPlan(planCode);
    }

    @Test
    @DisplayName("Should get all subscription plans with pagination")
    void getAllPlans_WithPageable_ShouldReturnPagedPlans() throws Exception {
        // Given
        List<PlanResponse> plans = Arrays.asList(createPlanResponse(), createPlanResponse());
        Pageable pageable = PageRequest.of(0, 20);
        Page<PlanResponse> page = new PageImpl<>(plans, pageable, plans.size());

        when(planService.getAllPlans(any(Pageable.class))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/subscription-plans")
                .param("page", "0")
                .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));

        verify(planService).getAllPlans(any(Pageable.class));
    }

    @Test
    @DisplayName("Should get active subscription plans")
    void getActivePlans_ShouldReturnActivePlans() throws Exception {
        // Given
        List<PlanResponse> activePlans = Arrays.asList(createPlanResponse(), createPlanResponse());

        when(planService.getActivePlans()).thenReturn(activePlans);

        // When & Then
        mockMvc.perform(get("/subscription-plans/active"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(planService).getActivePlans();
    }

    @Test
    @DisplayName("Should get plans by billing interval")
    void getPlansByInterval_WithValidInterval_ShouldReturnPlans() throws Exception {
        // Given
        String intervalUnit = "MONTH";
        List<PlanResponse> plans = Arrays.asList(createPlanResponse());

        when(planService.getPlansByInterval(intervalUnit)).thenReturn(plans);

        // When & Then
        mockMvc.perform(get("/subscription-plans/by-interval/{intervalUnit}", intervalUnit))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(planService).getPlansByInterval(intervalUnit);
    }

    @Test
    @DisplayName("Should get plans with trial periods")
    void getPlansWithTrial_ShouldReturnTrialPlans() throws Exception {
        // Given
        List<PlanResponse> trialPlans = Arrays.asList(createPlanResponse());

        when(planService.getPlansWithTrial()).thenReturn(trialPlans);

        // When & Then
        mockMvc.perform(get("/subscription-plans/with-trial"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(planService).getPlansWithTrial();
    }

    @Test
    @DisplayName("Should activate subscription plan successfully")
    void activatePlan_WithValidPlanCode_ShouldReturnActivatedPlan() throws Exception {
        // Given
        String planCode = "BASIC_PLAN";
        PlanResponse response = createPlanResponse();
        response.setIsActive(true);

        when(planService.activatePlan(planCode)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/subscription-plans/{planCode}/activate", planCode))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.planCode").value(response.getPlanCode()))
                .andExpect(jsonPath("$.isActive").value(true));

        verify(planService).activatePlan(planCode);
    }

    @Test
    @DisplayName("Should deactivate subscription plan successfully")
    void deactivatePlan_WithValidPlanCode_ShouldReturnDeactivatedPlan() throws Exception {
        // Given
        String planCode = "BASIC_PLAN";
        PlanResponse response = createPlanResponse();
        response.setIsActive(false);

        when(planService.deactivatePlan(planCode)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/subscription-plans/{planCode}/deactivate", planCode))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.planCode").value(response.getPlanCode()))
                .andExpect(jsonPath("$.isActive").value(false));

        verify(planService).deactivatePlan(planCode);
    }

    @Test
    @DisplayName("Should delete subscription plan successfully")
    void deletePlan_WithValidPlanCode_ShouldReturnNoContent() throws Exception {
        // Given
        String planCode = "BASIC_PLAN";

        doNothing().when(planService).deletePlan(planCode);

        // When & Then
        mockMvc.perform(delete("/subscription-plans/{planCode}", planCode))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("X-Correlation-ID"));

        verify(planService).deletePlan(planCode);
    }

    @Test
    @DisplayName("Should handle service exception during plan creation")
    void createPlan_WithServiceException_ShouldReturnError() throws Exception {
        // Given
        CreatePlanRequest request = createValidPlanRequest();

        when(planService.createPlan(any(CreatePlanRequest.class)))
                .thenThrow(new RuntimeException("Plan creation failed"));

        // When & Then
        mockMvc.perform(post("/subscription-plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());

        verify(planService).createPlan(any(CreatePlanRequest.class));
    }

    @Test
    @DisplayName("Should handle service exception during plan update")
    void updatePlan_WithServiceException_ShouldReturnError() throws Exception {
        // Given
        String planCode = "BASIC_PLAN";
        CreatePlanRequest request = createValidPlanRequest();

        when(planService.updatePlan(eq(planCode), any(CreatePlanRequest.class)))
                .thenThrow(new RuntimeException("Plan update failed"));

        // When & Then
        mockMvc.perform(put("/subscription-plans/{planCode}", planCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());

        verify(planService).updatePlan(eq(planCode), any(CreatePlanRequest.class));
    }

    @Test
    @DisplayName("Should handle service exception during plan retrieval")
    void getPlan_WithServiceException_ShouldReturnError() throws Exception {
        // Given
        String planCode = "BASIC_PLAN";

        when(planService.getPlan(planCode))
                .thenThrow(new RuntimeException("Plan not found"));

        // When & Then
        mockMvc.perform(get("/subscription-plans/{planCode}", planCode))
                .andExpect(status().is5xxServerError());

        verify(planService).getPlan(planCode);
    }

    @Test
    @DisplayName("Should handle service exception during plan activation")
    void activatePlan_WithServiceException_ShouldReturnError() throws Exception {
        // Given
        String planCode = "BASIC_PLAN";

        when(planService.activatePlan(planCode))
                .thenThrow(new RuntimeException("Plan activation failed"));

        // When & Then
        mockMvc.perform(post("/subscription-plans/{planCode}/activate", planCode))
                .andExpect(status().is5xxServerError());

        verify(planService).activatePlan(planCode);
    }

    @Test
    @DisplayName("Should handle service exception during plan deletion")
    void deletePlan_WithServiceException_ShouldReturnError() throws Exception {
        // Given
        String planCode = "BASIC_PLAN";

        doThrow(new RuntimeException("Plan deletion failed"))
                .when(planService).deletePlan(planCode);

        // When & Then
        mockMvc.perform(delete("/subscription-plans/{planCode}", planCode))
                .andExpect(status().is5xxServerError());

        verify(planService).deletePlan(planCode);
    }

    // Helper methods
    private CreatePlanRequest createValidPlanRequest() {
        CreatePlanRequest request = new CreatePlanRequest();
        request.setPlanCode("BASIC_PLAN");
        request.setName("Basic Plan");
        request.setDescription("Basic subscription plan");
        request.setAmount(new BigDecimal("29.99"));
        request.setCurrency("USD"); // Required field - must be 3-letter ISO code
        request.setIntervalCount(1);
        request.setIntervalUnit("MONTH");
        request.setTrialPeriodDays(0);
        // No totalOccurrences field in CreatePlanRequest
        return request;
    }

    private PlanResponse createPlanResponse() {
        PlanResponse response = new PlanResponse();
        response.setPlanCode("BASIC_PLAN");
        response.setName("Basic Plan");
        response.setDescription("Basic subscription plan");
        response.setAmount(new BigDecimal("29.99"));
        response.setCurrency("USD");
        response.setIntervalCount(1);
        response.setIntervalUnit("MONTH");
        response.setTrialPeriodDays(0);
        response.setIsActive(true);
        response.setCreatedAt(java.time.ZonedDateTime.now());
        response.setUpdatedAt(java.time.ZonedDateTime.now());
        return response;
    }
}
