package com.talentica.paymentgateway.controller;

import com.talentica.paymentgateway.dto.ARBSubscriptionsResponse;
import com.talentica.paymentgateway.dto.subscription.ARBSubscriptionRequest;
import com.talentica.paymentgateway.dto.subscription.ARBSubscriptionResponse;
import com.talentica.paymentgateway.entity.Customer;
import com.talentica.paymentgateway.entity.PaymentMethod;
import com.talentica.paymentgateway.entity.SubscriptionPlan;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import com.talentica.paymentgateway.repository.CustomerRepository;
import com.talentica.paymentgateway.repository.PaymentMethodRepository;
import com.talentica.paymentgateway.repository.SubscriptionPlanRepository;
import com.talentica.paymentgateway.service.AuthorizeNetARBService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import net.authorize.api.contract.v1.ARBGetSubscriptionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST Controller for Authorize.Net ARB (Automatic Recurring Billing) subscriptions.
 * These subscriptions appear directly in the Authorize.Net merchant portal.
 */
@Slf4j
@RestController
@RequestMapping("/arb-subscriptions")
@Tag(name = "ARB Subscriptions", description = "Authorize.Net ARB subscription management")
public class ARBSubscriptionController {

    private final AuthorizeNetARBService arbService;
    private final CustomerRepository customerRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public ARBSubscriptionController(
            AuthorizeNetARBService arbService,
            CustomerRepository customerRepository,
            SubscriptionPlanRepository planRepository,
            PaymentMethodRepository paymentMethodRepository) {
        this.arbService = arbService;
        this.customerRepository = customerRepository;
        this.planRepository = planRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    /**
     * Lists all ARB subscriptions (placeholder for future implementation).
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "List ARB subscriptions", 
               description = "Lists all ARB subscriptions from Authorize.Net")
    public ResponseEntity<ARBSubscriptionsResponse> listARBSubscriptions() {
        log.info("Listing ARB subscriptions");
        
        // For now, return a placeholder response
        // In a full implementation, this would query Authorize.Net for all subscriptions
        ARBSubscriptionsResponse response = ARBSubscriptionsResponse.builder()
                .message("ARB subscriptions list endpoint - implementation pending")
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Creates an ARB subscription that appears in Authorize.Net portal.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Create ARB subscription", 
               description = "Creates an Authorize.Net ARB subscription that appears in the merchant portal")
    public ResponseEntity<ARBSubscriptionResponse> createARBSubscription(@Valid @RequestBody ARBSubscriptionRequest request) {
        log.info("Creating ARB subscription for customer: {} with plan: {}", 
                   request.getCustomerId(), request.getPlanCode());

        try {
            // Validate customer exists
            Customer customer = customerRepository.findByCustomerId(request.getCustomerId())
                .orElseThrow(() -> new PaymentProcessingException(
                    "Customer not found: " + request.getCustomerId(), "CUSTOMER_NOT_FOUND"));

            // Validate plan exists
            SubscriptionPlan plan = planRepository.findByPlanCode(request.getPlanCode())
                .orElseThrow(() -> new PaymentProcessingException(
                    "Plan not found: " + request.getPlanCode(), "PLAN_NOT_FOUND"));

            // Validate payment method exists
            PaymentMethod paymentMethod = paymentMethodRepository.findByPaymentMethodId(request.getPaymentMethodId())
                .orElseThrow(() -> new PaymentProcessingException(
                    "Payment method not found: " + request.getPaymentMethodId(), "PAYMENT_METHOD_NOT_FOUND"));

            // Create ARB subscription
            String arbSubscriptionId = arbService.createARBSubscription(customer, plan, paymentMethod);

            // Create response
            ARBSubscriptionResponse response = new ARBSubscriptionResponse();
            response.setArbSubscriptionId(arbSubscriptionId);
            response.setCustomerId(customer.getCustomerId());
            response.setCustomerName(customer.getFirstName() + " " + customer.getLastName());
            response.setCustomerEmail(customer.getEmail());
            response.setPlanCode(plan.getPlanCode());
            response.setPlanName(plan.getName());
            response.setPlanAmount(plan.getAmount());
            response.setCurrency(plan.getCurrency());
            response.setIntervalUnit(plan.getIntervalUnit().toString());
            response.setIntervalCount(plan.getIntervalCount());
            response.setStatus("ACTIVE");
            response.setMessage("ARB subscription created successfully and is visible in Authorize.Net portal");

            log.info("✅ ARB subscription created successfully - ARB ID: {}, Customer: {}", 
                       arbSubscriptionId, customer.getCustomerId());

            return ResponseEntity.ok(response);

        } catch (PaymentProcessingException e) {
            log.error("❌ ARB subscription creation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error creating ARB subscription", e);
            throw new PaymentProcessingException("Failed to create ARB subscription: " + e.getMessage(), "ARB_CREATION_ERROR");
        }
    }

    /**
     * Gets ARB subscription details from Authorize.Net.
     */
    @GetMapping("/{arbSubscriptionId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Get ARB subscription", 
               description = "Gets ARB subscription details from Authorize.Net")
    public ResponseEntity<ARBGetSubscriptionResponse> getARBSubscription(@PathVariable String arbSubscriptionId) {
        log.info("Getting ARB subscription details: {}", arbSubscriptionId);

        try {
            ARBGetSubscriptionResponse response = arbService.getARBSubscription(arbSubscriptionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Failed to get ARB subscription: {}", arbSubscriptionId, e);
            throw new PaymentProcessingException("Failed to get ARB subscription: " + e.getMessage(), "ARB_GET_ERROR");
        }
    }

    /**
     * Cancels an ARB subscription in Authorize.Net.
     */
    @DeleteMapping("/{arbSubscriptionId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Cancel ARB subscription", 
               description = "Cancels ARB subscription in Authorize.Net portal")
    public ResponseEntity<String> cancelARBSubscription(@PathVariable String arbSubscriptionId) {
        log.info("Cancelling ARB subscription: {}", arbSubscriptionId);

        try {
            boolean cancelled = arbService.cancelARBSubscription(arbSubscriptionId);
            
            if (cancelled) {
                return ResponseEntity.ok("ARB subscription cancelled successfully");
            } else {
                throw new PaymentProcessingException("Failed to cancel ARB subscription", "ARB_CANCEL_FAILED");
            }

        } catch (Exception e) {
            log.error("❌ Failed to cancel ARB subscription: {}", arbSubscriptionId, e);
            throw new PaymentProcessingException("Failed to cancel ARB subscription: " + e.getMessage(), "ARB_CANCEL_ERROR");
        }
    }
}
