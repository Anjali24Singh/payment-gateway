package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.subscription.*;
import com.talentica.paymentgateway.entity.*;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import com.talentica.paymentgateway.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubscriptionServiceUnitTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionPlanRepository planRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private SubscriptionInvoiceRepository invoiceRepository;

    @Mock
    private MetricsService metricsService;

    @Mock
    private ProrationService prorationService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private Customer testCustomer;
    private SubscriptionPlan testPlan;
    private PaymentMethod testPaymentMethod;
    private Subscription testSubscription;
    private CreateSubscriptionRequest createRequest;
    private UpdateSubscriptionRequest updateRequest;
    private CancelSubscriptionRequest cancelRequest;

    @BeforeEach
    void setUp() {
        // Setup test customer
        testCustomer = new Customer();
        testCustomer.setId(UUID.randomUUID());
        testCustomer.setCustomerReference("CUST_001");
        testCustomer.setEmail("test@example.com");
        testCustomer.setFirstName("Test");
        testCustomer.setLastName("Customer");

        // Setup test plan
        testPlan = new SubscriptionPlan();
        testPlan.setId(UUID.randomUUID());
        testPlan.setPlanCode("BASIC_MONTHLY");
        testPlan.setName("Basic Monthly Plan");
        testPlan.setAmount(new BigDecimal("29.99"));
        testPlan.setCurrency("USD");
        testPlan.setIntervalUnit("MONTH");
        testPlan.setIntervalCount(1);
        testPlan.setIsActive(true);
        testPlan.setTrialPeriodDays(7);
        testPlan.setSetupFee(new BigDecimal("10.00"));

        // Setup test payment method
        testPaymentMethod = new PaymentMethod();
        testPaymentMethod.setId(UUID.randomUUID());
        testPaymentMethod.setPaymentToken("pm_123");
        testPaymentMethod.setPaymentType("CREDIT_CARD");
        testPaymentMethod.setCardLastFour("1234");
        testPaymentMethod.setCustomer(testCustomer);

        // Setup test subscription
        testSubscription = new Subscription();
        testSubscription.setId(UUID.randomUUID());
        testSubscription.setSubscriptionId("sub_123");
        testSubscription.setCustomer(testCustomer);
        testSubscription.setPlan(testPlan);
        testSubscription.setPaymentMethod(testPaymentMethod);
        testSubscription.setStatus(SubscriptionStatus.ACTIVE);
        testSubscription.setCurrentPeriodStart(ZonedDateTime.now());
        testSubscription.setCurrentPeriodEnd(ZonedDateTime.now().plusMonths(1));
        testSubscription.setBillingCycleAnchor(ZonedDateTime.now());
        testSubscription.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
        testSubscription.setUpdatedAt(ZonedDateTime.now().toLocalDateTime());
        // Initialize empty invoices list to avoid NullPointerException
        testSubscription.setInvoices(new ArrayList<>());

        // Setup create request
        createRequest = new CreateSubscriptionRequest();
        createRequest.setCustomerId("CUST_001");
        createRequest.setPlanCode("BASIC_MONTHLY");
        createRequest.setPaymentMethodId("pm_123");
        createRequest.setStartTrial(true);
        createRequest.setProrated(true);
        createRequest.setIdempotencyKey("idem_123");

        // Setup update request
        updateRequest = new UpdateSubscriptionRequest();
        updateRequest.setPlanCode("PREMIUM_MONTHLY");
        updateRequest.setPaymentMethodId("pm_456");
        updateRequest.setProrated(true);
        updateRequest.setChangeOption("IMMEDIATE");
        updateRequest.setChangeReason("Upgrade");

        // Setup cancel request
        cancelRequest = new CancelSubscriptionRequest();
        cancelRequest.setWhen("IMMEDIATE");
        cancelRequest.setReason("Customer request");
        cancelRequest.setRefundProrated(true);
        cancelRequest.setNotes("Customer no longer needs service");

    }

    @Test
    void createSubscription_WithValidRequest_ShouldReturnSubscriptionResponse() {
        when(customerRepository.findByCustomerId("CUST_001")).thenReturn(Optional.of(testCustomer));
        when(planRepository.findByPlanCode("BASIC_MONTHLY")).thenReturn(Optional.of(testPlan));
        when(paymentMethodRepository.findByPaymentMethodId("pm_123")).thenReturn(Optional.of(testPaymentMethod));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);
        when(invoiceRepository.save(any(SubscriptionInvoice.class))).thenAnswer(invocation -> {
            SubscriptionInvoice invoice = invocation.getArgument(0);
            invoice.setId(UUID.randomUUID());
            invoice.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
            invoice.setUpdatedAt(ZonedDateTime.now().toLocalDateTime());
            return invoice;
        });

        SubscriptionResponse result = subscriptionService.createSubscription(createRequest);

        assertNotNull(result);
        assertEquals("sub_123", result.getSubscriptionId());
        assertEquals("CUST_001", result.getCustomerId());
        assertEquals("BASIC_MONTHLY", result.getPlanCode());
        verify(subscriptionRepository, times(2)).save(any(Subscription.class));
        verify(metricsService).recordSubscriptionCreated("BASIC_MONTHLY");
    }

    @Test
    void createSubscription_WithIdempotencyKey_ShouldReturnExistingSubscription() {
        when(subscriptionRepository.findByCustomerIdAndIdempotencyKey("CUST_001", "idem_123"))
                .thenReturn(Optional.of(testSubscription));

        SubscriptionResponse result = subscriptionService.createSubscription(createRequest);

        assertNotNull(result);
        assertEquals("sub_123", result.getSubscriptionId());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(customerRepository, never()).findByCustomerId(anyString());
    }

    @Test
    void createSubscription_WithNonExistentCustomer_ShouldThrowException() {
        when(customerRepository.findByCustomerId("CUST_001")).thenReturn(Optional.empty());

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            subscriptionService.createSubscription(createRequest);
        });

        assertEquals("Customer not found: CUST_001", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void createSubscription_WithNonExistentPlan_ShouldThrowException() {
        when(customerRepository.findByCustomerId("CUST_001")).thenReturn(Optional.of(testCustomer));
        when(planRepository.findByPlanCode("BASIC_MONTHLY")).thenReturn(Optional.empty());

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            subscriptionService.createSubscription(createRequest);
        });

        assertEquals("Plan not found: BASIC_MONTHLY", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void createSubscription_WithInactivePlan_ShouldThrowException() {
        testPlan.setIsActive(false);
        when(customerRepository.findByCustomerId("CUST_001")).thenReturn(Optional.of(testCustomer));
        when(planRepository.findByPlanCode("BASIC_MONTHLY")).thenReturn(Optional.of(testPlan));

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            subscriptionService.createSubscription(createRequest);
        });

        assertEquals("Plan is not active: BASIC_MONTHLY", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void createSubscription_WithNonExistentPaymentMethod_ShouldThrowException() {
        when(customerRepository.findByCustomerId("CUST_001")).thenReturn(Optional.of(testCustomer));
        when(planRepository.findByPlanCode("BASIC_MONTHLY")).thenReturn(Optional.of(testPlan));
        when(paymentMethodRepository.findByPaymentMethodId("pm_123")).thenReturn(Optional.empty());

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            subscriptionService.createSubscription(createRequest);
        });

        assertEquals("Payment method not found: pm_123", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void createSubscription_WithTrialPeriod_ShouldStartTrial() {
        when(customerRepository.findByCustomerId("CUST_001")).thenReturn(Optional.of(testCustomer));
        when(planRepository.findByPlanCode("BASIC_MONTHLY")).thenReturn(Optional.of(testPlan));
        when(paymentMethodRepository.findByPaymentMethodId("pm_123")).thenReturn(Optional.of(testPaymentMethod));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            sub.setId(UUID.randomUUID());
            sub.setSubscriptionId("sub_trial_123");
            // Ensure plan is set to avoid NullPointerException in startTrial
            if (sub.getPlan() == null) {
                sub.setPlan(testPlan);
            }
            // Set required timestamp fields to avoid NullPointerException in mapToSubscriptionResponse
            if (sub.getCreatedAt() == null) {
                sub.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
            }
            if (sub.getUpdatedAt() == null) {
                sub.setUpdatedAt(ZonedDateTime.now().toLocalDateTime());
            }
            return sub;
        });

        SubscriptionResponse result = subscriptionService.createSubscription(createRequest);

        assertNotNull(result);
        verify(subscriptionRepository, times(2)).save(any(Subscription.class));
    }

    @Test
    void createSubscription_WithSetupFee_ShouldCreateSetupFeeInvoice() {
        when(customerRepository.findByCustomerId("CUST_001")).thenReturn(Optional.of(testCustomer));
        when(planRepository.findByPlanCode("BASIC_MONTHLY")).thenReturn(Optional.of(testPlan));
        when(paymentMethodRepository.findByPaymentMethodId("pm_123")).thenReturn(Optional.of(testPaymentMethod));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);
        when(invoiceRepository.save(any(SubscriptionInvoice.class))).thenAnswer(invocation -> {
            SubscriptionInvoice invoice = invocation.getArgument(0);
            invoice.setId(UUID.randomUUID());
            invoice.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
            invoice.setUpdatedAt(ZonedDateTime.now().toLocalDateTime());
            return invoice;
        });

        subscriptionService.createSubscription(createRequest);

        verify(invoiceRepository, times(2)).save(any(SubscriptionInvoice.class)); // Setup fee + immediate billing
    }

    @Test
    void updateSubscription_WithValidRequest_ShouldReturnUpdatedSubscription() {
        SubscriptionPlan newPlan = new SubscriptionPlan();
        newPlan.setPlanCode("PREMIUM_MONTHLY");
        newPlan.setIsActive(true);

        PaymentMethod newPaymentMethod = new PaymentMethod();
        newPaymentMethod.setPaymentToken("pm_456");

        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));
        when(planRepository.findByPlanCode("PREMIUM_MONTHLY")).thenReturn(Optional.of(newPlan));
        when(paymentMethodRepository.findByPaymentMethodId("pm_456")).thenReturn(Optional.of(newPaymentMethod));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        ProrationCalculation proration = new ProrationCalculation();
        proration.setProrationApplies(true);
        proration.setNetAmount(new BigDecimal("5.00"));
        when(prorationService.calculateProration(any(), any(), any())).thenReturn(proration);

        SubscriptionResponse result = subscriptionService.updateSubscription("sub_123", updateRequest);

        assertNotNull(result);
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(metricsService).recordPlanChange(anyString(), eq("PREMIUM_MONTHLY"));
    }

    @Test
    void updateSubscription_WithNonExistentSubscription_ShouldThrowException() {
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.empty());

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            subscriptionService.updateSubscription("sub_123", updateRequest);
        });

        assertEquals("Subscription not found: sub_123", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void updateSubscription_WithInactiveSubscription_ShouldThrowException() {
        testSubscription.setStatus(SubscriptionStatus.CANCELLED);
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            subscriptionService.updateSubscription("sub_123", updateRequest);
        });

        assertEquals("Cannot update inactive subscription: sub_123", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void updateSubscription_WithInactivePlan_ShouldThrowException() {
        SubscriptionPlan newPlan = new SubscriptionPlan();
        newPlan.setPlanCode("PREMIUM_MONTHLY");
        newPlan.setIsActive(false);

        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));
        when(planRepository.findByPlanCode("PREMIUM_MONTHLY")).thenReturn(Optional.of(newPlan));

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            subscriptionService.updateSubscription("sub_123", updateRequest);
        });

        assertEquals("Plan is not active: PREMIUM_MONTHLY", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void updateSubscription_WithMetadataOnly_ShouldUpdateMetadata() {
        UpdateSubscriptionRequest metadataRequest = new UpdateSubscriptionRequest();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("customField", "customValue");
        metadataRequest.setMetadata(metadata);

        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        SubscriptionResponse result = subscriptionService.updateSubscription("sub_123", metadataRequest);

        assertNotNull(result);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void cancelSubscription_WithImmediateCancellation_ShouldCancelImmediately() {
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        ProrationCalculation proration = new ProrationCalculation();
        proration.setNetAmount(new BigDecimal("10.00"));
        when(prorationService.calculateRefundProration(any(), any())).thenReturn(proration);

        SubscriptionResponse result = subscriptionService.cancelSubscription("sub_123", cancelRequest);

        assertNotNull(result);
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(metricsService).recordSubscriptionCancelled(anyString(), eq("Customer request"));
    }

    @Test
    void cancelSubscription_WithEndOfPeriodCancellation_ShouldScheduleCancellation() {
        cancelRequest.setWhen("END_OF_PERIOD");
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        SubscriptionResponse result = subscriptionService.cancelSubscription("sub_123", cancelRequest);

        assertNotNull(result);
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(metricsService).recordSubscriptionCancelled(anyString(), eq("Customer request"));
    }

    @Test
    void cancelSubscription_WithNonExistentSubscription_ShouldThrowException() {
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.empty());

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            subscriptionService.cancelSubscription("sub_123", cancelRequest);
        });

        assertEquals("Subscription not found: sub_123", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void cancelSubscription_WithAlreadyCancelledSubscription_ShouldReturnExisting() {
        testSubscription.setStatus(SubscriptionStatus.CANCELLED);
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));

        SubscriptionResponse result = subscriptionService.cancelSubscription("sub_123", cancelRequest);

        assertNotNull(result);
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(metricsService, never()).recordSubscriptionCancelled(anyString(), anyString());
    }

    @Test
    void pauseSubscription_WithActiveSubscription_ShouldPauseSuccessfully() {
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        SubscriptionResponse result = subscriptionService.pauseSubscription("sub_123");

        assertNotNull(result);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void pauseSubscription_WithNonExistentSubscription_ShouldThrowException() {
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.empty());

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            subscriptionService.pauseSubscription("sub_123");
        });

        assertEquals("Subscription not found: sub_123", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void pauseSubscription_WithInactiveSubscription_ShouldThrowException() {
        testSubscription.setStatus(SubscriptionStatus.CANCELLED);
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            subscriptionService.pauseSubscription("sub_123");
        });

        assertEquals("Cannot pause inactive subscription: sub_123", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void resumeSubscription_WithPausedSubscription_ShouldResumeSuccessfully() {
        testSubscription.setStatus(SubscriptionStatus.PAUSED);
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        SubscriptionResponse result = subscriptionService.resumeSubscription("sub_123");

        assertNotNull(result);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void resumeSubscription_WithNonExistentSubscription_ShouldThrowException() {
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.empty());

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            subscriptionService.resumeSubscription("sub_123");
        });

        assertEquals("Subscription not found: sub_123", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void resumeSubscription_WithNonPausedSubscription_ShouldThrowException() {
        testSubscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            subscriptionService.resumeSubscription("sub_123");
        });

        assertEquals("Cannot resume non-paused subscription: sub_123", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void getSubscription_WithValidId_ShouldReturnSubscription() {
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));

        SubscriptionResponse result = subscriptionService.getSubscription("sub_123");

        assertNotNull(result);
        assertEquals("sub_123", result.getSubscriptionId());
        assertEquals("CUST_001", result.getCustomerId());
    }

    @Test
    void getSubscription_WithNonExistentId_ShouldThrowException() {
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.empty());

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            subscriptionService.getSubscription("sub_123");
        });

        assertEquals("Subscription not found: sub_123", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void getCustomerSubscriptions_WithValidCustomer_ShouldReturnPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Subscription> subscriptions = Arrays.asList(testSubscription);
        Page<Subscription> subscriptionPage = new PageImpl<>(subscriptions, pageable, 1);

        when(customerRepository.findByCustomerId("CUST_001")).thenReturn(Optional.of(testCustomer));
        when(subscriptionRepository.findByCustomer(testCustomer, pageable)).thenReturn(subscriptionPage);

        Page<SubscriptionResponse> result = subscriptionService.getCustomerSubscriptions("CUST_001", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("sub_123", result.getContent().get(0).getSubscriptionId());
    }

    @Test
    void getCustomerSubscriptions_WithNonExistentCustomer_ShouldThrowException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.findByCustomerId("CUST_001")).thenReturn(Optional.empty());

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class, () -> {
            subscriptionService.getCustomerSubscriptions("CUST_001", pageable);
        });

        assertEquals("Customer not found: CUST_001", exception.getMessage());
        assertEquals("PAYMENT_PROCESSING_ERROR", exception.getErrorCode());
    }

    @Test
    void getSubscriptionsDueForBilling_ShouldReturnDueSubscriptions() {
        List<Subscription> dueSubscriptions = Arrays.asList(testSubscription);
        when(subscriptionRepository.findSubscriptionsDueForBilling(any(ZonedDateTime.class)))
                .thenReturn(dueSubscriptions);

        List<Subscription> result = subscriptionService.getSubscriptionsDueForBilling();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sub_123", result.get(0).getSubscriptionId());
    }

    @Test
    void getSubscriptionsDueForBilling_WithNoDueSubscriptions_ShouldReturnEmptyList() {
        when(subscriptionRepository.findSubscriptionsDueForBilling(any(ZonedDateTime.class)))
                .thenReturn(Arrays.asList());

        List<Subscription> result = subscriptionService.getSubscriptionsDueForBilling();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void createSubscription_WithIdempotencyCheckException_ShouldContinueWithCreation() {
        when(subscriptionRepository.findByCustomerIdAndIdempotencyKey("CUST_001", "idem_123"))
                .thenThrow(new RuntimeException("Database error"));
        when(customerRepository.findByCustomerId("CUST_001")).thenReturn(Optional.of(testCustomer));
        when(planRepository.findByPlanCode("BASIC_MONTHLY")).thenReturn(Optional.of(testPlan));
        when(paymentMethodRepository.findByPaymentMethodId("pm_123")).thenReturn(Optional.of(testPaymentMethod));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);
        when(invoiceRepository.save(any(SubscriptionInvoice.class))).thenAnswer(invocation -> {
            SubscriptionInvoice invoice = invocation.getArgument(0);
            invoice.setId(UUID.randomUUID());
            invoice.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
            invoice.setUpdatedAt(ZonedDateTime.now().toLocalDateTime());
            return invoice;
        });

        SubscriptionResponse result = subscriptionService.createSubscription(createRequest);

        assertNotNull(result);
        assertEquals("sub_123", result.getSubscriptionId());
        verify(subscriptionRepository, times(2)).save(any(Subscription.class));
    }

    @Test
    void createSubscription_WithoutTrialPeriod_ShouldNotStartTrial() {
        createRequest.setStartTrial(false);
        when(customerRepository.findByCustomerId("CUST_001")).thenReturn(Optional.of(testCustomer));
        when(planRepository.findByPlanCode("BASIC_MONTHLY")).thenReturn(Optional.of(testPlan));
        when(paymentMethodRepository.findByPaymentMethodId("pm_123")).thenReturn(Optional.of(testPaymentMethod));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);
        when(invoiceRepository.save(any(SubscriptionInvoice.class))).thenAnswer(invocation -> {
            SubscriptionInvoice invoice = invocation.getArgument(0);
            invoice.setId(UUID.randomUUID());
            invoice.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
            invoice.setUpdatedAt(ZonedDateTime.now().toLocalDateTime());
            return invoice;
        });

        SubscriptionResponse result = subscriptionService.createSubscription(createRequest);

        assertNotNull(result);
        verify(subscriptionRepository, times(2)).save(any(Subscription.class));
    }

    @Test
    void createSubscription_WithCustomStartDate_ShouldUseCustomDate() {
        ZonedDateTime customStartDate = ZonedDateTime.now().plusDays(7);
        createRequest.setStartDate(customStartDate);
        createRequest.setBillingCycleAnchor(customStartDate);

        when(customerRepository.findByCustomerId("CUST_001")).thenReturn(Optional.of(testCustomer));
        when(planRepository.findByPlanCode("BASIC_MONTHLY")).thenReturn(Optional.of(testPlan));
        when(paymentMethodRepository.findByPaymentMethodId("pm_123")).thenReturn(Optional.of(testPaymentMethod));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);
        when(invoiceRepository.save(any(SubscriptionInvoice.class))).thenAnswer(invocation -> {
            SubscriptionInvoice invoice = invocation.getArgument(0);
            invoice.setId(UUID.randomUUID());
            invoice.setCreatedAt(ZonedDateTime.now().toLocalDateTime());
            invoice.setUpdatedAt(ZonedDateTime.now().toLocalDateTime());
            return invoice;
        });

        SubscriptionResponse result = subscriptionService.createSubscription(createRequest);

        assertNotNull(result);
        verify(subscriptionRepository, times(2)).save(any(Subscription.class));
    }

    @Test
    void updateSubscription_WithBillingCycleAnchorChange_ShouldUpdateAnchor() {
        ZonedDateTime newAnchor = ZonedDateTime.now().plusDays(15);
        updateRequest = new UpdateSubscriptionRequest();
        updateRequest.setBillingCycleAnchor(newAnchor);

        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        SubscriptionResponse result = subscriptionService.updateSubscription("sub_123", updateRequest);

        assertNotNull(result);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void updateSubscription_WithNoChanges_ShouldNotSave() {
        UpdateSubscriptionRequest emptyRequest = new UpdateSubscriptionRequest();
        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));

        SubscriptionResponse result = subscriptionService.updateSubscription("sub_123", emptyRequest);

        assertNotNull(result);
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void cancelSubscription_WithSpecificCancelDate_ShouldScheduleForDate() {
        ZonedDateTime cancelDate = ZonedDateTime.now().plusDays(30);
        cancelRequest.setWhen("SPECIFIC_DATE");
        cancelRequest.setCancelAt(cancelDate);

        when(subscriptionRepository.findBySubscriptionId("sub_123")).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        SubscriptionResponse result = subscriptionService.cancelSubscription("sub_123", cancelRequest);

        assertNotNull(result);
        verify(subscriptionRepository).save(any(Subscription.class));
    }
}
