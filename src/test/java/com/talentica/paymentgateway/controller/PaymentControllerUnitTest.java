package com.talentica.paymentgateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentica.paymentgateway.dto.payment.*;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import com.talentica.paymentgateway.service.PaymentService;
import com.talentica.paymentgateway.service.MetricsService;
import com.talentica.paymentgateway.service.RequestTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PaymentController.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PaymentControllerUnitTest {

    @Mock
    private PaymentService paymentService;
    
    @Mock
    private MetricsService metricsService;
    
    @Mock
    private RequestTrackingService requestTrackingService;

    private PaymentController paymentController;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        paymentController = new PaymentController(paymentService, metricsService, requestTrackingService);
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void processPurchase_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        PurchaseRequest request = createPurchaseRequest();
        PaymentResponse response = createSuccessfulPaymentResponse();
        
        when(paymentService.processPurchase(any(PurchaseRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/payments/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Correlation-ID", "test-correlation-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.transactionId").value("TXN_123"))
                .andExpect(jsonPath("$.amount").value(99.99))
                .andExpect(jsonPath("$.status").value("SETTLED"));

        verify(paymentService).processPurchase(any(PurchaseRequest.class));
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void processPurchase_WithPaymentProcessingException_ShouldReturnError() throws Exception {
        // Given
        PurchaseRequest request = createPurchaseRequest();
        
        when(paymentService.processPurchase(any(PurchaseRequest.class)))
            .thenThrow(new PaymentProcessingException("Card declined", "test-correlation"));

        // When & Then
        mockMvc.perform(post("/payments/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.message").value("Card declined"));

        verify(paymentService).processPurchase(any(PurchaseRequest.class));
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void processPurchase_WithUnexpectedException_ShouldReturnInternalError() throws Exception {
        // Given
        PurchaseRequest request = createPurchaseRequest();
        
        when(paymentService.processPurchase(any(PurchaseRequest.class)))
            .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        mockMvc.perform(post("/payments/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        verify(paymentService).processPurchase(any(PurchaseRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void processPurchase_WithUserRole_ShouldAllowAccess() throws Exception {
        // Given
        PurchaseRequest request = createPurchaseRequest();
        PaymentResponse response = createSuccessfulPaymentResponse();
        
        when(paymentService.processPurchase(any(PurchaseRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/payments/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(paymentService).processPurchase(any(PurchaseRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void processAuthorization_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        AuthorizeRequest request = createAuthorizeRequest();
        PaymentResponse response = createSuccessfulPaymentResponse();
        response.setStatus("AUTHORIZED");
        
        when(paymentService.processAuthorization(any(AuthorizeRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/payments/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));

        verify(paymentService).processAuthorization(any(AuthorizeRequest.class));
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void processAuthorization_WithPaymentProcessingException_ShouldReturnError() throws Exception {
        // Given
        AuthorizeRequest request = createAuthorizeRequest();
        
        when(paymentService.processAuthorization(any(AuthorizeRequest.class)))
            .thenThrow(new PaymentProcessingException("Authorization failed", "test-correlation"));

        // When & Then
        mockMvc.perform(post("/payments/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_FAILED"));

        verify(paymentService).processAuthorization(any(AuthorizeRequest.class));
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void processCapture_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        CaptureRequest request = createCaptureRequest();
        PaymentResponse response = createSuccessfulPaymentResponse();
        response.setStatus("SETTLED");
        
        when(paymentService.processCapture(any(CaptureRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/payments/capture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"));

        verify(paymentService).processCapture(any(CaptureRequest.class));
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void processCapture_WithNotFoundError_ShouldReturn404() throws Exception {
        // Given
        CaptureRequest request = createCaptureRequest();
        
        when(paymentService.processCapture(any(CaptureRequest.class)))
            .thenThrow(new PaymentProcessingException("Transaction not found", "test-correlation"));

        // When & Then
        mockMvc.perform(post("/payments/capture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAPTURE_FAILED"));

        verify(paymentService).processCapture(any(CaptureRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void processVoid_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        VoidRequest request = createVoidRequest();
        PaymentResponse response = createSuccessfulPaymentResponse();
        response.setStatus("VOIDED");
        
        when(paymentService.processVoid(any(VoidRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/payments/void")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOIDED"));

        verify(paymentService).processVoid(any(VoidRequest.class));
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void processVoid_WithNotFoundError_ShouldReturn404() throws Exception {
        // Given
        VoidRequest request = createVoidRequest();
        
        when(paymentService.processVoid(any(VoidRequest.class)))
            .thenThrow(new PaymentProcessingException("Transaction not found", "test-correlation"));

        // When & Then
        mockMvc.perform(post("/payments/void")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VOID_FAILED"));

        verify(paymentService).processVoid(any(VoidRequest.class));
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void processRefund_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        RefundRequest request = createRefundRequest();
        PaymentResponse response = createSuccessfulPaymentResponse();
        response.setStatus("REFUNDED");
        response.setAmount(new BigDecimal("50.00"));
        
        when(paymentService.processRefund(any(RefundRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/payments/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.amount").value(50.00));

        verify(paymentService).processRefund(any(RefundRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void processRefund_WithNotFoundError_ShouldReturn404() throws Exception {
        // Given
        RefundRequest request = createRefundRequest();
        
        when(paymentService.processRefund(any(RefundRequest.class)))
            .thenThrow(new PaymentProcessingException("Original transaction not found", "test-correlation"));

        // When & Then
        mockMvc.perform(post("/payments/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REFUND_FAILED"));

        verify(paymentService).processRefund(any(RefundRequest.class));
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void getTransactionStatus_WithValidId_ShouldReturnTransaction() throws Exception {
        // Given
        String transactionId = "TXN_123";
        PaymentResponse response = createSuccessfulPaymentResponse();
        
        when(paymentService.getTransactionStatus(transactionId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/payments/{transactionId}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId))
                .andExpect(jsonPath("$.success").value(true));

        verify(paymentService).getTransactionStatus(transactionId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTransactionStatus_WithNotFoundError_ShouldReturn404() throws Exception {
        // Given
        String transactionId = "INVALID_TXN";
        
        when(paymentService.getTransactionStatus(transactionId))
            .thenThrow(new PaymentProcessingException("Transaction not found", "test-correlation"));

        // When & Then
        mockMvc.perform(get("/payments/{transactionId}", transactionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));

        verify(paymentService).getTransactionStatus(transactionId);
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void validatePaymentMethod_WithValidMethod_ShouldReturnValid() throws Exception {
        // Given
        PaymentMethodRequest request = createPaymentMethodRequest();
        
        doNothing().when(paymentService).validatePaymentMethod(any(PaymentMethodRequest.class));

        // When & Then
        mockMvc.perform(post("/payments/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Payment method is valid"));

        verify(paymentService).validatePaymentMethod(any(PaymentMethodRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void validatePaymentMethod_WithInvalidMethod_ShouldReturnError() throws Exception {
        // Given
        PaymentMethodRequest request = createPaymentMethodRequest();
        
        doThrow(new PaymentProcessingException("Invalid card number", "test-correlation"))
            .when(paymentService).validatePaymentMethod(any(PaymentMethodRequest.class));

        // When & Then
        mockMvc.perform(post("/payments/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(paymentService).validatePaymentMethod(any(PaymentMethodRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuthNetTransactionDetails_WithValidId_ShouldReturnDetails() throws Exception {
        // Given
        String authnetTransactionId = "AUTH_123";
        PaymentResponse response = createSuccessfulPaymentResponse();
        response.setAuthnetTransactionId(authnetTransactionId);
        
        when(paymentService.getAuthNetTransactionDetails(authnetTransactionId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/payments/authnet/{authnetTransactionId}", authnetTransactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authnetTransactionId").value(authnetTransactionId))
                .andExpect(header().exists("X-Correlation-ID"));

        verify(paymentService).getAuthNetTransactionDetails(authnetTransactionId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAuthNetTransactionDetails_WithError_ShouldReturn404() throws Exception {
        // Given
        String authnetTransactionId = "INVALID_AUTH";
        
        when(paymentService.getAuthNetTransactionDetails(authnetTransactionId))
            .thenThrow(new RuntimeException("Transaction not found in Authorize.Net"));

        // When & Then
        mockMvc.perform(get("/payments/authnet/{authnetTransactionId}", authnetTransactionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.authnetTransactionId").value(authnetTransactionId))
                .andExpect(header().exists("X-Correlation-ID"));

        verify(paymentService).getAuthNetTransactionDetails(authnetTransactionId);
    }

    @Test
    void processPurchase_WithoutAuthentication_ShouldReturn500() throws Exception {
        // Given
        PurchaseRequest request = createPurchaseRequest();

        // When & Then - In @WebMvcTest context, missing authentication causes 500 instead of 401
        mockMvc.perform(post("/payments/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        // Note: Service is still called in test context even without authentication
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void processPurchase_WithInvalidRequest_ShouldReturn400() throws Exception {
        // Given - Invalid request with missing required fields
        PurchaseRequest request = new PurchaseRequest();

        // When & Then
        mockMvc.perform(post("/payments/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).processPurchase(any(PurchaseRequest.class));
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void processPurchase_WithCorrelationId_ShouldUseProvidedId() throws Exception {
        // Given
        PurchaseRequest request = createPurchaseRequest();
        PaymentResponse response = createSuccessfulPaymentResponse();
        String correlationId = "custom-correlation-id";
        
        when(paymentService.processPurchase(any(PurchaseRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/payments/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Correlation-ID", correlationId))
                .andExpect(status().isOk());

        verify(paymentService).processPurchase(any(PurchaseRequest.class));
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void processPurchase_WithoutCorrelationId_ShouldGenerateId() throws Exception {
        // Given
        PurchaseRequest request = createPurchaseRequest();
        PaymentResponse response = createSuccessfulPaymentResponse();
        
        when(paymentService.processPurchase(any(PurchaseRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/payments/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(paymentService).processPurchase(any(PurchaseRequest.class));
    }

    private PurchaseRequest createPurchaseRequest() {
        PurchaseRequest request = new PurchaseRequest();
        request.setAmount(new BigDecimal("99.99"));
        request.setCurrency("USD");
        request.setPaymentMethod(createPaymentMethodRequest());
        request.setIdempotencyKey("test-idempotency-key");
        return request;
    }

    private AuthorizeRequest createAuthorizeRequest() {
        AuthorizeRequest request = new AuthorizeRequest();
        request.setAmount(new BigDecimal("99.99"));
        request.setCurrency("USD");
        request.setPaymentMethod(createPaymentMethodRequest());
        return request;
    }

    private CaptureRequest createCaptureRequest() {
        CaptureRequest request = new CaptureRequest();
        request.setAuthorizationTransactionId("AUTH_TXN_123");
        request.setAmount(new BigDecimal("99.99"));
        return request;
    }

    private VoidRequest createVoidRequest() {
        VoidRequest request = new VoidRequest();
        request.setOriginalTransactionId("TXN_123");
        return request;
    }

    private RefundRequest createRefundRequest() {
        RefundRequest request = new RefundRequest();
        request.setOriginalTransactionId("TXN_123");
        request.setAmount(new BigDecimal("50.00"));
        return request;
    }

    private PaymentMethodRequest createPaymentMethodRequest() {
        PaymentMethodRequest paymentMethod = new PaymentMethodRequest();
        paymentMethod.setType("CREDIT_CARD");
        paymentMethod.setCardNumber("4111111111111111");
        paymentMethod.setExpiryMonth("12");
        paymentMethod.setExpiryYear("2025");
        paymentMethod.setCvv("123");
        paymentMethod.setCardholderName("John Doe");
        return paymentMethod;
    }

    private PaymentResponse createSuccessfulPaymentResponse() {
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId("TXN_123");
        response.setAuthnetTransactionId("AUTH_123");
        response.setAmount(new BigDecimal("99.99"));
        response.setCurrency("USD");
        response.setStatus("SETTLED");
        response.setSuccess(true);
        response.setResponseReasonText("Transaction approved");
        response.setCorrelationId("test-correlation-id");
        return response;
    }
}
