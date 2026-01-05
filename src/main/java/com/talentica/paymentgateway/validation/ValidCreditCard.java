package com.talentica.paymentgateway.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for credit card numbers.
 * Validates credit card number format and checksum using Luhn algorithm.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Documented
@Constraint(validatedBy = CreditCardValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCreditCard {
    
    String message() default "Invalid credit card number";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to allow test credit card numbers.
     * Test numbers are useful for sandbox/development environments.
     */
    boolean allowTestNumbers() default false;
}
