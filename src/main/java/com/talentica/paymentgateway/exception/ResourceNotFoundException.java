package com.talentica.paymentgateway.exception;

/**
 * Exception thrown when a requested resource is not found.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
