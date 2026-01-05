package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.payment.PaymentResponse;
import com.talentica.paymentgateway.entity.PaymentStatus;
import com.talentica.paymentgateway.entity.Transaction;
import net.authorize.api.contract.v1.TransactionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling various payment error scenarios and providing
 * comprehensive error responses with appropriate retry strategies.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class PaymentErrorHandler {

    // Authorize.Net response codes mapping
    private static final Map<String, ErrorInfo> ERROR_CODE_MAPPING = new HashMap<>();
    
    static {
        // Card declined scenarios
        ERROR_CODE_MAPPING.put("2", new ErrorInfo("CARD_DECLINED", "Card was declined by the issuing bank", true, "Try a different payment method"));
        ERROR_CODE_MAPPING.put("3", new ErrorInfo("CARD_DECLINED", "Card has expired", false, "Please use a valid card with future expiry date"));
        ERROR_CODE_MAPPING.put("4", new ErrorInfo("CARD_DECLINED", "Card number is invalid", false, "Please check the card number and try again"));
        ERROR_CODE_MAPPING.put("5", new ErrorInfo("INSUFFICIENT_FUNDS", "Insufficient funds", true, "Please ensure sufficient funds or try another card"));
        ERROR_CODE_MAPPING.put("6", new ErrorInfo("CARD_DECLINED", "Credit card number is invalid", false, "Please verify the card number"));
        ERROR_CODE_MAPPING.put("7", new ErrorInfo("CARD_DECLINED", "Credit card expiration date is invalid", false, "Please check the expiration date"));
        ERROR_CODE_MAPPING.put("8", new ErrorInfo("CARD_DECLINED", "Credit card has expired", false, "Please use a valid card"));
        
        // Security and fraud scenarios
        ERROR_CODE_MAPPING.put("27", new ErrorInfo("AVS_MISMATCH", "Address verification failed", true, "Please verify billing address"));
        ERROR_CODE_MAPPING.put("28", new ErrorInfo("CARD_DECLINED", "Merchant does not accept this type of credit card", false, "Please try a different card type"));
        ERROR_CODE_MAPPING.put("37", new ErrorInfo("CVV_MISMATCH", "CVV verification failed", true, "Please check the CVV code"));
        ERROR_CODE_MAPPING.put("44", new ErrorInfo("CVV_MISMATCH", "Card code is required", true, "Please provide the CVV code"));
        ERROR_CODE_MAPPING.put("45", new ErrorInfo("CVV_MISMATCH", "Card code verification failed", true, "Please verify the CVV code"));
        
        // System and processing errors
        ERROR_CODE_MAPPING.put("11", new ErrorInfo("DUPLICATE_TRANSACTION", "Duplicate transaction detected", false, "Transaction already processed"));
        ERROR_CODE_MAPPING.put("13", new ErrorInfo("INVALID_MERCHANT", "Merchant login ID is invalid", false, "Please contact support"));
        ERROR_CODE_MAPPING.put("16", new ErrorInfo("INVALID_TRANSACTION", "Transaction ID is invalid", false, "Please retry the transaction"));
        ERROR_CODE_MAPPING.put("17", new ErrorInfo("INVALID_MERCHANT", "Merchant does not accept this type of credit card", false, "Card type not supported"));
        ERROR_CODE_MAPPING.put("19", new ErrorInfo("PROCESSING_ERROR", "Error occurred during processing", true, "Please try again later"));
        ERROR_CODE_MAPPING.put("20", new ErrorInfo("PROCESSING_ERROR", "Error occurred during processing", true, "Please try again later"));
        
        // Amount and currency errors
        ERROR_CODE_MAPPING.put("33", new ErrorInfo("INVALID_AMOUNT", "Amount is invalid", false, "Please check the transaction amount"));
        ERROR_CODE_MAPPING.put("78", new ErrorInfo("INVALID_AMOUNT", "Card/account number is invalid", false, "Please verify the card number"));
        ERROR_CODE_MAPPING.put("92", new ErrorInfo("PROCESSING_ERROR", "Gateway timed out", true, "Please retry the transaction"));
        
        // Velocity and limits
        ERROR_CODE_MAPPING.put("141", new ErrorInfo("VELOCITY_LIMIT", "Transaction velocity limit exceeded", true, "Please wait before retrying"));
        ERROR_CODE_MAPPING.put("165", new ErrorInfo("PROCESSING_ERROR", "Processor failure", true, "Please try again later"));
        ERROR_CODE_MAPPING.put("200", new ErrorInfo("RISK_MANAGEMENT", "Transaction blocked by risk management", false, "Transaction flagged for review"));
        ERROR_CODE_MAPPING.put("201", new ErrorInfo("RISK_MANAGEMENT", "Transaction blocked by risk management", false, "Transaction requires verification"));
        
        // Network and connectivity
        ERROR_CODE_MAPPING.put("250", new ErrorInfo("NETWORK_ERROR", "Network connection error", true, "Please check connection and retry"));
        ERROR_CODE_MAPPING.put("251", new ErrorInfo("NETWORK_ERROR", "Network timeout", true, "Please retry the transaction"));
        ERROR_CODE_MAPPING.put("252", new ErrorInfo("PROCESSING_ERROR", "Processor unavailable", true, "Service temporarily unavailable"));
    }

    /**
     * Handle Authorize.Net transaction response and create appropriate error response.
     */
    public PaymentResponse handleAuthorizeNetError(TransactionResponse response, Transaction transaction) {
        String responseCode = response.getResponseCode();
        String reasonCode = response.getMessages().getMessage().get(0).getCode();
        String reasonText = response.getMessages().getMessage().get(0).getDescription();
        
        log.warn("Payment failed - Response Code: {}, Reason Code: {}, Reason: {}", 
                   responseCode, reasonCode, reasonText);
        
        ErrorInfo errorInfo = ERROR_CODE_MAPPING.getOrDefault(reasonCode, 
            new ErrorInfo("PAYMENT_FAILED", reasonText, true, "Please try again or contact support"));
        
        // Update transaction status based on error type
        PaymentStatus status = determineTransactionStatus(errorInfo.category);
        transaction.setStatus(status);
        
        return createErrorResponse(transaction, errorInfo, reasonCode, reasonText);
    }

    /**
     * Handle general payment processing errors.
     */
    public PaymentResponse handleGeneralError(Exception exception, Transaction transaction, String operation) {
        log.error("Payment processing error during {}: {}", operation, exception.getMessage(), exception);
        
        String errorCategory = categorizeException(exception);
        ErrorInfo errorInfo = getErrorInfoForCategory(errorCategory);
        
        if (transaction != null) {
            transaction.setStatus(PaymentStatus.FAILED);
        }
        
        return createErrorResponse(transaction, errorInfo, "SYSTEM_ERROR", exception.getMessage());
    }

    /**
     * Handle network and connectivity errors.
     */
    public PaymentResponse handleNetworkError(Exception exception, Transaction transaction) {
        log.error("Network error during payment processing: {}", exception.getMessage(), exception);
        
        ErrorInfo errorInfo = new ErrorInfo("NETWORK_ERROR", 
            "Network connection failed", true, "Please check your connection and try again");
        
        if (transaction != null) {
            transaction.setStatus(PaymentStatus.FAILED);
        }
        
        return createErrorResponse(transaction, errorInfo, "NETWORK_ERROR", exception.getMessage());
    }

    /**
     * Handle timeout errors with specific retry guidance.
     */
    public PaymentResponse handleTimeoutError(Transaction transaction, long timeoutMs) {
        log.warn("Payment processing timeout after {}ms for transaction: {}", 
                   timeoutMs, transaction != null ? transaction.getTransactionId() : "null");
        
        ErrorInfo errorInfo = new ErrorInfo("TIMEOUT_ERROR", 
            "Payment processing timed out", true, 
            "Transaction may still be processing. Please wait before retrying.");
        
        if (transaction != null) {
            transaction.setStatus(PaymentStatus.PENDING);
        }
        
        return createErrorResponse(transaction, errorInfo, "TIMEOUT", 
            "Processing timeout after " + timeoutMs + "ms");
    }

    /**
     * Handle validation errors with detailed field information.
     */
    public PaymentResponse handleValidationError(String field, String message, Transaction transaction) {
        log.warn("Validation error for field {}: {}", field, message);
        
        ErrorInfo errorInfo = new ErrorInfo("VALIDATION_ERROR", 
            "Request validation failed", false, "Please correct the highlighted fields");
        
        if (transaction != null) {
            transaction.setStatus(PaymentStatus.FAILED);
        }
        
        PaymentResponse response = createErrorResponse(transaction, errorInfo, "VALIDATION_ERROR", message);
        response.addValidationError(field, message);
        
        return response;
    }

    /**
     * Create standardized error response.
     */
    private PaymentResponse createErrorResponse(Transaction transaction, ErrorInfo errorInfo, 
                                              String errorCode, String errorMessage) {
        PaymentResponse response = new PaymentResponse();
        
        if (transaction != null) {
            response.setTransactionId(transaction.getTransactionId());
            response.setAmount(transaction.getAmount());
            response.setCurrency(transaction.getCurrency());
            response.setStatus(transaction.getStatus().name());
            response.setTransactionType(transaction.getTransactionType().name());
            response.setCreatedAt(transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()));
        }
        
        response.setSuccess(false);
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorInfo.message);
        response.setErrorCategory(errorInfo.category);
        response.setRetryable(errorInfo.retryable);
        response.setSuggestedAction(errorInfo.suggestedAction);
        response.setDetailedError(errorMessage);
        
        // Add retry guidance
        if (errorInfo.retryable) {
            response.setRetryAfterSeconds(calculateRetryDelay(errorInfo.category));
            response.setMaxRetryAttempts(getMaxRetryAttempts(errorInfo.category));
        }
        
        return response;
    }

    /**
     * Determine transaction status based on error category.
     */
    private PaymentStatus determineTransactionStatus(String category) {
        switch (category) {
            case "NETWORK_ERROR":
            case "TIMEOUT_ERROR":
            case "PROCESSING_ERROR":
                return PaymentStatus.PENDING;
            case "DUPLICATE_TRANSACTION":
                return PaymentStatus.FAILED;
            default:
                return PaymentStatus.FAILED;
        }
    }

    /**
     * Categorize exception types for appropriate error handling.
     */
    private String categorizeException(Exception exception) {
        String message = exception.getMessage().toLowerCase();
        
        if (message.contains("timeout") || message.contains("timed out")) {
            return "TIMEOUT_ERROR";
        } else if (message.contains("connection") || message.contains("network")) {
            return "NETWORK_ERROR";
        } else if (message.contains("validation") || message.contains("invalid")) {
            return "VALIDATION_ERROR";
        } else if (message.contains("duplicate")) {
            return "DUPLICATE_TRANSACTION";
        } else {
            return "SYSTEM_ERROR";
        }
    }

    /**
     * Get error info for general error categories.
     */
    private ErrorInfo getErrorInfoForCategory(String category) {
        switch (category) {
            case "TIMEOUT_ERROR":
                return new ErrorInfo("TIMEOUT_ERROR", "Request timed out", true, "Please try again");
            case "NETWORK_ERROR":
                return new ErrorInfo("NETWORK_ERROR", "Network connection failed", true, "Check connection and retry");
            case "VALIDATION_ERROR":
                return new ErrorInfo("VALIDATION_ERROR", "Invalid request data", false, "Please correct the data");
            case "DUPLICATE_TRANSACTION":
                return new ErrorInfo("DUPLICATE_TRANSACTION", "Transaction already processed", false, "Check transaction history");
            default:
                return new ErrorInfo("SYSTEM_ERROR", "Internal system error", true, "Please contact support");
        }
    }

    /**
     * Calculate retry delay based on error category.
     */
    private int calculateRetryDelay(String category) {
        switch (category) {
            case "NETWORK_ERROR":
            case "TIMEOUT_ERROR":
                return 30; // 30 seconds
            case "PROCESSING_ERROR":
                return 60; // 1 minute
            case "VELOCITY_LIMIT":
                return 300; // 5 minutes
            default:
                return 10; // 10 seconds
        }
    }

    /**
     * Get maximum retry attempts for error category.
     */
    private int getMaxRetryAttempts(String category) {
        switch (category) {
            case "NETWORK_ERROR":
            case "TIMEOUT_ERROR":
                return 3;
            case "PROCESSING_ERROR":
                return 2;
            case "VELOCITY_LIMIT":
                return 1;
            default:
                return 1;
        }
    }

    /**
     * Inner class to hold error information.
     */
    private static class ErrorInfo {
        final String category;
        final String message;
        final boolean retryable;
        final String suggestedAction;

        ErrorInfo(String category, String message, boolean retryable, String suggestedAction) {
            this.category = category;
            this.message = message;
            this.retryable = retryable;
            this.suggestedAction = suggestedAction;
        }
    }
}
