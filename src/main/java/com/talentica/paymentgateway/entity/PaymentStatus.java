package com.talentica.paymentgateway.entity;

/**
 * Enumeration representing the status of a payment transaction.
 * These statuses align with industry standards and Authorize.Net payment processing lifecycle.
 */
public enum PaymentStatus {
    /**
     * Payment has been initiated but not yet processed
     */
    PENDING,
    
    /**
     * Payment has been authorized but not yet captured
     */
    AUTHORIZED,
    
    /**
     * Payment has been captured (funds reserved)
     */
    CAPTURED,
    
    /**
     * Payment has been settled (funds transferred)
     */
    SETTLED,
    
    /**
     * Payment has been voided (cancelled before settlement)
     */
    VOIDED,
    
    /**
     * Payment has been fully refunded
     */
    REFUNDED,
    
    /**
     * Payment has been partially refunded
     */
    PARTIALLY_REFUNDED,
    
    /**
     * Payment processing failed
     */
    FAILED,
    
    /**
     * Payment was cancelled by user or system
     */
    CANCELLED,
    
    /**
     * Payment authorization has expired
     */
    EXPIRED,
    
    /**
     * Payment is under fraud review or manual review
     */
    PENDING_REVIEW
}
