package com.talentica.paymentgateway.constants;

/**
 * HTTP and API-related constants.
 * Centralizes header names, content types, and API versioning.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
public final class ApiConstants {

    private ApiConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }

    // API Version
    public static final String API_VERSION = "v1";
    public static final String API_BASE_PATH = "/api/" + API_VERSION;

    // HTTP Headers
    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
    public static final String HEADER_REQUEST_ID = "X-Request-ID";
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";

    // Webhook Headers
    public static final String WEBHOOK_SIGNATURE_HEADER = "X-ANET-Signature";
    public static final String WEBHOOK_SIGNATURE_HEADER_ALT = "X-Signature";
    public static final String WEBHOOK_EVENT_TYPE_HEADER = "X-Event-Type";

    // Content Types
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_XML = "application/xml";
    public static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";

    // Authentication
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String BASIC_PREFIX = "Basic ";
    public static final int JWT_EXPIRATION_HOURS = 24;
    public static final int REFRESH_TOKEN_EXPIRATION_DAYS = 30;

    // Rate Limiting
    public static final int RATE_LIMIT_REQUESTS_PER_MINUTE = 100;
    public static final int RATE_LIMIT_REQUESTS_PER_HOUR = 1000;
    public static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String PAGE_PARAM = "page";
    public static final String SIZE_PARAM = "size";
    public static final String SORT_PARAM = "sort";

    // Request Tracking
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_CLIENT_ID = "clientId";
}
