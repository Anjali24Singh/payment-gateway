package com.talentica.paymentgateway.entity;

/**
 * Enumeration representing the status of a subscription.
 * These statuses cover the complete subscription lifecycle.
 */
public enum SubscriptionStatus {
    /**
     * Subscription is active and billing normally
     */
    ACTIVE,
    
    /**
     * Subscription is temporarily paused
     */
    PAUSED,
    
    /**
     * Subscription has been cancelled
     */
    CANCELLED,
    
    /**
     * Subscription has expired
     */
    EXPIRED,
    
    /**
     * Subscription payment is overdue
     */
    PAST_DUE,
    
    /**
     * Subscription is pending activation
     */
    PENDING
}
