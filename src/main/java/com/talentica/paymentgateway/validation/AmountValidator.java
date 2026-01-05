package com.talentica.paymentgateway.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Amount Validator for monetary amounts.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
public class AmountValidator implements ConstraintValidator<ValidAmount, BigDecimal> {

    private double min;
    private double max;
    private int decimalPlaces;

    @Override
    public void initialize(ValidAmount constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
        this.decimalPlaces = constraintAnnotation.decimalPlaces();
    }

    @Override
    public boolean isValid(BigDecimal amount, ConstraintValidatorContext context) {
        if (amount == null) {
            return false;
        }

        // Check if amount is within range
        double amountValue = amount.doubleValue();
        if (amountValue < min || amountValue > max) {
            return false;
        }

        // Check decimal places
        BigDecimal scaledAmount = amount.setScale(decimalPlaces, RoundingMode.HALF_UP);
        return amount.compareTo(scaledAmount) == 0;
    }
}
