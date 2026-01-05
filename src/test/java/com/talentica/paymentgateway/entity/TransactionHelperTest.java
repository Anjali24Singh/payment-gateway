package com.talentica.paymentgateway.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionHelperTest {

    @Test
    void state_predicates_refunds_children_and_data() {
        Customer customer = new Customer("c@example.com");
        Transaction txn = new Transaction("TXN-1", customer, TransactionType.AUTHORIZE, new BigDecimal("100.00"));
        txn.setStatus(PaymentStatus.AUTHORIZED);
        assertThat(txn.isSuccessful()).isTrue();
        assertThat(txn.isFailed()).isFalse();
        assertThat(txn.canBeVoided()).isTrue();
        assertThat(txn.canBeCaptured()).isTrue();
        assertThat(txn.canBeRefunded()).isFalse();

        txn.setTransactionType(TransactionType.PURCHASE);
        txn.setStatus(PaymentStatus.SETTLED);
        assertThat(txn.canBeRefunded()).isTrue();

        Transaction refund1 = new Transaction("TXN-2", customer, TransactionType.REFUND, new BigDecimal("30.00"));
        refund1.setStatus(PaymentStatus.SETTLED);
        txn.addChildTransaction(refund1);
        assertThat(txn.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(txn.getAvailableRefundAmount()).isEqualByComparingTo(new BigDecimal("70.00"));
        txn.removeChildTransaction(refund1);
        assertThat(txn.getChildTransactions()).isEmpty();

        txn.addRequestData("rk", "rv");
        txn.addResponseData("sk", "sv");
        assertThat(txn.getRequestData().get("rk")).isEqualTo("rv");
        assertThat(txn.getResponseData().get("sk")).isEqualTo("sv");

        txn.markAsProcessed();
        assertThat(txn.getProcessedAt()).isNotNull();
    }
}
