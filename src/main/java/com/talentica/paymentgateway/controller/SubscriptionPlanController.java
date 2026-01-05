package com.talentica.paymentgateway.controller;

import com.talentica.paymentgateway.dto.subscription.CreatePlanRequest;
import com.talentica.paymentgateway.dto.subscription.PlanResponse;
import com.talentica.paymentgateway.service.SubscriptionPlanService;
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

import java.util.List;

/**
 * REST controller for subscription plan management operations.
 * Provides endpoints for creating, updating, and managing subscription plans.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/subscription-plans")
@Tag(name = "Subscription Plans", description = "Subscription plan management operations")
@SecurityRequirement(name = "JWT")
@SecurityRequirement(name = "ApiKey")
public class SubscriptionPlanController {

    private final SubscriptionPlanService planService;

    public SubscriptionPlanController(SubscriptionPlanService planService) {
        this.planService = planService;
    }

    @Operation(summary = "Create a new subscription plan", 
               description = "Creates a new subscription plan with pricing and billing configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Plan created successfully",
                    content = @Content(schema = @Schema(implementation = PlanResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid plan configuration"),
        @ApiResponse(responseCode = "409", description = "Plan code already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Creating subscription plan - Code: {}, Name: {}, CorrelationId: {}", 
                   request.getPlanCode(), request.getName(), correlationId);

        PlanResponse response = planService.createPlan(request);
        
        log.info("Subscription plan created successfully - Code: {}, CorrelationId: {}", 
                   response.getPlanCode(), correlationId);

        return ResponseEntity.status(HttpStatus.CREATED)
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Update a subscription plan", 
               description = "Updates an existing subscription plan configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plan updated successfully",
                    content = @Content(schema = @Schema(implementation = PlanResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid plan configuration"),
        @ApiResponse(responseCode = "404", description = "Plan not found"),
        @ApiResponse(responseCode = "409", description = "Cannot update plan with active subscriptions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{planCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanResponse> updatePlan(
            @Parameter(description = "Plan code") @PathVariable String planCode,
            @Valid @RequestBody CreatePlanRequest request) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Updating subscription plan - Code: {}, CorrelationId: {}", planCode, correlationId);

        PlanResponse response = planService.updatePlan(planCode, request);
        
        log.info("Subscription plan updated successfully - Code: {}, CorrelationId: {}", 
                   planCode, correlationId);

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Get subscription plan details", 
               description = "Retrieves detailed information about a specific subscription plan")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plan details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PlanResponse.class))),
        @ApiResponse(responseCode = "404", description = "Plan not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{planCode}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<PlanResponse> getPlan(
            @Parameter(description = "Plan code") @PathVariable String planCode) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.debug("Retrieving subscription plan - Code: {}, CorrelationId: {}", planCode, correlationId);

        PlanResponse response = planService.getPlan(planCode);

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Get all subscription plans", 
               description = "Retrieves all subscription plans with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plans retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Page<PlanResponse>> getAllPlans(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.debug("Retrieving all subscription plans - CorrelationId: {}", correlationId);

        Page<PlanResponse> response = planService.getAllPlans(pageable);

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Get active subscription plans", 
               description = "Retrieves all active subscription plans")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active plans retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<PlanResponse>> getActivePlans() {
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.debug("Retrieving active subscription plans - CorrelationId: {}", correlationId);

        List<PlanResponse> response = planService.getActivePlans();

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Get plans by billing interval", 
               description = "Retrieves subscription plans filtered by billing interval")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plans retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid interval unit"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/by-interval/{intervalUnit}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<PlanResponse>> getPlansByInterval(
            @Parameter(description = "Interval unit (DAY, WEEK, MONTH, YEAR)") 
            @PathVariable String intervalUnit) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.debug("Retrieving plans by interval - Unit: {}, CorrelationId: {}", 
                    intervalUnit, correlationId);

        List<PlanResponse> response = planService.getPlansByInterval(intervalUnit.toUpperCase());

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Get plans with trial periods", 
               description = "Retrieves subscription plans that offer trial periods")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trial plans retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/with-trial")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<PlanResponse>> getPlansWithTrial() {
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.debug("Retrieving plans with trial periods - CorrelationId: {}", correlationId);

        List<PlanResponse> response = planService.getPlansWithTrial();

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Activate a subscription plan", 
               description = "Activates a subscription plan, making it available for new subscriptions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plan activated successfully",
                    content = @Content(schema = @Schema(implementation = PlanResponse.class))),
        @ApiResponse(responseCode = "404", description = "Plan not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{planCode}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanResponse> activatePlan(
            @Parameter(description = "Plan code") @PathVariable String planCode) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Activating subscription plan - Code: {}, CorrelationId: {}", planCode, correlationId);

        PlanResponse response = planService.activatePlan(planCode);
        
        log.info("Subscription plan activated successfully - Code: {}, CorrelationId: {}", 
                   planCode, correlationId);

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Deactivate a subscription plan", 
               description = "Deactivates a subscription plan, preventing new subscriptions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plan deactivated successfully",
                    content = @Content(schema = @Schema(implementation = PlanResponse.class))),
        @ApiResponse(responseCode = "404", description = "Plan not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{planCode}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanResponse> deactivatePlan(
            @Parameter(description = "Plan code") @PathVariable String planCode) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Deactivating subscription plan - Code: {}, CorrelationId: {}", planCode, correlationId);

        PlanResponse response = planService.deactivatePlan(planCode);
        
        log.info("Subscription plan deactivated successfully - Code: {}, CorrelationId: {}", 
                   planCode, correlationId);

        return ResponseEntity.ok()
                           .header("X-Correlation-ID", correlationId)
                           .body(response);
    }

    @Operation(summary = "Delete a subscription plan", 
               description = "Permanently deletes a subscription plan (only if no subscriptions exist)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Plan deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Plan not found"),
        @ApiResponse(responseCode = "409", description = "Cannot delete plan with existing subscriptions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{planCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePlan(
            @Parameter(description = "Plan code") @PathVariable String planCode) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Deleting subscription plan - Code: {}, CorrelationId: {}", planCode, correlationId);

        planService.deletePlan(planCode);
        
        log.info("Subscription plan deleted successfully - Code: {}, CorrelationId: {}", 
                   planCode, correlationId);

        return ResponseEntity.noContent()
                           .header("X-Correlation-ID", correlationId)
                           .build();
    }
}
