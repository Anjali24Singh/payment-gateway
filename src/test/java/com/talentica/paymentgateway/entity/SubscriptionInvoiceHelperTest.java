package com.talentica.paymentgateway.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionInvoiceHelperTest {

    @Test
    void invoice_state_transitions_and_helpers() {
        Customer customer = new Customer("c@example.com");
        SubscriptionPlan plan = new SubscriptionPlan("BASIC", "Basic", new BigDecimal("25.00"));
        plan.setIntervalUnit("MONTH");
        plan.setIntervalCount(1);
        PaymentMethod pm = new PaymentMethod(customer, "tok_1", "CREDIT_CARD");
        Subscription s = new Subscription("SUB-1", customer, plan, pm);

        SubscriptionInvoice inv = new SubscriptionInvoice("INV-1", s, customer, new BigDecimal("25.00"), ZonedDateTime.now(), ZonedDateTime.now().plusDays(30));
        assertThat(inv.isPending()).isTrue();
        inv.markAsProcessing();
        inv.markAsFailed();
        assertThat(inv.getNextPaymentAttempt()).isNotNull();
        inv.scheduleNextPaymentAttempt();
        inv.setDueDate(ZonedDateTime.now().minusDays(2));
        assertThat(inv.isOverdue()).isTrue();
        assertThat(inv.getDaysOverdue()).isGreaterThanOrEqualTo(0);
        inv.setDueDate(ZonedDateTime.now().plusDays(1));
        assertThat(inv.isDue()).isTrue();

        Transaction txn = new Transaction("TXN-10", customer, TransactionType.PURCHASE, new BigDecimal("25.00"));
        txn.setStatus(PaymentStatus.SETTLED);
        inv.markAsPaid(txn);
        assertThat(inv.isPaid()).isTrue();
        assertThat(inv.getFormattedAmount()).contains("25.00");
        assertThat(inv.getBillingPeriodDescription()).contains("Billing period:");
        inv.reset();
        assertThat(inv.isPending()).isTrue();
    }
}
