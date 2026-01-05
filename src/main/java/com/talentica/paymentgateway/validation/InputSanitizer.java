package com.talentica.paymentgateway.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Input Sanitizer for cleaning and validating user input.
 * Provides methods to sanitize various types of input data.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Component
public class InputSanitizer {

    // Common regex patterns for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[1-9]\\d{1,14}$"
    );
    
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile(
            "^[A-Za-z0-9]+$"
    );
    
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile(
            "^[A-Za-z0-9\\s\\-_.@]+$"
    );
    
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "^\\d+(\\.\\d{1,2})?$"
    );
    
    // Characters to remove from input
    private static final String[] DANGEROUS_CHARS = {
            "<", ">", "\"", "'", "&", "script", "javascript:", "vbscript:",
            "onload", "onerror", "onclick", "onmouseover", "onfocus", "onblur"
    };

    /**
     * Sanitize general text input by removing dangerous characters.
     * 
     * @param input Input string
     * @return Sanitized string
     */
    public String sanitizeText(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        
        String sanitized = input.trim();
        
        // Remove dangerous characters
        for (String dangerousChar : DANGEROUS_CHARS) {
            sanitized = sanitized.replaceAll("(?i)" + Pattern.quote(dangerousChar), "");
        }
        
        // Remove excessive whitespace
        sanitized = sanitized.replaceAll("\\s+", " ");
        
        return sanitized;
    }

    /**
     * Sanitize and validate email address.
     * 
     * @param email Email address
     * @return Sanitized email or null if invalid
     */
    public String sanitizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        
        String sanitized = email.trim().toLowerCase();
        
        if (EMAIL_PATTERN.matcher(sanitized).matches()) {
            return sanitized;
        }
        
        return null;
    }

    /**
     * Sanitize and validate phone number.
     * 
     * @param phone Phone number
     * @return Sanitized phone number or null if invalid
     */
    public String sanitizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        
        // Remove all non-digit characters except +
        String sanitized = phone.replaceAll("[^+\\d]", "");
        
        if (PHONE_PATTERN.matcher(sanitized).matches()) {
            return sanitized;
        }
        
        return null;
    }

    /**
     * Sanitize alphanumeric input (IDs, codes, etc.).
     * 
     * @param input Input string
     * @return Sanitized alphanumeric string or null if invalid
     */
    public String sanitizeAlphanumeric(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        
        String sanitized = input.trim();
        
        if (ALPHANUMERIC_PATTERN.matcher(sanitized).matches()) {
            return sanitized;
        }
        
        return null;
    }

    /**
     * Sanitize safe string input (names, descriptions with limited special chars).
     * 
     * @param input Input string
     * @return Sanitized string or null if invalid
     */
    public String sanitizeSafeString(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        
        String sanitized = input.trim();
        
        if (SAFE_STRING_PATTERN.matcher(sanitized).matches()) {
            return sanitized;
        }
        
        return null;
    }

    /**
     * Sanitize and validate monetary amount.
     * 
     * @param amount Amount string
     * @return Sanitized amount or null if invalid
     */
    public String sanitizeAmount(String amount) {
        if (!StringUtils.hasText(amount)) {
            return null;
        }
        
        String sanitized = amount.trim();
        
        if (AMOUNT_PATTERN.matcher(sanitized).matches()) {
            return sanitized;
        }
        
        return null;
    }

    /**
     * Sanitize credit card number (remove spaces, dashes).
     * 
     * @param cardNumber Credit card number
     * @return Sanitized card number or null if invalid
     */
    public String sanitizeCardNumber(String cardNumber) {
        if (!StringUtils.hasText(cardNumber)) {
            return null;
        }
        
        // Remove spaces and dashes
        String sanitized = cardNumber.replaceAll("[\\s\\-]", "");
        
        // Check if it's all digits and has valid length
        if (sanitized.matches("^\\d{13,19}$")) {
            return sanitized;
        }
        
        return null;
    }

    /**
     * Sanitize CVV code.
     * 
     * @param cvv CVV code
     * @return Sanitized CVV or null if invalid
     */
    public String sanitizeCvv(String cvv) {
        if (!StringUtils.hasText(cvv)) {
            return null;
        }
        
        String sanitized = cvv.trim();
        
        // CVV should be 3 or 4 digits
        if (sanitized.matches("^\\d{3,4}$")) {
            return sanitized;
        }
        
        return null;
    }

    /**
     * Sanitize expiry date (MM/YY or MM/YYYY format).
     * 
     * @param expiryDate Expiry date
     * @return Sanitized expiry date or null if invalid
     */
    public String sanitizeExpiryDate(String expiryDate) {
        if (!StringUtils.hasText(expiryDate)) {
            return null;
        }
        
        String sanitized = expiryDate.trim();
        
        // Check MM/YY or MM/YYYY format
        if (sanitized.matches("^(0[1-9]|1[0-2])/(\\d{2}|\\d{4})$")) {
            return sanitized;
        }
        
        return null;
    }

    /**
     * Sanitize URL input.
     * 
     * @param url URL string
     * @return Sanitized URL or null if invalid
     */
    public String sanitizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        
        String sanitized = url.trim();
        
        // Basic URL validation
        if (sanitized.matches("^https?://[\\w\\-.]+(:[\\d]+)?(/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]*)?$")) {
            return sanitized;
        }
        
        return null;
    }

    /**
     * Sanitize IP address.
     * 
     * @param ipAddress IP address
     * @return Sanitized IP address or null if invalid
     */
    public String sanitizeIpAddress(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return null;
        }
        
        String sanitized = ipAddress.trim();
        
        // IPv4 pattern
        if (sanitized.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")) {
            return sanitized;
        }
        
        // IPv6 pattern (simplified)
        if (sanitized.matches("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$")) {
            return sanitized;
        }
        
        return null;
    }

    /**
     * Check if string contains only safe characters.
     * 
     * @param input Input string
     * @return true if safe
     */
    public boolean isSafeInput(String input) {
        if (!StringUtils.hasText(input)) {
            return true;
        }
        
        // Check for dangerous patterns
        String lowerInput = input.toLowerCase();
        for (String dangerous : DANGEROUS_CHARS) {
            if (lowerInput.contains(dangerous.toLowerCase())) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Sanitize and truncate string to maximum length.
     * 
     * @param input Input string
     * @param maxLength Maximum allowed length
     * @return Sanitized and truncated string
     */
    public String sanitizeAndTruncate(String input, int maxLength) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        
        String sanitized = sanitizeText(input);
        
        if (sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength);
        }
        
        return sanitized;
    }

    /**
     * Remove all HTML tags from input.
     * 
     * @param input Input string with potential HTML
     * @return String with HTML tags removed
     */
    public String removeHtmlTags(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
