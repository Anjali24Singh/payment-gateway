package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.dto.payment.PurchaseRequest;
import com.talentica.paymentgateway.dto.payment.PaymentMethodRequest;
import com.talentica.paymentgateway.dto.payment.PaymentResponse;
import com.talentica.paymentgateway.entity.*;
import com.talentica.paymentgateway.exception.PaymentProcessingException;
import com.talentica.paymentgateway.repository.*;
import com.talentica.paymentgateway.util.CorrelationIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Billing engine for processing recurring subscription charges.
 * Handles automatic billing, failed payment retry logic, and dunning management.
 * 
 * Features:
 * - Automated recurring billing processing
 * - Invoice generation and payment attempts
 * - Failed payment retry with exponential backoff
 * - Dunning management and grace periods
 * - Subscription lifecycle management based on payment status
 * - Comprehensive billing analytics and reporting
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
@Transactional
public class SubscriptionBillingEngine {

    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int[] RETRY_DELAY_DAYS = {1, 3, 7, 14, 30}; // Exponential backoff
    private static final int GRACE_PERIOD_DAYS = 3;

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionInvoiceRepository invoiceRepository;
    private final PaymentService paymentService;
    private final MetricsService metricsService;
    private final NotificationService notificationService;

    public SubscriptionBillingEngine(SubscriptionRepository subscriptionRepository,
                                   SubscriptionInvoiceRepository invoiceRepository,
                                   PaymentService paymentService,
                                   MetricsService metricsService,
                                   NotificationService notificationService) {
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentService = paymentService;
        this.metricsService = metricsService;
        this.notificationService = notificationService;
    }

    /**
     * Processes all subscriptions due for billing.
     * Scheduled to run every hour during business hours.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void processDueBilling() {
        log.info("Starting scheduled billing process");
        
        List<Subscription> dueSubscriptions = subscriptionRepository.findSubscriptionsDueForBilling(ZonedDateTime.now());
        
        log.info("Found {} subscriptions due for billing", dueSubscriptions.size());
        
        for (Subscription subscription : dueSubscriptions) {
            try {
                processSubscriptionBilling(subscription);
            } catch (Exception e) {
                log.error("Error processing billing for subscription: {}", 
                           subscription.getSubscriptionId(), e);
                // Continue with next subscription
            }
        }
        
        log.info("Completed scheduled billing process");
    }

    /**
     * Processes failed payment retries.
     * Scheduled to run daily at 9 AM.
     */
    @Scheduled(cron = "0 0 9 * * *") // Daily at 9 AM
    public void processFailedPaymentRetries() {
        log.info("Starting failed payment retry process");
        
        List<SubscriptionInvoice> retryInvoices = invoiceRepository.findInvoicesDueForRetry(ZonedDateTime.now());
        
        log.info("Found {} invoices due for retry", retryInvoices.size());
        
        for (SubscriptionInvoice invoice : retryInvoices) {
            try {
                retryFailedPayment(invoice);
            } catch (Exception e) {
                log.error("Error retrying payment for invoice: {}", 
                           invoice.getInvoiceNumber(), e);
                // Continue with next invoice
            }
        }
        
        log.info("Completed failed payment retry process");
    }

    /**
     * Processes subscription lifecycle events.
     * Scheduled to run daily at 6 AM.
     */
    @Scheduled(cron = "0 0 6 * * *") // Daily at 6 AM
    public void processSubscriptionLifecycle() {
        log.info("Starting subscription lifecycle process");
        
        // Handle trial expirations
        handleTrialExpirations();
        
        // Handle past due subscriptions
        handlePastDueSubscriptions();
        
        // Handle scheduled cancellations
        handleScheduledCancellations();
        
        // Handle scheduled plan changes
        handleScheduledPlanChanges();
        
        log.info("Completed subscription lifecycle process");
    }

    /**
     * Processes billing for a specific subscription.
     * 
     * @param subscription Subscription to bill
     */
    @Async
    public CompletableFuture<Void> processSubscriptionBilling(Subscription subscription) {
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Processing billing for subscription: {} - CorrelationId: {}", 
                   subscription.getSubscriptionId(), correlationId);

