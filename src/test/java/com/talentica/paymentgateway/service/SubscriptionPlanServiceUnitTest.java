package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.subscription.CreatePlanRequest;
import com.talentica.paymentgateway.dto.subscription.PlanResponse;
import com.talentica.paymentgateway.entity.SubscriptionPlan;
import com.talentica.paymentgateway.entity.SubscriptionStatus;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import com.talentica.paymentgateway.repository.SubscriptionPlanRepository;
import com.talentica.paymentgateway.repository.SubscriptionRepository;
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

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for SubscriptionPlanService.
 * 
 * Tests cover:
 * - Plan creation with validation
 * - Plan updates and lifecycle management
 * - Plan retrieval and filtering
 * - Plan activation/deactivation
 * - Plan deletion with business rules
 * - Error handling and edge cases
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionPlanServiceUnitTest {

    @Mock
    private SubscriptionPlanRepository planRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private SubscriptionPlanService subscriptionPlanService;

    private CreatePlanRequest validPlanRequest;
    private SubscriptionPlan samplePlan;

    @BeforeEach
    void setUp() {
        // Create valid plan request
        validPlanRequest = new CreatePlanRequest();
        validPlanRequest.setPlanCode("premium_monthly");
        validPlanRequest.setName("Premium Monthly Plan");
        validPlanRequest.setDescription("Premium features with monthly billing");
        validPlanRequest.setAmount(new BigDecimal("29.99"));
        validPlanRequest.setCurrency("USD");
        validPlanRequest.setIntervalUnit("MONTH");
        validPlanRequest.setIntervalCount(1);
        validPlanRequest.setTrialPeriodDays(14);
        validPlanRequest.setIsActive(true);
        validPlanRequest.setSetupFee(new BigDecimal("5.00"));
        validPlanRequest.setMaxSubscribers(1000);
        validPlanRequest.setCategory("Premium");
        validPlanRequest.setSortOrder(1);
        validPlanRequest.setFeatures(Arrays.asList("Feature 1", "Feature 2", "Feature 3"));

        // Create sample plan entity
        samplePlan = new SubscriptionPlan();
        samplePlan.setPlanCode("premium_monthly");
        samplePlan.setName("Premium Monthly Plan");
        samplePlan.setDescription("Premium features with monthly billing");
        samplePlan.setAmount(new BigDecimal("29.99"));
        samplePlan.setCurrency("USD");
        samplePlan.setIntervalUnit("MONTH");
        samplePlan.setIntervalCount(1);
        samplePlan.setTrialPeriodDays(14);
        samplePlan.setIsActive(true);
        samplePlan.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
        samplePlan.setUpdatedAt(ZonedDateTime.now().toLocalDateTime());
        samplePlan.setMetadata(new HashMap<>());
    }

    @Test
    void createPlan_WithValidRequest_ShouldCreatePlan() {
        // Given
        when(planRepository.existsByPlanCode(validPlanRequest.getPlanCode())).thenReturn(false);
        when(planRepository.save(any(SubscriptionPlan.class))).thenReturn(samplePlan);
        when(subscriptionRepository.countByPlanAndStatus(any(SubscriptionPlan.class), anyString())).thenReturn(0L);
        when(subscriptionRepository.countByPlan(any(SubscriptionPlan.class))).thenReturn(0L);

        // When
        PlanResponse response = subscriptionPlanService.createPlan(validPlanRequest);

        // Then
        assertNotNull(response);
        assertEquals("premium_monthly", response.getPlanCode());
        assertEquals("Premium Monthly Plan", response.getName());
        assertEquals(new BigDecimal("29.99"), response.getAmount());
        assertEquals("USD", response.getCurrency());
        assertEquals("MONTH", response.getIntervalUnit());
        assertEquals(1, response.getIntervalCount());
        assertEquals(14, response.getTrialPeriodDays());
        assertTrue(response.getIsActive());

        verify(planRepository).existsByPlanCode("premium_monthly");
        verify(planRepository).save(any(SubscriptionPlan.class));
        verify(metricsService).recordPlanCreated("premium_monthly", new BigDecimal("29.99"));
    }

    @Test
    void createPlan_WithDuplicatePlanCode_ShouldThrowException() {
        // Given
        when(planRepository.existsByPlanCode(validPlanRequest.getPlanCode())).thenReturn(true);

        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
            () -> subscriptionPlanService.createPlan(validPlanRequest));

        assertEquals("Plan code already exists: premium_monthly", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());

        verify(planRepository).existsByPlanCode("premium_monthly");
        verify(planRepository, never()).save(any(SubscriptionPlan.class));
        verify(metricsService, never()).recordPlanCreated(anyString(), any(BigDecimal.class));
    }

    @Test
    void createPlan_WithAmountTooLow_ShouldThrowException() {
        // Given
        validPlanRequest.setAmount(new BigDecimal("0.25"));
        when(planRepository.existsByPlanCode(validPlanRequest.getPlanCode())).thenReturn(false);

        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
            () -> subscriptionPlanService.createPlan(validPlanRequest));

        assertEquals("Plan amount cannot be less than $0.50", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void createPlan_WithAmountTooHigh_ShouldThrowException() {
        // Given
        validPlanRequest.setAmount(new BigDecimal("150000.00"));
        when(planRepository.existsByPlanCode(validPlanRequest.getPlanCode())).thenReturn(false);

        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
            () -> subscriptionPlanService.createPlan(validPlanRequest));

        assertEquals("Plan amount cannot exceed $100,000", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void createPlan_WithInvalidDailyInterval_ShouldThrowException() {
        // Given
        validPlanRequest.setIntervalUnit("DAY");
        validPlanRequest.setIntervalCount(400);
        when(planRepository.existsByPlanCode(validPlanRequest.getPlanCode())).thenReturn(false);

        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
            () -> subscriptionPlanService.createPlan(validPlanRequest));

        assertEquals("Daily interval cannot exceed 365 days", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void createPlan_WithInvalidWeeklyInterval_ShouldThrowException() {
        // Given
        validPlanRequest.setIntervalUnit("WEEK");
        validPlanRequest.setIntervalCount(60);
        when(planRepository.existsByPlanCode(validPlanRequest.getPlanCode())).thenReturn(false);

        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
            () -> subscriptionPlanService.createPlan(validPlanRequest));

        assertEquals("Weekly interval cannot exceed 52 weeks", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void createPlan_WithInvalidMonthlyInterval_ShouldThrowException() {
        // Given
        validPlanRequest.setIntervalUnit("MONTH");
        validPlanRequest.setIntervalCount(15);
        when(planRepository.existsByPlanCode(validPlanRequest.getPlanCode())).thenReturn(false);

        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
            () -> subscriptionPlanService.createPlan(validPlanRequest));

        assertEquals("Monthly interval cannot exceed 12 months", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void createPlan_WithInvalidYearlyInterval_ShouldThrowException() {
        // Given
        validPlanRequest.setIntervalUnit("YEAR");
        validPlanRequest.setIntervalCount(10);
        when(planRepository.existsByPlanCode(validPlanRequest.getPlanCode())).thenReturn(false);

        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
            () -> subscriptionPlanService.createPlan(validPlanRequest));

        assertEquals("Yearly interval cannot exceed 5 years", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void updatePlan_WithValidRequest_ShouldUpdatePlan() {
        // Given
        CreatePlanRequest updateRequest = new CreatePlanRequest();
        updateRequest.setName("Updated Premium Plan");
        updateRequest.setDescription("Updated description");
        updateRequest.setIsActive(false);

        when(planRepository.findByPlanCode("premium_monthly")).thenReturn(Optional.of(samplePlan));
        when(subscriptionRepository.countByPlanAndStatus(samplePlan, SubscriptionStatus.ACTIVE.name())).thenReturn(0L);
        when(planRepository.save(any(SubscriptionPlan.class))).thenReturn(samplePlan);
        when(subscriptionRepository.countByPlan(any(SubscriptionPlan.class))).thenReturn(0L);

        // When
        PlanResponse response = subscriptionPlanService.updatePlan("premium_monthly", updateRequest);

        // Then
        assertNotNull(response);
        verify(planRepository).findByPlanCode("premium_monthly");
        verify(planRepository).save(any(SubscriptionPlan.class));
    }

    @Test
    void updatePlan_WithNonExistentPlan_ShouldThrowException() {
        // Given
        CreatePlanRequest updateRequest = new CreatePlanRequest();
        updateRequest.setName("Updated Plan");

        when(planRepository.findByPlanCode("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
            () -> subscriptionPlanService.updatePlan("nonexistent", updateRequest));

        assertEquals("Plan not found: nonexistent", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void updatePlan_WithActiveSubscriptionsAndIntervalChange_ShouldThrowException() {
        // Given
        CreatePlanRequest updateRequest = new CreatePlanRequest();
        updateRequest.setIntervalUnit("YEAR");

        when(planRepository.findByPlanCode("premium_monthly")).thenReturn(Optional.of(samplePlan));
        when(subscriptionRepository.countByPlanAndStatus(samplePlan, SubscriptionStatus.ACTIVE.name())).thenReturn(5L);

        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
            () -> subscriptionPlanService.updatePlan("premium_monthly", updateRequest));

        assertEquals("Cannot change interval unit for plan with active subscriptions", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void getPlan_WithValidPlanCode_ShouldReturnPlan() {
        // Given
        when(planRepository.findByPlanCode("premium_monthly")).thenReturn(Optional.of(samplePlan));
        when(subscriptionRepository.countByPlanAndStatus(any(SubscriptionPlan.class), anyString())).thenReturn(5L);
        when(subscriptionRepository.countByPlan(any(SubscriptionPlan.class))).thenReturn(10L);

        // When
        PlanResponse response = subscriptionPlanService.getPlan("premium_monthly");

        // Then
        assertNotNull(response);
        assertEquals("premium_monthly", response.getPlanCode());
        assertEquals("Premium Monthly Plan", response.getName());
        verify(planRepository).findByPlanCode("premium_monthly");
    }

    @Test
    void getPlan_WithNonExistentPlan_ShouldThrowException() {
        // Given
        when(planRepository.findByPlanCode("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
            () -> subscriptionPlanService.getPlan("nonexistent"));

        assertEquals("Plan not found: nonexistent", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void getAllPlans_ShouldReturnPagedPlans() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<SubscriptionPlan> plans = Arrays.asList(samplePlan);
        Page<SubscriptionPlan> planPage = new PageImpl<>(plans, pageable, 1);

        when(planRepository.findAll(pageable)).thenReturn(planPage);
        when(subscriptionRepository.countByPlanAndStatus(any(SubscriptionPlan.class), anyString())).thenReturn(0L);
        when(subscriptionRepository.countByPlan(any(SubscriptionPlan.class))).thenReturn(0L);

        // When
        Page<PlanResponse> response = subscriptionPlanService.getAllPlans(pageable);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertEquals("premium_monthly", response.getContent().get(0).getPlanCode());
        verify(planRepository).findAll(pageable);
    }

    @Test
    void getActivePlans_ShouldReturnActivePlansOnly() {
        // Given
        List<SubscriptionPlan> activePlans = Arrays.asList(samplePlan);
        when(planRepository.findByIsActiveTrue()).thenReturn(activePlans);
        when(subscriptionRepository.countByPlanAndStatus(any(SubscriptionPlan.class), anyString())).thenReturn(0L);
        when(subscriptionRepository.countByPlan(any(SubscriptionPlan.class))).thenReturn(0L);

        // When
        List<PlanResponse> response = subscriptionPlanService.getActivePlans();

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("premium_monthly", response.get(0).getPlanCode());
        assertTrue(response.get(0).getIsActive());
        verify(planRepository).findByIsActiveTrue();
    }

    @Test
    void getPlansByInterval_ShouldReturnPlansWithSpecificInterval() {
        // Given
        List<SubscriptionPlan> monthlyPlans = Arrays.asList(samplePlan);
        when(planRepository.findByIntervalUnit("MONTH")).thenReturn(monthlyPlans);
        when(subscriptionRepository.countByPlanAndStatus(any(SubscriptionPlan.class), anyString())).thenReturn(0L);
        when(subscriptionRepository.countByPlan(any(SubscriptionPlan.class))).thenReturn(0L);

        // When
        List<PlanResponse> response = subscriptionPlanService.getPlansByInterval("MONTH");

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("MONTH", response.get(0).getIntervalUnit());
        verify(planRepository).findByIntervalUnit("MONTH");
    }

    @Test
    void getPlansWithTrial_ShouldReturnPlansWithTrialPeriod() {
        // Given
        List<SubscriptionPlan> trialPlans = Arrays.asList(samplePlan);
        when(planRepository.findByTrialPeriodDaysGreaterThan(0)).thenReturn(trialPlans);
        when(subscriptionRepository.countByPlanAndStatus(any(SubscriptionPlan.class), anyString())).thenReturn(0L);
        when(subscriptionRepository.countByPlan(any(SubscriptionPlan.class))).thenReturn(0L);

        // When
        List<PlanResponse> response = subscriptionPlanService.getPlansWithTrial();

        // Then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertTrue(response.get(0).getTrialPeriodDays() > 0);
        verify(planRepository).findByTrialPeriodDaysGreaterThan(0);
    }

    @Test
    void activatePlan_WithValidPlan_ShouldActivatePlan() {
        // Given
        samplePlan.setIsActive(false);
        when(planRepository.findByPlanCode("premium_monthly")).thenReturn(Optional.of(samplePlan));
        when(planRepository.save(any(SubscriptionPlan.class))).thenReturn(samplePlan);
        when(subscriptionRepository.countByPlanAndStatus(any(SubscriptionPlan.class), anyString())).thenReturn(0L);
        when(subscriptionRepository.countByPlan(any(SubscriptionPlan.class))).thenReturn(0L);

        // When
        PlanResponse response = subscriptionPlanService.activatePlan("premium_monthly");

        // Then
        assertNotNull(response);
        verify(planRepository).findByPlanCode("premium_monthly");
        verify(planRepository).save(any(SubscriptionPlan.class));
    }

    @Test
    void activatePlan_WithNonExistentPlan_ShouldThrowException() {
        // Given
        when(planRepository.findByPlanCode("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
            () -> subscriptionPlanService.activatePlan("nonexistent"));

        assertEquals("Plan not found: nonexistent", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void deactivatePlan_WithValidPlan_ShouldDeactivatePlan() {
        // Given
        when(planRepository.findByPlanCode("premium_monthly")).thenReturn(Optional.of(samplePlan));
        when(subscriptionRepository.countByPlanAndStatus(samplePlan, SubscriptionStatus.ACTIVE.name())).thenReturn(0L);
        when(planRepository.save(any(SubscriptionPlan.class))).thenReturn(samplePlan);
        when(subscriptionRepository.countByPlan(any(SubscriptionPlan.class))).thenReturn(0L);

        // When
        PlanResponse response = subscriptionPlanService.deactivatePlan("premium_monthly");

        // Then
        assertNotNull(response);
        verify(planRepository).findByPlanCode("premium_monthly");
        verify(subscriptionRepository, atLeastOnce()).countByPlanAndStatus(samplePlan, SubscriptionStatus.ACTIVE.name());
        verify(planRepository).save(any(SubscriptionPlan.class));
    }

    @Test
    void deactivatePlan_WithActiveSubscriptions_ShouldStillDeactivate() {
        // Given
        when(planRepository.findByPlanCode("premium_monthly")).thenReturn(Optional.of(samplePlan));
        when(subscriptionRepository.countByPlanAndStatus(samplePlan, SubscriptionStatus.ACTIVE.name())).thenReturn(5L);
        when(planRepository.save(any(SubscriptionPlan.class))).thenReturn(samplePlan);
        when(subscriptionRepository.countByPlan(any(SubscriptionPlan.class))).thenReturn(5L);

        // When
        PlanResponse response = subscriptionPlanService.deactivatePlan("premium_monthly");

        // Then
        assertNotNull(response);
        verify(planRepository).save(any(SubscriptionPlan.class));
        // Should log warning but still proceed with deactivation
    }

    @Test
    void deletePlan_WithNoPlanSubscriptions_ShouldDeletePlan() {
        // Given
        when(planRepository.findByPlanCode("premium_monthly")).thenReturn(Optional.of(samplePlan));
        when(subscriptionRepository.countByPlan(samplePlan)).thenReturn(0L);

        // When
        subscriptionPlanService.deletePlan("premium_monthly");

        // Then
        verify(planRepository).findByPlanCode("premium_monthly");
        verify(subscriptionRepository).countByPlan(samplePlan);
        verify(planRepository).delete(samplePlan);
    }

    @Test
    void deletePlan_WithExistingSubscriptions_ShouldThrowException() {
        // Given
        when(planRepository.findByPlanCode("premium_monthly")).thenReturn(Optional.of(samplePlan));
        when(subscriptionRepository.countByPlan(samplePlan)).thenReturn(5L);

        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
            () -> subscriptionPlanService.deletePlan("premium_monthly"));

        assertEquals("Cannot delete plan with existing subscriptions: premium_monthly", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());

        verify(planRepository).findByPlanCode("premium_monthly");
        verify(subscriptionRepository).countByPlan(samplePlan);
        verify(planRepository, never()).delete(any(SubscriptionPlan.class));
    }

    @Test
    void deletePlan_WithNonExistentPlan_ShouldThrowException() {
        // Given
        when(planRepository.findByPlanCode("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
            () -> subscriptionPlanService.deletePlan("nonexistent"));

        assertEquals("Plan not found: nonexistent", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void createPlan_WithMetadataAndFeatures_ShouldSetAllFields() {
        // Given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("customField", "customValue");
        validPlanRequest.setMetadata(metadata);

        when(planRepository.existsByPlanCode(validPlanRequest.getPlanCode())).thenReturn(false);
        when(planRepository.save(any(SubscriptionPlan.class))).thenReturn(samplePlan);
        when(subscriptionRepository.countByPlanAndStatus(any(SubscriptionPlan.class), anyString())).thenReturn(0L);
        when(subscriptionRepository.countByPlan(any(SubscriptionPlan.class))).thenReturn(0L);

        // When
        PlanResponse response = subscriptionPlanService.createPlan(validPlanRequest);

        // Then
        assertNotNull(response);
        verify(planRepository).save(argThat(plan -> {
            // Verify that metadata fields are properly set
            return plan.getMetadata() != null &&
                   plan.getMetadata().containsKey("features") &&
                   plan.getMetadata().containsKey("setupFee") &&
                   plan.getMetadata().containsKey("maxSubscribers") &&
                   plan.getMetadata().containsKey("category") &&
                   plan.getMetadata().containsKey("sortOrder");
        }));
    }

    @Test
    void updatePlan_WithPriceChange_ShouldHandlePriceChangeCorrectly() {
        // Given
        CreatePlanRequest updateRequest = new CreatePlanRequest();
        updateRequest.setAmount(new BigDecimal("39.99"));

        when(planRepository.findByPlanCode("premium_monthly")).thenReturn(Optional.of(samplePlan));
        when(subscriptionRepository.countByPlanAndStatus(samplePlan, SubscriptionStatus.ACTIVE.name())).thenReturn(0L);
        when(planRepository.save(any(SubscriptionPlan.class))).thenReturn(samplePlan);
        when(subscriptionRepository.countByPlan(any(SubscriptionPlan.class))).thenReturn(0L);

        // When
        PlanResponse response = subscriptionPlanService.updatePlan("premium_monthly", updateRequest);

        // Then
        assertNotNull(response);
        verify(planRepository).save(argThat(plan -> {
            // Verify that price change history is added to metadata
            return plan.getMetadata() != null &&
                   plan.getMetadata().containsKey("priceChangeHistory");
        }));
    }
}
