package com.talentica.paymentgateway.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for monetary amounts.
 * Validates that the amount is positive and has valid decimal places.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Documented
@Constraint(validatedBy = AmountValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAmount {
    
    String message() default "Invalid amount";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Minimum allowed amount.
     */
    double min() default 0.01;
    
    /**
     * Maximum allowed amount.
     */
    double max() default 999999.99;
    
    /**
     * Maximum number of decimal places allowed.
     */
    int decimalPlaces() default 2;
}
