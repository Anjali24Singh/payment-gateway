package com.talentica.paymentgateway.exception;

import com.talentica.paymentgateway.dto.payment.PaymentErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Handles PaymentProcessingException with embedded error")
    void paymentProcessing() {
        PaymentErrorResponse per = new PaymentErrorResponse("E1", "m", "d", "PAYMENT_ERROR", "corr");
        PaymentProcessingException ex = new PaymentProcessingException(per);
        ResponseEntity<PaymentErrorResponse> res = handler.handlePaymentProcessingException(ex, new ServletWebRequest(new MockHttpServletRequest()));
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getCode()).isEqualTo("E1");
    }

    @Test
    @DisplayName("Handles IllegalStateException and IllegalArgumentException")
    void illegalStateAndArgument() {
        ResponseEntity<PaymentErrorResponse> res1 = handler.handleIllegalStateException(new IllegalStateException("bad state"), new ServletWebRequest(new MockHttpServletRequest()));
        assertThat(res1.getStatusCode().value()).isEqualTo(409);
        ResponseEntity<PaymentErrorResponse> res2 = handler.handleIllegalArgumentException(new IllegalArgumentException("bad arg"), new ServletWebRequest(new MockHttpServletRequest()));
        assertThat(res2.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("Handles ConstraintViolationException")
    void constraintViolation() {
        ConstraintViolationException ex = new ConstraintViolationException(Set.<ConstraintViolation<?>>of());
        ResponseEntity<PaymentErrorResponse> res = handler.handleConstraintViolationException(ex, new ServletWebRequest(new MockHttpServletRequest()));
        assertThat(res.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("Handles RuntimeException and generic exceptions")
    void runtimeAndGeneric() {
        ResponseEntity<PaymentErrorResponse> r1 = handler.handleRuntimeException(new RuntimeException("x"), new ServletWebRequest(new MockHttpServletRequest()));
        assertThat(r1.getStatusCode().value()).isEqualTo(500);
        ResponseEntity<PaymentErrorResponse> r2 = handler.handleGenericException(new Exception("x"), new ServletWebRequest(new MockHttpServletRequest()));
        assertThat(r2.getStatusCode().value()).isEqualTo(500);
    }
}
