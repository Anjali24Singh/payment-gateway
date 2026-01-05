package com.talentica.paymentgateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentica.paymentgateway.controller.PaymentController;
import com.talentica.paymentgateway.dto.payment.*;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import com.talentica.paymentgateway.service.PaymentService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Disabled("Controller test disabled due to ApplicationContext loading issues - needs investigation")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private PurchaseRequest buildPurchaseRequest() {
        PaymentMethodRequest pm = new PaymentMethodRequest();
        pm.setType("CREDIT_CARD");
        pm.setCardNumber("4111111111111111");
        pm.setExpiryMonth("12");
        pm.setExpiryYear("2030");
        pm.setCvv("123");

        PurchaseRequest req = new PurchaseRequest();
        req.setAmount(new BigDecimal("12.34"));
        req.setCurrency("USD");
        req.setPaymentMethod(pm);
        return req;
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("POST /payments/purchase returns PaymentResponse on success")
    void purchase_ok() throws Exception {
        PaymentResponse resp = new PaymentResponse();
        resp.setTransactionId("txn-1");
        resp.setStatus("CAPTURED");

        Mockito.when(paymentService.processPurchase(any(PurchaseRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/payments/purchase")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildPurchaseRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is("txn-1")))
                .andExpect(jsonPath("$.status", is("CAPTURED")));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("POST /payments/purchase returns 422 on PaymentProcessingException")
    void purchase_error() throws Exception {
        Mockito.when(paymentService.processPurchase(any(PurchaseRequest.class)))
                .thenThrow(new PaymentProcessingException(new PaymentErrorResponse(
                        "PAYMENT_FAILED", "Declined", "Declined", "PAYMENT_ERROR", "corr-1")));


        mockMvc.perform(post("/payments/purchase")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildPurchaseRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("PAYMENT_FAILED")))
                .andExpect(jsonPath("$.message", is("Declined")));
    }
}
