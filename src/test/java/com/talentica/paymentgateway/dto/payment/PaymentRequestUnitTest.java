package com.talentica.paymentgateway.dto.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PaymentRequest DTO.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
class PaymentRequestUnitTest {

    private Validator validator;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        paymentRequest = new PaymentRequest();
    }

    @Test
    void defaultConstructor_ShouldCreateEmptyRequest() {
        // When
        PaymentRequest request = new PaymentRequest();

        // Then
        assertNotNull(request);
        assertNull(request.getAmount());
        assertEquals("USD", request.getCurrency());
        assertNull(request.getDescription());
        assertNull(request.getCustomerId());
        assertNull(request.getInvoiceNumber());
        assertNull(request.getOrderNumber());
        assertNull(request.getPaymentMethod());
        assertNull(request.getCustomer());
        assertNull(request.getBillingAddress());
        assertNull(request.getShippingAddress());
        assertNull(request.getMetadata());
        assertNull(request.getIdempotencyKey());
        assertEquals(false, request.getTestMode());
    }

    @Test
    void constructorWithRequiredFields_ShouldSetFields() {
        // Given
        BigDecimal amount = new BigDecimal("99.99");
        String currency = "EUR";
        PaymentMethodRequest paymentMethod = new PaymentMethodRequest();

        // When
        PaymentRequest request = PaymentRequest.builder()
            .amount(amount)
            .currency(currency)
            .paymentMethod(paymentMethod)
            .build();

        // Then
        assertEquals(amount, request.getAmount());
        assertEquals(currency, request.getCurrency());
        assertEquals(paymentMethod, request.getPaymentMethod());
    }

    @Test
    void setAmount_WithValidAmount_ShouldSetAmount() {
        // Given
        BigDecimal amount = new BigDecimal("100.50");

        // When
        paymentRequest.setAmount(amount);

        // Then
        assertEquals(amount, paymentRequest.getAmount());
    }

    @Test
    void setCurrency_WithValidCurrency_ShouldSetCurrency() {
        // Given
        String currency = "EUR";

        // When
        paymentRequest.setCurrency(currency);

        // Then
        assertEquals(currency, paymentRequest.getCurrency());
    }

    @Test
    void setDescription_WithValidDescription_ShouldSetDescription() {
        // Given
        String description = "Payment for services";

        // When
        paymentRequest.setDescription(description);

        // Then
        assertEquals(description, paymentRequest.getDescription());
    }

    @Test
    void setCustomerId_WithValidCustomerId_ShouldSetCustomerId() {
        // Given
        String customerId = "CUST_12345";

        // When
        paymentRequest.setCustomerId(customerId);

        // Then
        assertEquals(customerId, paymentRequest.getCustomerId());
    }

    @Test
    void setInvoiceNumber_WithValidInvoiceNumber_ShouldSetInvoiceNumber() {
        // Given
        String invoiceNumber = "INV-2024-001";

        // When
        paymentRequest.setInvoiceNumber(invoiceNumber);

        // Then
        assertEquals(invoiceNumber, paymentRequest.getInvoiceNumber());
    }

    @Test
    void setOrderNumber_WithValidOrderNumber_ShouldSetOrderNumber() {
        // Given
        String orderNumber = "ORDER-2024-001";

        // When
        paymentRequest.setOrderNumber(orderNumber);

        // Then
        assertEquals(orderNumber, paymentRequest.getOrderNumber());
    }

    @Test
    void setPaymentMethod_WithValidPaymentMethod_ShouldSetPaymentMethod() {
        // Given
        PaymentMethodRequest paymentMethod = new PaymentMethodRequest();

        // When
        paymentRequest.setPaymentMethod(paymentMethod);

        // Then
        assertEquals(paymentMethod, paymentRequest.getPaymentMethod());
    }

    @Test
    void setCustomer_WithValidCustomer_ShouldSetCustomer() {
        // Given
        CustomerRequest customer = new CustomerRequest();

        // When
        paymentRequest.setCustomer(customer);

        // Then
        assertEquals(customer, paymentRequest.getCustomer());
    }

    @Test
    void setBillingAddress_WithValidAddress_ShouldSetBillingAddress() {
        // Given
        AddressRequest billingAddress = new AddressRequest();

        // When
        paymentRequest.setBillingAddress(billingAddress);

        // Then
        assertEquals(billingAddress, paymentRequest.getBillingAddress());
    }

    @Test
    void setShippingAddress_WithValidAddress_ShouldSetShippingAddress() {
        // Given
        AddressRequest shippingAddress = new AddressRequest();

        // When
        paymentRequest.setShippingAddress(shippingAddress);

        // Then
        assertEquals(shippingAddress, paymentRequest.getShippingAddress());
    }

    @Test
    void setMetadata_WithValidMetadata_ShouldSetMetadata() {
        // Given
        Map<String, String> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", "value2");

        // When
        paymentRequest.setMetadata(metadata);

        // Then
        assertEquals(metadata, paymentRequest.getMetadata());
        assertEquals("value1", paymentRequest.getMetadata().get("key1"));
        assertEquals("value2", paymentRequest.getMetadata().get("key2"));
    }

    @Test
    void setIdempotencyKey_WithValidKey_ShouldSetIdempotencyKey() {
        // Given
        String idempotencyKey = "idempotency-key-123";

        // When
        paymentRequest.setIdempotencyKey(idempotencyKey);

        // Then
        assertEquals(idempotencyKey, paymentRequest.getIdempotencyKey());
    }

    @Test
    void setTestMode_WithValidTestMode_ShouldSetTestMode() {
        // Given
        Boolean testMode = true;

        // When
        paymentRequest.setTestMode(testMode);

        // Then
        assertEquals(testMode, paymentRequest.getTestMode());
    }

    @Test
    void validation_WithNullAmount_ShouldHaveViolation() {
        // Given
        paymentRequest.setAmount(null);
        paymentRequest.setCurrency("USD");

        // When
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(paymentRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Amount is required")));
    }

    @Test
    void validation_WithInvalidCurrencyLength_ShouldHaveViolation() {
        // Given
        paymentRequest.setAmount(new BigDecimal("100.00"));
        paymentRequest.setCurrency("US"); // Too short

        // When
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(paymentRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Currency must be a 3-letter ISO code")));
    }

    @Test
    void validation_WithInvalidCurrencyPattern_ShouldHaveViolation() {
        // Given
        paymentRequest.setAmount(new BigDecimal("100.00"));
        paymentRequest.setCurrency("us1"); // Invalid pattern

        // When
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(paymentRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Currency must be a valid 3-letter ISO code")));
    }

    @Test
    void validation_WithBlankCurrency_ShouldHaveViolation() {
        // Given
        paymentRequest.setAmount(new BigDecimal("100.00"));
        paymentRequest.setCurrency("");

        // When
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(paymentRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Currency is required")));
    }

    @Test
    void validation_WithTooLongDescription_ShouldHaveViolation() {
        // Given
        paymentRequest.setAmount(new BigDecimal("100.00"));
        paymentRequest.setCurrency("USD");
        paymentRequest.setDescription("a".repeat(256)); // Too long

        // When
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(paymentRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Description must not exceed 255 characters")));
    }

    @Test
    void validation_WithTooLongCustomerId_ShouldHaveViolation() {
        // Given
        paymentRequest.setAmount(new BigDecimal("100.00"));
        paymentRequest.setCurrency("USD");
        paymentRequest.setCustomerId("a".repeat(51)); // Too long

        // When
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(paymentRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Customer ID must not exceed 50 characters")));
    }

    @Test
    void validation_WithTooLongInvoiceNumber_ShouldHaveViolation() {
        // Given
        paymentRequest.setAmount(new BigDecimal("100.00"));
        paymentRequest.setCurrency("USD");
        paymentRequest.setInvoiceNumber("a".repeat(26)); // Too long

        // When
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(paymentRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Invoice number must not exceed 25 characters")));
    }

    @Test
    void validation_WithTooLongOrderNumber_ShouldHaveViolation() {
        // Given
        paymentRequest.setAmount(new BigDecimal("100.00"));
        paymentRequest.setCurrency("USD");
        paymentRequest.setOrderNumber("a".repeat(51)); // Too long

        // When
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(paymentRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Order number must not exceed 50 characters")));
    }

    @Test
    void validation_WithTooLongIdempotencyKey_ShouldHaveViolation() {
        // Given
        paymentRequest.setAmount(new BigDecimal("100.00"));
        paymentRequest.setCurrency("USD");
        paymentRequest.setIdempotencyKey("a".repeat(256)); // Too long

        // When
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(paymentRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Idempotency key must not exceed 255 characters")));
    }

    @Test
    void validation_WithValidData_ShouldHaveNoViolations() {
        // Given
        paymentRequest.setAmount(new BigDecimal("100.00"));
        paymentRequest.setCurrency("USD");
        paymentRequest.setDescription("Valid payment");
        paymentRequest.setCustomerId("CUST_123");
        paymentRequest.setInvoiceNumber("INV-001");
        paymentRequest.setOrderNumber("ORDER-001");
        paymentRequest.setIdempotencyKey("key-123");
        paymentRequest.setTestMode(false);

        // When
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(paymentRequest);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void toString_ShouldReturnFormattedString() {
        // Given
        paymentRequest.setAmount(new BigDecimal("99.99"));
        paymentRequest.setCurrency("USD");
        paymentRequest.setDescription("Test payment");
        paymentRequest.setCustomerId("CUST_123");
        paymentRequest.setInvoiceNumber("INV-001");
        paymentRequest.setOrderNumber("ORDER-001");
        paymentRequest.setIdempotencyKey("key-123");
        paymentRequest.setTestMode(true);

        // When
        String result = paymentRequest.toString();

        // Then - Lombok format
        assertNotNull(result);
        assertTrue(result.contains("PaymentRequest(")); // Lombok uses parentheses
        assertTrue(result.contains("amount=99.99"));
        assertTrue(result.contains("currency=USD")); // No quotes in Lombok toString
        assertTrue(result.contains("description=Test payment"));
        assertTrue(result.contains("customerId=CUST_123"));
        assertTrue(result.contains("invoiceNumber=INV-001")); // No quotes
        assertTrue(result.contains("orderNumber=ORDER-001"));
        assertTrue(result.contains("idempotencyKey=key-123"));
        assertTrue(result.contains("testMode=true"));
    }

    @Test
    void setAmount_WithNullAmount_ShouldSetNull() {
        // When
        paymentRequest.setAmount(null);

        // Then
        assertNull(paymentRequest.getAmount());
    }

    @Test
    void setCurrency_WithNullCurrency_ShouldSetNull() {
        // When
        paymentRequest.setCurrency(null);

        // Then
        assertNull(paymentRequest.getCurrency());
    }

    @Test
    void setTestMode_WithNullTestMode_ShouldSetNull() {
        // When
        paymentRequest.setTestMode(null);

        // Then
        assertNull(paymentRequest.getTestMode());
    }

    @Test
    void setMetadata_WithNullMetadata_ShouldSetNull() {
        // When
        paymentRequest.setMetadata(null);

        // Then
        assertNull(paymentRequest.getMetadata());
    }

    @Test
    void setMetadata_WithEmptyMetadata_ShouldSetEmptyMap() {
        // Given
        Map<String, String> emptyMetadata = new HashMap<>();

        // When
        paymentRequest.setMetadata(emptyMetadata);

        // Then
        assertNotNull(paymentRequest.getMetadata());
        assertTrue(paymentRequest.getMetadata().isEmpty());
    }

    @Test
    void validation_WithValidCurrencyCodes_ShouldHaveNoViolations() {
        // Given
        String[] validCurrencies = {"USD", "EUR", "GBP", "JPY", "CAD"};
        
        for (String currency : validCurrencies) {
            PaymentRequest request = new PaymentRequest();
            request.setAmount(new BigDecimal("100.00"));
            request.setCurrency(currency);

            // When
            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

            // Then
            assertTrue(violations.stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("currency")),
                "Currency " + currency + " should be valid");
        }
    }

    @Test
    void validation_WithLowercaseCurrency_ShouldHaveViolation() {
        // Given
        paymentRequest.setAmount(new BigDecimal("100.00"));
        paymentRequest.setCurrency("usd"); // Lowercase

        // When
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(paymentRequest);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Currency must be a valid 3-letter ISO code")));
    }

    @Test
    void validation_WithMaxLengthFields_ShouldHaveNoViolations() {
        // Given
        paymentRequest.setAmount(new BigDecimal("100.00"));
        paymentRequest.setCurrency("USD");
        paymentRequest.setDescription("a".repeat(255)); // Max length
        paymentRequest.setCustomerId("a".repeat(50)); // Max length
        paymentRequest.setInvoiceNumber("a".repeat(25)); // Max length
        paymentRequest.setOrderNumber("a".repeat(50)); // Max length
        paymentRequest.setIdempotencyKey("a".repeat(255)); // Max length

        // When
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(paymentRequest);

        // Then
        assertTrue(violations.isEmpty());
    }
}