        try {
            // Skip if subscription is not active
            if (!subscription.isActive()) {
                log.warn("Skipping billing for inactive subscription: {}", 
                           subscription.getSubscriptionId());
                return CompletableFuture.completedFuture(null);
            }

            // Check if already billed for current period
            if (isAlreadyBilled(subscription)) {
                log.info("Subscription already billed for current period: {}", 
                           subscription.getSubscriptionId());
                return CompletableFuture.completedFuture(null);
            }

            // Create invoice
            SubscriptionInvoice invoice = createBillingInvoice(subscription);
            
            // Attempt payment
            boolean paymentSuccessful = attemptPayment(invoice);
            
            if (paymentSuccessful) {
                // Advance billing cycle
                subscription.advanceBillingCycle();
                subscriptionRepository.save(subscription);
                
                // Send success notification
                notificationService.sendBillingSuccessNotification(subscription, invoice);
                
                // Record metrics
                metricsService.recordSuccessfulBilling(subscription.getPlan().getPlanCode(), invoice.getAmount());
                
                log.info("Billing successful for subscription: {}", subscription.getSubscriptionId());
            } else {
                // Handle failed payment
                handleFailedPayment(subscription, invoice);
                
                log.warn("Billing failed for subscription: {}", subscription.getSubscriptionId());
            }

        } catch (Exception e) {
            log.error("Error processing billing for subscription: {}", 
                        subscription.getSubscriptionId(), e);
            metricsService.recordBillingError(subscription.getPlan().getPlanCode());
            throw new PaymentProcessingException("Billing processing failed", "BILLING_ERROR", e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Retries a failed payment for an invoice.
     * 
     * @param invoice Invoice to retry payment for
     */
    public void retryFailedPayment(SubscriptionInvoice invoice) {
        String correlationId = CorrelationIdUtil.getOrGenerate();
        
        log.info("Retrying failed payment for invoice: {} - Attempt: {} - CorrelationId: {}", 
                   invoice.getInvoiceNumber(), invoice.getPaymentAttempts() + 1, correlationId);

        if (invoice.getPaymentAttempts() >= MAX_RETRY_ATTEMPTS) {
            log.warn("Maximum retry attempts reached for invoice: {}", invoice.getInvoiceNumber());
            handleMaxRetriesReached(invoice);
            return;
        }

        boolean paymentSuccessful = attemptPayment(invoice);
        
        if (paymentSuccessful) {
            log.info("Retry payment successful for invoice: {}", invoice.getInvoiceNumber());
            
            // Reactivate subscription if it was past due
            Subscription subscription = invoice.getSubscription();
            if (subscription.isPastDue()) {
                subscription.activate();
                subscriptionRepository.save(subscription);
                log.info("Subscription reactivated after successful payment: {}", 
                           subscription.getSubscriptionId());
            }
            
            // Send success notification
            notificationService.sendPaymentRetrySuccessNotification(subscription, invoice);
            
        } else {
            log.warn("Retry payment failed for invoice: {}", invoice.getInvoiceNumber());
            
            // Schedule next retry if within limits
            if (invoice.getPaymentAttempts() < MAX_RETRY_ATTEMPTS) {
                invoice.scheduleNextPaymentAttempt();
                invoiceRepository.save(invoice);
                
                // Send retry notification
                notificationService.sendPaymentRetryNotification(
                    invoice.getSubscription(), invoice, invoice.getPaymentAttempts());
            }
        }
        
        // Record metrics
        metricsService.recordPaymentRetry(
            invoice.getSubscription().getPlan().getPlanCode(), 
            invoice.getPaymentAttempts(), 
            paymentSuccessful);
    }

    /**
     * Creates a billing invoice for a subscription.
     * 
     * @param subscription Subscription to create invoice for
     * @return Created invoice
     */
    private SubscriptionInvoice createBillingInvoice(Subscription subscription) {
        String invoiceNumber = generateInvoiceNumber();
        
        SubscriptionInvoice invoice = new SubscriptionInvoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setSubscription(subscription);
        invoice.setCustomer(subscription.getCustomer());
        invoice.setAmount(subscription.getPlan().getAmount());
        invoice.setCurrency(subscription.getPlan().getCurrency());
        invoice.setStatus("PENDING");
        invoice.setPeriodStart(subscription.getCurrentPeriodStart());
        invoice.setPeriodEnd(subscription.getCurrentPeriodEnd());
        invoice.setDueDate(ZonedDateTime.now().plusDays(GRACE_PERIOD_DAYS));
        invoice.addMetadata("billingCycle", subscription.getPlan().getFormattedInterval());
        invoice.addMetadata("subscriptionId", subscription.getSubscriptionId());
        
        invoice = invoiceRepository.save(invoice);
        subscription.addInvoice(invoice);
        
        log.info("Created billing invoice: {} for subscription: {}", 
                   invoiceNumber, subscription.getSubscriptionId());
        
        return invoice;
    }

    /**
     * Attempts payment for an invoice.
     * 
     * @param invoice Invoice to process payment for
     * @return true if payment successful, false otherwise
     */
    private boolean attemptPayment(SubscriptionInvoice invoice) {
        try {
            invoice.markAsProcessing();
            invoiceRepository.save(invoice);
            
            Subscription subscription = invoice.getSubscription();
            PaymentMethod paymentMethod = subscription.getPaymentMethod();
            
            // Create payment request
            PurchaseRequest paymentRequest = createPaymentRequest(invoice, paymentMethod);
            
            // Process payment
            PaymentResponse paymentResponse = paymentService.processPurchase(paymentRequest);
            
            if (paymentResponse.getSuccess()) {
                // Payment successful
                invoice.markAsPaid(null); // TODO: Link to actual transaction
                invoiceRepository.save(invoice);
                
                log.info("Payment successful for invoice: {}", invoice.getInvoiceNumber());
                return true;
                
            } else {
                // Payment failed
                invoice.markAsFailed();
                invoiceRepository.save(invoice);
                
                log.warn("Payment failed for invoice: {} - Reason: {}", 
                           invoice.getInvoiceNumber(), paymentResponse.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error processing payment for invoice: {}", invoice.getInvoiceNumber(), e);
            
            invoice.markAsFailed();
            invoiceRepository.save(invoice);
            
            return false;
        }
    }

    private PurchaseRequest createPaymentRequest(SubscriptionInvoice invoice, PaymentMethod paymentMethod) {
        PurchaseRequest request = new PurchaseRequest();
        request.setAmount(invoice.getAmount());
        request.setCurrency(invoice.getCurrency());
        request.setDescription("Subscription billing for " + invoice.getSubscription().getPlan().getName());
        request.setCustomerId(invoice.getCustomer().getCustomerId());
        request.setInvoiceNumber(invoice.getInvoiceNumber());
        
        // Create payment method request
        PaymentMethodRequest pmRequest = new PaymentMethodRequest();
        pmRequest.setType(paymentMethod.getType());
        pmRequest.setCardNumber(paymentMethod.getCardNumber());
        pmRequest.setExpiryMonth(paymentMethod.getExpiryMonth());
        pmRequest.setExpiryYear(paymentMethod.getExpiryYear());
        pmRequest.setCvv(paymentMethod.getCvv());
        pmRequest.setCardholderName(paymentMethod.getCardholderName());
        
        request.setPaymentMethod(pmRequest);
        
        // Set idempotency key
        request.setIdempotencyKey("billing_" + invoice.getInvoiceNumber() + "_attempt_" + 
                                (invoice.getPaymentAttempts() + 1));
        
        return request;
    }

    private void handleFailedPayment(Subscription subscription, SubscriptionInvoice invoice) {
        // Schedule retry
        invoice.scheduleNextPaymentAttempt();
        invoiceRepository.save(invoice);
        
        // Update subscription status if this is first failure
        if (invoice.getPaymentAttempts() == 1) {
            subscription.markAsPastDue();
            subscriptionRepository.save(subscription);
        }
        
        // Send failed payment notification
        notificationService.sendPaymentFailedNotification(subscription, invoice);
        
        // Record metrics
        metricsService.recordFailedBilling(subscription.getPlan().getPlanCode(), invoice.getAmount());
    }

    private void handleMaxRetriesReached(SubscriptionInvoice invoice) {
        Subscription subscription = invoice.getSubscription();
        
        log.warn("Maximum retries reached for subscription: {}", subscription.getSubscriptionId());
        
        // Cancel subscription after max retries
        subscription.cancel("Payment failures - max retry attempts reached");
        subscriptionRepository.save(subscription);
        
        // Mark invoice as cancelled
        invoice.markAsCancelled();
        invoiceRepository.save(invoice);
        
        // Send final notice
        notificationService.sendSubscriptionCancelledNotification(subscription, 
            "Subscription cancelled due to repeated payment failures");
        
        // Record metrics
        metricsService.recordSubscriptionCancelledForNonPayment(subscription.getPlan().getPlanCode());
    }

    private boolean isAlreadyBilled(Subscription subscription) {
        // Check if there's already a pending or paid invoice for current period
        List<SubscriptionInvoice> periodInvoices = invoiceRepository
            .findBySubscriptionAndPeriodStartAndPeriodEnd(
                subscription, 
                subscription.getCurrentPeriodStart(), 
                subscription.getCurrentPeriodEnd());
        
        return periodInvoices.stream()
            .anyMatch(inv -> "PENDING".equals(inv.getStatus()) || 
                           "PAID".equals(inv.getStatus()) || 
                           "PROCESSING".equals(inv.getStatus()));
    }

    private void handleTrialExpirations() {
        List<Subscription> trialExpiringSubscriptions = subscriptionRepository
            .findSubscriptionsEndingTrial(ZonedDateTime.now());
        
        for (Subscription subscription : trialExpiringSubscriptions) {
            log.info("Processing trial expiration for subscription: {}", 
                       subscription.getSubscriptionId());
            
            // Start regular billing cycle
            subscription.calculateNextBillingCycle();
            subscriptionRepository.save(subscription);
            
            // Send trial expiration notification
            notificationService.sendTrialExpirationNotification(subscription);
            
            // Process immediate billing
            processSubscriptionBilling(subscription);
        }
    }

    private void handlePastDueSubscriptions() {
        List<Subscription> pastDueSubscriptions = subscriptionRepository
            .findByStatus(SubscriptionStatus.PAST_DUE);
        
        for (Subscription subscription : pastDueSubscriptions) {
            // Check if subscription should be cancelled for non-payment
            List<SubscriptionInvoice> unpaidInvoices = subscription.getUnpaidInvoices();
            
            boolean shouldCancel = unpaidInvoices.stream()
                .anyMatch(inv -> inv.getPaymentAttempts() >= MAX_RETRY_ATTEMPTS);
            
            if (shouldCancel) {
                subscription.cancel("Cancelled for non-payment");
                subscriptionRepository.save(subscription);
                
                notificationService.sendSubscriptionCancelledNotification(subscription, 
                    "Subscription cancelled due to non-payment");
                
                log.info("Cancelled past due subscription: {}", subscription.getSubscriptionId());
            }
        }
    }

    private void handleScheduledCancellations() {
        List<Subscription> scheduledCancellations = subscriptionRepository
            .findSubscriptionsWithScheduledCancellation(ZonedDateTime.now());
        
        for (Subscription subscription : scheduledCancellations) {
            String reason = (String) subscription.getMetadata().get("cancellationReason");
            subscription.cancel(reason != null ? reason : "Scheduled cancellation");
            
            subscriptionRepository.save(subscription);
            
            notificationService.sendSubscriptionCancelledNotification(subscription, reason);
            
            log.info("Processed scheduled cancellation for subscription: {}", 
                       subscription.getSubscriptionId());
        }
    }

    private void handleScheduledPlanChanges() {
        List<Subscription> scheduledPlanChanges = subscriptionRepository
            .findSubscriptionsWithScheduledPlanChange(ZonedDateTime.now());
        
        for (Subscription subscription : scheduledPlanChanges) {
            String newPlanCode = (String) subscription.getMetadata().get("scheduledPlanChange");
            
            // Implementation would require plan repository lookup and plan change logic
            log.info("Processing scheduled plan change for subscription: {} to plan: {}", 
                       subscription.getSubscriptionId(), newPlanCode);
            
            // Remove scheduled change metadata
            subscription.getMetadata().remove("scheduledPlanChange");
            subscription.getMetadata().remove("planChangeDate");
            
            subscriptionRepository.save(subscription);
        }
    }

    private String generateInvoiceNumber() {
        return "INV_" + System.currentTimeMillis() + "_" + 
               UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
