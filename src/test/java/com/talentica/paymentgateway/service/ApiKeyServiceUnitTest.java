package com.talentica.paymentgateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ApiKeyService.
 * Tests API key validation, permissions, and management operations.
 * Note: Service now uses in-memory storage instead of Redis.
 */
class ApiKeyServiceUnitTest {

    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyService = new ApiKeyService();
    }

    @Test
    void constructor_WithoutRedis_ShouldInitializeSuccessfully() {
        // When
        ApiKeyService serviceWithoutRedis = new ApiKeyService();

        // Then
        assertThat(serviceWithoutRedis).isNotNull();
        // Should have default API keys initialized
        assertThat(serviceWithoutRedis.isValidApiKey("authnet_webhook_key_12345")).isTrue();
        assertThat(serviceWithoutRedis.isValidApiKey("internal_service_key_67890")).isTrue();
        assertThat(serviceWithoutRedis.isValidApiKey("partner_api_key_abcdef")).isTrue();
    }

    @Test
    void isValidApiKey_WithNullApiKey_ShouldReturnFalse() {
        // When
        boolean result = apiKeyService.isValidApiKey(null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isValidApiKey_WithEmptyApiKey_ShouldReturnFalse() {
        // When
        boolean result = apiKeyService.isValidApiKey("");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isValidApiKey_WithValidDefaultKey_ShouldReturnTrue() {
        // Given
        String validApiKey = "authnet_webhook_key_12345";

        // When
        boolean result = apiKeyService.isValidApiKey(validApiKey);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isValidApiKey_WithInvalidKey_ShouldReturnFalse() {
        // Given
        String invalidApiKey = "invalid_key";

        // When
        boolean result = apiKeyService.isValidApiKey(invalidApiKey);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isValidApiKey_WithMultipleCallsSameKey_ShouldReturnConsistentResult() {
        // Given
        String validApiKey = "authnet_webhook_key_12345";

        // When & Then - Multiple calls should return same result (testing cache consistency)
        assertThat(apiKeyService.isValidApiKey(validApiKey)).isTrue();
        assertThat(apiKeyService.isValidApiKey(validApiKey)).isTrue();
        assertThat(apiKeyService.isValidApiKey(validApiKey)).isTrue();
    }

    @Test
    void getClientId_WithValidApiKey_ShouldReturnClientId() {
        // Given
        String validApiKey = "authnet_webhook_key_12345";

        // When
        String clientId = apiKeyService.getClientId(validApiKey);

        // Then
        assertThat(clientId).isEqualTo("authorize_net");
    }

    @Test
    void getClientId_WithInvalidApiKey_ShouldReturnNull() {
        // Given
        String invalidApiKey = "invalid_key";

        // When
        String clientId = apiKeyService.getClientId(invalidApiKey);

        // Then
        assertThat(clientId).isNull();
    }

    @Test
    void getPermissions_WithValidApiKey_ShouldReturnPermissions() {
        // Given
        String validApiKey = "internal_service_key_67890";

        // When
        List<String> permissions = apiKeyService.getPermissions(validApiKey);

        // Then
        assertThat(permissions).containsExactlyInAnyOrder(
            "payment_process", "transaction_query", "user_management");
    }

    @Test
    void getPermissions_WithInvalidApiKey_ShouldReturnEmptyList() {
        // Given
        String invalidApiKey = "invalid_key";

        // When
        List<String> permissions = apiKeyService.getPermissions(invalidApiKey);

        // Then
        assertThat(permissions).isEmpty();
    }

    @Test
    void getPermissions_ShouldReturnImmutableCopy() {
        // Given
        String validApiKey = "partner_api_key_abcdef";

        // When
        List<String> permissions = apiKeyService.getPermissions(validApiKey);
        List<String> originalPermissions = new ArrayList<>(permissions);
        
        // Try to modify the returned list
        permissions.add("new_permission");

        // Then - Should not affect the stored permissions
        List<String> permissionsAgain = apiKeyService.getPermissions(validApiKey);
        assertThat(permissionsAgain).isEqualTo(originalPermissions);
    }

    @Test
    void createApiKey_ShouldGenerateNewKeyAndStoreInfo() {
        // Given
        String clientId = "test_client";
        List<String> permissions = Arrays.asList("test_permission");
        Date expiresAt = new Date(System.currentTimeMillis() + 86400000); // 1 day from now

        // When
        String apiKey = apiKeyService.createApiKey(clientId, permissions, expiresAt);

        // Then
        assertThat(apiKey).isNotNull();
        assertThat(apiKey).startsWith("pgw_");
        assertThat(apiKeyService.isValidApiKey(apiKey)).isTrue();
        assertThat(apiKeyService.getClientId(apiKey)).isEqualTo(clientId);
        assertThat(apiKeyService.getPermissions(apiKey)).isEqualTo(permissions);
    }

    @Test
    void createApiKey_WithNullExpiration_ShouldCreateNonExpiringKey() {
        // Given
        String clientId = "test_client";
        List<String> permissions = Arrays.asList("test_permission");

        // When
        String apiKey = apiKeyService.createApiKey(clientId, permissions, null);

        // Then
        assertThat(apiKey).isNotNull();
        assertThat(apiKeyService.isValidApiKey(apiKey)).isTrue();
        
        ApiKeyService.ApiKeyInfo keyInfo = apiKeyService.getApiKeyInfo(apiKey);
        assertThat(keyInfo.getExpiresAt()).isNull();
    }

    @Test
    void revokeApiKey_WithValidKey_ShouldDeactivateKey() {
        // Given
        String validApiKey = "authnet_webhook_key_12345";
        assertThat(apiKeyService.isValidApiKey(validApiKey)).isTrue();

        // When
        boolean result = apiKeyService.revokeApiKey(validApiKey);

        // Then
        assertThat(result).isTrue();
        assertThat(apiKeyService.isValidApiKey(validApiKey)).isFalse();
    }

    @Test
    void revokeApiKey_WithInvalidKey_ShouldReturnFalse() {
        // Given
        String invalidApiKey = "invalid_key";

        // When
        boolean result = apiKeyService.revokeApiKey(invalidApiKey);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void updatePermissions_WithValidKey_ShouldUpdatePermissions() {
        // Given
        String validApiKey = "partner_api_key_abcdef";
        List<String> newPermissions = Arrays.asList("new_permission", "another_permission");

        // When
        boolean result = apiKeyService.updatePermissions(validApiKey, newPermissions);

        // Then
        assertThat(result).isTrue();
        assertThat(apiKeyService.getPermissions(validApiKey)).isEqualTo(newPermissions);
    }

    @Test
    void updatePermissions_WithInvalidKey_ShouldReturnFalse() {
        // Given
        String invalidApiKey = "invalid_key";
        List<String> newPermissions = Arrays.asList("new_permission");

        // When
        boolean result = apiKeyService.updatePermissions(invalidApiKey, newPermissions);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getApiKeyInfo_WithValidKey_ShouldReturnKeyInfo() {
        // Given
        String validApiKey = "internal_service_key_67890";

        // When
        ApiKeyService.ApiKeyInfo keyInfo = apiKeyService.getApiKeyInfo(validApiKey);

        // Then
        assertThat(keyInfo).isNotNull();
        assertThat(keyInfo.getApiKey()).isEqualTo(validApiKey);
        assertThat(keyInfo.getClientId()).isEqualTo("internal_service");
        assertThat(keyInfo.isActive()).isTrue();
        assertThat(keyInfo.getCreatedAt()).isNotNull();
        assertThat(keyInfo.getExpiresAt()).isNull();
    }

    @Test
    void getApiKeyInfo_WithInvalidKey_ShouldReturnNull() {
        // Given
        String invalidApiKey = "invalid_key";

        // When
        ApiKeyService.ApiKeyInfo keyInfo = apiKeyService.getApiKeyInfo(invalidApiKey);

        // Then
        assertThat(keyInfo).isNull();
    }

    @Test
    void getApiKeysForClient_ShouldReturnClientKeys() {
        // Given
        String clientId = "authorize_net";

        // When
        List<ApiKeyService.ApiKeyInfo> keys = apiKeyService.getApiKeysForClient(clientId);

        // Then
        assertThat(keys).hasSize(1);
        assertThat(keys.get(0).getClientId()).isEqualTo(clientId);
        assertThat(keys.get(0).getApiKey()).isEqualTo("authnet_webhook_key_12345");
    }

    @Test
    void getApiKeysForClient_WithNonExistentClient_ShouldReturnEmptyList() {
        // Given
        String clientId = "non_existent_client";

        // When
        List<ApiKeyService.ApiKeyInfo> keys = apiKeyService.getApiKeysForClient(clientId);

        // Then
        assertThat(keys).isEmpty();
    }

    @Test
    void getApiKeysForClient_WithMultipleKeys_ShouldReturnAllClientKeys() {
        // Given
        String clientId = "test_client";
        apiKeyService.createApiKey(clientId, Arrays.asList("perm1"), null);
        apiKeyService.createApiKey(clientId, Arrays.asList("perm2"), null);

        // When
        List<ApiKeyService.ApiKeyInfo> keys = apiKeyService.getApiKeysForClient(clientId);

        // Then
        assertThat(keys).hasSize(2);
        assertThat(keys).allMatch(key -> key.getClientId().equals(clientId));
    }

    @Test
    void isValidApiKey_WithExpiredKey_ShouldReturnFalse() {
        // Given
        String clientId = "test_client";
        List<String> permissions = Arrays.asList("test_permission");
        Date expiredDate = new Date(System.currentTimeMillis() - 86400000); // 1 day ago
        String expiredApiKey = apiKeyService.createApiKey(clientId, permissions, expiredDate);

        // When
        boolean result = apiKeyService.isValidApiKey(expiredApiKey);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isValidApiKey_WithFutureExpirationKey_ShouldReturnTrue() {
        // Given
        String clientId = "test_client";
        List<String> permissions = Arrays.asList("test_permission");
        Date futureDate = new Date(System.currentTimeMillis() + 86400000); // 1 day from now
        String futureApiKey = apiKeyService.createApiKey(clientId, permissions, futureDate);

        // When
        boolean result = apiKeyService.isValidApiKey(futureApiKey);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void apiKeyInfo_GettersAndSetters_ShouldWorkCorrectly() {
        // Given
        String apiKey = "test_key";
        String clientId = "test_client";
        List<String> permissions = Arrays.asList("perm1", "perm2");
        Date createdAt = new Date();
        Date expiresAt = new Date(System.currentTimeMillis() + 86400000);

        // When
        ApiKeyService.ApiKeyInfo keyInfo = new ApiKeyService.ApiKeyInfo(
            apiKey, clientId, permissions, true, createdAt, expiresAt);

        // Then
        assertThat(keyInfo.getApiKey()).isEqualTo(apiKey);
        assertThat(keyInfo.getClientId()).isEqualTo(clientId);
        assertThat(keyInfo.getPermissions()).isEqualTo(permissions);
        assertThat(keyInfo.isActive()).isTrue();
        assertThat(keyInfo.getCreatedAt()).isEqualTo(createdAt);
        assertThat(keyInfo.getExpiresAt()).isEqualTo(expiresAt);

        // Test setters
        List<String> newPermissions = Arrays.asList("new_perm");
        keyInfo.setPermissions(newPermissions);
        keyInfo.setActive(false);

        assertThat(keyInfo.getPermissions()).isEqualTo(newPermissions);
        assertThat(keyInfo.isActive()).isFalse();
    }

    @Test
    void apiKeyInfo_PermissionsImmutability_ShouldReturnCopies() {
        // Given
        List<String> originalPermissions = new ArrayList<>(Arrays.asList("perm1", "perm2"));
        ApiKeyService.ApiKeyInfo keyInfo = new ApiKeyService.ApiKeyInfo(
            "key", "client", originalPermissions, true, new Date(), null);

        // When
        List<String> retrievedPermissions = keyInfo.getPermissions();
        retrievedPermissions.add("modified_perm");

        // Then - Original permissions should not be modified
        assertThat(keyInfo.getPermissions()).hasSize(2);
        assertThat(keyInfo.getPermissions()).doesNotContain("modified_perm");
    }

    @Test
    void serviceWithoutRedis_ShouldWorkCorrectly() {
        // Given
        ApiKeyService serviceWithoutRedis = new ApiKeyService();
        String clientId = "test_client";
        List<String> permissions = Arrays.asList("test_permission");

        // When
        String apiKey = serviceWithoutRedis.createApiKey(clientId, permissions, null);

        // Then
        assertThat(serviceWithoutRedis.isValidApiKey(apiKey)).isTrue();
        assertThat(serviceWithoutRedis.getClientId(apiKey)).isEqualTo(clientId);
        assertThat(serviceWithoutRedis.getPermissions(apiKey)).isEqualTo(permissions);
    }

    @Test
    void defaultApiKeys_ShouldHaveCorrectConfiguration() {
        // Test Authorize.Net webhook key
        String authNetKey = "authnet_webhook_key_12345";
        assertThat(apiKeyService.isValidApiKey(authNetKey)).isTrue();
        assertThat(apiKeyService.getClientId(authNetKey)).isEqualTo("authorize_net");
        assertThat(apiKeyService.getPermissions(authNetKey))
            .containsExactlyInAnyOrder("webhook_receive", "payment_notification");

        // Test internal service key
        String internalKey = "internal_service_key_67890";
        assertThat(apiKeyService.isValidApiKey(internalKey)).isTrue();
        assertThat(apiKeyService.getClientId(internalKey)).isEqualTo("internal_service");
        assertThat(apiKeyService.getPermissions(internalKey))
            .containsExactlyInAnyOrder("payment_process", "transaction_query", "user_management");

        // Test partner key
        String partnerKey = "partner_api_key_abcdef";
        assertThat(apiKeyService.isValidApiKey(partnerKey)).isTrue();
        assertThat(apiKeyService.getClientId(partnerKey)).isEqualTo("external_partner");
        assertThat(apiKeyService.getPermissions(partnerKey))
            .containsExactlyInAnyOrder("payment_process", "transaction_query");
    }
}
