package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.payment.PaymentResponse;
import com.talentica.paymentgateway.dto.payment.PurchaseRequest;
import com.talentica.paymentgateway.entity.*;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import com.talentica.paymentgateway.repository.SubscriptionInvoiceRepository;
import com.talentica.paymentgateway.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SubscriptionBillingEngine service.
 * Tests billing calculations, proration logic, and subscription lifecycle management.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionBillingEngineUnitTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionInvoiceRepository invoiceRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private MetricsService metricsService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SubscriptionBillingEngine billingEngine;

    private Subscription activeSubscription;
    private SubscriptionPlan subscriptionPlan;
    private Customer customer;
    private PaymentMethod paymentMethod;
    private SubscriptionInvoice invoice;

    @BeforeEach
    void setUp() {
        // Create test customer
        customer = new Customer();
        customer.setCustomerReference("CUST_123");
        customer.setEmail("test@example.com");
        customer.setFirstName("John");
        customer.setLastName("Doe");

        // Create test subscription plan
        subscriptionPlan = new SubscriptionPlan();
        subscriptionPlan.setPlanCode("BASIC_MONTHLY");
        subscriptionPlan.setName("Basic Monthly Plan");
        subscriptionPlan.setAmount(new BigDecimal("29.99"));
        subscriptionPlan.setCurrency("USD");
        subscriptionPlan.setIntervalUnit("MONTH");
        subscriptionPlan.setIntervalCount(1);

        // Create test payment method
        paymentMethod = new PaymentMethod();
        paymentMethod.setPaymentType("CREDIT_CARD");
        paymentMethod.setCardNumber("****1234");
        paymentMethod.setExpiryMonth("12");
        paymentMethod.setExpiryYear("2025");
        paymentMethod.setCvv("123");
        paymentMethod.setCardholderName("John Doe");

        // Create test subscription
        activeSubscription = new Subscription();
        activeSubscription.setSubscriptionId("SUB_123");
        activeSubscription.setCustomer(customer);
        activeSubscription.setPlan(subscriptionPlan);
        activeSubscription.setPaymentMethod(paymentMethod);
        activeSubscription.setStatus(SubscriptionStatus.ACTIVE);
        activeSubscription.setCurrentPeriodStart(ZonedDateTime.now().minusDays(30));
        activeSubscription.setCurrentPeriodEnd(ZonedDateTime.now());
        activeSubscription.setNextBillingDate(ZonedDateTime.now());

        // Create test invoice
        invoice = new SubscriptionInvoice();
        invoice.setInvoiceNumber("INV_123");
        invoice.setSubscription(activeSubscription);
        invoice.setCustomer(customer);
        invoice.setAmount(new BigDecimal("29.99"));
        invoice.setCurrency("USD");
        invoice.setStatus("PENDING");
        invoice.setPaymentAttempts(0);
        invoice.setPeriodStart(activeSubscription.getCurrentPeriodStart());
        invoice.setPeriodEnd(activeSubscription.getCurrentPeriodEnd());
        invoice.setDueDate(ZonedDateTime.now().plusDays(3));
    }

    @Test
    void testProcessDueBilling_WithDueSubscriptions_ProcessesSuccessfully() {
        // Given
        List<Subscription> dueSubscriptions = Arrays.asList(activeSubscription);
        when(subscriptionRepository.findSubscriptionsDueForBilling(any(ZonedDateTime.class)))
            .thenReturn(dueSubscriptions);
        when(invoiceRepository.findBySubscriptionAndPeriodStartAndPeriodEnd(
            any(Subscription.class), any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(invoiceRepository.save(any(SubscriptionInvoice.class))).thenReturn(invoice);
        when(paymentService.processPurchase(any(PurchaseRequest.class)))
            .thenReturn(createSuccessfulPaymentResponse());

        // When
        billingEngine.processDueBilling();

        // Then
        verify(subscriptionRepository).findSubscriptionsDueForBilling(any(ZonedDateTime.class));
        verify(invoiceRepository, atLeastOnce()).save(any(SubscriptionInvoice.class));
        verify(paymentService).processPurchase(any(PurchaseRequest.class));
        verify(notificationService).sendBillingSuccessNotification(any(Subscription.class), any(SubscriptionInvoice.class));
        verify(metricsService).recordSuccessfulBilling(eq("BASIC_MONTHLY"), eq(new BigDecimal("29.99")));
    }

    @Test
    void testProcessDueBilling_WithNoDueSubscriptions_CompletesWithoutProcessing() {
        // Given
        when(subscriptionRepository.findSubscriptionsDueForBilling(any(ZonedDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        billingEngine.processDueBilling();

        // Then
        verify(subscriptionRepository).findSubscriptionsDueForBilling(any(ZonedDateTime.class));
        verify(invoiceRepository, never()).save(any(SubscriptionInvoice.class));
        verify(paymentService, never()).processPurchase(any(PurchaseRequest.class));
    }

    @Test
    void testProcessFailedPaymentRetries_WithRetryInvoices_ProcessesSuccessfully() {
        // Given
        invoice.setPaymentAttempts(1);
        List<SubscriptionInvoice> retryInvoices = Arrays.asList(invoice);
        when(invoiceRepository.findInvoicesDueForRetry(any(ZonedDateTime.class)))
            .thenReturn(retryInvoices);
        when(paymentService.processPurchase(any(PurchaseRequest.class)))
            .thenReturn(createSuccessfulPaymentResponse());

        // When
        billingEngine.processFailedPaymentRetries();

        // Then
        verify(invoiceRepository).findInvoicesDueForRetry(any(ZonedDateTime.class));
        verify(paymentService).processPurchase(any(PurchaseRequest.class));
        verify(notificationService).sendPaymentRetrySuccessNotification(any(Subscription.class), any(SubscriptionInvoice.class));
        verify(metricsService).recordPaymentRetry(eq("BASIC_MONTHLY"), anyInt(), eq(true));
    }

    @Test
    void testProcessFailedPaymentRetries_WithMaxRetriesReached_CancelsSubscription() {
        // Given
        invoice.setPaymentAttempts(5); // Max retries reached
        List<SubscriptionInvoice> retryInvoices = Arrays.asList(invoice);
        when(invoiceRepository.findInvoicesDueForRetry(any(ZonedDateTime.class)))
            .thenReturn(retryInvoices);

        // When
        billingEngine.processFailedPaymentRetries();

        // Then
        verify(invoiceRepository).findInvoicesDueForRetry(any(ZonedDateTime.class));
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(invoiceRepository).save(any(SubscriptionInvoice.class));
        verify(notificationService).sendSubscriptionCancelledNotification(any(Subscription.class), anyString());
        verify(metricsService).recordSubscriptionCancelledForNonPayment(eq("BASIC_MONTHLY"));
    }

    @Test
    void testProcessSubscriptionLifecycle_CallsAllLifecycleHandlers() {
        // Given
        when(subscriptionRepository.findSubscriptionsEndingTrial(any(ZonedDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(subscriptionRepository.findByStatus(SubscriptionStatus.PAST_DUE))
            .thenReturn(Collections.emptyList());
        when(subscriptionRepository.findSubscriptionsWithScheduledCancellation(any(ZonedDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(subscriptionRepository.findSubscriptionsWithScheduledPlanChange(any(ZonedDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        billingEngine.processSubscriptionLifecycle();

        // Then
        verify(subscriptionRepository).findSubscriptionsEndingTrial(any(ZonedDateTime.class));
        verify(subscriptionRepository).findByStatus(SubscriptionStatus.PAST_DUE);
        verify(subscriptionRepository).findSubscriptionsWithScheduledCancellation(any(ZonedDateTime.class));
        verify(subscriptionRepository).findSubscriptionsWithScheduledPlanChange(any(ZonedDateTime.class));
    }

    @Test
    void testProcessSubscriptionBilling_WithActiveSubscription_ProcessesSuccessfully() throws Exception {
        // Given
        when(invoiceRepository.findBySubscriptionAndPeriodStartAndPeriodEnd(
            any(Subscription.class), any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(invoiceRepository.save(any(SubscriptionInvoice.class))).thenReturn(invoice);
        when(paymentService.processPurchase(any(PurchaseRequest.class)))
            .thenReturn(createSuccessfulPaymentResponse());

        // When
        CompletableFuture<Void> result = billingEngine.processSubscriptionBilling(activeSubscription);
        result.get(); // Wait for completion

        // Then
        verify(invoiceRepository, atLeastOnce()).save(any(SubscriptionInvoice.class));
        verify(paymentService).processPurchase(any(PurchaseRequest.class));
        verify(subscriptionRepository).save(activeSubscription);
        verify(notificationService).sendBillingSuccessNotification(activeSubscription, invoice);
        verify(metricsService).recordSuccessfulBilling("BASIC_MONTHLY", new BigDecimal("29.99"));
    }

    @Test
    void testProcessSubscriptionBilling_WithInactiveSubscription_SkipsProcessing() throws Exception {
        // Given
        activeSubscription.setStatus(SubscriptionStatus.CANCELLED);

        // When
        CompletableFuture<Void> result = billingEngine.processSubscriptionBilling(activeSubscription);
        result.get(); // Wait for completion

        // Then
        verify(invoiceRepository, never()).save(any(SubscriptionInvoice.class));
        verify(paymentService, never()).processPurchase(any(PurchaseRequest.class));
    }

    @Test
    void testProcessSubscriptionBilling_WithAlreadyBilledSubscription_SkipsProcessing() throws Exception {
        // Given
        SubscriptionInvoice existingInvoice = new SubscriptionInvoice();
        existingInvoice.setStatus("PAID");
        when(invoiceRepository.findBySubscriptionAndPeriodStartAndPeriodEnd(
            any(Subscription.class), any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(Arrays.asList(existingInvoice));

        // When
        CompletableFuture<Void> result = billingEngine.processSubscriptionBilling(activeSubscription);
        result.get(); // Wait for completion

        // Then
        verify(paymentService, never()).processPurchase(any(PurchaseRequest.class));
    }

    @Test
    void testProcessSubscriptionBilling_WithPaymentFailure_HandlesFailedPayment() throws Exception {
        // Given
        when(invoiceRepository.findBySubscriptionAndPeriodStartAndPeriodEnd(
            any(Subscription.class), any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(invoiceRepository.save(any(SubscriptionInvoice.class))).thenReturn(invoice);
        when(paymentService.processPurchase(any(PurchaseRequest.class)))
            .thenReturn(createFailedPaymentResponse());

        // When
        CompletableFuture<Void> result = billingEngine.processSubscriptionBilling(activeSubscription);
        result.get(); // Wait for completion

        // Then
        verify(invoiceRepository, atLeast(2)).save(any(SubscriptionInvoice.class)); // Save invoice and update after failure
        verify(notificationService).sendPaymentFailedNotification(any(Subscription.class), any(SubscriptionInvoice.class));
        verify(metricsService).recordFailedBilling("BASIC_MONTHLY", new BigDecimal("29.99"));
    }

    @Test
    void testProcessSubscriptionBilling_WithException_ThrowsPaymentProcessingException() {
        // Given
        when(invoiceRepository.findBySubscriptionAndPeriodStartAndPeriodEnd(
            any(Subscription.class), any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> 
            billingEngine.processSubscriptionBilling(activeSubscription)
        )
        .isInstanceOf(PaymentProcessingException.class)
        .hasMessageContaining("Billing processing failed");

        verify(metricsService).recordBillingError("BASIC_MONTHLY");
    }

    @Test
    void testRetryFailedPayment_WithSuccessfulRetry_ReactivatesSubscription() {
        // Given
        activeSubscription.setStatus(SubscriptionStatus.PAST_DUE);
        invoice.setPaymentAttempts(2);
        when(paymentService.processPurchase(any(PurchaseRequest.class)))
            .thenReturn(createSuccessfulPaymentResponse());

        // When
        billingEngine.retryFailedPayment(invoice);

        // Then
        verify(paymentService).processPurchase(any(PurchaseRequest.class));
        verify(subscriptionRepository).save(activeSubscription);
        verify(notificationService).sendPaymentRetrySuccessNotification(activeSubscription, invoice);
        verify(metricsService).recordPaymentRetry(eq("BASIC_MONTHLY"), anyInt(), eq(true));
    }

    @Test
    void testRetryFailedPayment_WithFailedRetry_SchedulesNextAttempt() {
        // Given
        invoice.setPaymentAttempts(2);
        when(paymentService.processPurchase(any(PurchaseRequest.class)))
            .thenReturn(createFailedPaymentResponse());

        // When
        billingEngine.retryFailedPayment(invoice);

        // Then
        verify(paymentService).processPurchase(any(PurchaseRequest.class));
        verify(invoiceRepository, atLeast(1)).save(invoice);
        verify(notificationService).sendPaymentRetryNotification(eq(activeSubscription), eq(invoice), anyInt());
        verify(metricsService).recordPaymentRetry(eq("BASIC_MONTHLY"), anyInt(), eq(false));
    }

    @Test
    void testRetryFailedPayment_WithMaxRetriesReached_DoesNotAttemptPayment() {
        // Given
        invoice.setPaymentAttempts(5); // Max retries reached

        // When
        billingEngine.retryFailedPayment(invoice);

        // Then
        verify(paymentService, never()).processPurchase(any(PurchaseRequest.class));
        verify(subscriptionRepository).save(activeSubscription);
        verify(invoiceRepository).save(invoice);
        verify(notificationService).sendSubscriptionCancelledNotification(eq(activeSubscription), anyString());
        verify(metricsService).recordSubscriptionCancelledForNonPayment("BASIC_MONTHLY");
    }

    @Test
    void testHandleTrialExpirations_WithExpiringTrials_ProcessesBilling() {
        // Given
        Subscription trialSubscription = createTrialSubscription();
        when(subscriptionRepository.findSubscriptionsEndingTrial(any(ZonedDateTime.class)))
            .thenReturn(Arrays.asList(trialSubscription));
        lenient().when(invoiceRepository.findBySubscriptionAndPeriodStartAndPeriodEnd(
            any(Subscription.class), any(ZonedDateTime.class), any(ZonedDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(invoiceRepository.save(any(SubscriptionInvoice.class))).thenReturn(invoice);
        when(paymentService.processPurchase(any(PurchaseRequest.class)))
            .thenReturn(createSuccessfulPaymentResponse());

        // When
        billingEngine.processSubscriptionLifecycle();

        // Then
        verify(subscriptionRepository, times(2)).save(trialSubscription); // Saved in both handleTrialExpirations and processSubscriptionBilling
        verify(notificationService).sendTrialExpirationNotification(trialSubscription);
        verify(invoiceRepository, atLeastOnce()).save(any(SubscriptionInvoice.class));
    }

    @Test
    void testHandlePastDueSubscriptions_WithPastDueSubscriptions_CancelsWhenNeeded() {
        // Given
        activeSubscription.setStatus(SubscriptionStatus.PAST_DUE);
        SubscriptionInvoice maxRetriedInvoice = new SubscriptionInvoice();
        maxRetriedInvoice.setPaymentAttempts(5);
        activeSubscription.setInvoices(Arrays.asList(maxRetriedInvoice));
        
        when(subscriptionRepository.findByStatus(SubscriptionStatus.PAST_DUE))
            .thenReturn(Arrays.asList(activeSubscription));

        // When
        billingEngine.processSubscriptionLifecycle();

        // Then
        verify(subscriptionRepository).save(activeSubscription);
        verify(notificationService).sendSubscriptionCancelledNotification(eq(activeSubscription), anyString());
    }

    @Test
    void testHandleScheduledCancellations_WithScheduledCancellations_ProcessesCancellations() {
        // Given
        activeSubscription.setMetadata(new HashMap<>());
        activeSubscription.getMetadata().put("cancellationReason", "User requested");
        when(subscriptionRepository.findSubscriptionsWithScheduledCancellation(any(ZonedDateTime.class)))
            .thenReturn(Arrays.asList(activeSubscription));

        // When
        billingEngine.processSubscriptionLifecycle();

        // Then
        verify(subscriptionRepository).save(activeSubscription);
        verify(notificationService).sendSubscriptionCancelledNotification(activeSubscription, "User requested");
    }

    @Test
    void testHandleScheduledPlanChanges_WithScheduledChanges_ProcessesChanges() {
        // Given
        activeSubscription.setMetadata(new HashMap<>());
        activeSubscription.getMetadata().put("scheduledPlanChange", "PREMIUM_MONTHLY");
        activeSubscription.getMetadata().put("planChangeDate", ZonedDateTime.now().toString());
        when(subscriptionRepository.findSubscriptionsWithScheduledPlanChange(any(ZonedDateTime.class)))
            .thenReturn(Arrays.asList(activeSubscription));

        // When
        billingEngine.processSubscriptionLifecycle();

        // Then
        verify(subscriptionRepository).save(activeSubscription);
        assertThat(activeSubscription.getMetadata()).doesNotContainKey("scheduledPlanChange");
        assertThat(activeSubscription.getMetadata()).doesNotContainKey("planChangeDate");
    }

    // Helper methods

    private PaymentResponse createSuccessfulPaymentResponse() {
        PaymentResponse response = new PaymentResponse();
        response.setSuccess(true);
        response.setTransactionId("TXN_123");
        response.setMessage("Payment successful");
        return response;
    }

    private PaymentResponse createFailedPaymentResponse() {
        PaymentResponse response = new PaymentResponse();
        response.setSuccess(false);
        response.setMessage("Payment failed - insufficient funds");
        response.setErrorCode("INSUFFICIENT_FUNDS");
        return response;
    }

    private Subscription createTrialSubscription() {
        Subscription trialSub = new Subscription();
        trialSub.setSubscriptionId("SUB_TRIAL_123");
        trialSub.setCustomer(customer);
        trialSub.setPlan(subscriptionPlan);
        trialSub.setPaymentMethod(paymentMethod);
        trialSub.setStatus(SubscriptionStatus.ACTIVE);
        trialSub.setTrialEnd(ZonedDateTime.now());
        return trialSub;
    }
}
