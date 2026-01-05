package com.talentica.paymentgateway.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe configuration properties for Authorize.Net integration.
 * Replaces @Value annotations with validated configuration beans.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "app.authorize-net")
public class AuthorizeNetProperties {

    /**
     * Authorize.Net API Login ID for authentication.
     */
    @NotBlank(message = "Authorize.Net API Login ID is required")
    private String apiLoginId;

    /**
     * Authorize.Net Transaction Key for authentication.
     */
    @NotBlank(message = "Authorize.Net Transaction Key is required")
    private String transactionKey;

    /**
     * Environment mode: SANDBOX or PRODUCTION.
     */
    @NotBlank(message = "Authorize.Net environment is required")
    private String environment = "SANDBOX";

    /**
     * API endpoint URL (auto-configured based on environment).
     */
    private String apiEndpoint;

    /**
     * Request timeout in seconds.
     */
    private int requestTimeoutSeconds = 30;

    /**
     * Enable request/response logging for debugging.
     */
    private boolean enableLogging = false;

    /**
     * Check if running in sandbox mode.
     */
    public boolean isSandbox() {
        return "SANDBOX".equalsIgnoreCase(environment);
    }

    /**
     * Check if running in production mode.
     */
    public boolean isProduction() {
        return "PRODUCTION".equalsIgnoreCase(environment);
    }
}
