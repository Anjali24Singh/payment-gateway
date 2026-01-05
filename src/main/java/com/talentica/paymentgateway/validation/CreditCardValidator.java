package com.talentica.paymentgateway.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Credit Card Validator implementing Luhn algorithm for card number validation.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
public class CreditCardValidator implements ConstraintValidator<ValidCreditCard, String> {

    private boolean allowTestNumbers;
    
    // Test credit card numbers for development/sandbox
    private static final List<String> TEST_CARD_NUMBERS = Arrays.asList(
            "4111111111111111", // Visa
            "4012888888881881", // Visa
            "5555555555554444", // MasterCard
            "5105105105105100", // MasterCard
            "378282246310005",  // American Express
            "371449635398431",  // American Express
            "6011111111111117", // Discover
            "6011000990139424"  // Discover
    );

    @Override
    public void initialize(ValidCreditCard constraintAnnotation) {
        this.allowTestNumbers = constraintAnnotation.allowTestNumbers();
    }

    @Override
    public boolean isValid(String cardNumber, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(cardNumber)) {
            return false;
        }

        // Remove spaces and dashes
        String cleanCardNumber = cardNumber.replaceAll("[\\s\\-]", "");

        // Check if it's a test card number
        if (allowTestNumbers && TEST_CARD_NUMBERS.contains(cleanCardNumber)) {
            return true;
        }

        // Basic format validation
        if (!cleanCardNumber.matches("^\\d{13,19}$")) {
            return false;
        }

        // Validate using Luhn algorithm
        return isValidLuhn(cleanCardNumber);
    }

    /**
     * Validate credit card number using Luhn algorithm.
     * 
     * @param cardNumber Clean card number (digits only)
     * @return true if valid according to Luhn algorithm
     */
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        // Process digits from right to left
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return (sum % 10) == 0;
    }
}
