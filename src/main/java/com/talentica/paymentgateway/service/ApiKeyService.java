package com.talentica.paymentgateway.service;

import com.talentica.paymentgateway.constants.CacheConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * API Key Management Service for external service authentication.
 * Handles API key validation, caching, and client management.
 * 
 * @author Payment Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class ApiKeyService {
    
    // In-memory store for API keys (in production, this should be database-backed)
    private final Map<String, ApiKeyInfo> apiKeyStore = new HashMap<>();

    public ApiKeyService() {
        initializeDefaultApiKeys();
    }

    /**
     * Validate if API key is active and not expired.
     * Cached in 'apiKeyValidation' cache with 1 hour TTL.
     * 
     * @param apiKey API key to validate
     * @return true if valid
     */
    @Cacheable(value = CacheConstants.API_KEY_VALIDATION_CACHE, key = "#apiKey", unless = "#result == false")
    public boolean isValidApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return false;
        }

        try {
            // Validate from store
            ApiKeyInfo keyInfo = apiKeyStore.get(apiKey);
            boolean isValid = keyInfo != null && 
                             keyInfo.isActive() && 
                             (keyInfo.getExpiresAt() == null || keyInfo.getExpiresAt().after(new Date()));
            
            if (isValid) {
                log.debug("Valid API key for client: {}", keyInfo.getClientId());
            } else {
                log.warn("Invalid API key provided");
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error validating API key: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get client ID for API key.
     * 
     * @param apiKey API key
     * @return Client ID
     */
    public String getClientId(String apiKey) {
        ApiKeyInfo keyInfo = apiKeyStore.get(apiKey);
        return keyInfo != null ? keyInfo.getClientId() : null;
    }

    /**
     * Get permissions for API key.
     * Cached in 'clientPermissions' cache with 1 hour TTL.
     * 
     * @param apiKey API key
     * @return List of permissions
     */
    @Cacheable(value = CacheConstants.CLIENT_PERMISSIONS_CACHE, key = "#apiKey")
    public List<String> getPermissions(String apiKey) {
        try {
            ApiKeyInfo keyInfo = apiKeyStore.get(apiKey);
            if (keyInfo == null) {
                return Collections.emptyList();
            }
            
            return keyInfo.getPermissions();
            
        } catch (Exception e) {
            log.error("Error getting permissions for API key: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Create new API key for client.
     * 
     * @param clientId Client identifier
     * @param permissions List of permissions
     * @param expiresAt Expiration date (null for no expiration)
     * @return Generated API key
     */
    public String createApiKey(String clientId, List<String> permissions, Date expiresAt) {
        String apiKey = generateApiKey();
        
        ApiKeyInfo keyInfo = new ApiKeyInfo(
                apiKey,
                clientId,
                permissions,
                true,
                new Date(),
                expiresAt
        );
        
        apiKeyStore.put(apiKey, keyInfo);
        
        // Invalidate cache
        invalidateCache(apiKey, clientId);
        
        log.info("Created new API key for client: {}", clientId);
        return apiKey;
    }

    /**
     * Revoke API key.
     * 
     * @param apiKey API key to revoke
     * @return true if successfully revoked
     */
    public boolean revokeApiKey(String apiKey) {
        ApiKeyInfo keyInfo = apiKeyStore.get(apiKey);
        if (keyInfo != null) {
            keyInfo.setActive(false);
            invalidateCache(apiKey, keyInfo.getClientId());
            log.info("Revoked API key for client: {}", keyInfo.getClientId());
            return true;
        }
        return false;
    }

    /**
     * Update permissions for API key.
     * 
     * @param apiKey API key
     * @param permissions New permissions
     * @return true if successfully updated
     */
    public boolean updatePermissions(String apiKey, List<String> permissions) {
        ApiKeyInfo keyInfo = apiKeyStore.get(apiKey);
        if (keyInfo != null) {
            keyInfo.setPermissions(permissions);
            invalidateCache(apiKey, keyInfo.getClientId());
            log.info("Updated permissions for client: {}", keyInfo.getClientId());
            return true;
        }
        return false;
    }

    /**
     * Get API key information.
     * 
     * @param apiKey API key
     * @return API key info or null if not found
     */
    public ApiKeyInfo getApiKeyInfo(String apiKey) {
        return apiKeyStore.get(apiKey);
    }

    /**
     * List all API keys for client.
     * 
     * @param clientId Client identifier
     * @return List of API keys
     */
    public List<ApiKeyInfo> getApiKeysForClient(String clientId) {
        return apiKeyStore.values().stream()
                .filter(keyInfo -> clientId.equals(keyInfo.getClientId()))
                .toList();
    }

    /**
     * Initialize default API keys for testing and initial setup.
     */
    private void initializeDefaultApiKeys() {
        // Default API key for Authorize.Net webhook
        String authorizeNetKey = "authnet_webhook_key_12345";
        apiKeyStore.put(authorizeNetKey, new ApiKeyInfo(
                authorizeNetKey,
                "authorize_net",
                Arrays.asList("webhook_receive", "payment_notification"),
                true,
                new Date(),
                null
        ));

        // Default API key for internal services
        String internalKey = "internal_service_key_67890";
        apiKeyStore.put(internalKey, new ApiKeyInfo(
                internalKey,
                "internal_service",
                Arrays.asList("payment_process", "transaction_query", "user_management"),
                true,
                new Date(),
                null
        ));

        // Default API key for external partners
        String partnerKey = "partner_api_key_abcdef";
        apiKeyStore.put(partnerKey, new ApiKeyInfo(
                partnerKey,
                "external_partner",
                Arrays.asList("payment_process", "transaction_query"),
                true,
                new Date(),
                null
        ));

        log.info("Initialized {} default API keys", apiKeyStore.size());
    }

    /**
     * Generate random API key.
     * 
     * @return Generated API key
     */
    private String generateApiKey() {
        return "pgw_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Invalidate cache for API key and client.
     * 
     * @param apiKey API key
     * @param clientId Client ID
     */
    @Caching(evict = {
        @CacheEvict(value = CacheConstants.API_KEY_VALIDATION_CACHE, key = "#apiKey"),
        @CacheEvict(value = CacheConstants.CLIENT_PERMISSIONS_CACHE, key = "#apiKey")
    })
    private void invalidateCache(String apiKey, String clientId) {
        log.debug("Cache invalidated for API key and client: {}", clientId);
    }

    /**
     * API Key Information class.
     */
    public static class ApiKeyInfo {
        private final String apiKey;
        private final String clientId;
        private List<String> permissions;
        private boolean active;
        private final Date createdAt;
        private final Date expiresAt;

        public ApiKeyInfo(String apiKey, String clientId, List<String> permissions, 
                         boolean active, Date createdAt, Date expiresAt) {
            this.apiKey = apiKey;
            this.clientId = clientId;
            this.permissions = new ArrayList<>(permissions);
            this.active = active;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }

        // Getters and setters
        public String getApiKey() { return apiKey; }
        public String getClientId() { return clientId; }
        public List<String> getPermissions() { return new ArrayList<>(permissions); }
        public void setPermissions(List<String> permissions) { this.permissions = new ArrayList<>(permissions); }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public Date getCreatedAt() { return createdAt; }
        public Date getExpiresAt() { return expiresAt; }
    }
}
