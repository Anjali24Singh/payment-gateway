package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.subscription.*;
import com.talentica.paymentgateway.entity.*;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import com.talentica.paymentgateway.repository.*;
import com.talentica.paymentgateway.util.CorrelationIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing subscriptions and subscription lifecycle.
 * Handles subscription creation, updates, cancellation, and billing operations.
 * 
 * Features:
 * - Subscription creation with trial periods
 * - Plan upgrades and downgrades with proration
 * - Subscription pause/resume functionality
 * - Subscription cancellation with different options
 * - Integration with billing engine for recurring charges
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final CustomerRepository customerRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final SubscriptionInvoiceRepository invoiceRepository;
    private final MetricsService metricsService;
    private final ProrationService prorationService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                             SubscriptionPlanRepository planRepository,
                             CustomerRepository customerRepository,
                             PaymentMethodRepository paymentMethodRepository,
                             SubscriptionInvoiceRepository invoiceRepository,
                             MetricsService metricsService,
                             ProrationService prorationService) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.customerRepository = customerRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.invoiceRepository = invoiceRepository;
        this.metricsService = metricsService;
        this.prorationService = prorationService;
    }

    /**
     * Creates a new subscription for a customer.
     * 
     * @param request Subscription creation request
     * @return Created subscription response
     * @throws PaymentProcessingException if subscription creation fails
     */
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Creating subscription - Customer: {}, Plan: {}, CorrelationId: {}", 
                   request.getCustomerId(), request.getPlanCode(), correlationId);

        // Check for idempotency (optimized - only if key provided and not empty)
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty()) {
            log.debug("Checking idempotency for key: {}", request.getIdempotencyKey());
            try {
                Optional<Subscription> existing = subscriptionRepository
                    .findByCustomerIdAndIdempotencyKey(request.getCustomerId(), request.getIdempotencyKey());
                if (existing.isPresent()) {
                    log.info("Returning existing subscription for idempotency key: {}", 
                               request.getIdempotencyKey());
                    return mapToSubscriptionResponse(existing.get());
                }
            } catch (Exception e) {
                log.warn("Idempotency check failed, proceeding with subscription creation: {}", e.getMessage());
                // Continue with creation if idempotency check fails
            }
        }

        // Validate customer exists
        Customer customer = customerRepository.findByCustomerId(request.getCustomerId())
            .orElseThrow(() -> new PaymentProcessingException(
                "Customer not found: " + request.getCustomerId(), "CUSTOMER_NOT_FOUND"));

        // Validate plan exists and is active
        SubscriptionPlan plan = planRepository.findByPlanCode(request.getPlanCode())
            .orElseThrow(() -> new PaymentProcessingException(
                "Plan not found: " + request.getPlanCode(), "PLAN_NOT_FOUND"));

        if (!plan.getIsActive()) {
            throw new PaymentProcessingException(
                "Plan is not active: " + request.getPlanCode(), "PLAN_INACTIVE");
        }

        // Validate payment method exists
        PaymentMethod paymentMethod = paymentMethodRepository.findByPaymentMethodId(request.getPaymentMethodId())
            .orElseThrow(() -> new PaymentProcessingException(
                "Payment method not found: " + request.getPaymentMethodId(), "PAYMENT_METHOD_NOT_FOUND"));

        // Generate subscription ID
        String subscriptionId = generateSubscriptionId();

        // Create subscription entity
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(subscriptionId);
        subscription.setCustomer(customer);
        subscription.setPlan(plan);
        subscription.setPaymentMethod(paymentMethod);
        subscription.setStatus(SubscriptionStatus.PENDING);

        // Set dates
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime startDate = request.getStartDate() != null ? request.getStartDate() : now;
        subscription.setCurrentPeriodStart(startDate);

        // Set billing cycle anchor
        if (request.getBillingCycleAnchor() != null) {
            subscription.setBillingCycleAnchor(request.getBillingCycleAnchor());
        } else {
            subscription.setBillingCycleAnchor(startDate);
        }

        // Handle trial period
        if (request.getStartTrial() && plan.hasTrialPeriod()) {
            subscription.startTrial();
            log.info("Started trial period for subscription: {}, Trial ends: {}", 
                       subscriptionId, subscription.getTrialEnd());
        } else {
            subscription.calculateNextBillingCycle();
        }

        // Set metadata
        if (request.getMetadata() != null) {
            subscription.setMetadata(request.getMetadata());
        }

        // Add idempotency key to metadata
        if (request.getIdempotencyKey() != null) {
            subscription.addMetadata("idempotencyKey", request.getIdempotencyKey());
        }

        // Save subscription
        subscription = subscriptionRepository.save(subscription);

        // Activate subscription
        subscription.activate();
        subscription = subscriptionRepository.save(subscription);

        // Handle setup fee if applicable
        if (plan.getSetupFee() != null && plan.getSetupFee().compareTo(BigDecimal.ZERO) > 0) {
            // Create setup fee invoice
            createSetupFeeInvoice(subscription, plan.getSetupFee());
        }

        // Handle immediate billing if not in trial
        if (!subscription.isInTrial() && request.getProrated()) {
            handleImmediateBilling(subscription, startDate);
        }

        log.info("Subscription created successfully - ID: {}, Status: {}", 
                   subscription.getSubscriptionId(), subscription.getStatus());

        // Record metrics
        metricsService.recordSubscriptionCreated(plan.getPlanCode());

        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Updates an existing subscription.
     * 
     * @param subscriptionId Subscription identifier
     * @param request Update request
     * @return Updated subscription response
     * @throws PaymentProcessingException if update fails
     */
    public SubscriptionResponse updateSubscription(String subscriptionId, UpdateSubscriptionRequest request) {
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Updating subscription - ID: {}, CorrelationId: {}", subscriptionId, correlationId);

        Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
            .orElseThrow(() -> new PaymentProcessingException(
                "Subscription not found: " + subscriptionId, "SUBSCRIPTION_NOT_FOUND"));

        if (!subscription.isActive()) {
            throw new PaymentProcessingException(
                "Cannot update inactive subscription: " + subscriptionId, "SUBSCRIPTION_INACTIVE");
        }

        boolean hasChanges = false;

        // Handle plan change
        if (request.isPlanChange()) {
            SubscriptionPlan newPlan = planRepository.findByPlanCode(request.getPlanCode())
                .orElseThrow(() -> new PaymentProcessingException(
                    "Plan not found: " + request.getPlanCode(), "PLAN_NOT_FOUND"));

            if (!newPlan.getIsActive()) {
                throw new PaymentProcessingException(
                    "Plan is not active: " + request.getPlanCode(), "PLAN_INACTIVE");
            }

            handlePlanChange(subscription, newPlan, request);
            hasChanges = true;
        }

        // Handle payment method change
        if (request.isPaymentMethodChange()) {
            PaymentMethod newPaymentMethod = paymentMethodRepository
                .findByPaymentMethodId(request.getPaymentMethodId())
                .orElseThrow(() -> new PaymentProcessingException(
                    "Payment method not found: " + request.getPaymentMethodId(), "PAYMENT_METHOD_NOT_FOUND"));

            subscription.setPaymentMethod(newPaymentMethod);
            hasChanges = true;
        }

        // Handle billing cycle anchor change
        if (request.getBillingCycleAnchor() != null) {
            subscription.setBillingCycleAnchor(request.getBillingCycleAnchor());
            subscription.calculateNextBillingCycle();
            hasChanges = true;
        }

        // Update metadata
        if (request.getMetadata() != null) {
            if (subscription.getMetadata() == null) {
                subscription.setMetadata(request.getMetadata());
            } else {
                subscription.getMetadata().putAll(request.getMetadata());
            }
            hasChanges = true;
        }

        if (hasChanges) {
            subscription = subscriptionRepository.save(subscription);
            log.info("Subscription updated successfully - ID: {}", subscriptionId);
        }

        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Cancels a subscription.
     * 
     * @param subscriptionId Subscription identifier
     * @param request Cancellation request
     * @return Updated subscription response
     * @throws PaymentProcessingException if cancellation fails
     */
    public SubscriptionResponse cancelSubscription(String subscriptionId, CancelSubscriptionRequest request) {
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Cancelling subscription - ID: {}, When: {}, CorrelationId: {}", 
                   subscriptionId, request.getWhen(), correlationId);

        Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
            .orElseThrow(() -> new PaymentProcessingException(
                "Subscription not found: " + subscriptionId, "SUBSCRIPTION_NOT_FOUND"));

        if (subscription.isCancelled()) {
            log.warn("Subscription already cancelled: {}", subscriptionId);
            return mapToSubscriptionResponse(subscription);
        }

        if (request.isImmediateCancellation()) {
            // Immediate cancellation
            subscription.cancel(request.getReason());
            
            // Handle prorated refund if requested
            if (request.getRefundProrated() && subscription.getCurrentPeriodEnd() != null) {
                handleProratedRefund(subscription);
            }
        } else {
            // End-of-period cancellation
            ZonedDateTime cancelAt = request.getCancelAt() != null ? 
                request.getCancelAt() : subscription.getCurrentPeriodEnd();
            
            subscription.addMetadata("scheduledCancellation", cancelAt);
            subscription.addMetadata("cancellationReason", request.getReason());
            subscription.addMetadata("cancelAtPeriodEnd", true);
        }

        if (request.getNotes() != null) {
            subscription.addMetadata("cancellationNotes", request.getNotes());
        }

        subscription = subscriptionRepository.save(subscription);

        log.info("Subscription cancellation processed - ID: {}, Status: {}", 
                   subscriptionId, subscription.getStatus());

        // Record metrics
        metricsService.recordSubscriptionCancelled(subscription.getPlan().getPlanCode(), request.getReason());

        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Pauses a subscription.
     * 
     * @param subscriptionId Subscription identifier
     * @return Updated subscription response
     */
    public SubscriptionResponse pauseSubscription(String subscriptionId) {
        log.info("Pausing subscription: {}", subscriptionId);

        Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
            .orElseThrow(() -> new PaymentProcessingException(
                "Subscription not found: " + subscriptionId, "SUBSCRIPTION_NOT_FOUND"));

        if (!subscription.isActive()) {
            throw new PaymentProcessingException(
                "Cannot pause inactive subscription: " + subscriptionId, "SUBSCRIPTION_INACTIVE");
        }

        subscription.pause();
        subscription.addMetadata("pausedAt", ZonedDateTime.now());
        subscription = subscriptionRepository.save(subscription);

        log.info("Subscription paused successfully: {}", subscriptionId);
        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Resumes a paused subscription.
     * 
     * @param subscriptionId Subscription identifier
     * @return Updated subscription response
     */
    public SubscriptionResponse resumeSubscription(String subscriptionId) {
        log.info("Resuming subscription: {}", subscriptionId);

        Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
            .orElseThrow(() -> new PaymentProcessingException(
                "Subscription not found: " + subscriptionId, "SUBSCRIPTION_NOT_FOUND"));

        if (subscription.getStatus() != SubscriptionStatus.PAUSED) {
            throw new PaymentProcessingException(
                "Cannot resume non-paused subscription: " + subscriptionId, "SUBSCRIPTION_NOT_PAUSED");
        }

        subscription.activate();
        subscription.calculateNextBillingCycle();
        subscription.addMetadata("resumedAt", ZonedDateTime.now());
        subscription = subscriptionRepository.save(subscription);

        log.info("Subscription resumed successfully: {}", subscriptionId);
        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Retrieves a subscription by ID.
     * 
     * @param subscriptionId Subscription identifier
     * @return Subscription response
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(String subscriptionId) {
        Subscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
            .orElseThrow(() -> new PaymentProcessingException(
                "Subscription not found: " + subscriptionId, "SUBSCRIPTION_NOT_FOUND"));

        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Retrieves subscriptions for a customer.
     * 
     * @param customerId Customer identifier
     * @param pageable Pagination information
     * @return Page of subscription responses
     */
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getCustomerSubscriptions(String customerId, Pageable pageable) {
        Customer customer = customerRepository.findByCustomerId(customerId)
            .orElseThrow(() -> new PaymentProcessingException(
                "Customer not found: " + customerId, "CUSTOMER_NOT_FOUND"));

        Page<Subscription> subscriptions = subscriptionRepository.findByCustomer(customer, pageable);
        return subscriptions.map(this::mapToSubscriptionResponse);
    }

    /**
     * Retrieves subscriptions due for billing.
     * 
     * @return List of subscriptions due for billing
     */
    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsDueForBilling() {
        return subscriptionRepository.findSubscriptionsDueForBilling(ZonedDateTime.now());
    }

    // Private helper methods

    private void handlePlanChange(Subscription subscription, SubscriptionPlan newPlan, UpdateSubscriptionRequest request) {
        SubscriptionPlan currentPlan = subscription.getPlan();
        
        log.info("Changing subscription plan - ID: {}, From: {}, To: {}", 
                   subscription.getSubscriptionId(), currentPlan.getPlanCode(), newPlan.getPlanCode());

        // Calculate proration if requested
        if (request.getProrated()) {
            ProrationCalculation proration = prorationService.calculateProration(
                subscription, newPlan, ZonedDateTime.now());
            
            if (proration.getProrationApplies() && proration.hasAmount()) {
                handleProrationAdjustment(subscription, proration);
            }
        }

        // Update subscription
        subscription.setPlan(newPlan);
        
        if ("IMMEDIATE".equals(request.getChangeOption())) {
            subscription.calculateNextBillingCycle();
        } else {
            // Schedule change for end of period
            subscription.addMetadata("scheduledPlanChange", newPlan.getPlanCode());
            subscription.addMetadata("planChangeDate", subscription.getCurrentPeriodEnd());
        }

        if (request.getChangeReason() != null) {
            subscription.addMetadata("planChangeReason", request.getChangeReason());
        }

        // Record metrics
        metricsService.recordPlanChange(currentPlan.getPlanCode(), newPlan.getPlanCode());
    }

    private void handleProrationAdjustment(Subscription subscription, ProrationCalculation proration) {
        if (proration.isCharge()) {
            // Create invoice for additional amount
            createProrationInvoice(subscription, proration.getNetAmount(), "Plan upgrade proration");
        } else if (proration.isCredit()) {
            // Create credit for unused amount
            createProrationCredit(subscription, proration.getNetAmount().abs(), "Plan downgrade credit");
        }
    }

    private void createSetupFeeInvoice(Subscription subscription, BigDecimal setupFee) {
        String invoiceNumber = generateInvoiceNumber("SETUP");
        
        SubscriptionInvoice invoice = new SubscriptionInvoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setSubscription(subscription);
        invoice.setCustomer(subscription.getCustomer());
        invoice.setAmount(setupFee);
        invoice.setCurrency(subscription.getPlan().getCurrency());
        invoice.setStatus("PENDING");
        invoice.setPeriodStart(ZonedDateTime.now());
        invoice.setPeriodEnd(ZonedDateTime.now());
        invoice.setDueDate(ZonedDateTime.now().plusDays(1)); // Due immediately
        invoice.addMetadata("type", "setup_fee");
        
        invoiceRepository.save(invoice);
        subscription.addInvoice(invoice);
        
        log.info("Created setup fee invoice: {} for subscription: {}", 
                   invoiceNumber, subscription.getSubscriptionId());
    }

    private void createProrationInvoice(Subscription subscription, BigDecimal amount, String description) {
        String invoiceNumber = generateInvoiceNumber("PRORATE");
        
        SubscriptionInvoice invoice = new SubscriptionInvoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setSubscription(subscription);
        invoice.setCustomer(subscription.getCustomer());
        invoice.setAmount(amount);
        invoice.setCurrency(subscription.getPlan().getCurrency());
        invoice.setStatus("PENDING");
        invoice.setPeriodStart(subscription.getCurrentPeriodStart());
        invoice.setPeriodEnd(subscription.getCurrentPeriodEnd());
        invoice.setDueDate(ZonedDateTime.now().plusDays(1));
        invoice.addMetadata("type", "proration_charge");
        invoice.addMetadata("description", description);
        
        invoiceRepository.save(invoice);
        subscription.addInvoice(invoice);
        
        log.info("Created proration invoice: {} for subscription: {}", 
                   invoiceNumber, subscription.getSubscriptionId());
    }

    private void createProrationCredit(Subscription subscription, BigDecimal amount, String description) {
        // In a real implementation, this would create a credit note or adjustment
        subscription.addMetadata("proratedCredit", amount);
        subscription.addMetadata("proratedCreditDescription", description);
        subscription.addMetadata("proratedCreditDate", ZonedDateTime.now());
        
        log.info("Created proration credit: {} for subscription: {}", 
                   amount, subscription.getSubscriptionId());
    }

    private void handleImmediateBilling(Subscription subscription, ZonedDateTime startDate) {
        // Create immediate billing invoice for first period
        String invoiceNumber = generateInvoiceNumber("BILL");
        
        SubscriptionInvoice invoice = new SubscriptionInvoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setSubscription(subscription);
        invoice.setCustomer(subscription.getCustomer());
        invoice.setAmount(subscription.getPlan().getAmount());
        invoice.setCurrency(subscription.getPlan().getCurrency());
        invoice.setStatus("PENDING");
        invoice.setPeriodStart(subscription.getCurrentPeriodStart());
        invoice.setPeriodEnd(subscription.getCurrentPeriodEnd());
        invoice.setDueDate(ZonedDateTime.now().plusDays(1));
        invoice.addMetadata("type", "subscription_billing");
        
        invoiceRepository.save(invoice);
        subscription.addInvoice(invoice);
        
        log.info("Created immediate billing invoice: {} for subscription: {}", 
                   invoiceNumber, subscription.getSubscriptionId());
    }

    private void handleProratedRefund(Subscription subscription) {
        // Calculate unused portion of current period
        ZonedDateTime now = ZonedDateTime.now();
        if (subscription.getCurrentPeriodEnd() != null && now.isBefore(subscription.getCurrentPeriodEnd())) {
            ProrationCalculation proration = prorationService.calculateRefundProration(subscription, now);
            
            if (proration.hasAmount()) {
                createProrationCredit(subscription, proration.getNetAmount(), "Cancellation refund");
            }
        }
    }

    private SubscriptionResponse mapToSubscriptionResponse(Subscription subscription) {
        SubscriptionResponse response = new SubscriptionResponse();
        
        // Basic subscription info
        response.setSubscriptionId(subscription.getSubscriptionId());
        response.setStatus(subscription.getStatus());
        response.setCreatedAt(subscription.getCreatedAt().atZone(java.time.ZoneId.systemDefault()));
        response.setUpdatedAt(subscription.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()));
        
        // Customer info
        if (subscription.getCustomer() != null) {
            response.setCustomerId(subscription.getCustomer().getCustomerId());
            response.setCustomerName(subscription.getCustomer().getFirstName() + " " + 
                                   subscription.getCustomer().getLastName());
            response.setCustomerEmail(subscription.getCustomer().getEmail());
        }
        
        // Plan info
        if (subscription.getPlan() != null) {
            response.setPlanCode(subscription.getPlan().getPlanCode());
            response.setPlanName(subscription.getPlan().getName());
            response.setPlanAmount(subscription.getPlan().getAmount());
            response.setCurrency(subscription.getPlan().getCurrency());
            response.setIntervalUnit(subscription.getPlan().getIntervalUnit());
            response.setIntervalCount(subscription.getPlan().getIntervalCount());
        }
        
        // Payment method info
        if (subscription.getPaymentMethod() != null) {
            response.setPaymentMethodId(subscription.getPaymentMethod().getPaymentMethodId());
            response.setPaymentMethodType(subscription.getPaymentMethod().getType());
            response.setPaymentMethodLast4(subscription.getPaymentMethod().getLast4());
        }
        
        // Billing dates
        response.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        response.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        response.setTrialStart(subscription.getTrialStart());
        response.setTrialEnd(subscription.getTrialEnd());
        response.setNextBillingDate(subscription.getNextBillingDate());
        response.setBillingCycleAnchor(subscription.getBillingCycleAnchor());
        
        // Cancellation info
        response.setCancelledAt(subscription.getCancelledAt());
        response.setCancellationReason(subscription.getCancellationReason());
        
        // Calculated fields
        response.setDaysUntilNextBilling(subscription.getDaysUntilNextBilling());
        response.setInTrialPeriod(subscription.isInTrial());
        response.setTrialExpired(subscription.hasTrialExpired());
        
        // Invoice info
        SubscriptionInvoice latestInvoice = subscription.getLatestInvoice();
        if (latestInvoice != null) {
            response.setLatestInvoiceId(latestInvoice.getInvoiceNumber());
        }
        response.setUnpaidInvoicesCount(subscription.getUnpaidInvoices().size());
        
        // Metadata
        response.setMetadata(subscription.getMetadata());
        
        // Pending changes
        response.setHasPendingChanges(subscription.getMetadata() != null && 
            (subscription.getMetadata().containsKey("scheduledPlanChange") ||
             subscription.getMetadata().containsKey("scheduledCancellation")));
        
        if (subscription.getMetadata() != null && subscription.getMetadata().containsKey("planChangeDate")) {
            response.setPendingChangesDate((ZonedDateTime) subscription.getMetadata().get("planChangeDate"));
        }
        
        return response;
    }

    private String generateSubscriptionId() {
        return "sub_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private String generateInvoiceNumber(String type) {
        return type + "_" + System.currentTimeMillis() + "_" + 
               UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
