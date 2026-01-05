package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.payment.PaymentResponse;
import com.talentica.paymentgateway.entity.PaymentStatus;
import com.talentica.paymentgateway.entity.Transaction;
import com.talentica.paymentgateway.entity.TransactionType;
import net.authorize.api.contract.v1.TransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PaymentErrorHandler.
 * Tests error handling scenarios, response mapping, and retry strategies.
 */
@ExtendWith(MockitoExtension.class)
class PaymentErrorHandlerUnitTest {

    @InjectMocks
    private PaymentErrorHandler paymentErrorHandler;

    private Transaction testTransaction;
    private TransactionResponse mockTransactionResponse;

    @BeforeEach
    void setUp() {
        // Create test transaction
        testTransaction = new Transaction();
        testTransaction.setTransactionId("TXN_TEST_001");
        testTransaction.setAmount(new BigDecimal("100.00"));
        testTransaction.setCurrency("USD");
        testTransaction.setTransactionType(TransactionType.PURCHASE);
        testTransaction.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
        testTransaction.setStatus(PaymentStatus.PENDING);

        // Create mock Authorize.Net response
        mockTransactionResponse = new TransactionResponse();
        mockTransactionResponse.setResponseCode("2");
        
        TransactionResponse.Messages messages = new TransactionResponse.Messages();
        TransactionResponse.Messages.Message message = new TransactionResponse.Messages.Message();
        message.setCode("2");
        message.setDescription("This transaction has been declined");
        messages.getMessage().add(message);
        mockTransactionResponse.setMessages(messages);
    }

