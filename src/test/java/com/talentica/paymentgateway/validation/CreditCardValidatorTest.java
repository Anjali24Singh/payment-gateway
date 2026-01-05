package com.talentica.paymentgateway.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreditCardValidatorTest {

    private static Validator validator;

    private static class Sample {
        @ValidCreditCard(allowTestNumbers = true)
        String card;
        Sample(String card) { this.card = card; }
    }

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Accepts known test numbers and valid Luhn numbers")
    void acceptsValid() {
        assertThat(validator.validate(new Sample("4111 1111 1111 1111"))).isEmpty();
        assertThat(validator.validate(new Sample("4012888888881881"))).isEmpty();
    }

    @Test
    @DisplayName("Rejects invalid format or bad checksum")
    void rejectsInvalid() {
        assertThat(validator.validate(new Sample("abc"))).isNotEmpty();
        assertThat(validator.validate(new Sample("4111111111111112"))).isNotEmpty();
        assertThat(validator.validate(new Sample(null))).isNotEmpty();
        assertThat(validator.validate(new Sample(""))).isNotEmpty();
    }
}
