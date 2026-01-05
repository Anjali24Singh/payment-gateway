package com.talentica.paymentgateway.config;

import net.authorize.Environment;
import net.authorize.api.contract.v1.MerchantAuthenticationType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration class for Authorize.Net integration.
 * Provides environment-specific configuration for sandbox and production environments.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "app.authorize-net")
@Validated
public class AuthorizeNetConfig {

    @NotBlank(message = "Authorize.Net API Login ID is required")
    private String apiLoginId;

    @NotBlank(message = "Authorize.Net Transaction Key is required")
    private String transactionKey;

    @NotNull(message = "Authorize.Net Environment is required")
    private AuthNetEnvironment environment = AuthNetEnvironment.SANDBOX;

    @NotBlank(message = "Authorize.Net Base URL is required")
    private String baseUrl;

    private Integer timeout = 30000; // 30 seconds default
    private Integer retryAttempts = 3;
    private Long retryDelay = 1000L; // 1 second default

    /**
     * Creates the Authorize.Net environment configuration.
     * 
     * @return Environment instance for Authorize.Net API calls
     */
    @Bean
    public Environment authorizeNetEnvironment() {
        return environment == AuthNetEnvironment.PRODUCTION 
            ? Environment.PRODUCTION 
            : Environment.SANDBOX;
    }

    /**
     * Creates the merchant authentication type for Authorize.Net API calls.
     * 
     * @return MerchantAuthenticationType with configured credentials
     */
    @Bean
    public MerchantAuthenticationType merchantAuthentication() {
        MerchantAuthenticationType merchant = new MerchantAuthenticationType();
        merchant.setName(apiLoginId);
        merchant.setTransactionKey(transactionKey);
        return merchant;
    }

    // Getters and Setters
    public String getApiLoginId() {
        return apiLoginId;
    }

    public void setApiLoginId(String apiLoginId) {
        this.apiLoginId = apiLoginId;
    }

    public String getTransactionKey() {
        return transactionKey;
    }

    public void setTransactionKey(String transactionKey) {
        this.transactionKey = transactionKey;
    }

    public AuthNetEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(AuthNetEnvironment environment) {
        this.environment = environment;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(Integer retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public Long getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Long retryDelay) {
        this.retryDelay = retryDelay;
    }

    /**
     * Enum for Authorize.Net environment types.
     */
    public enum AuthNetEnvironment {
        SANDBOX,
        PRODUCTION
    }
}
