package com.talentica.paymentgateway.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AmountValidatorTest {

    private static Validator validator;

    private static class Sample {
        @ValidAmount(min = 0.01, max = 1000.00, decimalPlaces = 2)
        @NotNull
        BigDecimal amount;

        Sample(BigDecimal amount) { this.amount = amount; }
    }

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid amounts within range and scale should pass")
    void validAmounts() {
        assertThat(validator.validate(new Sample(new BigDecimal("0.01")))).isEmpty();
        assertThat(validator.validate(new Sample(new BigDecimal("12.34")))).isEmpty();
        assertThat(validator.validate(new Sample(new BigDecimal("1000.00")))).isEmpty();
    }

    @Test
    @DisplayName("Null, out-of-range, or invalid scale should fail")
    void invalidAmounts() {
        assertThat(validator.validate(new Sample(null))).isNotEmpty();
        assertThat(validator.validate(new Sample(new BigDecimal("0.001")))).isNotEmpty();
        assertThat(validator.validate(new Sample(new BigDecimal("0.00")))).isNotEmpty();
        assertThat(validator.validate(new Sample(new BigDecimal("1000.001")))).isNotEmpty();
        assertThat(validator.validate(new Sample(new BigDecimal("1000.01")))).isNotEmpty();
    }
}
