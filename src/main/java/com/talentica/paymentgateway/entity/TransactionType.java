package com.talentica.paymentgateway.entity;

/**
 * Enumeration representing different types of payment transactions.
 * These types correspond to standard payment processing operations.
 */
public enum TransactionType {
    /**
     * Direct purchase transaction (authorize and capture in one step)
     */
    PURCHASE,
    
    /**
     * Authorization only (reserve funds without capturing)
     */
    AUTHORIZE,
    
    /**
     * Capture previously authorized transaction
     */
    CAPTURE,
    
    /**
     * Void a transaction (cancel before settlement)
     */
    VOID,
    
    /**
     * Full refund of a settled transaction
     */
    REFUND,
    
    /**
     * Partial refund of a settled transaction
     */
    PARTIAL_REFUND
}
