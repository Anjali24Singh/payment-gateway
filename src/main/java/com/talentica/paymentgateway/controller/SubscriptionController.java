package com.talentica.paymentgateway.controller;

import com.talentica.paymentgateway.dto.subscription.*;
import com.talentica.paymentgateway.service.SubscriptionService;
import com.talentica.paymentgateway.util.CorrelationIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for subscription management operations.
 * Provides endpoints for subscription lifecycle management including
 * creation, updates, cancellation, pause/resume, and retrieval.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/subscriptions")
@Tag(name = "Subscriptions", description = "Subscription management operations")
@SecurityRequirement(name = "JWT")
@SecurityRequirement(name = "ApiKey")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Operation(summary = "List all subscriptions", 
               description = "Retrieves a paginated list of all subscriptions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscriptions retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Page<SubscriptionResponse>> getAllSubscriptions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Retrieving all subscriptions - CorrelationId: {}", correlationId);

        // For now, return empty page - full implementation would call subscriptionService.getAllSubscriptions(pageable)
        Page<SubscriptionResponse> subscriptions = Page.empty(pageable);
        
        log.info("Retrieved {} subscriptions - CorrelationId: {}", 
                   subscriptions.getTotalElements(), correlationId);

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(subscriptions);
    }

    @Operation(summary = "Create a new subscription", 
               description = "Creates a new subscription for a customer with specified plan and payment method")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Subscription created successfully",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "404", description = "Customer, plan, or payment method not found"),
        @ApiResponse(responseCode = "409", description = "Duplicate subscription (idempotency)"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Creating subscription - Customer: {}, Plan: {}, CorrelationId: {}", 
                   request.getCustomerId(), request.getPlanCode(), correlationId);

        SubscriptionResponse response = subscriptionService.createSubscription(request);
        
        log.info("Subscription created successfully - ID: {}, CorrelationId: {}", 
                   response.getSubscriptionId(), correlationId);

        return ResponseEntity.status(HttpStatus.CREATED)
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Update a subscription", 
               description = "Updates an existing subscription with plan changes, payment method updates, etc.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription updated successfully",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "404", description = "Subscription, plan, or payment method not found"),
        @ApiResponse(responseCode = "409", description = "Cannot update inactive subscription"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{subscriptionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @Parameter(description = "Subscription ID") @PathVariable String subscriptionId,
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Updating subscription - ID: {}, CorrelationId: {}", subscriptionId, correlationId);

        SubscriptionResponse response = subscriptionService.updateSubscription(subscriptionId, request);
        
        log.info("Subscription updated successfully - ID: {}, CorrelationId: {}", 
                   subscriptionId, correlationId);

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Cancel a subscription", 
               description = "Cancels a subscription either immediately or at the end of the current period")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription cancelled successfully",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid cancellation request"),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "409", description = "Subscription already cancelled"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{subscriptionId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @Parameter(description = "Subscription ID") @PathVariable String subscriptionId,
            @Valid @RequestBody CancelSubscriptionRequest request) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Cancelling subscription - ID: {}, When: {}, CorrelationId: {}", 
                   subscriptionId, request.getWhen(), correlationId);

        SubscriptionResponse response = subscriptionService.cancelSubscription(subscriptionId, request);
        
        log.info("Subscription cancelled successfully - ID: {}, CorrelationId: {}", 
                   subscriptionId, correlationId);

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Pause a subscription", 
               description = "Temporarily pauses an active subscription")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription paused successfully",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "409", description = "Cannot pause inactive subscription"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{subscriptionId}/pause")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<SubscriptionResponse> pauseSubscription(
            @Parameter(description = "Subscription ID") @PathVariable String subscriptionId) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Pausing subscription - ID: {}, CorrelationId: {}", subscriptionId, correlationId);

        SubscriptionResponse response = subscriptionService.pauseSubscription(subscriptionId);
        
        log.info("Subscription paused successfully - ID: {}, CorrelationId: {}", 
                   subscriptionId, correlationId);

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Resume a paused subscription", 
               description = "Resumes a previously paused subscription")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription resumed successfully",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "409", description = "Cannot resume non-paused subscription"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{subscriptionId}/resume")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<SubscriptionResponse> resumeSubscription(
            @Parameter(description = "Subscription ID") @PathVariable String subscriptionId) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Resuming subscription - ID: {}, CorrelationId: {}", subscriptionId, correlationId);

        SubscriptionResponse response = subscriptionService.resumeSubscription(subscriptionId);
        
        log.info("Subscription resumed successfully - ID: {}, CorrelationId: {}", 
                   subscriptionId, correlationId);

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Get subscription details", 
               description = "Retrieves detailed information about a specific subscription")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{subscriptionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<SubscriptionResponse> getSubscription(
            @Parameter(description = "Subscription ID") @PathVariable String subscriptionId) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.debug("Retrieving subscription - ID: {}, CorrelationId: {}", subscriptionId, correlationId);

        SubscriptionResponse response = subscriptionService.getSubscription(subscriptionId);

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Get customer subscriptions", 
               description = "Retrieves all subscriptions for a specific customer with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer subscriptions retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Customer not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Page<SubscriptionResponse>> getCustomerSubscriptions(
            @Parameter(description = "Customer ID") @PathVariable String customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.debug("Retrieving customer subscriptions - Customer: {}, CorrelationId: {}", 
                    customerId, correlationId);

        Page<SubscriptionResponse> response = subscriptionService.getCustomerSubscriptions(customerId, pageable);

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Calculate proration for subscription change", 
               description = "Calculates proration amount for a potential subscription plan change")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Proration calculated successfully",
                    content = @Content(schema = @Schema(implementation = ProrationCalculation.class))),
        @ApiResponse(responseCode = "404", description = "Subscription or plan not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{subscriptionId}/calculate-proration")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ProrationCalculation> calculateProration(
            @Parameter(description = "Subscription ID") @PathVariable String subscriptionId,
            @Parameter(description = "New plan code") @RequestParam String newPlanCode,
            @Parameter(description = "Change date (optional, defaults to now)") 
            @RequestParam(required = false) String changeDate) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.debug("Calculating proration - Subscription: {}, New Plan: {}, CorrelationId: {}", 
                    subscriptionId, newPlanCode, correlationId);

        // This would require implementing the calculation in the service
        // For now, return a placeholder response
        ProrationCalculation response = new ProrationCalculation("Proration calculation not yet implemented");

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Preview subscription change", 
               description = "Previews the effects of a subscription change without applying it")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Change preview generated successfully"),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{subscriptionId}/preview-change")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Object> previewSubscriptionChange(
            @Parameter(description = "Subscription ID") @PathVariable String subscriptionId,
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.debug("Previewing subscription change - ID: {}, CorrelationId: {}", 
                    subscriptionId, correlationId);

        // This would require implementing preview logic in the service
        // For now, return a placeholder response
        Object response = new Object() {
            public String message = "Change preview not yet implemented";
        };

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }
}
