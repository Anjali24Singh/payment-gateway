package com.talentica.paymentgateway.dto;

import com.talentica.paymentgateway.dto.payment.PaymentMethodRequest;
import com.talentica.paymentgateway.dto.payment.PaymentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("AuthenticationRequest requires valid email and password length")
    void authenticationRequest_validation() {
        AuthenticationRequest req = new AuthenticationRequest("bad-email", "short");
        Set<ConstraintViolation<AuthenticationRequest>> violations = validator.validate(req);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("RegistrationRequest enforces strong password pattern")
    void registrationRequest_passwordPattern() {
        RegistrationRequest req = new RegistrationRequest();
        req.setEmail("user@example.com");
        req.setPassword("weakpass");
        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(req);
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password"))).isTrue();
    }

    @Test
    @DisplayName("PaymentRequest requires amount, currency, and nested payment method when provided")
    void paymentRequest_basicValidation() {
        PaymentRequest req = new PaymentRequest();
        Set<ConstraintViolation<PaymentRequest>> v1 = validator.validate(req);
        assertThat(v1).isNotEmpty();

        req.setAmount(new BigDecimal("10.50"));
        req.setCurrency("USD");
        PaymentMethodRequest pm = new PaymentMethodRequest();
        pm.setType("CREDIT_CARD");
        pm.setCardNumber("4111111111111111");
        pm.setExpiryMonth("12");
        pm.setExpiryYear("2030");
        pm.setCvv("123");
        req.setPaymentMethod(pm);
        Set<ConstraintViolation<PaymentRequest>> v2 = validator.validate(req);
        assertThat(v2).isEmpty();
    }

    @Test
    @DisplayName("PaymentMethodRequest validates by type: CREDIT_CARD")
    void paymentMethod_creditCardValidation() {
        PaymentMethodRequest pm = new PaymentMethodRequest();
        pm.setType("CREDIT_CARD");
        pm.setCardNumber("4111111111111111");
        pm.setExpiryMonth("05");
        pm.setExpiryYear("2031");
        pm.setCvv("123");
        Set<ConstraintViolation<PaymentMethodRequest>> violations = validator.validate(pm);
        assertThat(violations).isEmpty();
    }
}
