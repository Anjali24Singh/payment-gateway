package com.talentica.paymentgateway.constants;

/**
 * Security-related constants.
 * Centralizes security configuration, password policies, and encryption settings.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
public final class SecurityConstants {

    private SecurityConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }

    // Password Policy
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 128;
    public static final int PASSWORD_HISTORY_COUNT = 5;
    public static final int PASSWORD_EXPIRY_DAYS = 90;

    // Account Lockout
    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    public static final int ACCOUNT_LOCKOUT_DURATION_MINUTES = 30;

    // Session Management
    public static final int SESSION_TIMEOUT_MINUTES = 30;
    public static final int MAX_CONCURRENT_SESSIONS = 3;

    // Token Expiration
    public static final int ACCESS_TOKEN_EXPIRY_MINUTES = 15;
    public static final int REFRESH_TOKEN_EXPIRY_DAYS = 30;
    public static final int EMAIL_VERIFICATION_TOKEN_EXPIRY_HOURS = 24;
    public static final int PASSWORD_RESET_TOKEN_EXPIRY_HOURS = 2;

    // Encryption Algorithms
    public static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final int ENCRYPTION_KEY_LENGTH = 256;

    // CORS
    public static final String[] ALLOWED_ORIGINS = {"http://localhost:3000", "https://app.example.com"};
    public static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"};
    public static final String[] ALLOWED_HEADERS = {"*"};
    public static final int CORS_MAX_AGE_SECONDS = 3600;

    // Security Headers
    public static final String HEADER_X_FRAME_OPTIONS = "X-Frame-Options";
    public static final String HEADER_X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    public static final String HEADER_X_XSS_PROTECTION = "X-XSS-Protection";
    public static final String HEADER_STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    public static final String HEADER_CONTENT_SECURITY_POLICY = "Content-Security-Policy";

    // Role Names
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_API_CLIENT = "ROLE_API_CLIENT";
    public static final String ROLE_MERCHANT = "ROLE_MERCHANT";

    // Permission Names
    public static final String PERMISSION_READ = "READ";
    public static final String PERMISSION_WRITE = "WRITE";
    public static final String PERMISSION_DELETE = "DELETE";
    public static final String PERMISSION_ADMIN = "ADMIN";
}