    @Test
    void handleAuthorizeNetError_WithCardDeclinedError_ShouldReturnAppropriateResponse() {
        // When
        PaymentResponse response = paymentErrorHandler.handleAuthorizeNetError(mockTransactionResponse, testTransaction);

        // Then
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getTransactionId()).isEqualTo("TXN_TEST_001");
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getErrorCode()).isEqualTo("2");
        assertThat(response.getErrorCategory()).isEqualTo("CARD_DECLINED");
        assertThat(response.getErrorMessage()).isEqualTo("Card was declined by the issuing bank");
        assertThat(response.getRetryable()).isTrue();
        assertThat(response.getSuggestedAction()).isEqualTo("Try a different payment method");
        assertThat(response.getDetailedError()).isEqualTo("This transaction has been declined");
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void handleAuthorizeNetError_WithExpiredCardError_ShouldReturnNonRetryableResponse() {
        // Given
        mockTransactionResponse.setResponseCode("3");
        mockTransactionResponse.getMessages().getMessage().get(0).setCode("3");
        mockTransactionResponse.getMessages().getMessage().get(0).setDescription("Card has expired");

        // When
        PaymentResponse response = paymentErrorHandler.handleAuthorizeNetError(mockTransactionResponse, testTransaction);

        // Then
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("3");
        assertThat(response.getErrorCategory()).isEqualTo("CARD_DECLINED");
        assertThat(response.getErrorMessage()).isEqualTo("Card has expired");
        assertThat(response.getRetryable()).isFalse();
        assertThat(response.getSuggestedAction()).isEqualTo("Please use a valid card with future expiry date");
        assertThat(response.getRetryAfterSeconds()).isNull();
        assertThat(response.getMaxRetryAttempts()).isNull();
    }

    @Test
    void handleAuthorizeNetError_WithInsufficientFundsError_ShouldReturnRetryableResponse() {
        // Given
        mockTransactionResponse.setResponseCode("5");
        mockTransactionResponse.getMessages().getMessage().get(0).setCode("5");
        mockTransactionResponse.getMessages().getMessage().get(0).setDescription("Insufficient funds");

        // When
        PaymentResponse response = paymentErrorHandler.handleAuthorizeNetError(mockTransactionResponse, testTransaction);

        // Then
        assertThat(response.getErrorCategory()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(response.getErrorMessage()).isEqualTo("Insufficient funds");
        assertThat(response.getRetryable()).isTrue();
        assertThat(response.getSuggestedAction()).isEqualTo("Please ensure sufficient funds or try another card");
        assertThat(response.getRetryAfterSeconds()).isEqualTo(10);
        assertThat(response.getMaxRetryAttempts()).isEqualTo(1);
    }

    @Test
    void handleAuthorizeNetError_WithCVVMismatchError_ShouldReturnRetryableResponse() {
        // Given
        mockTransactionResponse.setResponseCode("37");
        mockTransactionResponse.getMessages().getMessage().get(0).setCode("37");
        mockTransactionResponse.getMessages().getMessage().get(0).setDescription("CVV verification failed");

        // When
        PaymentResponse response = paymentErrorHandler.handleAuthorizeNetError(mockTransactionResponse, testTransaction);

        // Then
        assertThat(response.getErrorCategory()).isEqualTo("CVV_MISMATCH");
        assertThat(response.getErrorMessage()).isEqualTo("CVV verification failed");
        assertThat(response.getRetryable()).isTrue();
        assertThat(response.getSuggestedAction()).isEqualTo("Please check the CVV code");
    }

    @Test
    void handleAuthorizeNetError_WithDuplicateTransactionError_ShouldReturnNonRetryableResponse() {
        // Given
        mockTransactionResponse.setResponseCode("11");
        mockTransactionResponse.getMessages().getMessage().get(0).setCode("11");
        mockTransactionResponse.getMessages().getMessage().get(0).setDescription("Duplicate transaction detected");

        // When
        PaymentResponse response = paymentErrorHandler.handleAuthorizeNetError(mockTransactionResponse, testTransaction);

        // Then
        assertThat(response.getErrorCategory()).isEqualTo("DUPLICATE_TRANSACTION");
        assertThat(response.getErrorMessage()).isEqualTo("Duplicate transaction detected");
        assertThat(response.getRetryable()).isFalse();
        assertThat(response.getSuggestedAction()).isEqualTo("Transaction already processed");
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void handleAuthorizeNetError_WithProcessingError_ShouldSetPendingStatus() {
        // Given
        mockTransactionResponse.setResponseCode("19");
        mockTransactionResponse.getMessages().getMessage().get(0).setCode("19");
        mockTransactionResponse.getMessages().getMessage().get(0).setDescription("Error occurred during processing");

        // When
        PaymentResponse response = paymentErrorHandler.handleAuthorizeNetError(mockTransactionResponse, testTransaction);

        // Then
        assertThat(response.getErrorCategory()).isEqualTo("PROCESSING_ERROR");
        assertThat(response.getRetryable()).isTrue();
        assertThat(response.getRetryAfterSeconds()).isEqualTo(60);
        assertThat(response.getMaxRetryAttempts()).isEqualTo(2);
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void handleAuthorizeNetError_WithVelocityLimitError_ShouldReturnLongRetryDelay() {
        // Given
        mockTransactionResponse.setResponseCode("141");
        mockTransactionResponse.getMessages().getMessage().get(0).setCode("141");
        mockTransactionResponse.getMessages().getMessage().get(0).setDescription("Transaction velocity limit exceeded");

        // When
        PaymentResponse response = paymentErrorHandler.handleAuthorizeNetError(mockTransactionResponse, testTransaction);

        // Then
        assertThat(response.getErrorCategory()).isEqualTo("VELOCITY_LIMIT");
        assertThat(response.getRetryable()).isTrue();
        assertThat(response.getRetryAfterSeconds()).isEqualTo(300); // 5 minutes
        assertThat(response.getMaxRetryAttempts()).isEqualTo(1);
    }

    @Test
    void handleAuthorizeNetError_WithUnknownErrorCode_ShouldReturnGenericError() {
        // Given
        mockTransactionResponse.setResponseCode("999");
        mockTransactionResponse.getMessages().getMessage().get(0).setCode("999");
        mockTransactionResponse.getMessages().getMessage().get(0).setDescription("Unknown error occurred");

        // When
        PaymentResponse response = paymentErrorHandler.handleAuthorizeNetError(mockTransactionResponse, testTransaction);

        // Then
        assertThat(response.getErrorCode()).isEqualTo("999");
        assertThat(response.getErrorCategory()).isEqualTo("PAYMENT_FAILED");
        assertThat(response.getErrorMessage()).isEqualTo("Unknown error occurred");
        assertThat(response.getRetryable()).isTrue();
        assertThat(response.getSuggestedAction()).isEqualTo("Please try again or contact support");
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
    void handleGeneralError_WithDuplicateException_ShouldCategorizeCorrectly() {
        // Given
        Exception duplicateException = new RuntimeException("Duplicate transaction found");

        // When
        PaymentResponse response = paymentErrorHandler.handleGeneralError(duplicateException, testTransaction, "capture");

        // Then
        assertThat(response.getErrorCategory()).isEqualTo("DUPLICATE_TRANSACTION");
        assertThat(response.getErrorMessage()).isEqualTo("Transaction already processed");
        assertThat(response.getRetryable()).isFalse();
        assertThat(response.getSuggestedAction()).isEqualTo("Check transaction history");
    }

    @Test
    void handleGeneralError_WithGenericException_ShouldCategorizeAsSystemError() {
        // Given
        Exception genericException = new RuntimeException("Something went wrong");

        // When
        PaymentResponse response = paymentErrorHandler.handleGeneralError(genericException, testTransaction, "void");

        // Then
        assertThat(response.getErrorCategory()).isEqualTo("SYSTEM_ERROR");
        assertThat(response.getErrorMessage()).isEqualTo("Internal system error");
        assertThat(response.getRetryable()).isTrue();
        assertThat(response.getSuggestedAction()).isEqualTo("Please contact support");
        assertThat(response.getRetryAfterSeconds()).isEqualTo(10);
        assertThat(response.getMaxRetryAttempts()).isEqualTo(1);
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
    void handleValidationError_WithTransactionShouldReturnValidationErrorResponse() {
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
        assertThat(response.getValidationErrors()).hasSize(1);
        assertThat(response.getValidationErrors().get(0).getField()).isEqualTo(field);
        assertThat(response.getValidationErrors().get(0).getMessage()).isEqualTo(message);
    }

    @Test
    void createErrorResponse_WithNullTransaction_ShouldHandleGracefully() {
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
    void errorHandling_ShouldSetCorrectRetryParametersForDifferentCategories() {
        // Test network error retry parameters
        Exception networkException = new RuntimeException("network connection failed");
        PaymentResponse networkResponse = paymentErrorHandler.handleGeneralError(networkException, testTransaction, "test");
        assertThat(networkResponse.getRetryAfterSeconds()).isEqualTo(30);
        assertThat(networkResponse.getMaxRetryAttempts()).isEqualTo(3);

        // Test processing error retry parameters
        testTransaction.setStatus(PaymentStatus.PENDING); // Reset status
        Exception processingException = new RuntimeException("processing error occurred");
        PaymentResponse processingResponse = paymentErrorHandler.handleGeneralError(processingException, testTransaction, "test");
        assertThat(processingResponse.getRetryAfterSeconds()).isEqualTo(10); // Default for SYSTEM_ERROR
        assertThat(processingResponse.getMaxRetryAttempts()).isEqualTo(1);

        // Test timeout error retry parameters
        testTransaction.setStatus(PaymentStatus.PENDING); // Reset status
        PaymentResponse timeoutResponse = paymentErrorHandler.handleTimeoutError(testTransaction, 5000);
        assertThat(timeoutResponse.getRetryAfterSeconds()).isEqualTo(30);
        assertThat(timeoutResponse.getMaxRetryAttempts()).isEqualTo(3);
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
    void transactionStatusDetermination_ShouldSetCorrectStatusBasedOnErrorCategory() {
        // Test network error -> PENDING
        Exception networkException = new RuntimeException("network failed");
        paymentErrorHandler.handleGeneralError(networkException, testTransaction, "test");
        // Note: handleGeneralError sets FAILED for all general errors, but handleNetworkError sets FAILED
        // and handleTimeoutError sets PENDING

        // Test timeout -> PENDING
        testTransaction.setStatus(PaymentStatus.PENDING); // Reset
        paymentErrorHandler.handleTimeoutError(testTransaction, 1000);
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.PENDING);

        // Test duplicate transaction via Authorize.Net -> FAILED
        testTransaction.setStatus(PaymentStatus.PENDING); // Reset
        mockTransactionResponse.setResponseCode("11");
        mockTransactionResponse.getMessages().getMessage().get(0).setCode("11");
        paymentErrorHandler.handleAuthorizeNetError(mockTransactionResponse, testTransaction);
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.FAILED);

        // Test processing error via Authorize.Net -> PENDING
        testTransaction.setStatus(PaymentStatus.PENDING); // Reset
        mockTransactionResponse.setResponseCode("19");
        mockTransactionResponse.getMessages().getMessage().get(0).setCode("19");
        paymentErrorHandler.handleAuthorizeNetError(mockTransactionResponse, testTransaction);
        assertThat(testTransaction.getStatus()).isEqualTo(PaymentStatus.PENDING);
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
