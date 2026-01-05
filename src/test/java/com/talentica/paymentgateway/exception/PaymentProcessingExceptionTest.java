package com.talentica.paymentgateway.exception;

import com.talentica.paymentgateway.dto.payment.PaymentErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentProcessingExceptionTest {

    @Test
    @DisplayName("Constructors set fields and toString contains key parts")
    void constructors() {
        PaymentProcessingException ex1 = new PaymentProcessingException("msg", "corr-1");
        assertThat(ex1.getMessage()).isEqualTo("msg");
        assertThat(ex1.getCorrelationId()).isEqualTo("corr-1");
        assertThat(ex1.getErrorCode()).isEqualTo("PAYMENT_PROCESSING_ERROR");
        assertThat(ex1.getErrorResponse()).isNull();
        assertThat(ex1.toString()).contains("msg", "corr-1", "PAYMENT_PROCESSING_ERROR");

        PaymentProcessingException ex2 = new PaymentProcessingException("msg2", new RuntimeException("cause"), "corr-2");
        assertThat(ex2.getCorrelationId()).isEqualTo("corr-2");

        PaymentProcessingException ex3 = new PaymentProcessingException("msg3", "ERR", (Throwable) null);
        assertThat(ex3.getErrorCode()).isEqualTo("ERR");

        PaymentProcessingException ex4 = new PaymentProcessingException("msg4", "ERR2", new RuntimeException());
        assertThat(ex4.getErrorCode()).isEqualTo("ERR2");

        PaymentErrorResponse per = new PaymentErrorResponse("E1", "m", "d", "PAYMENT_ERROR", "corr-3");
        PaymentProcessingException ex5 = new PaymentProcessingException(per);
        assertThat(ex5.getErrorResponse()).isEqualTo(per);
        assertThat(ex5.getErrorCode()).isEqualTo("E1");
        assertThat(ex5.getCorrelationId()).isEqualTo("corr-3");
    }
}
