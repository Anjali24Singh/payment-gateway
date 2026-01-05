package com.talentica.paymentgateway.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionHelperTest {

    private SubscriptionPlan plan() {
        SubscriptionPlan p = new SubscriptionPlan("BASIC", "Basic", new BigDecimal("25.00"));
        p.setIntervalUnit("MONTH");
        p.setIntervalCount(1);
        p.setTrialPeriodDays(7);
        return p;
    }

    @Test
    void lifecycle_trial_billing_and_invoices() {
        Customer customer = new Customer("c@example.com");
        PaymentMethod pm = new PaymentMethod(customer, "tok_1", "CREDIT_CARD");
        Subscription s = new Subscription("SUB-1", customer, plan(), pm);

        assertThat(s.isActive()).isFalse();
        s.activate();
        assertThat(s.isActive()).isTrue();
        assertThat(s.getCurrentPeriodEnd()).isNotNull();
        assertThat(s.getNextBillingDate()).isEqualTo(s.getCurrentPeriodEnd());

        s.startTrial();
        assertThat(s.isInTrial()).isTrue();
        assertThat(s.hasTrialExpired()).isFalse();

        s.markAsPastDue();
        assertThat(s.isPastDue()).isTrue();

        s.cancel("no need");
        assertThat(s.isCancelled()).isTrue();
        assertThat(s.getNextBillingDate()).isNull();

        // invoices
        SubscriptionInvoice inv = new SubscriptionInvoice("INV-1", s, customer, new BigDecimal("25.00"), ZonedDateTime.now(), ZonedDateTime.now().plusDays(30));
        s.addInvoice(inv);
        assertThat(s.getLatestInvoice()).isEqualTo(inv);
        assertThat(s.getUnpaidInvoices()).isNotEmpty();
        s.removeInvoice(inv);
        assertThat(s.getInvoices()).isEmpty();

        // metadata helpers
        s.addMetadata("k","v");
        assertThat(s.getMetadata("k")).isEqualTo("v");

        // days until next billing when null
        s.setNextBillingDate(null);
        assertThat(s.getDaysUntilNextBilling()).isEqualTo(-1);
    }
}
