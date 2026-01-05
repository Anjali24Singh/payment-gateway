package com.talentica.paymentgateway.exception;

import com.talentica.paymentgateway.dto.payment.PaymentErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for payment processing and other application errors.
 * Provides consistent error responses with proper logging and correlation tracking.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles PaymentProcessingException with detailed error information.
     */
    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<PaymentErrorResponse> handlePaymentProcessingException(
            PaymentProcessingException ex, WebRequest request) {
        
        String correlationId = ex.getCorrelationId() != null ? 
            ex.getCorrelationId() : getOrGenerateCorrelationId();
        
        log.error("Payment processing error - CorrelationId: {}, ErrorCode: {}, Message: {}", 
                    correlationId, ex.getErrorCode(), ex.getMessage(), ex);

        PaymentErrorResponse errorResponse;
        if (ex.getErrorResponse() != null) {
            errorResponse = ex.getErrorResponse();
        } else {
            errorResponse = new PaymentErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                "Payment processing failed due to an error",
                "PAYMENT_ERROR",
                correlationId
            );
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles validation errors from request body validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PaymentErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        String correlationId = getOrGenerateCorrelationId();
        
        log.warn("Validation error - CorrelationId: {}, Errors: {}", 
                   correlationId, ex.getBindingResult().getFieldErrors().size());

        List<PaymentErrorResponse.ErrorDetail> details = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::mapFieldError)
            .collect(Collectors.toList());

        PaymentErrorResponse errorResponse = PaymentErrorResponse.validationError(
            "Request validation failed", correlationId);
        errorResponse.setDetails(details);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle malformed JSON or missing request body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<PaymentErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {
        String correlationId = getOrGenerateCorrelationId();
        log.warn("Malformed JSON body - CorrelationId: {}, Message: {}", correlationId, ex.getMessage());
        PaymentErrorResponse errorResponse = PaymentErrorResponse.validationError(
            "Malformed or missing request body", correlationId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles constraint validation errors.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<PaymentErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        String correlationId = getOrGenerateCorrelationId();
        
        log.warn("Constraint validation error - CorrelationId: {}, Violations: {}", 
                   correlationId, ex.getConstraintViolations().size());

        List<PaymentErrorResponse.ErrorDetail> details = ex.getConstraintViolations()
            .stream()
            .map(this::mapConstraintViolation)
            .collect(Collectors.toList());

        PaymentErrorResponse errorResponse = PaymentErrorResponse.validationError(
            "Request validation failed", correlationId);
        errorResponse.setDetails(details);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles IllegalArgumentException.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PaymentErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        String correlationId = getOrGenerateCorrelationId();
        
        log.warn("Invalid argument error - CorrelationId: {}, Message: {}", 
                   correlationId, ex.getMessage());

        PaymentErrorResponse errorResponse = PaymentErrorResponse.validationError(
            ex.getMessage(), correlationId);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles IllegalStateException.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<PaymentErrorResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        
        String correlationId = getOrGenerateCorrelationId();
        
        log.warn("Invalid state error - CorrelationId: {}, Message: {}", 
                   correlationId, ex.getMessage());

        PaymentErrorResponse errorResponse = new PaymentErrorResponse(
            "INVALID_STATE",
            ex.getMessage(),
            "The operation cannot be performed in the current state",
            "BUSINESS_ERROR",
            correlationId
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles RuntimeException and other unexpected errors.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<PaymentErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        String correlationId = getOrGenerateCorrelationId();
        
        log.error("Runtime error - CorrelationId: {}, Message: {}", 
                    correlationId, ex.getMessage(), ex);

        PaymentErrorResponse errorResponse = PaymentErrorResponse.processingError(
            "An unexpected error occurred", correlationId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles NoResourceFoundException (404 errors).
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<PaymentErrorResponse> handleNoResourceFoundException(
            org.springframework.web.servlet.resource.NoResourceFoundException ex, WebRequest request) {
        
        String correlationId = getOrGenerateCorrelationId();
        
        log.warn("Resource not found - CorrelationId: {}, Path: {}", 
                   correlationId, ex.getResourcePath());

        PaymentErrorResponse errorResponse = new PaymentErrorResponse(
            "RESOURCE_NOT_FOUND",
            "The requested resource was not found",
            "The endpoint you are trying to access does not exist",
            "VALIDATION_ERROR",
            correlationId
        );
        errorResponse.setRetryable(false);
        errorResponse.setSuggestedAction("Please check the API documentation for valid endpoints");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles AccessDeniedException (403 Forbidden - Authorization failures).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<PaymentErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        String correlationId = getOrGenerateCorrelationId();
        
        log.warn("Access denied - CorrelationId: {}, Message: {}", 
                   correlationId, ex.getMessage());

        PaymentErrorResponse errorResponse = new PaymentErrorResponse(
            "ACCESS_DENIED",
            "Access denied",
            "You do not have permission to access this resource",
            "AUTHORIZATION_ERROR",
            correlationId
        );
        errorResponse.setRetryable(false);
        errorResponse.setSuggestedAction("Please check your user role and permissions");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handles all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<PaymentErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        String correlationId = getOrGenerateCorrelationId();
        
        log.error("Unexpected error - CorrelationId: {}, Exception Type: {}, Message: {}", 
                    correlationId, ex.getClass().getSimpleName(), ex.getMessage(), ex);

        PaymentErrorResponse errorResponse = PaymentErrorResponse.processingError(
            "An unexpected error occurred", correlationId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // Helper methods

    private PaymentErrorResponse.ErrorDetail mapFieldError(FieldError fieldError) {
        return new PaymentErrorResponse.ErrorDetail(
            fieldError.getField(),
            "VALIDATION_ERROR",
            fieldError.getDefaultMessage()
        );
    }

    private PaymentErrorResponse.ErrorDetail mapConstraintViolation(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath().toString();
        return new PaymentErrorResponse.ErrorDetail(
            field,
            "VALIDATION_ERROR",
            violation.getMessage()
        );
    }

    private String getOrGenerateCorrelationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "corr-" + UUID.randomUUID().toString().substring(0, 8);
            MDC.put("correlationId", correlationId);
        }
        return correlationId;
    }
}
