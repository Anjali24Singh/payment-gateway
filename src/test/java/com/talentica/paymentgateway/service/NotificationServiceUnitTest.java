package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.entity.Subscription;
import com.talentica.paymentgateway.entity.SubscriptionInvoice;
import com.talentica.paymentgateway.entity.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NotificationService - focusing on line coverage.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

    @InjectMocks
    private NotificationService notificationService;

    private Subscription testSubscription;
    private SubscriptionInvoice testInvoice;

    @BeforeEach
    void setUp() {
        testSubscription = new Subscription();
        testSubscription.setSubscriptionId("SUB_001");
        testSubscription.setStatus(SubscriptionStatus.ACTIVE);

        testInvoice = new SubscriptionInvoice();
        testInvoice.setInvoiceNumber("INV_001");
        testInvoice.setSubscription(testSubscription);
        testInvoice.setAmount(new BigDecimal("99.99"));
    }

    @Test
    void sendBillingSuccessNotification_ShouldExecuteWithoutException() {
        assertDoesNotThrow(() -> 
            notificationService.sendBillingSuccessNotification(testSubscription, testInvoice));
    }

    @Test
    void sendBillingSuccessNotification_WithNullSubscription_ShouldThrowException() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            notificationService.sendBillingSuccessNotification(null, testInvoice));
    }

    @Test
    void sendBillingSuccessNotification_WithNullInvoice_ShouldThrowException() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            notificationService.sendBillingSuccessNotification(testSubscription, null));
    }

    @Test
    void sendPaymentFailedNotification_ShouldExecuteWithoutException() {
        assertDoesNotThrow(() -> 
            notificationService.sendPaymentFailedNotification(testSubscription, testInvoice));
    }

    @Test
    void sendPaymentFailedNotification_WithNullParameters_ShouldThrowException() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            notificationService.sendPaymentFailedNotification(null, null));
    }

    @Test
    void sendPaymentRetryNotification_ShouldExecuteWithoutException() {
        assertDoesNotThrow(() -> 
            notificationService.sendPaymentRetryNotification(testSubscription, testInvoice, 2));
    }

    @Test
    void sendPaymentRetryNotification_WithFirstAttempt_ShouldExecute() {
        assertDoesNotThrow(() -> 
            notificationService.sendPaymentRetryNotification(testSubscription, testInvoice, 1));
    }

    @Test
    void sendPaymentRetryNotification_WithMaxAttempts_ShouldExecute() {
        assertDoesNotThrow(() -> 
            notificationService.sendPaymentRetryNotification(testSubscription, testInvoice, 5));
    }

    @Test
    void sendPaymentRetryNotification_WithNullSubscription_ShouldThrowException() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            notificationService.sendPaymentRetryNotification(null, testInvoice, 1));
    }

    @Test
    void sendPaymentRetrySuccessNotification_ShouldExecuteWithoutException() {
        assertDoesNotThrow(() -> 
            notificationService.sendPaymentRetrySuccessNotification(testSubscription, testInvoice));
    }

    @Test
    void sendPaymentRetrySuccessNotification_WithNullSubscription_ShouldThrowException() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            notificationService.sendPaymentRetrySuccessNotification(null, testInvoice));
    }

    @Test
    void sendPaymentRetrySuccessNotification_WithNullInvoice_ShouldExecute() {
        assertDoesNotThrow(() -> 
            notificationService.sendPaymentRetrySuccessNotification(testSubscription, null));
    }

    @Test
    void sendSubscriptionCancelledNotification_ShouldExecuteWithoutException() {
        assertDoesNotThrow(() -> 
            notificationService.sendSubscriptionCancelledNotification(testSubscription, "Customer requested"));
    }

    @Test
    void sendSubscriptionCancelledNotification_WithNullReason_ShouldExecute() {
        assertDoesNotThrow(() -> 
            notificationService.sendSubscriptionCancelledNotification(testSubscription, null));
    }

    @Test
    void sendSubscriptionCancelledNotification_WithEmptyReason_ShouldExecute() {
        assertDoesNotThrow(() -> 
            notificationService.sendSubscriptionCancelledNotification(testSubscription, ""));
    }

    @Test
    void sendSubscriptionCancelledNotification_WithNullSubscription_ShouldThrowException() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            notificationService.sendSubscriptionCancelledNotification(null, "test reason"));
    }

    @Test
    void sendTrialExpirationNotification_ShouldExecuteWithoutException() {
        assertDoesNotThrow(() -> 
            notificationService.sendTrialExpirationNotification(testSubscription));
    }

    @Test
    void sendTrialExpirationNotification_WithNullSubscription_ShouldThrowException() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            notificationService.sendTrialExpirationNotification(null));
    }

    @Test
    void sendTrialExpirationNotification_WithDifferentSubscriptionId_ShouldExecute() {
        Subscription differentSubscription = new Subscription();
        differentSubscription.setSubscriptionId("SUB_999");
        
        assertDoesNotThrow(() -> 
            notificationService.sendTrialExpirationNotification(differentSubscription));
    }

    @Test
    void allNotificationMethods_ShouldExecuteWithoutException() {
        assertDoesNotThrow(() -> {
            notificationService.sendBillingSuccessNotification(testSubscription, testInvoice);
            notificationService.sendPaymentFailedNotification(testSubscription, testInvoice);
            notificationService.sendPaymentRetryNotification(testSubscription, testInvoice, 1);
            notificationService.sendPaymentRetrySuccessNotification(testSubscription, testInvoice);
            notificationService.sendSubscriptionCancelledNotification(testSubscription, "test");
            notificationService.sendTrialExpirationNotification(testSubscription);
        });
    }

    @Test
    void multipleNotificationCalls_ShouldExecuteWithoutException() {
        assertDoesNotThrow(() -> {
            notificationService.sendBillingSuccessNotification(testSubscription, testInvoice);
            notificationService.sendBillingSuccessNotification(testSubscription, testInvoice);
            notificationService.sendBillingSuccessNotification(testSubscription, testInvoice);
        });
    }
}
