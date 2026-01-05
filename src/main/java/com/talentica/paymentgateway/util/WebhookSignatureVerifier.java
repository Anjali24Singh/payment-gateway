package com.talentica.paymentgateway.util;

import com.talentica.paymentgateway.config.properties.WebhookProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Utility class for verifying webhook signatures.
 * Supports multiple signature algorithms including HMAC-SHA256 and SHA-256.
 * 
 * Signature verification is crucial for webhook security to ensure that:
 * 1. The webhook request actually came from Authorize.Net
 * 2. The payload hasn't been tampered with during transmission
 * 3. The request is not a replay attack
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookSignatureVerifier {
    
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String HMAC_SHA512_ALGORITHM = "HmacSHA512";
    private static final String SHA256_ALGORITHM = "SHA-256";
    
    // Standard webhook signature headers
    private static final String SIGNATURE_HEADER = "X-ANET-Signature";
    private static final String SIGNATURE_HEADER_ALT = "X-Signature";
    private static final String SHA1_SIGNATURE_HEADER = "sha1";
    private static final String SHA256_SIGNATURE_HEADER = "sha256";
    
    private final WebhookProperties webhookProperties;
    
    /**
     * Verifies webhook signature from HTTP headers.
     * 
     * @param headers HTTP headers from the webhook request
     * @param payload Raw webhook payload
     * @return true if signature is valid or verification is disabled, false otherwise
     */
    public boolean verifySignature(Map<String, String> headers, String payload) {
        if (!webhookProperties.getSignature().isEnabled()) {
            log.debug("Webhook signature verification is disabled");
            return true;
        }
        
        String webhookSecret = webhookProperties.getSignature().getSecret();
        if (!StringUtils.hasText(webhookSecret)) {
            log.warn("Webhook signature secret not configured, allowing request");
            return true;
        }
        
        if (payload == null) {
            log.error("Webhook payload is null, cannot verify signature");
            return false;
        }
        
        try {
            // Try different signature header formats
            String signature = extractSignature(headers);
            if (!StringUtils.hasText(signature)) {
                log.error("No signature found in webhook headers");
                return false;
            }
            
            // Verify signature using configured algorithm
            boolean isValid = verifySignatureWithAlgorithm(signature, payload);
            
            if (isValid) {
                log.debug("Webhook signature verification successful");
            } else {
                log.error("Webhook signature verification failed");
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error verifying webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Verifies webhook signature with a specific secret.
     * Useful for testing or when using different secrets for different endpoints.
     * 
     * @param headers HTTP headers from the webhook request
     * @param payload Raw webhook payload
     * @param secret Webhook secret to use for verification
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignatureWithSecret(Map<String, String> headers, String payload, String secret) {
        if (!StringUtils.hasText(secret) || payload == null) {
            return false;
        }
        
        try {
            String signature = extractSignature(headers);
            if (!StringUtils.hasText(signature)) {
                return false;
            }
            
            return verifySignatureWithAlgorithm(signature, payload, secret);
            
        } catch (Exception e) {
            log.error("Error verifying webhook signature with custom secret: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Generates signature for outgoing webhook requests.
     * 
     * @param payload Webhook payload to sign
     * @return Generated signature
     */
    public String generateSignature(String payload) {
        String webhookSecret = webhookProperties.getSignature().getSecret();
        if (!StringUtils.hasText(webhookSecret) || payload == null) {
            log.warn("Cannot generate signature: secret or payload is empty");
            return null;
        }
        
        try {
            return generateSignatureWithSecret(payload, webhookSecret);
        } catch (Exception e) {
            log.error("Error generating webhook signature: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Generates signature with a specific secret.
     * 
     * @param payload Webhook payload to sign
     * @param secret Secret to use for signing
     * @return Generated signature
     */
    public String generateSignatureWithSecret(String payload, String secret) {
        if (!StringUtils.hasText(secret) || payload == null) {
            return null;
        }
        
        String algorithm = webhookProperties.getSignature().getAlgorithm();
        try {
            switch (algorithm.toUpperCase()) {
                case "HMAC_SHA256":
                    return generateHmacSha256Signature(payload, secret);
                case "SHA256":
                    return generateSha256Signature(payload + secret);
                default:
                    log.warn("Unknown signature algorithm: {}, using HMAC_SHA256", algorithm);
                    return generateHmacSha256Signature(payload, secret);
            }
        } catch (Exception e) {
            log.error("Error generating signature with secret: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Extracts signature from webhook headers.
     * Supports multiple header formats used by different webhook providers.
     * 
     * @param headers HTTP headers
     * @return Extracted signature or null if not found
     */
    private String extractSignature(Map<String, String> headers) {
        // Convert headers to case-insensitive lookup
        Map<String, String> lowerHeaders = headers.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    entry -> entry.getKey().toLowerCase(),
                    Map.Entry::getValue,
                    (existing, replacement) -> existing
                ));
        
        // Try standard Authorize.Net signature header
        String signature = lowerHeaders.get(SIGNATURE_HEADER.toLowerCase());
        if (StringUtils.hasText(signature)) {
            return cleanSignature(signature);
        }
        
        // Try alternative signature header
        signature = lowerHeaders.get(SIGNATURE_HEADER_ALT.toLowerCase());
        if (StringUtils.hasText(signature)) {
            return cleanSignature(signature);
        }
        
        // Try GitHub-style signature header
        signature = lowerHeaders.get("x-hub-signature-256");
        if (StringUtils.hasText(signature)) {
            return cleanSignature(signature);
        }
        
        // Try Stripe-style signature header
        signature = lowerHeaders.get("stripe-signature");
        if (StringUtils.hasText(signature)) {
            return extractStripeSignature(signature);
        }
        
        return null;
    }
    
    /**
     * Cleans signature by removing algorithm prefixes.
     */
    private String cleanSignature(String signature) {
        if (!StringUtils.hasText(signature)) {
            return signature;
        }
        
        // Remove common prefixes
        if (signature.startsWith("sha512=")) {
            return signature.substring(7);
        }
        if (signature.startsWith("sha256=")) {
            return signature.substring(7);
        }
        if (signature.startsWith("sha1=")) {
            return signature.substring(5);
        }
        
        return signature;
    }
    
    /**
     * Extracts signature from Stripe-style signature header.
     * Format: t=timestamp,v1=signature
     */
    private String extractStripeSignature(String signatureHeader) {
        if (!StringUtils.hasText(signatureHeader)) {
            return null;
        }
        
        String[] parts = signatureHeader.split(",");
        for (String part : parts) {
            if (part.startsWith("v1=")) {
                return part.substring(3);
            }
        }
        
        return null;
    }
    
    /**
     * Verifies signature using the configured algorithm.
     */
    private boolean verifySignatureWithAlgorithm(String signature, String payload) {
        return verifySignatureWithAlgorithm(signature, payload, webhookProperties.getSignature().getSecret());
    }
    
    /**
     * Verifies signature using the configured algorithm with a specific secret.
     */
    private boolean verifySignatureWithAlgorithm(String signature, String payload, String secret) {
        String algorithm = webhookProperties.getSignature().getAlgorithm();
        try {
            log.debug("Using signature algorithm: {}", algorithm);
            String expectedSignature;
            
            switch (algorithm.toUpperCase()) {
                case "HMAC_SHA256":
                    expectedSignature = generateHmacSha256Signature(payload, secret);
                    break;
                case "HMAC_SHA512":
                    expectedSignature = generateHmacSha512Signature(payload, secret);
                    break;
                case "SHA256":
                    expectedSignature = generateSha256Signature(payload + secret);
                    break;
                default:
                    log.warn("Unknown signature algorithm: {}, using HMAC_SHA256", algorithm);
                    expectedSignature = generateHmacSha256Signature(payload, secret);
                    break;
            }
            
            if (expectedSignature == null) {
                log.error("Failed to generate expected signature");
                return false;
            }
            
            log.debug("Expected signature: {}", expectedSignature);
            log.debug("Received signature: {}", signature);
            
            // Use secure comparison to prevent timing attacks
            boolean isValid = secureCompare(signature.toLowerCase(), expectedSignature.toLowerCase());
            log.debug("Signature validation result: {}", isValid);
            return isValid;
            
        } catch (Exception e) {
            log.error("Error verifying signature with algorithm {}: {}", algorithm, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Generates HMAC-SHA256 signature.
     */
    private String generateHmacSha256Signature(String payload, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
        mac.init(secretKeySpec);
        
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }
    
    /**
     * Generates HMAC-SHA512 signature.
     */
    private String generateHmacSha512Signature(String payload, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA512_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA512_ALGORITHM);
        mac.init(secretKeySpec);
        
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }
    
    /**
     * Generates SHA-256 signature.
     */
    private String generateSha256Signature(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }
    
    /**
     * Converts byte array to hexadecimal string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Secure string comparison to prevent timing attacks.
     */
    private boolean secureCompare(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
    
    /**
     * Validates webhook timestamp to prevent replay attacks.
     * 
     * @param timestamp Webhook timestamp
     * @param toleranceMinutes Allowed time difference in minutes
     * @return true if timestamp is within tolerance, false otherwise
     */
    public boolean validateTimestamp(long timestamp, int toleranceMinutes) {
        long currentTime = System.currentTimeMillis() / 1000; // Convert to seconds
        long timeDifference = Math.abs(currentTime - timestamp);
        long toleranceSeconds = toleranceMinutes * 60L;
        
        boolean isValid = timeDifference <= toleranceSeconds;
        
        if (!isValid) {
            log.warn("Webhook timestamp validation failed. Current: {}, Webhook: {}, Difference: {} seconds", 
                       currentTime, timestamp, timeDifference);
        }
        
        return isValid;
    }
    
    /**
     * Gets configuration info for debugging.
     */
    public String getConfigurationInfo() {
        return String.format("WebhookSignatureVerifier{enabled=%s, algorithm=%s, secretConfigured=%s}", 
                           webhookProperties.getSignature().isEnabled(), 
                           webhookProperties.getSignature().getAlgorithm(), 
                           StringUtils.hasText(webhookProperties.getSignature().getSecret()));
    }
}
