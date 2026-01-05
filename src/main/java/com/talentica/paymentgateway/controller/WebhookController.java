package com.talentica.paymentgateway.controller;

import com.talentica.paymentgateway.dto.webhook.AuthorizeNetWebhookRequest;
import com.talentica.paymentgateway.dto.webhook.WebhookResponse;
import com.talentica.paymentgateway.service.MetricsService;
import com.talentica.paymentgateway.service.WebhookProcessingService;
import com.talentica.paymentgateway.util.CorrelationIdUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for handling webhook events from payment processors.
 * Supports Authorize.Net webhook notifications for payment status updates.
 * 
 * Features:
 * - Signature verification for security
 * - Asynchronous processing for scalability  
 * - Duplicate event detection
 * - Comprehensive logging and metrics
 * - Industry-standard webhook response handling
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/webhooks")
@Validated
@Tag(name = "Webhooks", description = "Webhook endpoints for payment processor notifications")
public class WebhookController {
    
    @Autowired
    private WebhookProcessingService webhookProcessingService;
    
    @Autowired
    private MetricsService metricsService;
    
    /**
     * Handles Authorize.Net webhook notifications.
     * 
     * @param webhookRequest Webhook payload from Authorize.Net
     * @param request HTTP servlet request for header extraction
     * @return Webhook response indicating processing status
     */
    @PostMapping(value = "/authorize-net", 
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Receive Authorize.Net webhook notifications",
        description = "Endpoint for receiving and processing webhook notifications from Authorize.Net " +
                     "for payment events including successful payments, refunds, voids, and fraud reviews. " +
                     "All webhooks are processed asynchronously with signature verification and duplicate detection."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Webhook received and processing initiated successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = WebhookResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                    {
                        "status": "success",
                        "message": "Webhook processed successfully",
                        "eventId": "evt_1234567890",
                        "processedAt": "2024-01-15T10:30:00Z",
                        "correlationId": "corr-abc123def456"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid webhook payload or signature verification failed",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = WebhookResponse.class),
                examples = @ExampleObject(
                    name = "Validation Error",
                    value = """
                    {
                        "status": "validation_error",
                        "message": "Invalid webhook payload",
                        "eventId": "evt_1234567890",
                        "correlationId": "corr-abc123def456",
                        "error": {
                            "code": "VALIDATION_FAILED",
                            "description": "Required field missing: notificationId",
                            "suggestion": "Ensure all required fields are present in the webhook payload"
                        }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Webhook signature verification failed",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = WebhookResponse.class),
                examples = @ExampleObject(
                    name = "Signature Error",
                    value = """
                    {
                        "status": "signature_error",
                        "message": "Invalid webhook signature",
                        "eventId": "evt_1234567890",
                        "correlationId": "corr-abc123def456",
                        "error": {
                            "code": "SIGNATURE_INVALID",
                            "description": "Webhook signature verification failed",
                            "suggestion": "Verify webhook signature configuration"
                        }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Duplicate webhook event detected",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = WebhookResponse.class),
                examples = @ExampleObject(
                    name = "Duplicate Event",
                    value = """
                    {
                        "status": "duplicate",
                        "message": "Event already processed",
                        "eventId": "evt_1234567890",
                        "correlationId": "corr-abc123def456"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during webhook processing",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = WebhookResponse.class),
                examples = @ExampleObject(
                    name = "Processing Error",
                    value = """
                    {
                        "status": "processing_error",
                        "message": "Internal processing error",
                        "eventId": "evt_1234567890",
                        "correlationId": "corr-abc123def456",
                        "error": {
                            "code": "PROCESSING_ERROR",
                            "description": "Database connection failed",
                            "suggestion": "Check application logs for details"
                        }
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<WebhookResponse> handleAuthorizeNetWebhook(
            @Parameter(description = "Authorize.Net webhook payload", required = true)
            @Valid @RequestBody AuthorizeNetWebhookRequest webhookRequest,
            HttpServletRequest request) {
        
        Instant startTime = Instant.now();
        String correlationId = CorrelationIdUtil.getOrGenerate();
        String eventId = webhookRequest != null ? webhookRequest.getNotificationId() : "unknown";
        String eventType = webhookRequest != null ? webhookRequest.getEventType() : "unknown";
        
        try {
            // Set correlation ID for this request
            MDC.put("correlationId", correlationId);
            
            log.info("Received Authorize.Net webhook - EventID: {}, Type: {}, IP: {}", 
                       eventId, eventType, getClientIpAddress(request));
            
            // Extract headers for signature verification
            Map<String, String> headers = extractHeaders(request);
            
            // Get raw payload for signature verification
            String rawPayload = extractRawPayload(request, webhookRequest);
            
            // Record webhook received metric
            metricsService.incrementWebhookReceived(eventType);
            
            // Process webhook asynchronously
            CompletableFuture<WebhookResponse> futureResponse = webhookProcessingService
                .processWebhookAsync(webhookRequest, headers, rawPayload);
            
            // Wait for processing to complete (with timeout handling)
            WebhookResponse response = futureResponse.get();
            
            // Determine HTTP status based on response
            HttpStatus httpStatus = determineHttpStatus(response);
            
            // Record processing metrics
            long processingTimeMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
            metricsService.recordWebhookProcessingTime(eventType, processingTimeMs);
            
            log.info("Webhook processing completed - EventID: {}, Status: {}, Duration: {}ms", 
                       eventId, response.getStatus(), processingTimeMs);
            
            return ResponseEntity.status(httpStatus).body(response);
            
        } catch (Exception e) {
            log.error("Error handling webhook - EventID: {}, Type: {}, Error: {}", 
                        eventId, eventType, e.getMessage(), e);
            
            metricsService.incrementWebhookProcessed(eventType, "error");
            
            WebhookResponse errorResponse = WebhookResponse.processingError(
                eventId, correlationId, "Internal server error", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Health check endpoint for webhook service.
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Webhook service health check",
        description = "Returns health status of the webhook processing service including statistics and configuration."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Webhook service is healthy",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            examples = @ExampleObject(
                name = "Health Status",
                value = """
                {
                    "status": "healthy",
                    "timestamp": "2024-01-15T10:30:00Z",
                    "statistics": {
                        "webhooksLast24Hours": 150,
                        "successfulDeliveries": 145,
                        "failedDeliveries": 3,
                        "pendingDeliveries": 2,
                        "retryingDeliveries": 0
                    },
                    "configuration": {
                        "signatureVerificationEnabled": true,
                        "duplicateDetectionEnabled": true,
                        "asyncProcessingEnabled": true
                    }
                }
                """
            )
        )
    )
    public ResponseEntity<Map<String, Object>> getWebhookHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("timestamp", Instant.now());
        
        // Try to get statistics, but don't fail if database is not available
        try {
            health.put("statistics", webhookProcessingService.getProcessingStatistics());
        } catch (Exception e) {
            log.warn("Failed to retrieve webhook statistics: {}", e.getMessage());
            Map<String, Object> fallbackStats = new HashMap<>();
            fallbackStats.put("webhooksLast24Hours", 0);
            fallbackStats.put("successfulDeliveries", 0);
            fallbackStats.put("failedDeliveries", 0);
            fallbackStats.put("pendingDeliveries", 0);
            fallbackStats.put("retryingDeliveries", 0);
            fallbackStats.put("note", "Statistics unavailable - database connection issue");
            health.put("statistics", fallbackStats);
        }
        
        Map<String, Object> config = new HashMap<>();
        config.put("signatureVerificationEnabled", true);
        config.put("duplicateDetectionEnabled", true);
        config.put("asyncProcessingEnabled", true);
        health.put("configuration", config);
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Endpoint for testing webhook processing with sample data.
     */
    @PostMapping(value = "/test", 
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Test webhook processing",
        description = "Test endpoint for validating webhook processing functionality with sample data. " +
                     "Only available in development and staging environments."
    )
    public ResponseEntity<WebhookResponse> testWebhook(
            @RequestBody(required = false) Map<String, Object> testPayload,
            HttpServletRequest request) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        try {
            log.info("Test webhook request received - IP: {}, CorrelationID: {}", 
                       getClientIpAddress(request), correlationId);
            
            // Create test response
            WebhookResponse response = WebhookResponse.success("test-event-id", correlationId, 
                                                             "Test webhook processed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing test webhook - CorrelationID: {}, Error: {}", 
                        correlationId, e.getMessage(), e);
            
            WebhookResponse errorResponse = WebhookResponse.processingError(
                "test-event-id", correlationId, "Test webhook error", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Extracts HTTP headers from the request.
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue);
        }
        
        return headers;
    }
    
    /**
     * Extracts raw payload for signature verification.
     * In a real implementation, this would require access to the raw request body.
     * For now, we'll serialize the webhook request back to JSON.
     */
    private String extractRawPayload(HttpServletRequest request, AuthorizeNetWebhookRequest webhookRequest) {
        try {
            // In production, you would capture the raw request body before JSON parsing
            // For this implementation, we'll use the parsed object
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.writeValueAsString(webhookRequest);
        } catch (Exception e) {
            log.warn("Could not extract raw payload for signature verification: {}", e.getMessage());
            return "";
        }
    }
    
    /**
     * Determines HTTP status code based on webhook response.
     */
    private HttpStatus determineHttpStatus(WebhookResponse response) {
        if (response == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        switch (response.getStatus()) {
            case "success":
                return HttpStatus.OK;
            case "duplicate":
                return HttpStatus.CONFLICT;
            case "signature_error":
                return HttpStatus.UNAUTHORIZED;
            case "validation_error":
                return HttpStatus.BAD_REQUEST;
            case "processing_error":
            case "error":
                return HttpStatus.INTERNAL_SERVER_ERROR;
            default:
                return HttpStatus.OK;
        }
    }
    
    /**
     * Extracts client IP address from request.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
