package com.talentica.paymentgateway.controller;

import com.talentica.paymentgateway.dto.payment.*;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import com.talentica.paymentgateway.service.PaymentService;
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
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import com.talentica.paymentgateway.service.RequestTrackingService;
import com.talentica.paymentgateway.service.MetricsService;

/**
 * REST Controller for payment processing operations.
 * Provides endpoints for all payment-related functionality including:
 * - Purchase transactions (auth + capture)
 * - Authorization-only transactions
 * - Capture of authorized transactions
 * - Void/cancel transactions
 * - Refund transactions (full and partial)
 * - Transaction status inquiry
 * 
 * All endpoints support idempotency and include comprehensive error handling.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/payments")
@Tag(name = "Payment Processing", description = "Core payment processing operations")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    private final PaymentService paymentService;
    private final MetricsService metricsService;
    private final RequestTrackingService requestTrackingService;

    public PaymentController(PaymentService paymentService, MetricsService metricsService, RequestTrackingService requestTrackingService) {
        this.paymentService = paymentService;
        this.metricsService = metricsService;
        this.requestTrackingService = requestTrackingService;
    }

    /**
     * Process a direct purchase transaction (auth + capture in one step).
     * This is the most common payment operation for immediate fund capture.
     */
    @PostMapping("/purchase")
    @Operation(
        summary = "Process a purchase transaction",
        description = "Processes a direct purchase transaction (authorization + capture in one step). " +
                     "This immediately charges the customer's payment method and is suitable for most e-commerce transactions."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchase processed successfully",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = PaymentErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing API key",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = PaymentErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Payment processing failed",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = PaymentErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = PaymentErrorResponse.class)))
    })
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> processPurchase(@Valid @RequestBody PurchaseRequest request) {
        
        // Correlation ID and Idempotency Key are automatically handled by RequestTrackingFilter
        String correlationId = requestTrackingService.getCurrentCorrelationId();
        String idempotencyKey = requestTrackingService.getCurrentIdempotencyKey();
        
        // Set idempotency key in request if auto-generated
        if (idempotencyKey != null && request.getIdempotencyKey() == null) {
            request.setIdempotencyKey(idempotencyKey);
        }
        MDC.put("correlationId", correlationId);

        try {
            log.info("Processing purchase request - Amount: {}, Currency: {}, CorrelationId: {}", 
                       request.getAmount(), request.getCurrency(), correlationId);

            PaymentResponse response = paymentService.processPurchase(request);
            
            log.info("Purchase completed successfully - TransactionId: {}, Status: {}", 
                       response.getTransactionId(), response.getStatus());

            return ResponseEntity.ok(response);

        } catch (PaymentProcessingException e) {
            log.error("Purchase failed - Error: {}, CorrelationId: {}", e.getMessage(), correlationId);
            
            PaymentErrorResponse errorResponse = new PaymentErrorResponse(
                "PAYMENT_FAILED", e.getMessage(), e.getMessage(), "PAYMENT_ERROR", correlationId);
            
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);

        } catch (Exception e) {
            log.error("Unexpected error during purchase - Error: {}, CorrelationId: {}", 
                        e.getMessage(), correlationId, e);
            
            PaymentErrorResponse errorResponse = new PaymentErrorResponse(
                "INTERNAL_ERROR", "An unexpected error occurred", "An unexpected error occurred", "SYSTEM_ERROR", correlationId);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Process an authorization-only transaction.
     * This holds funds on the customer's payment method without capturing them.
     */
    @PostMapping("/authorize")
    @Operation(
        summary = "Process an authorization transaction",
        description = "Processes an authorization-only transaction that holds funds without capturing them. " +
                     "Useful for pre-orders or when final amount needs to be confirmed before capture."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authorization processed successfully",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "422", description = "Authorization failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<?> processAuthorization(
            @Valid @RequestBody AuthorizeRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        if (correlationId == null) {
            correlationId = "corr-" + UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put("correlationId", correlationId);

        try {
            log.info("Processing authorization request - Amount: {}, Currency: {}, CorrelationId: {}", 
                       request.getAmount(), request.getCurrency(), correlationId);

            PaymentResponse response = paymentService.processAuthorization(request);
            
            log.info("Authorization completed successfully - TransactionId: {}, Status: {}", 
                       response.getTransactionId(), response.getStatus());

            return ResponseEntity.ok(response);

        } catch (PaymentProcessingException e) {
            log.error("Authorization failed - Error: {}, CorrelationId: {}", e.getMessage(), correlationId);
            
            PaymentErrorResponse errorResponse = new PaymentErrorResponse(
                "AUTHORIZATION_FAILED", e.getMessage(), e.getMessage(), "AUTHORIZATION_ERROR", correlationId);
            
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);

        } catch (Exception e) {
            log.error("Unexpected error during authorization - Error: {}, CorrelationId: {}", 
                        e.getMessage(), correlationId, e);
            
            PaymentErrorResponse errorResponse = new PaymentErrorResponse(
                "INTERNAL_ERROR", "An unexpected error occurred", "An unexpected error occurred", "SYSTEM_ERROR", correlationId);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Capture a previously authorized transaction.
     * This completes the payment process by actually charging the customer.
     */
    @PostMapping("/capture")
    @Operation(
        summary = "Capture an authorized transaction",
        description = "Captures funds from a previously authorized transaction. " +
                     "The authorization must be in AUTHORIZED status and within the capture window."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Capture processed successfully",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Original authorization not found"),
        @ApiResponse(responseCode = "422", description = "Capture failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<?> processCapture(
            @Valid @RequestBody CaptureRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        if (correlationId == null) {
            correlationId = "corr-" + UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put("correlationId", correlationId);

        try {
            log.info("Processing capture request - AuthTransactionId: {}, Amount: {}, CorrelationId: {}", 
                       request.getAuthorizationTransactionId(), request.getAmount(), correlationId);

            PaymentResponse response = paymentService.processCapture(request);
            
            log.info("Capture completed successfully - TransactionId: {}, Status: {}", 
                       response.getTransactionId(), response.getStatus());

            return ResponseEntity.ok(response);

        } catch (PaymentProcessingException e) {
            log.error("Capture failed - Error: {}, CorrelationId: {}", e.getMessage(), correlationId);
            
            // Determine appropriate status code based on error
            HttpStatus status = e.getMessage().contains("not found") ? 
                HttpStatus.NOT_FOUND : HttpStatus.UNPROCESSABLE_ENTITY;
            
            PaymentErrorResponse errorResponse = new PaymentErrorResponse(
                "CAPTURE_FAILED", e.getMessage(), e.getMessage(), "CAPTURE_ERROR", correlationId);
            
            return ResponseEntity.status(status).body(errorResponse);

        } catch (Exception e) {
            log.error("Unexpected error during capture - Error: {}, CorrelationId: {}", 
                        e.getMessage(), correlationId, e);
            
            PaymentErrorResponse errorResponse = new PaymentErrorResponse(
                "INTERNAL_ERROR", "An unexpected error occurred", "An unexpected error occurred", "SYSTEM_ERROR", correlationId);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Void/cancel an authorized transaction.
     * This cancels the authorization and releases the held funds.
     */
    @PostMapping("/void")
    @Operation(
        summary = "Void an authorized transaction",
        description = "Voids (cancels) an authorized transaction, releasing any held funds. " +
                     "Only transactions in AUTHORIZED status can be voided."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Void processed successfully",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Original transaction not found"),
        @ApiResponse(responseCode = "422", description = "Void failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<?> processVoid(
            @Valid @RequestBody VoidRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        if (correlationId == null) {
            correlationId = "corr-" + UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put("correlationId", correlationId);

        try {
            log.info("Processing void request - OriginalTransactionId: {}, CorrelationId: {}", 
                       request.getOriginalTransactionId(), correlationId);

            PaymentResponse response = paymentService.processVoid(request);
            
            log.info("Void completed successfully - TransactionId: {}, Status: {}", 
                       response.getTransactionId(), response.getStatus());

            return ResponseEntity.ok(response);

        } catch (PaymentProcessingException e) {
            log.error("Void failed - Error: {}, CorrelationId: {}", e.getMessage(), correlationId);
            
            HttpStatus status = e.getMessage().contains("not found") ? 
                HttpStatus.NOT_FOUND : HttpStatus.UNPROCESSABLE_ENTITY;
            
            PaymentErrorResponse errorResponse = new PaymentErrorResponse(
                "VOID_FAILED", e.getMessage(), e.getMessage(), "VOID_ERROR", correlationId);
            
            return ResponseEntity.status(status).body(errorResponse);

        } catch (Exception e) {
            log.error("Unexpected error during void - Error: {}, CorrelationId: {}", 
                        e.getMessage(), correlationId, e);
            
            PaymentErrorResponse errorResponse = new PaymentErrorResponse(
                "INTERNAL_ERROR", "An unexpected error occurred", "An unexpected error occurred", "SYSTEM_ERROR", correlationId);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Process a refund transaction.
     * This refunds funds from a previously captured transaction.
     */
    @PostMapping("/refund")
    @Operation(
        summary = "Process a refund transaction",
        description = "Processes a refund transaction for a previously captured payment. " +
                     "Supports both full refunds (amount not specified) and partial refunds."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Refund processed successfully",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Original transaction not found"),
        @ApiResponse(responseCode = "422", description = "Refund failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<?> processRefund(
            @Valid @RequestBody RefundRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        if (correlationId == null) {
            correlationId = "corr-" + UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put("correlationId", correlationId);

        try {
            log.info("Processing refund request - OriginalTransactionId: {}, Amount: {}, CorrelationId: {}", 
                       request.getOriginalTransactionId(), request.getAmount(), correlationId);

            PaymentResponse response = paymentService.processRefund(request);
            
            log.info("Refund completed successfully - TransactionId: {}, Status: {}, Amount: {}", 
                       response.getTransactionId(), response.getStatus(), response.getAmount());

            return ResponseEntity.ok(response);

        } catch (PaymentProcessingException e) {
            log.error("Refund failed - Error: {}, CorrelationId: {}", e.getMessage(), correlationId);
            
            HttpStatus status = e.getMessage().contains("not found") ? 
                HttpStatus.NOT_FOUND : HttpStatus.UNPROCESSABLE_ENTITY;
            
            PaymentErrorResponse errorResponse = new PaymentErrorResponse(
                "REFUND_FAILED", e.getMessage(), e.getMessage(), "REFUND_ERROR", correlationId);
            
            return ResponseEntity.status(status).body(errorResponse);

        } catch (Exception e) {
            log.error("Unexpected error during refund - Error: {}, CorrelationId: {}", 
                        e.getMessage(), correlationId, e);
            
            PaymentErrorResponse errorResponse = new PaymentErrorResponse(
                "INTERNAL_ERROR", "An unexpected error occurred", "An unexpected error occurred", "SYSTEM_ERROR", correlationId);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Get transaction status and details.
     * This retrieves the current status of any transaction.
     */
    @GetMapping("/{transactionId}")
    @Operation(
        summary = "Get transaction status",
        description = "Retrieves the current status and details of a transaction by its ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction status retrieved successfully",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Transaction not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<?> getTransactionStatus(
            @Parameter(description = "Transaction ID", required = true)
            @PathVariable String transactionId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        if (correlationId == null) {
            correlationId = "corr-" + UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put("correlationId", correlationId);

        try {
            log.info("Getting transaction status - TransactionId: {}, CorrelationId: {}", 
                       transactionId, correlationId);

            PaymentResponse response = paymentService.getTransactionStatus(transactionId);
            
            log.info("Transaction status retrieved - TransactionId: {}, Status: {}", 
                       transactionId, response.getStatus());

            return ResponseEntity.ok(response);

        } catch (PaymentProcessingException e) {
            log.error("Failed to get transaction status - Error: {}, CorrelationId: {}", 
                        e.getMessage(), correlationId);
            
            HttpStatus status = e.getMessage().contains("not found") ? 
                HttpStatus.NOT_FOUND : HttpStatus.INTERNAL_SERVER_ERROR;
            
            PaymentErrorResponse errorResponse = new PaymentErrorResponse(
                "TRANSACTION_NOT_FOUND", e.getMessage(), e.getMessage(), "INQUIRY_ERROR", correlationId);
            
            return ResponseEntity.status(status).body(errorResponse);

        } catch (Exception e) {
            log.error("Unexpected error getting transaction status - Error: {}, CorrelationId: {}", 
                        e.getMessage(), correlationId, e);
            
            PaymentErrorResponse errorResponse = new PaymentErrorResponse(
                "INTERNAL_ERROR", "An unexpected error occurred", "An unexpected error occurred", "SYSTEM_ERROR", correlationId);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Validate payment method without processing a transaction.
     * This can be used to validate customer payment information before checkout.
     */
    @PostMapping("/validate")
    @Operation(
        summary = "Validate payment method",
        description = "Validates payment method information without processing a transaction. " +
                     "Useful for pre-validating customer payment details."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment method is valid"),
        @ApiResponse(responseCode = "400", description = "Invalid payment method"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")
    public ResponseEntity<?> validatePaymentMethod(
            @Valid @RequestBody PaymentMethodRequest paymentMethod,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        if (correlationId == null) {
            correlationId = "corr-" + UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put("correlationId", correlationId);

        try {
            log.info("Validating payment method - Type: {}, CorrelationId: {}", 
                       paymentMethod.getType(), correlationId);

            paymentService.validatePaymentMethod(paymentMethod);
            
            log.info("Payment method validation successful - Type: {}, CorrelationId: {}", 
                       paymentMethod.getType(), correlationId);

            return ResponseEntity.ok().body(new PaymentValidationResponse(true, "Payment method is valid"));

        } catch (PaymentProcessingException e) {
            log.error("Payment method validation failed - Error: {}, CorrelationId: {}", 
                        e.getMessage(), correlationId);
            
            PaymentErrorResponse errorResponse = new PaymentErrorResponse(
                "VALIDATION_FAILED", e.getMessage(), e.getMessage(), "VALIDATION_ERROR", correlationId);
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            log.error("Unexpected error during validation - Error: {}, CorrelationId: {}", 
                        e.getMessage(), correlationId, e);
            
            PaymentErrorResponse errorResponse = new PaymentErrorResponse(
                "INTERNAL_ERROR", "An unexpected error occurred", "An unexpected error occurred", "SYSTEM_ERROR", correlationId);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Simple response class for payment validation endpoint.
     */
    public static class PaymentValidationResponse {
        private boolean valid;
        private String message;

        public PaymentValidationResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    @Operation(summary = "Get transaction details from Authorize.Net directly", 
               description = "Queries Authorize.Net directly for transaction details to verify integration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Transaction not found in Authorize.Net"),
        @ApiResponse(responseCode = "500", description = "Error querying Authorize.Net")
    })
    @GetMapping("/authnet/{authnetTransactionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<PaymentResponse> getAuthNetTransactionDetails(
            @Parameter(description = "Authorize.Net Transaction ID") @PathVariable String authnetTransactionId) {
        
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Querying Authorize.Net directly - AuthNet TransactionId: {}, CorrelationId: {}", 
                   authnetTransactionId, correlationId);

        try {
            PaymentResponse response = paymentService.getAuthNetTransactionDetails(authnetTransactionId);
            
            log.info("AuthNet transaction details retrieved - AuthNet ID: {}, Status: {}, CorrelationId: {}", 
                       authnetTransactionId, response.getStatus(), correlationId);

            return ResponseEntity.ok()
                               .header("X-Correlation-ID", correlationId)
                               .body(response);
                               
        } catch (Exception e) {
            log.error("Failed to retrieve transaction from Authorize.Net - AuthNet ID: {}, Error: {}, CorrelationId: {}", 
                        authnetTransactionId, e.getMessage(), correlationId);
            
            PaymentResponse errorResponse = new PaymentResponse();
            errorResponse.setAuthnetTransactionId(authnetTransactionId);
            errorResponse.setSuccess(false);
            errorResponse.setResponseReasonText("Failed to retrieve from Authorize.Net: " + e.getMessage());
            errorResponse.setCorrelationId(correlationId);
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                               .header("X-Correlation-ID", correlationId)
                               .body(errorResponse);
        }
    }
}
