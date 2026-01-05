package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.subscription.CreatePlanRequest;
import com.talentica.paymentgateway.dto.subscription.PlanResponse;
import com.talentica.paymentgateway.entity.SubscriptionPlan;
import com.talentica.paymentgateway.entity.SubscriptionStatus;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import com.talentica.paymentgateway.repository.SubscriptionPlanRepository;
import com.talentica.paymentgateway.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for managing subscription plans.
 * Handles plan creation, updates, activation/deactivation, and analytics.
 * 
 * Features:
 * - Plan lifecycle management
 * - Plan validation and business rules
 * - Usage statistics and analytics
 * - Plan comparison and recommendations
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
@Transactional
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MetricsService metricsService;

    public SubscriptionPlanService(SubscriptionPlanRepository planRepository,
                                 SubscriptionRepository subscriptionRepository,
                                 MetricsService metricsService) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.metricsService = metricsService;
    }

    /**
     * Creates a new subscription plan.
     * 
     * @param request Plan creation request
     * @return Created plan response
     * @throws PaymentProcessingException if plan creation fails
     */
    public PlanResponse createPlan(CreatePlanRequest request) {
        log.info("Creating subscription plan: {}", request.getPlanCode());

        // Validate plan code uniqueness
        if (planRepository.existsByPlanCode(request.getPlanCode())) {
            throw new PaymentProcessingException(
                "Plan code already exists: " + request.getPlanCode(), "PLAN_CODE_EXISTS");
        }

        // Validate business rules
        validatePlanRequest(request);

        // Create plan entity
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setPlanCode(request.getPlanCode());
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setAmount(request.getAmount());
        plan.setCurrency(request.getCurrency());
        plan.setIntervalUnit(request.getIntervalUnit());
        plan.setIntervalCount(request.getIntervalCount());
        plan.setTrialPeriodDays(request.getTrialPeriodDays());
        plan.setIsActive(request.getIsActive());

        // Set metadata
        if (request.getMetadata() != null) {
            plan.setMetadata(request.getMetadata());
        }

        // Add additional fields
        if (request.getFeatures() != null) {
            plan.addMetadata("features", request.getFeatures());
        }
        if (request.getSetupFee() != null) {
            plan.addMetadata("setupFee", request.getSetupFee());
        }
        if (request.getMaxSubscribers() != null) {
            plan.addMetadata("maxSubscribers", request.getMaxSubscribers());
        }
        if (request.getCategory() != null) {
            plan.addMetadata("category", request.getCategory());
        }
        if (request.getSortOrder() != null) {
            plan.addMetadata("sortOrder", request.getSortOrder());
        }

        // Save plan
        plan = planRepository.save(plan);

        log.info("Subscription plan created successfully: {}", plan.getPlanCode());

        // Record metrics
        metricsService.recordPlanCreated(plan.getPlanCode(), plan.getAmount());

        return mapToPlanResponse(plan);
    }

    /**
     * Updates an existing subscription plan.
     * 
     * @param planCode Plan code to update
     * @param request Update request
     * @return Updated plan response
     */
    public PlanResponse updatePlan(String planCode, CreatePlanRequest request) {
        log.info("Updating subscription plan: {}", planCode);

        SubscriptionPlan plan = planRepository.findByPlanCode(planCode)
            .orElseThrow(() -> new PaymentProcessingException(
                "Plan not found: " + planCode, "PLAN_NOT_FOUND"));

        // Validate that we can update this plan
        if (hasActiveSubscriptions(plan)) {
            log.warn("Updating plan with active subscriptions: {}", planCode);
            // Only allow certain updates for plans with active subscriptions
            validateUpdateForActivePlan(request, plan);
        }

        // Update fields
        if (request.getName() != null) {
            plan.setName(request.getName());
        }
        if (request.getDescription() != null) {
            plan.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            plan.setIsActive(request.getIsActive());
        }

        // Amount changes require special handling
        if (request.getAmount() != null && !request.getAmount().equals(plan.getAmount())) {
            handlePriceChange(plan, request.getAmount());
        }

        // Update metadata
        if (request.getMetadata() != null) {
            if (plan.getMetadata() == null) {
                plan.setMetadata(request.getMetadata());
            } else {
                plan.getMetadata().putAll(request.getMetadata());
            }
        }

        plan = planRepository.save(plan);

        log.info("Subscription plan updated successfully: {}", planCode);

        return mapToPlanResponse(plan);
    }

    /**
     * Retrieves a subscription plan by code.
     * 
     * @param planCode Plan code
     * @return Plan response
     */
    @Transactional(readOnly = true)
    public PlanResponse getPlan(String planCode) {
        SubscriptionPlan plan = planRepository.findByPlanCode(planCode)
            .orElseThrow(() -> new PaymentProcessingException(
                "Plan not found: " + planCode, "PLAN_NOT_FOUND"));

        return mapToPlanResponse(plan);
    }

    /**
     * Retrieves all subscription plans.
     * 
     * @param pageable Pagination information
     * @return Page of plan responses
     */
    @Transactional(readOnly = true)
    public Page<PlanResponse> getAllPlans(Pageable pageable) {
        Page<SubscriptionPlan> plans = planRepository.findAll(pageable);
        return plans.map(this::mapToPlanResponse);
    }

    /**
     * Retrieves active subscription plans.
     * 
     * @return List of active plan responses
     */
    @Transactional(readOnly = true)
    public List<PlanResponse> getActivePlans() {
        List<SubscriptionPlan> plans = planRepository.findByIsActiveTrue();
        return plans.stream()
                   .map(this::mapToPlanResponse)
                   .collect(Collectors.toList());
    }

    /**
     * Retrieves plans by interval.
     * 
     * @param intervalUnit Interval unit (DAY, WEEK, MONTH, YEAR)
     * @return List of plan responses
     */
    @Transactional(readOnly = true)
    public List<PlanResponse> getPlansByInterval(String intervalUnit) {
        List<SubscriptionPlan> plans = planRepository.findByIntervalUnit(intervalUnit);
        return plans.stream()
                   .map(this::mapToPlanResponse)
                   .collect(Collectors.toList());
    }

    /**
     * Retrieves plans with trial periods.
     * 
     * @return List of plan responses with trial periods
     */
    @Transactional(readOnly = true)
    public List<PlanResponse> getPlansWithTrial() {
        List<SubscriptionPlan> plans = planRepository.findByTrialPeriodDaysGreaterThan(0);
        return plans.stream()
                   .map(this::mapToPlanResponse)
                   .collect(Collectors.toList());
    }

    /**
     * Activates a subscription plan.
     * 
     * @param planCode Plan code to activate
     * @return Updated plan response
     */
    public PlanResponse activatePlan(String planCode) {
        log.info("Activating subscription plan: {}", planCode);

        SubscriptionPlan plan = planRepository.findByPlanCode(planCode)
            .orElseThrow(() -> new PaymentProcessingException(
                "Plan not found: " + planCode, "PLAN_NOT_FOUND"));

        plan.setIsActive(true);
        plan = planRepository.save(plan);

        log.info("Subscription plan activated: {}", planCode);

        return mapToPlanResponse(plan);
    }

    /**
     * Deactivates a subscription plan.
     * 
     * @param planCode Plan code to deactivate
     * @return Updated plan response
     */
    public PlanResponse deactivatePlan(String planCode) {
        log.info("Deactivating subscription plan: {}", planCode);

        SubscriptionPlan plan = planRepository.findByPlanCode(planCode)
            .orElseThrow(() -> new PaymentProcessingException(
                "Plan not found: " + planCode, "PLAN_NOT_FOUND"));

        // Check if plan has active subscriptions
        long activeSubscriptions = subscriptionRepository.countByPlanAndStatus(plan, SubscriptionStatus.ACTIVE.name());
        if (activeSubscriptions > 0) {
            log.warn("Deactivating plan with {} active subscriptions: {}", activeSubscriptions, planCode);
            // Allow deactivation but log warning
        }

        plan.setIsActive(false);
        plan = planRepository.save(plan);

        log.info("Subscription plan deactivated: {}", planCode);

        return mapToPlanResponse(plan);
    }

    /**
     * Deletes a subscription plan.
     * 
     * @param planCode Plan code to delete
     * @throws PaymentProcessingException if plan cannot be deleted
     */
    public void deletePlan(String planCode) {
        log.info("Deleting subscription plan: {}", planCode);

        SubscriptionPlan plan = planRepository.findByPlanCode(planCode)
            .orElseThrow(() -> new PaymentProcessingException(
                "Plan not found: " + planCode, "PLAN_NOT_FOUND"));

        // Check if plan has any subscriptions
        long totalSubscriptions = subscriptionRepository.countByPlan(plan);
        if (totalSubscriptions > 0) {
            throw new PaymentProcessingException(
                "Cannot delete plan with existing subscriptions: " + planCode, "PLAN_HAS_SUBSCRIPTIONS");
        }

        planRepository.delete(plan);

        log.info("Subscription plan deleted: {}", planCode);
    }

    // Private helper methods

    private void validatePlanRequest(CreatePlanRequest request) {
        // Validate interval combination
        if ("DAY".equals(request.getIntervalUnit()) && request.getIntervalCount() > 365) {
            throw new PaymentProcessingException(
                "Daily interval cannot exceed 365 days", "INVALID_INTERVAL");
        }
        if ("WEEK".equals(request.getIntervalUnit()) && request.getIntervalCount() > 52) {
            throw new PaymentProcessingException(
                "Weekly interval cannot exceed 52 weeks", "INVALID_INTERVAL");
        }
        if ("MONTH".equals(request.getIntervalUnit()) && request.getIntervalCount() > 12) {
            throw new PaymentProcessingException(
                "Monthly interval cannot exceed 12 months", "INVALID_INTERVAL");
        }
        if ("YEAR".equals(request.getIntervalUnit()) && request.getIntervalCount() > 5) {
            throw new PaymentProcessingException(
                "Yearly interval cannot exceed 5 years", "INVALID_INTERVAL");
        }

        // Validate minimum amounts
        BigDecimal minAmount = new BigDecimal("0.50");
        if (request.getAmount().compareTo(minAmount) < 0) {
            throw new PaymentProcessingException(
                "Plan amount cannot be less than $0.50", "AMOUNT_TOO_LOW");
        }

        // Validate maximum amounts
        BigDecimal maxAmount = new BigDecimal("100000.00");
        if (request.getAmount().compareTo(maxAmount) > 0) {
            throw new PaymentProcessingException(
                "Plan amount cannot exceed $100,000", "AMOUNT_TOO_HIGH");
        }
    }

    private void validateUpdateForActivePlan(CreatePlanRequest request, SubscriptionPlan existingPlan) {
        // Don't allow critical changes for plans with active subscriptions
        if (request.getAmount() != null && !request.getAmount().equals(existingPlan.getAmount())) {
            log.warn("Price change attempted for plan with active subscriptions: {}", existingPlan.getPlanCode());
            // Allow price changes but log warning - they should be handled carefully
        }

        if (request.getIntervalUnit() != null && !request.getIntervalUnit().equals(existingPlan.getIntervalUnit())) {
            throw new PaymentProcessingException(
                "Cannot change interval unit for plan with active subscriptions", "CANNOT_CHANGE_INTERVAL");
        }

        if (request.getIntervalCount() != null && !request.getIntervalCount().equals(existingPlan.getIntervalCount())) {
            throw new PaymentProcessingException(
                "Cannot change interval count for plan with active subscriptions", "CANNOT_CHANGE_INTERVAL");
        }
    }

    private void handlePriceChange(SubscriptionPlan plan, BigDecimal newAmount) {
        BigDecimal oldAmount = plan.getAmount();
        plan.setAmount(newAmount);
        
        // Add price change to metadata for audit trail
        plan.addMetadata("priceChangeHistory", String.format(
            "Changed from $%.2f to $%.2f on %s", 
            oldAmount, newAmount, java.time.ZonedDateTime.now()
        ));

        log.info("Price changed for plan {}: ${} -> ${}", 
                   plan.getPlanCode(), oldAmount, newAmount);
    }

    private boolean hasActiveSubscriptions(SubscriptionPlan plan) {
        return subscriptionRepository.countByPlanAndStatus(plan, SubscriptionStatus.ACTIVE.name()) > 0;
    }

    private PlanResponse mapToPlanResponse(SubscriptionPlan plan) {
        PlanResponse response = new PlanResponse();
        
        // Basic plan info
        response.setPlanCode(plan.getPlanCode());
        response.setName(plan.getName());
        response.setDescription(plan.getDescription());
        response.setAmount(plan.getAmount());
        response.setCurrency(plan.getCurrency());
        response.setIntervalUnit(plan.getIntervalUnit());
        response.setIntervalCount(plan.getIntervalCount());
        response.setTrialPeriodDays(plan.getTrialPeriodDays());
        response.setIsActive(plan.getIsActive());
        response.setCreatedAt(plan.getCreatedAt().atZone(java.time.ZoneId.systemDefault()));
        response.setUpdatedAt(plan.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()));
        response.setMetadata(plan.getMetadata());

        // Extract metadata fields with safe casting
        if (plan.getMetadata() != null) {
            response.setFeatures((List<String>) plan.getMetadata().get("features"));
            
            // Safe cast for BigDecimal fields
            Object setupFeeObj = plan.getMetadata().get("setupFee");
            if (setupFeeObj instanceof BigDecimal) {
                response.setSetupFee((BigDecimal) setupFeeObj);
            } else if (setupFeeObj instanceof Number) {
                response.setSetupFee(new BigDecimal(setupFeeObj.toString()));
            } else if (setupFeeObj != null) {
                response.setSetupFee(new BigDecimal(setupFeeObj.toString()));
            }
            
            // Safe cast for Integer fields
            Object maxSubsObj = plan.getMetadata().get("maxSubscribers");
            if (maxSubsObj instanceof Integer) {
                response.setMaxSubscribers((Integer) maxSubsObj);
            } else if (maxSubsObj instanceof Number) {
                response.setMaxSubscribers(((Number) maxSubsObj).intValue());
            } else if (maxSubsObj != null) {
                try {
                    response.setMaxSubscribers(Integer.parseInt(maxSubsObj.toString()));
                } catch (NumberFormatException e) {
                    response.setMaxSubscribers(0);
                }
            }
            
            response.setCategory((String) plan.getMetadata().get("category"));
            
            // Safe cast for sort order
            Object sortOrderObj = plan.getMetadata().get("sortOrder");
            if (sortOrderObj instanceof Integer) {
                response.setSortOrder((Integer) sortOrderObj);
            } else if (sortOrderObj instanceof Number) {
                response.setSortOrder(((Number) sortOrderObj).intValue());
            } else if (sortOrderObj != null) {
                try {
                    response.setSortOrder(Integer.parseInt(sortOrderObj.toString()));
                } catch (NumberFormatException e) {
                    response.setSortOrder(0);
                }
            }
        }

        // Calculated fields
        response.setFormattedInterval(plan.getFormattedInterval());
        response.setFormattedPrice(plan.getFormattedPrice());
        response.setDisplayName(plan.getDisplayName());
        response.setHasTrialPeriod(plan.hasTrialPeriod());
        response.setHasSetupFee(response.getSetupFee() != null && 
                              response.getSetupFee().compareTo(BigDecimal.ZERO) > 0);

        // Usage statistics
        long activeSubscriptions = subscriptionRepository.countByPlanAndStatus(plan, SubscriptionStatus.ACTIVE.name());
        long totalSubscriptions = subscriptionRepository.countByPlan(plan);
        
        response.setActiveSubscriptions(activeSubscriptions);
        response.setTotalSubscriptions(totalSubscriptions);
        response.setMonthlyRecurringRevenue(plan.getTotalMonthlyRevenue());
        
        // Check subscriber limit
        if (response.getMaxSubscribers() != null && response.getMaxSubscribers() > 0) {
            response.setAtSubscriberLimit(activeSubscriptions >= response.getMaxSubscribers());
        } else {
            response.setAtSubscriberLimit(false);
        }

        return response;
    }
}
