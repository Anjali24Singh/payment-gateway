package com.talentica.paymentgateway.entity;

/**
 * Enumeration representing the status of webhook delivery.
 * These statuses track the webhook delivery lifecycle.
 */
public enum WebhookStatus {
    /**
     * Webhook is pending delivery
     */
    PENDING,
    
    /**
     * Webhook is currently being processed
     */
    PROCESSING,
    
    /**
     * Webhook has been successfully delivered
     */
    DELIVERED,
    
    /**
     * Webhook delivery failed
     */
    FAILED,
    
    /**
     * Webhook is being retried after a failure
     */
    RETRYING
}
