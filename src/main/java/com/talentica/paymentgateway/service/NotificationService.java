package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.entity.Subscription;
import com.talentica.paymentgateway.entity.SubscriptionInvoice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for sending notifications related to subscription billing.
 * In a production environment, this would integrate with email services,
 * SMS providers, and other notification channels.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class NotificationService {

    /**
     * Sends billing success notification.
     */
    public void sendBillingSuccessNotification(Subscription subscription, SubscriptionInvoice invoice) {
        log.info("Sending billing success notification for subscription: {} - Invoice: {}", 
                   subscription.getSubscriptionId(), invoice.getInvoiceNumber());
        
        // In production, this would send actual email/SMS notifications
        // For now, we just log the notification
    }

    /**
     * Sends payment failed notification.
     */
    public void sendPaymentFailedNotification(Subscription subscription, SubscriptionInvoice invoice) {
        log.info("Sending payment failed notification for subscription: {} - Invoice: {}", 
                   subscription.getSubscriptionId(), invoice.getInvoiceNumber());
    }

    /**
     * Sends payment retry notification.
     */
    public void sendPaymentRetryNotification(Subscription subscription, SubscriptionInvoice invoice, int attemptNumber) {
        log.info("Sending payment retry notification for subscription: {} - Attempt: {}", 
                   subscription.getSubscriptionId(), attemptNumber);
    }

    /**
     * Sends payment retry success notification.
     */
    public void sendPaymentRetrySuccessNotification(Subscription subscription, SubscriptionInvoice invoice) {
        log.info("Sending payment retry success notification for subscription: {}", 
                   subscription.getSubscriptionId());
    }

    /**
     * Sends subscription cancelled notification.
     */
    public void sendSubscriptionCancelledNotification(Subscription subscription, String reason) {
        log.info("Sending subscription cancelled notification for subscription: {} - Reason: {}", 
                   subscription.getSubscriptionId(), reason);
    }

    /**
     * Sends trial expiration notification.
     */
    public void sendTrialExpirationNotification(Subscription subscription) {
        log.info("Sending trial expiration notification for subscription: {}", 
                   subscription.getSubscriptionId());
    }
}
