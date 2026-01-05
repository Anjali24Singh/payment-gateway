package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.config.AuthorizeNetConfig;
import com.talentica.paymentgateway.dto.payment.*;
import com.talentica.paymentgateway.entity.*;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import com.talentica.paymentgateway.repository.*;
import com.talentica.paymentgateway.util.AuthorizeNetMapper;
import net.authorize.Environment;
import net.authorize.api.contract.v1.*;
import net.authorize.api.controller.CreateTransactionController;
import net.authorize.api.controller.GetTransactionDetailsController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock
    private AuthorizeNetConfig config;
    
    @Mock
    private MerchantAuthenticationType merchant;
    
    @Mock
    private Environment environment;
    
    @Mock
    private AuthorizeNetMapper mapper;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private MetricsService metricsService;
    
    @Mock
    private AuthorizeNetCustomerService authorizeNetCustomerService;
    
    @InjectMocks
    private PaymentService paymentService;
    
    private MockedStatic<MDC> mdcMock;
    
    @BeforeEach
    void setUp() {
        mdcMock = mockStatic(MDC.class);
        mdcMock.when(() -> MDC.get("correlationId")).thenReturn("test-correlation-id");
    }
    
    @AfterEach
    void tearDown() {
        if (mdcMock != null) {
            mdcMock.close();
        }
    }

    @Test
    void testProcessPurchase_Success() {
        PurchaseRequest request = createPurchaseRequest();
        Transaction transaction = createTransaction();
        CreateTransactionResponse authNetResponse = createSuccessfulAuthNetResponse();
        PaymentResponse expectedResponse = createSuccessfulPaymentResponse();
        Customer customer = createCustomer();
        PaymentMethod paymentMethod = createPaymentMethod();
        
        when(mapper.generateTransactionId()).thenReturn("test-transaction-id");
        when(customerRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(customer));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(paymentMethod);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(mapper.mapToPurchaseTransaction(any(PurchaseRequest.class), any(MerchantAuthenticationType.class))).thenReturn(new CreateTransactionRequest());
        when(mapper.mapToPaymentResponse(any(CreateTransactionResponse.class), anyString(), anyString(), any(PaymentMethodRequest.class), anyString())).thenReturn(expectedResponse);
        
        try (MockedConstruction<CreateTransactionController> mockedController = mockConstruction(CreateTransactionController.class, (mock, context) -> {
            when(mock.getApiResponse()).thenReturn(authNetResponse);
        })) {
            PaymentResponse result = paymentService.processPurchase(request);
            
            assertNotNull(result);
            assertEquals(expectedResponse.getTransactionId(), result.getTransactionId());
            verify(metricsService).recordPaymentRequest();
            verify(metricsService).recordPaymentMethodUsage("credit_card");
            verify(transactionRepository, times(2)).save(any(Transaction.class));
        }
    }
    
    @Test
    void testProcessPurchase_WithIdempotencyKey_ExistingTransaction() {
        PurchaseRequest request = createPurchaseRequest();
        request.setIdempotencyKey("test-idempotency-key");
        Transaction existingTransaction = createTransaction();
        
        when(transactionRepository.findByIdempotencyKey("test-idempotency-key")).thenReturn(Optional.of(existingTransaction));
        when(mapper.generateTransactionId()).thenReturn("test-transaction-id");
        
        PaymentResponse result = paymentService.processPurchase(request);
        
        assertNotNull(result);
        assertEquals(existingTransaction.getTransactionId(), result.getTransactionId());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(metricsService).recordTransaction(eq(TransactionType.PURCHASE), any());
    }

    private PurchaseRequest createPurchaseRequest() {
        PurchaseRequest request = new PurchaseRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setPaymentMethod(createValidPaymentMethodRequest());
        request.setCustomer(createCustomerRequest());
        return request;
    }

    private AuthorizeRequest createAuthorizeRequest() {
        AuthorizeRequest request = new AuthorizeRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setPaymentMethod(createValidPaymentMethodRequest());
        request.setCustomer(createCustomerRequest());
        return request;
    }

    private CaptureRequest createCaptureRequest() {
        CaptureRequest request = new CaptureRequest();
        request.setTransactionId("auth-transaction-id");
        request.setAmount(new BigDecimal("100.00"));
        return request;
    }

    private VoidRequest createVoidRequest() {
        VoidRequest request = new VoidRequest();
        request.setTransactionId("auth-transaction-id");
        return request;
    }

    private RefundRequest createRefundRequest() {
        RefundRequest request = new RefundRequest();
        request.setTransactionId("captured-transaction-id");
        request.setAmount(new BigDecimal("100.00"));
        request.setReason("Customer request");
        return request;
    }

    private PaymentMethodRequest createValidPaymentMethodRequest() {
        PaymentMethodRequest request = new PaymentMethodRequest();
        request.setType("CREDIT_CARD");
        request.setCardNumber("4111111111111111");
        request.setExpiryMonth("12");
        request.setExpiryYear("2030");  // Future year to pass expiry validation
        request.setCvv("123");
        request.setCardholderName("John Doe");
        return request;
    }

    private CustomerRequest createCustomerRequest() {
        CustomerRequest request = new CustomerRequest();
        request.setEmail("test@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");
        return request;
    }

    private Transaction createTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId("test-transaction-id");
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setCurrency("USD");
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setTransactionType(TransactionType.PURCHASE);
        transaction.setCorrelationId("test-correlation-id");
        transaction.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
        transaction.setResponseData(new HashMap<>());
        return transaction;
    }

    private Transaction createAuthorizedTransaction() {
        Transaction transaction = createTransaction();
        transaction.setStatus(PaymentStatus.AUTHORIZED);
        transaction.setTransactionType(TransactionType.AUTHORIZE);
        transaction.setAuthnetTransactionId("authnet-123");
        return transaction;
    }

    private Transaction createCapturedTransaction() {
        Transaction transaction = createTransaction();
        transaction.setStatus(PaymentStatus.CAPTURED);
        transaction.setTransactionType(TransactionType.PURCHASE);
        transaction.setAuthnetTransactionId("authnet-123");
        transaction.setPaymentMethod(createPaymentMethod());
        return transaction;
    }

    private Customer createCustomer() {
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setAuthorizeNetCustomerProfileId("profile-123");
        return customer;
    }

    private PaymentMethod createPaymentMethod() {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setPaymentType("CREDIT_CARD");
        paymentMethod.setCardNumber("4111111111111111");
        paymentMethod.setCardLastFour("1111");
        paymentMethod.setExpiryMonth("12");
        paymentMethod.setExpiryYear("2025");
        paymentMethod.setCardholderName("John Doe");
        return paymentMethod;
    }

    private CreateTransactionResponse createSuccessfulAuthNetResponse() {
        CreateTransactionResponse response = new CreateTransactionResponse();
        MessagesType messages = new MessagesType();
        messages.setResultCode(MessageTypeEnum.OK);
        response.setMessages(messages);
        
        TransactionResponse transactionResponse = new TransactionResponse();
        transactionResponse.setResponseCode("1");
        transactionResponse.setAuthCode("ABC123");
        transactionResponse.setTransId("authnet-transaction-id");
        response.setTransactionResponse(transactionResponse);
        
        return response;
    }

    private PaymentResponse createSuccessfulPaymentResponse() {
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId("test-transaction-id");
        response.setAuthnetTransactionId("authnet-transaction-id");
        response.setStatus("CAPTURED");
        response.setSuccess(true);
        response.setAmount(new BigDecimal("100.00"));
        response.setCurrency("USD");
        response.setCorrelationId("test-correlation-id");
        response.setCreatedAt(ZonedDateTime.now());
        return response;
    }

    private GetTransactionDetailsResponse createAuthNetDetailsResponse() {
        GetTransactionDetailsResponse response = new GetTransactionDetailsResponse();
        MessagesType messages = new MessagesType();
        messages.setResultCode(MessageTypeEnum.OK);
        response.setMessages(messages);
        
        TransactionDetailsType transaction = new TransactionDetailsType();
        transaction.setTransId("authnet-transaction-id");
        transaction.setAuthAmount(new BigDecimal("100.00"));
        transaction.setResponseCode(1);
        transaction.setAuthCode("ABC123");
        response.setTransaction(transaction);
        
        return response;
    }

    private GetTransactionDetailsResponse createErrorAuthNetDetailsResponse() {
        GetTransactionDetailsResponse response = new GetTransactionDetailsResponse();
        MessagesType messages = new MessagesType();
        messages.setResultCode(MessageTypeEnum.ERROR);
        
        List<MessagesType.Message> messageList = new ArrayList<>();
        MessagesType.Message message = new MessagesType.Message();
        message.setText("Transaction not found");
        messageList.add(message);
        messages.getMessage().addAll(messageList);
        
        response.setMessages(messages);
        return response;
    }

    @Test
    void testProcessPurchase_ValidationFailure() {
        PurchaseRequest request = createPurchaseRequest();
        request.getPaymentMethod().setCardNumber(null);
        
        when(mapper.generateTransactionId()).thenReturn("test-transaction-id");
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.processPurchase(request);
        });
        
        assertTrue(exception.getMessage().contains("Purchase transaction failed"));
        verify(metricsService).recordPaymentError(eq("processing_exception"), anyString());
    }

    @Test
    void testProcessAuthorization_Success() {
        AuthorizeRequest request = createAuthorizeRequest();
        Transaction transaction = createTransaction();
        CreateTransactionResponse authNetResponse = createSuccessfulAuthNetResponse();
        PaymentResponse expectedResponse = createSuccessfulPaymentResponse();
        Customer customer = createCustomer();
        PaymentMethod paymentMethod = createPaymentMethod();
        
        when(mapper.generateTransactionId()).thenReturn("test-transaction-id");
        when(customerRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(customer));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(paymentMethod);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(mapper.mapToAuthorizeTransaction(any(AuthorizeRequest.class), any(MerchantAuthenticationType.class))).thenReturn(new CreateTransactionRequest());
        when(mapper.mapToPaymentResponse(any(CreateTransactionResponse.class), anyString(), anyString(), any(PaymentMethodRequest.class), anyString())).thenReturn(expectedResponse);
        
        try (MockedConstruction<CreateTransactionController> mockedController = mockConstruction(CreateTransactionController.class, (mock, context) -> {
            when(mock.getApiResponse()).thenReturn(authNetResponse);
        })) {
            PaymentResponse result = paymentService.processAuthorization(request);
            
            assertNotNull(result);
            assertEquals(expectedResponse.getTransactionId(), result.getTransactionId());
            verify(transactionRepository, times(2)).save(any(Transaction.class));
        }
    }

    @Test
    void testProcessCapture_Success() {
        CaptureRequest request = createCaptureRequest();
        Transaction authTransaction = createAuthorizedTransaction();
        Transaction captureTransaction = createTransaction();
        CreateTransactionResponse authNetResponse = createSuccessfulAuthNetResponse();
        PaymentResponse expectedResponse = createSuccessfulPaymentResponse();
        
        when(mapper.generateTransactionId()).thenReturn("test-capture-id");
        when(transactionRepository.findByTransactionId("auth-transaction-id")).thenReturn(Optional.of(authTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(captureTransaction);
        when(mapper.mapToCaptureTransaction(any(CaptureRequest.class), anyString(), any(MerchantAuthenticationType.class))).thenReturn(new CreateTransactionRequest());
        when(mapper.mapToPaymentResponse(any(CreateTransactionResponse.class), eq("test-capture-id"), eq("CAPTURE"), any(), eq("test-correlation-id"))).thenReturn(expectedResponse);
        
        try (MockedConstruction<CreateTransactionController> mockedController = mockConstruction(CreateTransactionController.class, (mock, context) -> {
            when(mock.getApiResponse()).thenReturn(authNetResponse);
        })) {
            PaymentResponse result = paymentService.processCapture(request);
            
            assertNotNull(result);
            assertEquals(expectedResponse.getTransactionId(), result.getTransactionId());
            verify(transactionRepository, times(3)).save(any(Transaction.class));
        }
    }

    @Test
    void testProcessCapture_OriginalTransactionNotFound() {
        CaptureRequest request = createCaptureRequest();
        
        when(mapper.generateTransactionId()).thenReturn("test-capture-id");
        when(transactionRepository.findByTransactionId("auth-transaction-id")).thenReturn(Optional.empty());
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.processCapture(request);
        });
        
        assertTrue(exception.getMessage().contains("Original authorization transaction not found"));
    }

    @Test
    void testProcessVoid_Success() {
        VoidRequest request = createVoidRequest();
        Transaction authTransaction = createAuthorizedTransaction();
        Transaction voidTransaction = createTransaction();
        CreateTransactionResponse authNetResponse = createSuccessfulAuthNetResponse();
        PaymentResponse expectedResponse = createSuccessfulPaymentResponse();
        
        when(mapper.generateTransactionId()).thenReturn("test-void-id");
        when(transactionRepository.findByTransactionId("auth-transaction-id")).thenReturn(Optional.of(authTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(voidTransaction);
        when(mapper.mapToVoidTransaction(any(VoidRequest.class), anyString(), any(MerchantAuthenticationType.class))).thenReturn(new CreateTransactionRequest());
        when(mapper.mapToPaymentResponse(any(CreateTransactionResponse.class), anyString(), anyString(), any(), anyString())).thenReturn(expectedResponse);
        
        try (MockedConstruction<CreateTransactionController> mockedController = mockConstruction(CreateTransactionController.class, (mock, context) -> {
            when(mock.getApiResponse()).thenReturn(authNetResponse);
        })) {
            PaymentResponse result = paymentService.processVoid(request);
            
            assertNotNull(result);
            assertEquals(expectedResponse.getTransactionId(), result.getTransactionId());
            verify(transactionRepository, times(3)).save(any(Transaction.class));
        }
    }

    @Test
    void testProcessRefund_Success_FullRefund() {
        RefundRequest request = createRefundRequest();
        request.setAmount(null);
        Transaction origTransaction = createCapturedTransaction();
        Transaction refundTransaction = createTransaction();
        CreateTransactionResponse authNetResponse = createSuccessfulAuthNetResponse();
        PaymentResponse expectedResponse = createSuccessfulPaymentResponse();
        
        when(mapper.generateTransactionId()).thenReturn("test-refund-id");
        when(transactionRepository.findByTransactionIdWithPaymentMethod("captured-transaction-id")).thenReturn(Optional.of(origTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(refundTransaction);
        when(mapper.mapToRefundTransaction(any(RefundRequest.class), anyString(), any(PaymentMethod.class), any(MerchantAuthenticationType.class))).thenReturn(new CreateTransactionRequest());
        when(mapper.mapToPaymentResponse(any(CreateTransactionResponse.class), anyString(), anyString(), any(PaymentMethodRequest.class), anyString())).thenReturn(expectedResponse);
        
        try (MockedConstruction<CreateTransactionController> mockedController = mockConstruction(CreateTransactionController.class, (mock, context) -> {
            when(mock.getApiResponse()).thenReturn(authNetResponse);
        })) {
            PaymentResponse result = paymentService.processRefund(request);
            
            assertNotNull(result);
            assertEquals(expectedResponse.getTransactionId(), result.getTransactionId());
            verify(transactionRepository, times(3)).save(any(Transaction.class));
        }
    }

    @Test
    void testGetTransactionStatus_Success() {
        Transaction transaction = createTransaction();
        
        when(transactionRepository.findByTransactionId("test-transaction-id")).thenReturn(Optional.of(transaction));
        
        PaymentResponse result = paymentService.getTransactionStatus("test-transaction-id");
        
        assertNotNull(result);
        assertEquals(transaction.getTransactionId(), result.getTransactionId());
        assertEquals(transaction.getStatus().name(), result.getStatus());
    }

    @Test
    void testGetTransactionStatus_TransactionNotFound() {
        when(transactionRepository.findByTransactionId("non-existent-id")).thenReturn(Optional.empty());
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.getTransactionStatus("non-existent-id");
        });
        
        assertTrue(exception.getMessage().contains("Transaction not found"));
    }

    @Test
    void testGetAuthNetTransactionDetails_Success() {
        GetTransactionDetailsResponse authNetResponse = createAuthNetDetailsResponse();
        
        try (MockedConstruction<GetTransactionDetailsController> mockedController = mockConstruction(GetTransactionDetailsController.class, (mock, context) -> {
            when(mock.getApiResponse()).thenReturn(authNetResponse);
        })) {
            PaymentResponse result = paymentService.getAuthNetTransactionDetails("authnet-transaction-id");
            
            assertNotNull(result);
            assertEquals("authnet-transaction-id", result.getAuthnetTransactionId());
        }
    }

    @Test
    void testValidatePaymentMethod_Success_CreditCard() {
        PaymentMethodRequest paymentMethod = createValidPaymentMethodRequest();
        
        assertDoesNotThrow(() -> {
            paymentService.validatePaymentMethod(paymentMethod);
        });
    }

    @Test
    void testValidatePaymentMethod_NullPaymentMethod() {
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.validatePaymentMethod(null);
        });
        
        assertTrue(exception.getMessage().contains("Payment method is required"));
    }

    @Test
    void testValidatePaymentMethod_MissingCardNumber() {
        PaymentMethodRequest paymentMethod = createValidPaymentMethodRequest();
        paymentMethod.setCardNumber(null);
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.validatePaymentMethod(paymentMethod);
        });
        
        assertTrue(exception.getMessage().contains("Card number is required"));
    }

    @Test
    void testValidatePaymentMethod_InvalidCardLength() {
        PaymentMethodRequest paymentMethod = createValidPaymentMethodRequest();
        paymentMethod.setCardNumber("123");
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.validatePaymentMethod(paymentMethod);
        });
        
        assertTrue(exception.getMessage().contains("Invalid card number length"));
    }

    @Test
    void testValidatePaymentMethod_ExpiredCard() {
        PaymentMethodRequest paymentMethod = createValidPaymentMethodRequest();
        paymentMethod.setExpiryMonth("01");
        paymentMethod.setExpiryYear("2020");
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.validatePaymentMethod(paymentMethod);
        });
        
        assertTrue(exception.getMessage().contains("Card has expired"));
    }

    @Test
    void testValidatePaymentMethod_InvalidCvvLength() {
        PaymentMethodRequest paymentMethod = createValidPaymentMethodRequest();
        paymentMethod.setCvv("12");
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.validatePaymentMethod(paymentMethod);
        });
        
        assertTrue(exception.getMessage().contains("Invalid CVV length"));
    }

    @Test
    void testProcessPurchase_AuthorizeNetException() {
        PurchaseRequest request = createPurchaseRequest();
        Transaction transaction = createTransaction();
        Customer customer = createCustomer();
        PaymentMethod paymentMethod = createPaymentMethod();
        
        when(mapper.generateTransactionId()).thenReturn("test-transaction-id");
        when(customerRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(customer));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(paymentMethod);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(mapper.mapToPurchaseTransaction(any(PurchaseRequest.class), any(MerchantAuthenticationType.class))).thenReturn(new CreateTransactionRequest());
        
        try (MockedConstruction<CreateTransactionController> mockedController = mockConstruction(CreateTransactionController.class, (mock, context) -> {
            doThrow(new RuntimeException("Authorize.Net API Error")).when(mock).execute();
        })) {
            PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
                paymentService.processPurchase(request);
            });
            
            assertTrue(exception.getMessage().contains("Purchase transaction failed"));
            verify(metricsService).recordPaymentCompletion(eq(PaymentStatus.FAILED), any(BigDecimal.class), any());
        }
    }

    @Test
    void testProcessAuthorization_WithIdempotencyKey_ExistingTransaction() {
        AuthorizeRequest request = createAuthorizeRequest();
        request.setIdempotencyKey("test-idempotency-key");
        Transaction existingTransaction = createTransaction();
        
        when(transactionRepository.findByIdempotencyKey("test-idempotency-key")).thenReturn(Optional.of(existingTransaction));
        when(mapper.generateTransactionId()).thenReturn("test-transaction-id");
        
        PaymentResponse result = paymentService.processAuthorization(request);
        
        assertNotNull(result);
        assertEquals(existingTransaction.getTransactionId(), result.getTransactionId());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testProcessAuthorization_ValidationFailure() {
        AuthorizeRequest request = createAuthorizeRequest();
        request.getPaymentMethod().setCvv(null);
        
        when(mapper.generateTransactionId()).thenReturn("test-transaction-id");
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.processAuthorization(request);
        });
        
        assertTrue(exception.getMessage().contains("Authorization transaction failed"));
    }

    @Test
    void testProcessCapture_TransactionNotAuthorized() {
        CaptureRequest request = createCaptureRequest();
        Transaction authTransaction = createTransaction();
        authTransaction.setStatus(PaymentStatus.FAILED);
        
        when(mapper.generateTransactionId()).thenReturn("test-capture-id");
        when(transactionRepository.findByTransactionId("auth-transaction-id")).thenReturn(Optional.of(authTransaction));
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.processCapture(request);
        });
        
        assertTrue(exception.getMessage().contains("Transaction is not in authorized status"));
    }

    @Test
    void testProcessVoid_OriginalTransactionNotFound() {
        VoidRequest request = createVoidRequest();
        
        when(mapper.generateTransactionId()).thenReturn("test-void-id");
        when(transactionRepository.findByTransactionId("auth-transaction-id")).thenReturn(Optional.empty());
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.processVoid(request);
        });
        
        assertTrue(exception.getMessage().contains("Original transaction not found"));
    }

    @Test
    void testProcessVoid_TransactionNotAuthorized() {
        VoidRequest request = createVoidRequest();
        Transaction authTransaction = createTransaction();
        authTransaction.setStatus(PaymentStatus.CAPTURED);
        
        when(mapper.generateTransactionId()).thenReturn("test-void-id");
        when(transactionRepository.findByTransactionId("auth-transaction-id")).thenReturn(Optional.of(authTransaction));
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.processVoid(request);
        });
        
        assertTrue(exception.getMessage().contains("Only authorized transactions can be voided"));
    }

    @Test
    void testProcessRefund_Success_PartialRefund() {
        RefundRequest request = createRefundRequest();
        request.setAmount(new BigDecimal("50.00"));
        Transaction origTransaction = createCapturedTransaction();
        Transaction refundTransaction = createTransaction();
        CreateTransactionResponse authNetResponse = createSuccessfulAuthNetResponse();
        PaymentResponse expectedResponse = createSuccessfulPaymentResponse();
        
        when(mapper.generateTransactionId()).thenReturn("test-refund-id");
        when(transactionRepository.findByTransactionIdWithPaymentMethod("captured-transaction-id")).thenReturn(Optional.of(origTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(refundTransaction);
        when(mapper.mapToRefundTransaction(any(RefundRequest.class), anyString(), any(PaymentMethod.class), any(MerchantAuthenticationType.class))).thenReturn(new CreateTransactionRequest());
        when(mapper.mapToPaymentResponse(any(CreateTransactionResponse.class), anyString(), anyString(), any(PaymentMethodRequest.class), anyString())).thenReturn(expectedResponse);
        
        try (MockedConstruction<CreateTransactionController> mockedController = mockConstruction(CreateTransactionController.class, (mock, context) -> {
            when(mock.getApiResponse()).thenReturn(authNetResponse);
        })) {
            PaymentResponse result = paymentService.processRefund(request);
            
            assertNotNull(result);
            assertEquals(expectedResponse.getTransactionId(), result.getTransactionId());
            verify(transactionRepository, times(3)).save(any(Transaction.class));
        }
    }

    @Test
    void testProcessRefund_WithIdempotencyKey_ExistingTransaction() {
        RefundRequest request = createRefundRequest();
        request.setIdempotencyKey("test-idempotency-key");
        Transaction existingTransaction = createTransaction();
        
        when(transactionRepository.findByIdempotencyKey("test-idempotency-key")).thenReturn(Optional.of(existingTransaction));
        when(mapper.generateTransactionId()).thenReturn("test-refund-id");
        
        PaymentResponse result = paymentService.processRefund(request);
        
        assertNotNull(result);
        assertEquals(existingTransaction.getTransactionId(), result.getTransactionId());
    }

    @Test
    void testProcessRefund_OriginalTransactionNotFound() {
        RefundRequest request = createRefundRequest();
        
        when(mapper.generateTransactionId()).thenReturn("test-refund-id");
        when(transactionRepository.findByTransactionIdWithPaymentMethod("captured-transaction-id")).thenReturn(Optional.empty());
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.processRefund(request);
        });
        
        assertTrue(exception.getMessage().contains("Original transaction not found"));
    }

    @Test
    void testProcessRefund_TransactionNotCapturedOrSettled() {
        RefundRequest request = createRefundRequest();
        Transaction origTransaction = createTransaction();
        origTransaction.setStatus(PaymentStatus.AUTHORIZED);
        
        when(mapper.generateTransactionId()).thenReturn("test-refund-id");
        when(transactionRepository.findByTransactionIdWithPaymentMethod("captured-transaction-id")).thenReturn(Optional.of(origTransaction));
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.processRefund(request);
        });
        
        assertTrue(exception.getMessage().contains("Only captured/settled transactions can be refunded"));
    }

    @Test
    void testProcessRefund_RefundAmountExceedsOriginal() {
        RefundRequest request = createRefundRequest();
        request.setAmount(new BigDecimal("200.00"));
        Transaction origTransaction = createCapturedTransaction();
        
        when(mapper.generateTransactionId()).thenReturn("test-refund-id");
        when(transactionRepository.findByTransactionIdWithPaymentMethod("captured-transaction-id")).thenReturn(Optional.of(origTransaction));
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.processRefund(request);
        });
        
        assertTrue(exception.getMessage().contains("Refund amount cannot exceed original transaction amount"));
    }

    @Test
    void testGetTransactionStatus_RepositoryException() {
        when(transactionRepository.findByTransactionId("test-transaction-id")).thenThrow(new RuntimeException("Database error"));
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.getTransactionStatus("test-transaction-id");
        });
        
        assertTrue(exception.getMessage().contains("Failed to retrieve transaction status"));
    }

    @Test
    void testGetAuthNetTransactionDetails_NoResponse() {
        try (MockedConstruction<GetTransactionDetailsController> mockedController = mockConstruction(GetTransactionDetailsController.class, (mock, context) -> {
            when(mock.getApiResponse()).thenReturn(null);
        })) {
            PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
                paymentService.getAuthNetTransactionDetails("authnet-transaction-id");
            });
            
            assertTrue(exception.getMessage().contains("No response from Authorize.Net"));
        }
    }

    @Test
    void testGetAuthNetTransactionDetails_ErrorResponse() {
        GetTransactionDetailsResponse authNetResponse = createErrorAuthNetDetailsResponse();
        
        try (MockedConstruction<GetTransactionDetailsController> mockedController = mockConstruction(GetTransactionDetailsController.class, (mock, context) -> {
            when(mock.getApiResponse()).thenReturn(authNetResponse);
        })) {
            PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
                paymentService.getAuthNetTransactionDetails("authnet-transaction-id");
            });
            
            assertTrue(exception.getMessage().contains("Authorize.Net error"));
        }
    }

    @Test
    void testGetAuthNetTransactionDetails_Exception() {
        try (MockedConstruction<GetTransactionDetailsController> mockedController = mockConstruction(GetTransactionDetailsController.class, (mock, context) -> {
            doThrow(new RuntimeException("API Error")).when(mock).execute();
        })) {
            PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
                paymentService.getAuthNetTransactionDetails("authnet-transaction-id");
            });
            
            assertTrue(exception.getMessage().contains("Failed to retrieve transaction from Authorize.Net"));
        }
    }

    @Test
    void testValidatePaymentMethod_MissingExpiryDate() {
        PaymentMethodRequest paymentMethod = createValidPaymentMethodRequest();
        paymentMethod.setExpiryMonth(null);
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.validatePaymentMethod(paymentMethod);
        });
        
        assertTrue(exception.getMessage().contains("Card expiry date is required"));
    }

    @Test
    void testValidatePaymentMethod_MissingCvv() {
        PaymentMethodRequest paymentMethod = createValidPaymentMethodRequest();
        paymentMethod.setCvv(null);
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.validatePaymentMethod(paymentMethod);
        });
        
        assertTrue(exception.getMessage().contains("CVV is required"));
    }

    @Test
    void testValidatePaymentMethod_EmptyCardNumber() {
        PaymentMethodRequest paymentMethod = createValidPaymentMethodRequest();
        paymentMethod.setCardNumber("");
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.validatePaymentMethod(paymentMethod);
        });
        
        assertTrue(exception.getMessage().contains("Card number is required"));
    }

    @Test
    void testValidatePaymentMethod_EmptyCvv() {
        PaymentMethodRequest paymentMethod = createValidPaymentMethodRequest();
        paymentMethod.setCvv("");
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.validatePaymentMethod(paymentMethod);
        });
        
        assertTrue(exception.getMessage().contains("CVV is required"));
    }

    @Test
    void testValidatePaymentMethod_InvalidExpiryFormat() {
        PaymentMethodRequest paymentMethod = createValidPaymentMethodRequest();
        paymentMethod.setExpiryYear("invalid");
        
        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            paymentService.validatePaymentMethod(paymentMethod);
        });
        
        assertTrue(exception.getMessage().contains("Invalid expiry date format"));
    }

    @Test
    void testValidatePaymentMethod_CardNumberWithSpaces() {
        PaymentMethodRequest paymentMethod = createValidPaymentMethodRequest();
        paymentMethod.setCardNumber("4111 1111 1111 1111");
        
        assertDoesNotThrow(() -> {
            paymentService.validatePaymentMethod(paymentMethod);
        });
    }

    @Test
    void testValidatePaymentMethod_FourDigitCvv() {
        PaymentMethodRequest paymentMethod = createValidPaymentMethodRequest();
        paymentMethod.setCvv("1234");
        
        assertDoesNotThrow(() -> {
            paymentService.validatePaymentMethod(paymentMethod);
        });
    }

    @Test
    void testProcessPurchase_WithOrderNumber() {
        PurchaseRequest request = createPurchaseRequest();
        request.setOrderNumber("ORDER-123");
        Transaction transaction = createTransaction();
        CreateTransactionResponse authNetResponse = createSuccessfulAuthNetResponse();
        PaymentResponse expectedResponse = createSuccessfulPaymentResponse();
        Customer customer = createCustomer();
        PaymentMethod paymentMethod = createPaymentMethod();
        Order order = new Order();
        order.setOrderNumber("ORDER-123");
        
        when(mapper.generateTransactionId()).thenReturn("test-transaction-id");
        when(customerRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(customer));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(paymentMethod);
        when(orderRepository.findByOrderNumber("ORDER-123")).thenReturn(Optional.of(order));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(mapper.mapToPurchaseTransaction(any(PurchaseRequest.class), any(MerchantAuthenticationType.class))).thenReturn(new CreateTransactionRequest());
        when(mapper.mapToPaymentResponse(any(CreateTransactionResponse.class), anyString(), anyString(), any(PaymentMethodRequest.class), anyString())).thenReturn(expectedResponse);
        
        try (MockedConstruction<CreateTransactionController> mockedController = mockConstruction(CreateTransactionController.class, (mock, context) -> {
            when(mock.getApiResponse()).thenReturn(authNetResponse);
        })) {
            PaymentResponse result = paymentService.processPurchase(request);
            
            assertNotNull(result);
            assertEquals(expectedResponse.getTransactionId(), result.getTransactionId());
            verify(orderRepository).findByOrderNumber("ORDER-123");
        }
    }

    @Test
    void testProcessPurchase_NewCustomerCreation() {
        PurchaseRequest request = createPurchaseRequest();
        Transaction transaction = createTransaction();
        CreateTransactionResponse authNetResponse = createSuccessfulAuthNetResponse();
        PaymentResponse expectedResponse = createSuccessfulPaymentResponse();
        Customer newCustomer = createCustomer();
        PaymentMethod paymentMethod = createPaymentMethod();
        
        when(mapper.generateTransactionId()).thenReturn("test-transaction-id");
        when(customerRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(newCustomer);
        when(authorizeNetCustomerService.createCustomerProfile(any(Customer.class), any(CustomerRequest.class))).thenReturn("profile-123");
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(paymentMethod);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(mapper.mapToPurchaseTransaction(any(PurchaseRequest.class), any(MerchantAuthenticationType.class))).thenReturn(new CreateTransactionRequest());
        when(mapper.mapToPaymentResponse(any(CreateTransactionResponse.class), anyString(), anyString(), any(PaymentMethodRequest.class), anyString())).thenReturn(expectedResponse);
        
        try (MockedConstruction<CreateTransactionController> mockedController = mockConstruction(CreateTransactionController.class, (mock, context) -> {
            when(mock.getApiResponse()).thenReturn(authNetResponse);
        })) {
            PaymentResponse result = paymentService.processPurchase(request);
            
            assertNotNull(result);
            assertEquals(expectedResponse.getTransactionId(), result.getTransactionId());
            verify(customerRepository, times(2)).save(any(Customer.class));
            verify(authorizeNetCustomerService).createCustomerProfile(any(Customer.class), any(CustomerRequest.class));
        }
    }

    @Test
    void testProcessPurchase_CustomerCreationWithBillingAddress() {
        PurchaseRequest request = createPurchaseRequest();
        AddressRequest billingAddress = new AddressRequest();
        billingAddress.setAddress1("123 Main St");
        billingAddress.setCity("New York");
        billingAddress.setState("NY");
        billingAddress.setZipCode("10001");
        billingAddress.setCountry("US");
        request.getCustomer().setBillingAddress(billingAddress);
        
        Transaction transaction = createTransaction();
        CreateTransactionResponse authNetResponse = createSuccessfulAuthNetResponse();
        PaymentResponse expectedResponse = createSuccessfulPaymentResponse();
        Customer newCustomer = createCustomer();
        PaymentMethod paymentMethod = createPaymentMethod();
        
        when(mapper.generateTransactionId()).thenReturn("test-transaction-id");
        when(customerRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(newCustomer);
        when(authorizeNetCustomerService.createCustomerProfile(any(Customer.class), any(CustomerRequest.class))).thenReturn("profile-123");
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(paymentMethod);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(mapper.mapToPurchaseTransaction(any(PurchaseRequest.class), any(MerchantAuthenticationType.class))).thenReturn(new CreateTransactionRequest());
        when(mapper.mapToPaymentResponse(any(CreateTransactionResponse.class), anyString(), anyString(), any(PaymentMethodRequest.class), anyString())).thenReturn(expectedResponse);
        
        try (MockedConstruction<CreateTransactionController> mockedController = mockConstruction(CreateTransactionController.class, (mock, context) -> {
            when(mock.getApiResponse()).thenReturn(authNetResponse);
        })) {
            PaymentResponse result = paymentService.processPurchase(request);
            
            assertNotNull(result);
            verify(customerRepository, times(2)).save(any(Customer.class));
        }
    }
}
