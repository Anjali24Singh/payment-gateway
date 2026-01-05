package com.talentica.paymentgateway.constants;

/**
 * Cache-related constants for Spring Cache annotations.
 * Centralizes cache names and TTL configurations.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
public final class CacheConstants {

    private CacheConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }

    // Cache Names
    public static final String API_KEY_VALIDATION_CACHE = "apiKeyValidation";
    public static final String CLIENT_PERMISSIONS_CACHE = "clientPermissions";
    public static final String CUSTOMER_PROFILES_CACHE = "customerProfiles";
    public static final String SUBSCRIPTION_PLANS_CACHE = "subscriptionPlans";
    public static final String PAYMENT_METHODS_CACHE = "paymentMethods";
    public static final String USER_DETAILS_CACHE = "userDetails";
    public static final String ANALYTICS_CACHE = "analytics";

    // Cache TTL (in seconds)
    public static final long API_KEY_CACHE_TTL = 3600; // 1 hour
    public static final long PERMISSIONS_CACHE_TTL = 3600; // 1 hour
    public static final long CUSTOMER_CACHE_TTL = 1800; // 30 minutes
    public static final long SUBSCRIPTION_PLAN_CACHE_TTL = 21600; // 6 hours
    public static final long PAYMENT_METHOD_CACHE_TTL = 900; // 15 minutes
    public static final long USER_CACHE_TTL = 1800; // 30 minutes
    public static final long ANALYTICS_CACHE_TTL = 300; // 5 minutes
}
