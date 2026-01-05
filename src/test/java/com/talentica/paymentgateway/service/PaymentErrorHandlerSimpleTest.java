package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.payment.PaymentResponse;
import com.talentica.paymentgateway.entity.PaymentStatus;
import com.talentica.paymentgateway.entity.Transaction;
import com.talentica.paymentgateway.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simplified unit tests for PaymentErrorHandler focusing on core functionality.
 */
class PaymentErrorHandlerSimpleTest {

    private PaymentErrorHandler paymentErrorHandler;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        paymentErrorHandler = new PaymentErrorHandler();
        
        // Create test transaction
        testTransaction = new Transaction();
        testTransaction.setTransactionId("TXN_TEST_001");
        testTransaction.setAmount(new BigDecimal("100.00"));
        testTransaction.setCurrency("USD");
        testTransaction.setTransactionType(TransactionType.PURCHASE);
        testTransaction.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
        testTransaction.setStatus(PaymentStatus.PENDING);
    }

    @Test
    void handleGeneralError_WithTimeoutException_ShouldCategorizeCorrectly() {
        // Given
        Exception timeoutException = new RuntimeException("Connection timed out");

        // When
        PaymentResponse response = paymentErrorHandler.handleGeneralError(timeoutException, testTransaction, "purchase");

        // Then
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("SYSTEM_ERROR");
        assertThat(response.getErrorCategory()).isEqualTo("TIMEOUT_ERROR");
        assertThat(response.getErrorMessage()).isEqualTo("Request timed out");
        assertThat(response.getRetryable()).isTrue();
        assertThat(response.getSuggestedAction()).isEqualTo("Please try again");
        assertThat(response.getDetailedError()).isEqualTo("Connection timed out");
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void handleGeneralError_WithNetworkException_ShouldCategorizeCorrectly() {
        // Given
        Exception networkException = new RuntimeException("Network connection failed");

        // When
        PaymentResponse response = paymentErrorHandler.handleGeneralError(networkException, testTransaction, "refund");

        // Then
        assertThat(response.getErrorCategory()).isEqualTo("NETWORK_ERROR");
        assertThat(response.getErrorMessage()).isEqualTo("Network connection failed");
        assertThat(response.getRetryable()).isTrue();
        assertThat(response.getSuggestedAction()).isEqualTo("Check connection and retry");
        assertThat(response.getRetryAfterSeconds()).isEqualTo(30);
        assertThat(response.getMaxRetryAttempts()).isEqualTo(3);
    }

    @Test
    void handleGeneralError_WithValidationException_ShouldCategorizeCorrectly() {
        // Given
        Exception validationException = new RuntimeException("Invalid request data");

        // When
        PaymentResponse response = paymentErrorHandler.handleGeneralError(validationException, testTransaction, "authorize");

        // Then
        assertThat(response.getErrorCategory()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getErrorMessage()).isEqualTo("Invalid request data");
        assertThat(response.getRetryable()).isFalse();
        assertThat(response.getSuggestedAction()).isEqualTo("Please correct the data");
        assertThat(response.getRetryAfterSeconds()).isNull();
    }

    @Test
    void handleNetworkError_ShouldReturnNetworkErrorResponse() {
        // Given
        Exception networkException = new RuntimeException("Connection refused");

        // When
        PaymentResponse response = paymentErrorHandler.handleNetworkError(networkException, testTransaction);

        // Then
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("NETWORK_ERROR");
        assertThat(response.getErrorCategory()).isEqualTo("NETWORK_ERROR");
        assertThat(response.getErrorMessage()).isEqualTo("Network connection failed");
        assertThat(response.getRetryable()).isTrue();
        assertThat(response.getSuggestedAction()).isEqualTo("Please check your connection and try again");
        assertThat(response.getDetailedError()).isEqualTo("Connection refused");
        assertThat(response.getRetryAfterSeconds()).isEqualTo(30);
        assertThat(response.getMaxRetryAttempts()).isEqualTo(3);
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void handleTimeoutError_ShouldReturnTimeoutErrorResponse() {
        // Given
        long timeoutMs = 30000;

        // When
        PaymentResponse response = paymentErrorHandler.handleTimeoutError(testTransaction, timeoutMs);

        // Then
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("TIMEOUT");
        assertThat(response.getErrorCategory()).isEqualTo("TIMEOUT_ERROR");
        assertThat(response.getErrorMessage()).isEqualTo("Payment processing timed out");
        assertThat(response.getRetryable()).isTrue();
        assertThat(response.getSuggestedAction()).isEqualTo("Transaction may still be processing. Please wait before retrying.");
        assertThat(response.getDetailedError()).isEqualTo("Processing timeout after 30000ms");
        assertThat(response.getRetryAfterSeconds()).isEqualTo(30);
        assertThat(response.getMaxRetryAttempts()).isEqualTo(3);
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void handleValidationError_WithTransaction_ShouldReturnValidationErrorResponse() {
        // Given
        String field = "cardNumber";
        String message = "Card number is required";

        // When
        PaymentResponse response = paymentErrorHandler.handleValidationError(field, message, testTransaction);

        // Then
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getErrorCategory()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getErrorMessage()).isEqualTo("Request validation failed");
        assertThat(response.getRetryable()).isFalse();
        assertThat(response.getSuggestedAction()).isEqualTo("Please correct the highlighted fields");
        assertThat(response.getDetailedError()).isEqualTo(message);
        assertThat(response.getValidationErrors()).hasSize(1);
        assertThat(response.getValidationErrors().get(0).getField()).isEqualTo(field);
        assertThat(response.getValidationErrors().get(0).getMessage()).isEqualTo(message);
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void handleValidationError_WithoutTransaction_ShouldReturnValidationErrorResponse() {
        // Given
        String field = "amount";
        String message = "Amount must be positive";

        // When
        PaymentResponse response = paymentErrorHandler.handleValidationError(field, message, null);

        // Then
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getErrorCategory()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getTransactionId()).isNull();
        assertThat(response.getAmount()).isNull();
        assertThat(response.getValidationErrors()).hasSize(1);
        assertThat(response.getValidationErrors().get(0).getField()).isEqualTo(field);
        assertThat(response.getValidationErrors().get(0).getMessage()).isEqualTo(message);
    }

    @Test
    void handleGeneralError_WithNullTransaction_ShouldHandleGracefully() {
        // Given
        Exception exception = new RuntimeException("Test error");

        // When
        PaymentResponse response = paymentErrorHandler.handleGeneralError(exception, null, "test");

        // Then
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getTransactionId()).isNull();
        assertThat(response.getAmount()).isNull();
        assertThat(response.getCurrency()).isNull();
        assertThat(response.getStatus()).isNull();
        assertThat(response.getErrorCategory()).isEqualTo("SYSTEM_ERROR");
    }

    @Test
    void errorCategorization_ShouldHandleVariousExceptionMessages() {
        // Test timeout variations
        Exception timeoutException1 = new RuntimeException("Request timeout");
        PaymentResponse timeoutResponse1 = paymentErrorHandler.handleGeneralError(timeoutException1, testTransaction, "test");
        assertThat(timeoutResponse1.getErrorCategory()).isEqualTo("TIMEOUT_ERROR");

        testTransaction.setStatus(PaymentStatus.PENDING); // Reset status
        Exception timeoutException2 = new RuntimeException("Operation timed out");
        PaymentResponse timeoutResponse2 = paymentErrorHandler.handleGeneralError(timeoutException2, testTransaction, "test");
        assertThat(timeoutResponse2.getErrorCategory()).isEqualTo("TIMEOUT_ERROR");

        // Test connection variations
        testTransaction.setStatus(PaymentStatus.PENDING); // Reset status
        Exception connectionException = new RuntimeException("Connection error");
        PaymentResponse connectionResponse = paymentErrorHandler.handleGeneralError(connectionException, testTransaction, "test");
        assertThat(connectionResponse.getErrorCategory()).isEqualTo("NETWORK_ERROR");

        // Test validation variations
        testTransaction.setStatus(PaymentStatus.PENDING); // Reset status
        Exception validationException = new RuntimeException("Invalid input provided");
        PaymentResponse validationResponse = paymentErrorHandler.handleGeneralError(validationException, testTransaction, "test");
        assertThat(validationResponse.getErrorCategory()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void errorResponse_ShouldIncludeAllTransactionDetails() {
        // Given
        testTransaction.setTransactionType(TransactionType.REFUND);
        testTransaction.setCurrency("EUR");
        testTransaction.setAmount(new BigDecimal("250.75"));

        // When
        PaymentResponse response = paymentErrorHandler.handleGeneralError(
            new RuntimeException("Test error"), testTransaction, "refund");

        // Then
        assertThat(response.getTransactionId()).isEqualTo(testTransaction.getTransactionId());
        assertThat(response.getAmount()).isEqualTo(testTransaction.getAmount());
        assertThat(response.getCurrency()).isEqualTo(testTransaction.getCurrency());
        assertThat(response.getTransactionType()).isEqualTo(testTransaction.getTransactionType().name());
        assertThat(response.getCreatedAt()).isEqualTo(testTransaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()));
        assertThat(response.getStatus()).isEqualTo(testTransaction.getStatus().name());
    }
}
