package com.talentica.paymentgateway.constants;

/**
 * Payment and transaction-related constants.
 * Centralizes payment processor codes, statuses, and transaction types.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
public final class PaymentConstants {

    private PaymentConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }

    // Transaction Types
    public static final String TRANSACTION_TYPE_PURCHASE = "PURCHASE";
    public static final String TRANSACTION_TYPE_AUTHORIZE = "AUTHORIZE";
    public static final String TRANSACTION_TYPE_CAPTURE = "CAPTURE";
    public static final String TRANSACTION_TYPE_REFUND = "REFUND";
    public static final String TRANSACTION_TYPE_VOID = "VOID";

    // Payment Statuses
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_AUTHORIZED = "AUTHORIZED";
    public static final String STATUS_CAPTURED = "CAPTURED";
    public static final String STATUS_REFUNDED = "REFUNDED";
    public static final String STATUS_VOIDED = "VOIDED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_DECLINED = "DECLINED";

    // Payment Method Types
    public static final String PAYMENT_METHOD_CREDIT_CARD = "CREDIT_CARD";
    public static final String PAYMENT_METHOD_BANK_ACCOUNT = "BANK_ACCOUNT";
    public static final String PAYMENT_METHOD_TOKEN = "TOKEN";

    // Card Brands
    public static final String CARD_BRAND_VISA = "VISA";
    public static final String CARD_BRAND_MASTERCARD = "MASTERCARD";
    public static final String CARD_BRAND_AMEX = "AMEX";
    public static final String CARD_BRAND_DISCOVER = "DISCOVER";
    public static final String CARD_BRAND_UNKNOWN = "UNKNOWN";

    // Currency Codes
    public static final String CURRENCY_USD = "USD";
    public static final String CURRENCY_EUR = "EUR";
    public static final String CURRENCY_GBP = "GBP";

    // Error Codes
    public static final String ERROR_CARD_DECLINED = "CARD_DECLINED";
    public static final String ERROR_INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
    public static final String ERROR_INVALID_CARD = "INVALID_CARD";
    public static final String ERROR_INVALID_EXPIRY = "INVALID_EXPIRY";
    public static final String ERROR_INVALID_CVV = "INVALID_CVV";
    public static final String ERROR_INVALID_AMOUNT = "INVALID_AMOUNT";
    public static final String ERROR_PROCESSING_ERROR = "PROCESSING_ERROR";
    public static final String ERROR_AVS_MISMATCH = "AVS_MISMATCH";
    public static final String ERROR_DUPLICATE_TRANSACTION = "DUPLICATE_TRANSACTION";

    // Transaction ID Prefixes
    public static final String TRANSACTION_ID_PREFIX = "txn_";
    public static final String AUTH_CODE_PREFIX = "auth_";

    // Timeout and Retry Settings
    public static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 30;
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long RETRY_DELAY_MILLISECONDS = 1000;
}
