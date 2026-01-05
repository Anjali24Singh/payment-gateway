package com.talentica.paymentgateway.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityUtilMethodsTest {

    @Test
    @DisplayName("User.getFullName falls back to username when names missing")
    void user_getFullName() {
        User u = new User();
        u.setUsername("jdoe");
        assertThat(u.getFullName()).isEqualTo("jdoe");
        u.setFirstName("John");
        assertThat(u.getFullName()).isEqualTo("John");
        u.setLastName("Doe");
        assertThat(u.getFullName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Customer address helpers and default payment method")
    void customer_helpers() {
        Customer c = new Customer("john@example.com", "John", "Doe");
        assertThat(c.hasBillingAddress()).isFalse();
        c.setBillingAddressLine1("123 St");
        c.setBillingCity("NY");
        c.setBillingState("NY");
        c.setBillingPostalCode("10001");
        assertThat(c.hasBillingAddress()).isTrue();
    }

    @Test
    @DisplayName("PaymentMethod masking and derived properties")
    void paymentMethod_helpers() {
        Customer c = new Customer("john@example.com");
        PaymentMethod pm = new PaymentMethod(c, "tok_abc", "CREDIT_CARD");
        pm.setCardLastFour("4242");
        pm.setCardBrand("VISA");
        assertThat(pm.getMaskedCardNumber()).isEqualTo("**** **** **** 4242");
        assertThat(pm.getDisplayName()).contains("VISA").contains("4242");
    }
}
