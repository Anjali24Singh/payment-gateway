package com.talentica.paymentgateway.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe configuration properties for webhook processing.
 * Replaces @Value annotations with validated configuration beans.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "app.webhook")
public class WebhookProperties {

    /**
     * Webhook signature verification settings.
     */
    @NotNull
    private Signature signature = new Signature();

    /**
     * Webhook duplicate detection settings.
     */
    @NotNull
    private DuplicateDetection duplicateDetection = new DuplicateDetection();

    /**
     * Webhook processing settings.
     */
    @NotNull
    private Processing processing = new Processing();

    /**
     * Webhook retry settings.
     */
    @NotNull
    private Retry retry = new Retry();

    /**
     * Webhook cleanup settings.
     */
    @NotNull
    private Cleanup cleanup = new Cleanup();

    /**
     * Signature verification configuration.
     */
    @Data
    public static class Signature {
        /**
         * Signature secret key for HMAC verification.
         */
        private String secret;

        /**
         * Signature algorithm (HMAC_SHA256, HMAC_SHA512).
         */
        private String algorithm = "HMAC_SHA256";

        /**
         * Enable/disable signature verification.
         */
        private boolean enabled = true;
    }

    /**
     * Duplicate detection configuration.
     */
    @Data
    public static class DuplicateDetection {
        /**
         * Enable/disable duplicate webhook detection.
         */
        private boolean enabled = true;

        /**
         * Time window in minutes for duplicate detection.
         */
        @Min(1)
        private int windowMinutes = 60;
    }

    /**
     * Processing configuration.
     */
    @Data
    public static class Processing {
        /**
         * Webhook processing timeout in seconds.
         */
        @Min(1)
        private int timeoutSeconds = 30;
    }

    /**
     * Retry configuration.
     */
    @Data
    public static class Retry {
        /**
         * Maximum number of retry attempts.
         */
        @Min(0)
        private int maxAttempts = 5;

        /**
         * Initial delay before first retry (in minutes).
         */
        @Min(1)
        private int initialDelayMinutes = 1;

        /**
         * Maximum delay between retries (in minutes).
         */
        @Min(1)
        private int maxDelayMinutes = 1440; // 24 hours

        /**
         * Backoff multiplier for exponential retry.
         */
        @Min(1)
        private double multiplier = 2.0;

        /**
         * Enable jitter in retry delays.
         */
        private boolean jitterEnabled = true;

        /**
         * Retry request timeout in seconds.
         */
        @Min(1)
        private int timeoutSeconds = 30;
    }

    /**
     * Cleanup configuration.
     */
    @Data
    public static class Cleanup {
        /**
         * Enable/disable automatic cleanup of old webhooks.
         */
        private boolean enabled = true;

        /**
         * Retention period for delivered webhooks (in days).
         */
        @Min(1)
        private int deliveredRetentionDays = 7;

        /**
         * Retention period for failed webhooks (in days).
         */
        @Min(1)
        private int failedRetentionDays = 30;
    }
}
