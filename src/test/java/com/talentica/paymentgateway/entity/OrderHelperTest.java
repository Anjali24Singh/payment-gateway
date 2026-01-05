package com.talentica.paymentgateway.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class OrderHelperTest {

    @Test
    void totals_and_payment_flags_and_metadata() {
        Customer customer = new Customer("c@example.com");
        Order order = new Order("ORD-1", customer, new BigDecimal("100.00"));
        order.setTaxAmount(new BigDecimal("5.00"));
        order.setShippingAmount(new BigDecimal("10.00"));
        order.setDiscountAmount(new BigDecimal("3.00"));
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("112.00"));

        // Add transactions
        Transaction t1 = new Transaction("TXN-1", customer, TransactionType.PURCHASE, new BigDecimal("50.00"));
        t1.setStatus(PaymentStatus.CAPTURED);
        order.addTransaction(t1);

        Transaction t2 = new Transaction("TXN-2", customer, TransactionType.CAPTURE, new BigDecimal("30.00"));
        t2.setStatus(PaymentStatus.SETTLED);
        order.addTransaction(t2);

        // Refund
        Transaction refund = new Transaction("TXN-3", customer, TransactionType.REFUND, new BigDecimal("10.00"));
        refund.setStatus(PaymentStatus.SETTLED);
        order.addTransaction(refund);

        assertThat(order.getPaidAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(order.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(order.getOutstandingAmount()).isEqualByComparingTo(new BigDecimal("42.00"));
        assertThat(order.isPartiallyPaid()).isTrue();
        assertThat(order.isFullyPaid()).isFalse();

        // Metadata helpers
        order.setMetadata(new HashMap<>());
        order.addMetadata("key", "val");
        assertThat(order.getMetadata("key")).isEqualTo("val");

        // remove
        order.removeTransaction(refund);
        assertThat(order.getTransactions()).doesNotContain(refund);
    }
}
