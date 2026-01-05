package com.talentica.paymentgateway.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Transaction entity.
 * Tests constructors, getters, setters, validation constraints, relationships, and utility methods.
 */
@DisplayName("Transaction Entity Unit Tests")
class TransactionUnitTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create Transaction with default constructor")
    void shouldCreateWithDefaultConstructor() {
        // When
        Transaction transaction = new Transaction();

        // Then
        assertThat(transaction).isNotNull();
        assertThat(transaction.getTransactionId()).isNull();
        assertThat(transaction.getOrder()).isNull();
        assertThat(transaction.getCustomer()).isNull();
        assertThat(transaction.getPaymentMethod()).isNull();
        assertThat(transaction.getTransactionType()).isNull();
        assertThat(transaction.getAmount()).isNull();
        assertThat(transaction.getCurrency()).isEqualTo("USD");
        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(transaction.getParentTransaction()).isNull();
        assertThat(transaction.getRequestData()).isNotNull().isEmpty();
        assertThat(transaction.getResponseData()).isNotNull().isEmpty();
        assertThat(transaction.getChildTransactions()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should create Transaction with parameterized constructor")
    void shouldCreateWithParameterizedConstructor() {
        // Given
        String transactionId = "TXN_123";
        Customer customer = new Customer();
        TransactionType transactionType = TransactionType.PURCHASE;
        BigDecimal amount = new BigDecimal("99.99");

        // When
        Transaction transaction = new Transaction(transactionId, customer, transactionType, amount);

        // Then
        assertThat(transaction).isNotNull();
        assertThat(transaction.getTransactionId()).isEqualTo(transactionId);
        assertThat(transaction.getCustomer()).isEqualTo(customer);
        assertThat(transaction.getTransactionType()).isEqualTo(transactionType);
        assertThat(transaction.getAmount()).isEqualTo(amount);
        assertThat(transaction.getCurrency()).isEqualTo("USD");
        assertThat(transaction.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Should set and get all basic fields")
    void shouldSetAndGetAllBasicFields() {
        // Given
        Transaction transaction = new Transaction();
        String transactionId = "TXN_456";
        Order order = new Order();
        Customer customer = new Customer();
        PaymentMethod paymentMethod = new PaymentMethod();
        TransactionType transactionType = TransactionType.AUTHORIZE;
        BigDecimal amount = new BigDecimal("150.00");
        String currency = "EUR";
        PaymentStatus status = PaymentStatus.AUTHORIZED;
        String idempotencyKey = "IDEM_123";
        String correlationId = "CORR_456";
        ZonedDateTime processedAt = ZonedDateTime.now();

        // When
        transaction.setTransactionId(transactionId);
        transaction.setOrder(order);
        transaction.setCustomer(customer);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setStatus(status);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setCorrelationId(correlationId);
        transaction.setProcessedAt(processedAt);

        // Then
        assertThat(transaction.getTransactionId()).isEqualTo(transactionId);
        assertThat(transaction.getOrder()).isEqualTo(order);
        assertThat(transaction.getCustomer()).isEqualTo(customer);
        assertThat(transaction.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(transaction.getTransactionType()).isEqualTo(transactionType);
        assertThat(transaction.getAmount()).isEqualTo(amount);
        assertThat(transaction.getCurrency()).isEqualTo(currency);
        assertThat(transaction.getStatus()).isEqualTo(status);
        assertThat(transaction.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(transaction.getCorrelationId()).isEqualTo(correlationId);
        assertThat(transaction.getProcessedAt()).isEqualTo(processedAt);
    }

    @Test
    @DisplayName("Should set and get Authorize.Net specific fields")
    void shouldSetAndGetAuthorizeNetFields() {
        // Given
        Transaction transaction = new Transaction();
        String authnetTransactionId = "AUTH_123";
        String authnetAuthCode = "ABC123";
        String authnetAvsResult = "Y";
        String authnetCvvResult = "M";
        String authnetResponseCode = "1";
        String authnetResponseReason = "Approved";

        // When
        transaction.setAuthnetTransactionId(authnetTransactionId);
        transaction.setAuthnetAuthCode(authnetAuthCode);
        transaction.setAuthnetAvsResult(authnetAvsResult);
        transaction.setAuthnetCvvResult(authnetCvvResult);
        transaction.setAuthnetResponseCode(authnetResponseCode);
        transaction.setAuthnetResponseReason(authnetResponseReason);

        // Then
        assertThat(transaction.getAuthnetTransactionId()).isEqualTo(authnetTransactionId);
        assertThat(transaction.getAuthnetAuthCode()).isEqualTo(authnetAuthCode);
        assertThat(transaction.getAuthnetAvsResult()).isEqualTo(authnetAvsResult);
        assertThat(transaction.getAuthnetCvvResult()).isEqualTo(authnetCvvResult);
        assertThat(transaction.getAuthnetResponseCode()).isEqualTo(authnetResponseCode);
        assertThat(transaction.getAuthnetResponseReason()).isEqualTo(authnetResponseReason);
    }

    @Test
    @DisplayName("Should validate successfully with valid data")
    void shouldValidateSuccessfullyWithValidData() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN_123");
        transaction.setCustomer(new Customer());
        transaction.setTransactionType(TransactionType.PURCHASE);
        transaction.setAmount(new BigDecimal("99.99"));

        // When
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when transaction ID is null")
    void shouldFailValidationWhenTransactionIdIsNull() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setTransactionId(null);
        transaction.setCustomer(new Customer());
        transaction.setTransactionType(TransactionType.PURCHASE);
        transaction.setAmount(new BigDecimal("99.99"));

        // When
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Transaction ID is required");
    }

    @Test
    @DisplayName("Should fail validation when customer is null")
    void shouldFailValidationWhenCustomerIsNull() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN_123");
        transaction.setCustomer(null);
        transaction.setTransactionType(TransactionType.PURCHASE);
        transaction.setAmount(new BigDecimal("99.99"));

        // When
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Customer is required");
    }

    @Test
    @DisplayName("Should fail validation when transaction type is null")
    void shouldFailValidationWhenTransactionTypeIsNull() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN_123");
        transaction.setCustomer(new Customer());
        transaction.setTransactionType(null);
        transaction.setAmount(new BigDecimal("99.99"));

        // When
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Transaction type is required");
    }

    @Test
    @DisplayName("Should fail validation when amount is null")
    void shouldFailValidationWhenAmountIsNull() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN_123");
        transaction.setCustomer(new Customer());
        transaction.setTransactionType(TransactionType.PURCHASE);
        transaction.setAmount(null);

        // When
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Then - Expects 2 violations: @NotNull and @ValidAmount
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
            .contains("Amount is required");
    }

    @Test
    @DisplayName("Should fail validation when amount is zero or negative")
    void shouldFailValidationWhenAmountIsZeroOrNegative() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN_123");
        transaction.setCustomer(new Customer());
        transaction.setTransactionType(TransactionType.PURCHASE);
        transaction.setAmount(BigDecimal.ZERO);

        // When
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Then - Expects 2 violations: @DecimalMin and @ValidAmount
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
            .contains("Amount must be greater than zero");
    }

    @Test
    @DisplayName("Should detect successful transaction status")
    void shouldDetectSuccessfulTransactionStatus() {
        // Given
        Transaction transaction = new Transaction();

        // When & Then - Test authorized status
        transaction.setStatus(PaymentStatus.AUTHORIZED);
        assertThat(transaction.isSuccessful()).isTrue();
        assertThat(transaction.isFailed()).isFalse();

        // When & Then - Test captured status
        transaction.setStatus(PaymentStatus.CAPTURED);
        assertThat(transaction.isSuccessful()).isTrue();
        assertThat(transaction.isFailed()).isFalse();

        // When & Then - Test settled status
        transaction.setStatus(PaymentStatus.SETTLED);
        assertThat(transaction.isSuccessful()).isTrue();
        assertThat(transaction.isFailed()).isFalse();
    }

    @Test
    @DisplayName("Should detect failed transaction status")
    void shouldDetectFailedTransactionStatus() {
        // Given
        Transaction transaction = new Transaction();

        // When & Then - Test failed status
        transaction.setStatus(PaymentStatus.FAILED);
        assertThat(transaction.isFailed()).isTrue();
        assertThat(transaction.isSuccessful()).isFalse();

        // When & Then - Test voided status
        transaction.setStatus(PaymentStatus.VOIDED);
        assertThat(transaction.isFailed()).isTrue();
        assertThat(transaction.isSuccessful()).isFalse();

        // When & Then - Test cancelled status
        transaction.setStatus(PaymentStatus.CANCELLED);
        assertThat(transaction.isFailed()).isTrue();
        assertThat(transaction.isSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Should determine if transaction can be voided")
    void shouldDetermineIfTransactionCanBeVoided() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setStatus(PaymentStatus.AUTHORIZED);

        // When & Then - Test authorize transaction
        transaction.setTransactionType(TransactionType.AUTHORIZE);
        assertThat(transaction.canBeVoided()).isTrue();

        // When & Then - Test purchase transaction
        transaction.setTransactionType(TransactionType.PURCHASE);
        assertThat(transaction.canBeVoided()).isTrue();

        // When & Then - Test other transaction types
        transaction.setTransactionType(TransactionType.CAPTURE);
        assertThat(transaction.canBeVoided()).isFalse();

        // When & Then - Test non-authorized status
        transaction.setTransactionType(TransactionType.AUTHORIZE);
        transaction.setStatus(PaymentStatus.SETTLED);
        assertThat(transaction.canBeVoided()).isFalse();
    }

    @Test
    @DisplayName("Should determine if transaction can be captured")
    void shouldDetermineIfTransactionCanBeCaptured() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setStatus(PaymentStatus.AUTHORIZED);
        transaction.setTransactionType(TransactionType.AUTHORIZE);

        // When & Then
        assertThat(transaction.canBeCaptured()).isTrue();

        // When & Then - Test non-authorize transaction
        transaction.setTransactionType(TransactionType.PURCHASE);
        assertThat(transaction.canBeCaptured()).isFalse();

        // When & Then - Test non-authorized status
        transaction.setTransactionType(TransactionType.AUTHORIZE);
        transaction.setStatus(PaymentStatus.SETTLED);
        assertThat(transaction.canBeCaptured()).isFalse();
    }

    @Test
    @DisplayName("Should determine if transaction can be refunded")
    void shouldDetermineIfTransactionCanBeRefunded() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setStatus(PaymentStatus.SETTLED);

        // When & Then - Test purchase transaction
        transaction.setTransactionType(TransactionType.PURCHASE);
        assertThat(transaction.canBeRefunded()).isTrue();

        // When & Then - Test capture transaction
        transaction.setTransactionType(TransactionType.CAPTURE);
        assertThat(transaction.canBeRefunded()).isTrue();

        // When & Then - Test other transaction types
        transaction.setTransactionType(TransactionType.AUTHORIZE);
        assertThat(transaction.canBeRefunded()).isFalse();

        // When & Then - Test non-settled status
        transaction.setTransactionType(TransactionType.PURCHASE);
        transaction.setStatus(PaymentStatus.AUTHORIZED);
        assertThat(transaction.canBeRefunded()).isFalse();
    }

    @Test
    @DisplayName("Should add and remove child transactions")
    void shouldAddAndRemoveChildTransactions() {
        // Given
        Transaction parentTransaction = new Transaction();
        Transaction childTransaction = new Transaction();

        // When - Add child transaction
        parentTransaction.addChildTransaction(childTransaction);

        // Then
        assertThat(parentTransaction.getChildTransactions()).contains(childTransaction);
        assertThat(childTransaction.getParentTransaction()).isEqualTo(parentTransaction);

        // When - Remove child transaction
        parentTransaction.removeChildTransaction(childTransaction);

        // Then
        assertThat(parentTransaction.getChildTransactions()).doesNotContain(childTransaction);
        assertThat(childTransaction.getParentTransaction()).isNull();
    }

    @Test
    @DisplayName("Should calculate refunded amount correctly")
    void shouldCalculateRefundedAmountCorrectly() {
        // Given
        Transaction parentTransaction = new Transaction();
        parentTransaction.setAmount(new BigDecimal("100.00"));

        Transaction refund1 = new Transaction();
        refund1.setTransactionType(TransactionType.REFUND);
        refund1.setStatus(PaymentStatus.SETTLED);
        refund1.setAmount(new BigDecimal("30.00"));

        Transaction refund2 = new Transaction();
        refund2.setTransactionType(TransactionType.PARTIAL_REFUND);
        refund2.setStatus(PaymentStatus.SETTLED);
        refund2.setAmount(new BigDecimal("20.00"));

        Transaction pendingRefund = new Transaction();
        pendingRefund.setTransactionType(TransactionType.REFUND);
        pendingRefund.setStatus(PaymentStatus.PENDING);
        pendingRefund.setAmount(new BigDecimal("10.00"));

        parentTransaction.addChildTransaction(refund1);
        parentTransaction.addChildTransaction(refund2);
        parentTransaction.addChildTransaction(pendingRefund);

        // When
        BigDecimal refundedAmount = parentTransaction.getRefundedAmount();

        // Then
        assertThat(refundedAmount).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Should calculate available refund amount correctly")
    void shouldCalculateAvailableRefundAmountCorrectly() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setStatus(PaymentStatus.SETTLED);
        transaction.setTransactionType(TransactionType.PURCHASE);

        Transaction refund = new Transaction();
        refund.setTransactionType(TransactionType.REFUND);
        refund.setStatus(PaymentStatus.SETTLED);
        refund.setAmount(new BigDecimal("30.00"));

        transaction.addChildTransaction(refund);

        // When
        BigDecimal availableRefund = transaction.getAvailableRefundAmount();

        // Then
        assertThat(availableRefund).isEqualTo(new BigDecimal("70.00"));
    }

    @Test
    @DisplayName("Should return zero available refund for non-refundable transaction")
    void shouldReturnZeroAvailableRefundForNonRefundableTransaction() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setStatus(PaymentStatus.AUTHORIZED);
        transaction.setTransactionType(TransactionType.AUTHORIZE);

        // When
        BigDecimal availableRefund = transaction.getAvailableRefundAmount();

        // Then
        assertThat(availableRefund).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should mark transaction as processed")
    void shouldMarkTransactionAsProcessed() {
        // Given
        Transaction transaction = new Transaction();
        assertThat(transaction.getProcessedAt()).isNull();

        // When
        transaction.markAsProcessed();

        // Then
        assertThat(transaction.getProcessedAt()).isNotNull();
        assertThat(transaction.getProcessedAt()).isBeforeOrEqualTo(ZonedDateTime.now());
    }

    @Test
    @DisplayName("Should add request and response data")
    void shouldAddRequestAndResponseData() {
        // Given
        Transaction transaction = new Transaction();
        String requestKey = "cardNumber";
        String requestValue = "****1234";
        String responseKey = "authCode";
        String responseValue = "ABC123";

        // When
        transaction.addRequestData(requestKey, requestValue);
        transaction.addResponseData(responseKey, responseValue);

        // Then
        assertThat(transaction.getRequestData()).containsEntry(requestKey, requestValue);
        assertThat(transaction.getResponseData()).containsEntry(responseKey, responseValue);
    }

    @Test
    @DisplayName("Should handle null request and response data maps")
    void shouldHandleNullRequestAndResponseDataMaps() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setRequestData(null);
        transaction.setResponseData(null);

        // When
        transaction.addRequestData("key1", "value1");
        transaction.addResponseData("key2", "value2");

        // Then
        assertThat(transaction.getRequestData()).isNotNull();
        assertThat(transaction.getRequestData()).containsEntry("key1", "value1");
        assertThat(transaction.getResponseData()).isNotNull();
        assertThat(transaction.getResponseData()).containsEntry("key2", "value2");
    }

    @Test
    @DisplayName("Should set and get relationship collections")
    void shouldSetAndGetRelationshipCollections() {
        // Given
        Transaction transaction = new Transaction();
        List<Transaction> childTransactions = new ArrayList<>();
        Map<String, Object> requestData = new HashMap<>();
        Map<String, Object> responseData = new HashMap<>();

        // When
        transaction.setChildTransactions(childTransactions);
        transaction.setRequestData(requestData);
        transaction.setResponseData(responseData);

        // Then
        assertThat(transaction.getChildTransactions()).isEqualTo(childTransactions);
        assertThat(transaction.getRequestData()).isEqualTo(requestData);
        assertThat(transaction.getResponseData()).isEqualTo(responseData);
    }

    @Test
    @DisplayName("Should handle parent-child transaction relationships")
    void shouldHandleParentChildTransactionRelationships() {
        // Given
        Transaction parentTransaction = new Transaction();
        Transaction childTransaction = new Transaction();

        // When
        childTransaction.setParentTransaction(parentTransaction);

        // Then
        assertThat(childTransaction.getParentTransaction()).isEqualTo(parentTransaction);
    }

    @Test
    @DisplayName("Should validate with maximum length fields")
    void shouldValidateWithMaximumLengthFields() {
        // Given
        Transaction transaction = new Transaction();
        transaction.setTransactionId("a".repeat(100));
        transaction.setCustomer(new Customer());
        transaction.setTransactionType(TransactionType.PURCHASE);
        transaction.setAmount(new BigDecimal("99.99"));
        transaction.setCurrency("USD");
        transaction.setAuthnetTransactionId("a".repeat(100));
        transaction.setAuthnetAuthCode("a".repeat(20));
        transaction.setAuthnetAvsResult("a".repeat(10));
        transaction.setAuthnetCvvResult("a".repeat(10));
        transaction.setAuthnetResponseCode("a".repeat(10));
        transaction.setAuthnetResponseReason("a".repeat(255));
        transaction.setIdempotencyKey("a".repeat(255));
        transaction.setCorrelationId("a".repeat(100));

        // When
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple child transactions of different types")
    void shouldHandleMultipleChildTransactionsOfDifferentTypes() {
        // Given
        Transaction parentTransaction = new Transaction();
        parentTransaction.setAmount(new BigDecimal("100.00"));
        parentTransaction.setStatus(PaymentStatus.SETTLED);
        parentTransaction.setTransactionType(TransactionType.PURCHASE);

        Transaction refund = new Transaction();
        refund.setTransactionType(TransactionType.REFUND);
        refund.setStatus(PaymentStatus.SETTLED);
        refund.setAmount(new BigDecimal("25.00"));

        Transaction partialRefund = new Transaction();
        partialRefund.setTransactionType(TransactionType.PARTIAL_REFUND);
        partialRefund.setStatus(PaymentStatus.SETTLED);
        partialRefund.setAmount(new BigDecimal("15.00"));

        Transaction voidTransaction = new Transaction();
        voidTransaction.setTransactionType(TransactionType.VOID);
        voidTransaction.setStatus(PaymentStatus.VOIDED);
        voidTransaction.setAmount(new BigDecimal("0.00"));

        // When
        parentTransaction.addChildTransaction(refund);
        parentTransaction.addChildTransaction(partialRefund);
        parentTransaction.addChildTransaction(voidTransaction);

        // Then
        assertThat(parentTransaction.getChildTransactions()).hasSize(3);
        assertThat(parentTransaction.getRefundedAmount()).isEqualTo(new BigDecimal("40.00"));
        assertThat(parentTransaction.getAvailableRefundAmount()).isEqualTo(new BigDecimal("60.00"));
    }
}
