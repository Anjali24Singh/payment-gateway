package com.talentica.paymentgateway.config;

import net.authorize.Environment;
import net.authorize.api.contract.v1.MerchantAuthenticationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthorizeNetConfig.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AuthorizeNetConfigUnitTest {

    private AuthorizeNetConfig config;

    @BeforeEach
    void setUp() {
        config = new AuthorizeNetConfig();
    }

    @Test
    void constructor_ShouldCreateConfigWithDefaults() {
        // When & Then
        assertNotNull(config);
        assertEquals(AuthorizeNetConfig.AuthNetEnvironment.SANDBOX, config.getEnvironment());
        assertEquals(Integer.valueOf(30000), config.getTimeout());
        assertEquals(Integer.valueOf(3), config.getRetryAttempts());
        assertEquals(Long.valueOf(1000L), config.getRetryDelay());
    }

    @Test
    void setApiLoginId_ShouldSetValue() {
        // Given
        String apiLoginId = "test-api-login-id";

        // When
        config.setApiLoginId(apiLoginId);

        // Then
        assertEquals(apiLoginId, config.getApiLoginId());
    }

    @Test
    void setTransactionKey_ShouldSetValue() {
        // Given
        String transactionKey = "test-transaction-key";

        // When
        config.setTransactionKey(transactionKey);

        // Then
        assertEquals(transactionKey, config.getTransactionKey());
    }

    @Test
    void setEnvironment_WithSandbox_ShouldSetValue() {
        // Given
        AuthorizeNetConfig.AuthNetEnvironment environment = AuthorizeNetConfig.AuthNetEnvironment.SANDBOX;

        // When
        config.setEnvironment(environment);

        // Then
        assertEquals(environment, config.getEnvironment());
    }

    @Test
    void setEnvironment_WithProduction_ShouldSetValue() {
        // Given
        AuthorizeNetConfig.AuthNetEnvironment environment = AuthorizeNetConfig.AuthNetEnvironment.PRODUCTION;

        // When
        config.setEnvironment(environment);

        // Then
        assertEquals(environment, config.getEnvironment());
    }

    @Test
    void setBaseUrl_ShouldSetValue() {
        // Given
        String baseUrl = "https://test.authorize.net";

        // When
        config.setBaseUrl(baseUrl);

        // Then
        assertEquals(baseUrl, config.getBaseUrl());
    }

    @Test
    void setTimeout_ShouldSetValue() {
        // Given
        Integer timeout = 45000;

        // When
        config.setTimeout(timeout);

        // Then
        assertEquals(timeout, config.getTimeout());
    }

    @Test
    void setRetryAttempts_ShouldSetValue() {
        // Given
        Integer retryAttempts = 5;

        // When
        config.setRetryAttempts(retryAttempts);

        // Then
        assertEquals(retryAttempts, config.getRetryAttempts());
    }

    @Test
    void setRetryDelay_ShouldSetValue() {
        // Given
        Long retryDelay = 2000L;

        // When
        config.setRetryDelay(retryDelay);

        // Then
        assertEquals(retryDelay, config.getRetryDelay());
    }

    @Test
    void authorizeNetEnvironment_WithSandboxEnvironment_ShouldReturnSandbox() {
        // Given
        config.setEnvironment(AuthorizeNetConfig.AuthNetEnvironment.SANDBOX);

        // When
        Environment environment = config.authorizeNetEnvironment();

        // Then
        assertEquals(Environment.SANDBOX, environment);
    }

    @Test
    void authorizeNetEnvironment_WithProductionEnvironment_ShouldReturnProduction() {
        // Given
        config.setEnvironment(AuthorizeNetConfig.AuthNetEnvironment.PRODUCTION);

        // When
        Environment environment = config.authorizeNetEnvironment();

        // Then
        assertEquals(Environment.PRODUCTION, environment);
    }

    @Test
    void merchantAuthentication_WithValidCredentials_ShouldReturnMerchantAuth() {
        // Given
        String apiLoginId = "test-login-id";
        String transactionKey = "test-transaction-key";
        config.setApiLoginId(apiLoginId);
        config.setTransactionKey(transactionKey);

        // When
        MerchantAuthenticationType merchantAuth = config.merchantAuthentication();

        // Then
        assertNotNull(merchantAuth);
        assertEquals(apiLoginId, merchantAuth.getName());
        assertEquals(transactionKey, merchantAuth.getTransactionKey());
    }

    @Test
    void merchantAuthentication_WithNullCredentials_ShouldReturnMerchantAuthWithNullValues() {
        // Given
        config.setApiLoginId(null);
        config.setTransactionKey(null);

        // When
        MerchantAuthenticationType merchantAuth = config.merchantAuthentication();

        // Then
        assertNotNull(merchantAuth);
        assertNull(merchantAuth.getName());
        assertNull(merchantAuth.getTransactionKey());
    }

    @Test
    void authNetEnvironment_EnumValues_ShouldHaveCorrectValues() {
        // When & Then
        assertEquals("SANDBOX", AuthorizeNetConfig.AuthNetEnvironment.SANDBOX.name());
        assertEquals("PRODUCTION", AuthorizeNetConfig.AuthNetEnvironment.PRODUCTION.name());
        assertEquals(2, AuthorizeNetConfig.AuthNetEnvironment.values().length);
    }

    @Test
    void authNetEnvironment_ValueOf_ShouldReturnCorrectEnum() {
        // When & Then
        assertEquals(AuthorizeNetConfig.AuthNetEnvironment.SANDBOX, 
                    AuthorizeNetConfig.AuthNetEnvironment.valueOf("SANDBOX"));
        assertEquals(AuthorizeNetConfig.AuthNetEnvironment.PRODUCTION, 
                    AuthorizeNetConfig.AuthNetEnvironment.valueOf("PRODUCTION"));
    }

    @Test
    void config_WithAllProperties_ShouldSetAllValues() {
        // Given
        String apiLoginId = "test-api-login";
        String transactionKey = "test-transaction-key";
        AuthorizeNetConfig.AuthNetEnvironment environment = AuthorizeNetConfig.AuthNetEnvironment.PRODUCTION;
        String baseUrl = "https://api.authorize.net";
        Integer timeout = 60000;
        Integer retryAttempts = 5;
        Long retryDelay = 2000L;

        // When
        config.setApiLoginId(apiLoginId);
        config.setTransactionKey(transactionKey);
        config.setEnvironment(environment);
        config.setBaseUrl(baseUrl);
        config.setTimeout(timeout);
        config.setRetryAttempts(retryAttempts);
        config.setRetryDelay(retryDelay);

        // Then
        assertEquals(apiLoginId, config.getApiLoginId());
        assertEquals(transactionKey, config.getTransactionKey());
        assertEquals(environment, config.getEnvironment());
        assertEquals(baseUrl, config.getBaseUrl());
        assertEquals(timeout, config.getTimeout());
        assertEquals(retryAttempts, config.getRetryAttempts());
        assertEquals(retryDelay, config.getRetryDelay());
    }

    @Test
    void authorizeNetEnvironment_WithNullEnvironment_ShouldHandleGracefully() {
        // Given
        config.setEnvironment(null);

        // When
        Environment environment = config.authorizeNetEnvironment();

        // Then
        // Should default to SANDBOX when environment is null
        assertEquals(Environment.SANDBOX, environment);
    }

    @Test
    void merchantAuthentication_WithEmptyStrings_ShouldSetEmptyValues() {
        // Given
        config.setApiLoginId("");
        config.setTransactionKey("");

        // When
        MerchantAuthenticationType merchantAuth = config.merchantAuthentication();

        // Then
        assertNotNull(merchantAuth);
        assertEquals("", merchantAuth.getName());
        assertEquals("", merchantAuth.getTransactionKey());
    }

    @Test
    void config_DefaultValues_ShouldBeCorrect() {
        // When & Then
        assertEquals(AuthorizeNetConfig.AuthNetEnvironment.SANDBOX, config.getEnvironment());
        assertEquals(Integer.valueOf(30000), config.getTimeout());
        assertEquals(Integer.valueOf(3), config.getRetryAttempts());
        assertEquals(Long.valueOf(1000L), config.getRetryDelay());
        assertNull(config.getApiLoginId());
        assertNull(config.getTransactionKey());
        assertNull(config.getBaseUrl());
    }

    @Test
    void setTimeout_WithZero_ShouldSetValue() {
        // Given
        Integer timeout = 0;

        // When
        config.setTimeout(timeout);

        // Then
        assertEquals(timeout, config.getTimeout());
    }

    @Test
    void setRetryAttempts_WithZero_ShouldSetValue() {
        // Given
        Integer retryAttempts = 0;

        // When
        config.setRetryAttempts(retryAttempts);

        // Then
        assertEquals(retryAttempts, config.getRetryAttempts());
    }

    @Test
    void setRetryDelay_WithZero_ShouldSetValue() {
        // Given
        Long retryDelay = 0L;

        // When
        config.setRetryDelay(retryDelay);

        // Then
        assertEquals(retryDelay, config.getRetryDelay());
    }

    @Test
    void merchantAuthentication_MultipleCallsShouldReturnNewInstances() {
        // Given
        config.setApiLoginId("test-login");
        config.setTransactionKey("test-key");

        // When
        MerchantAuthenticationType merchantAuth1 = config.merchantAuthentication();
        MerchantAuthenticationType merchantAuth2 = config.merchantAuthentication();

        // Then
        assertNotNull(merchantAuth1);
        assertNotNull(merchantAuth2);
        assertNotSame(merchantAuth1, merchantAuth2); // Different instances
        assertEquals(merchantAuth1.getName(), merchantAuth2.getName());
        assertEquals(merchantAuth1.getTransactionKey(), merchantAuth2.getTransactionKey());
    }

    @Test
    void authorizeNetEnvironment_MultipleCallsShouldReturnSameInstance() {
        // Given
        config.setEnvironment(AuthorizeNetConfig.AuthNetEnvironment.PRODUCTION);

        // When
        Environment environment1 = config.authorizeNetEnvironment();
        Environment environment2 = config.authorizeNetEnvironment();

        // Then
        assertEquals(Environment.PRODUCTION, environment1);
        assertEquals(Environment.PRODUCTION, environment2);
        assertSame(environment1, environment2); // Same instance (enum singleton)
    }
}
