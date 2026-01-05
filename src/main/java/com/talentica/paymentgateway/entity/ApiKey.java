package com.talentica.paymentgateway.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Entity representing API keys for authentication and authorization.
 * Each API key belongs to a user and has specific permissions and rate limits.
 */
@Entity
@Table(name = "api_keys",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "keyHash")
       },
       indexes = {
           @Index(name = "idx_api_keys_user_id", columnList = "user_id"),
           @Index(name = "idx_api_keys_key_hash", columnList = "keyHash"),
           @Index(name = "idx_api_keys_active", columnList = "isActive")
       })
public class ApiKey extends BaseEntity {

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Key name is required")
    @Size(max = 100, message = "Key name must not exceed 100 characters")
    @Column(name = "key_name", nullable = false, length = 100)
    private String keyName;

    @NotBlank(message = "Key hash is required")
    @Size(max = 255, message = "Key hash must not exceed 255 characters")
    @Column(name = "key_hash", nullable = false, unique = true, length = 255)
    private String keyHash;

    @NotBlank(message = "Key prefix is required")
    @Size(max = 20, message = "Key prefix must not exceed 20 characters")
    @Column(name = "key_prefix", nullable = false, length = 20)
    private String keyPrefix;

    @Column(name = "permissions", columnDefinition = "TEXT[]")
    private String[] permissions = new String[0];

    @Positive(message = "Rate limit must be positive")
    @Column(name = "rate_limit_per_hour", nullable = false)
    private Integer rateLimitPerHour = 1000;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "expires_at")
    private ZonedDateTime expiresAt;

    @Column(name = "last_used_at")
    private ZonedDateTime lastUsedAt;

    // Relationships
    @OneToMany(mappedBy = "apiKey", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AuditLog> auditLogs = new ArrayList<>();

    // Constructors
    public ApiKey() {
        super();
    }

    public ApiKey(User user, String keyName, String keyHash, String keyPrefix) {
        this();
        this.user = user;
        this.keyName = keyName;
        this.keyHash = keyHash;
        this.keyPrefix = keyPrefix;
    }

    // Getters and Setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public void setPermissions(String[] permissions) {
        this.permissions = permissions;
    }

    public Integer getRateLimitPerHour() {
        return rateLimitPerHour;
    }

    public void setRateLimitPerHour(Integer rateLimitPerHour) {
        this.rateLimitPerHour = rateLimitPerHour;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public ZonedDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(ZonedDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public ZonedDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(ZonedDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogs;
    }

    public void setAuditLogs(List<AuditLog> auditLogs) {
        this.auditLogs = auditLogs;
    }

    // Utility methods
    public boolean isExpired() {
        return expiresAt != null && ZonedDateTime.now().isAfter(expiresAt);
    }

    public boolean hasPermission(String permission) {
        return permissions != null && Arrays.asList(permissions).contains(permission);
    }

    public List<String> getPermissionsList() {
        return permissions != null ? Arrays.asList(permissions) : new ArrayList<>();
    }

    public void setPermissionsList(List<String> permissionsList) {
        this.permissions = permissionsList.toArray(new String[0]);
    }

    public void addPermission(String permission) {
        List<String> currentPermissions = getPermissionsList();
        if (!currentPermissions.contains(permission)) {
            currentPermissions.add(permission);
            setPermissionsList(currentPermissions);
        }
    }

    public void removePermission(String permission) {
        List<String> currentPermissions = getPermissionsList();
        currentPermissions.remove(permission);
        setPermissionsList(currentPermissions);
    }
}
