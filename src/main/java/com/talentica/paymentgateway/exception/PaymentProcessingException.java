package com.talentica.paymentgateway.exception;

import com.talentica.paymentgateway.dto.payment.PaymentErrorResponse;

/**
 * Exception thrown when payment processing fails.
 * Provides detailed error information and correlation tracking.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
public class PaymentProcessingException extends RuntimeException {

    private final String correlationId;
    private final String errorCode;
    private final PaymentErrorResponse errorResponse;

    /**
     * Constructs a PaymentProcessingException with a message and correlation ID.
     */
    public PaymentProcessingException(String message, String correlationId) {
        super(message);
        this.correlationId = correlationId;
        this.errorCode = "PAYMENT_PROCESSING_ERROR";
        this.errorResponse = null;
    }

    /**
     * Constructs a PaymentProcessingException with a message, cause, and correlation ID.
     */
    public PaymentProcessingException(String message, Throwable cause, String correlationId) {
        super(message, cause);
        this.correlationId = correlationId;
        this.errorCode = "PAYMENT_PROCESSING_ERROR";
        this.errorResponse = null;
    }

    /**
     * Constructs a PaymentProcessingException with detailed error information.
     */
    public PaymentProcessingException(String message, String errorCode, String correlationId) {
        super(message);
        this.correlationId = correlationId;
        this.errorCode = errorCode;
        this.errorResponse = null;
    }

    /**
     * Constructs a PaymentProcessingException with a message, error code, and cause.
     */
    public PaymentProcessingException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.correlationId = null;
        this.errorCode = errorCode;
        this.errorResponse = null;
    }

    /**
     * Constructs a PaymentProcessingException with a PaymentErrorResponse.
     */
    public PaymentProcessingException(PaymentErrorResponse errorResponse) {
        super(errorResponse.getMessage());
        this.correlationId = errorResponse.getCorrelationId();
        this.errorCode = errorResponse.getCode();
        this.errorResponse = errorResponse;
    }

    /**
     * Gets the correlation ID for tracking this error.
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Gets the error code.
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the detailed error response.
     */
    public PaymentErrorResponse getErrorResponse() {
        return errorResponse;
    }

    @Override
    public String toString() {
        return "PaymentProcessingException{" +
                "message='" + getMessage() + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }
}
