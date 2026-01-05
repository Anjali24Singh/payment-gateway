package com.talentica.paymentgateway.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.net.InetAddress;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing an audit log entry for tracking all system changes.
 * Audit logs provide a complete history of who changed what and when.
 */
@Entity
@Table(name = "audit_logs",
       indexes = {
           @Index(name = "idx_audit_logs_entity", columnList = "entityType, entityId"),
           @Index(name = "idx_audit_logs_user_id", columnList = "user_id"),
           @Index(name = "idx_audit_logs_correlation_id", columnList = "correlationId"),
           @Index(name = "idx_audit_logs_created_at", columnList = "createdAt")
       })
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Entity type is required")
    @Size(max = 100, message = "Entity type must not exceed 100 characters")
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @NotNull(message = "Entity ID is required")
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @NotBlank(message = "Action is required")
    @Size(max = 50, message = "Action must not exceed 50 characters")
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id")
    private ApiKey apiKey;

    @Column(name = "ip_address", columnDefinition = "INET")
    private InetAddress ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Size(max = 100, message = "Correlation ID must not exceed 100 characters")
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "old_values", columnDefinition = "JSONB")
    private Map<String, Object> oldValues = new HashMap<>();

    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "new_values", columnDefinition = "JSONB")
    private Map<String, Object> newValues = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    // Constructors
    public AuditLog() {
        this.createdAt = ZonedDateTime.now();
    }

    public AuditLog(String entityType, UUID entityId, String action) {
        this();
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
    }

    public AuditLog(String entityType, UUID entityId, String action, User user) {
        this(entityType, entityId, action);
        this.user = user;
    }

    public AuditLog(String entityType, UUID entityId, String action, ApiKey apiKey) {
        this(entityType, entityId, action);
        this.apiKey = apiKey;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    public void setApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Map<String, Object> getOldValues() {
        return oldValues;
    }

    public void setOldValues(Map<String, Object> oldValues) {
        this.oldValues = oldValues;
    }

    public Map<String, Object> getNewValues() {
        return newValues;
    }

    public void setNewValues(Map<String, Object> newValues) {
        this.newValues = newValues;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Utility methods
    public String getActorDescription() {
        if (user != null) {
            return "User: " + user.getUsername();
        } else if (apiKey != null) {
            return "API Key: " + apiKey.getKeyName();
        } else {
            return "System";
        }
    }

    public boolean hasChanges() {
        return (oldValues != null && !oldValues.isEmpty()) || 
               (newValues != null && !newValues.isEmpty());
    }

    public void addOldValue(String field, Object value) {
        if (oldValues == null) {
            oldValues = new HashMap<>();
        }
        oldValues.put(field, value);
    }

    public void addNewValue(String field, Object value) {
        if (newValues == null) {
            newValues = new HashMap<>();
        }
        newValues.put(field, value);
    }

    public void addChange(String field, Object oldValue, Object newValue) {
        addOldValue(field, oldValue);
        addNewValue(field, newValue);
    }

    public String getChangeDescription() {
        StringBuilder description = new StringBuilder();
        description.append(getActorDescription())
                  .append(" performed ")
                  .append(action)
                  .append(" on ")
                  .append(entityType)
                  .append(" (ID: ")
                  .append(entityId)
                  .append(")");
        
        if (hasChanges()) {
            description.append(" with changes: ");
            if (newValues != null && !newValues.isEmpty()) {
                description.append(newValues.size()).append(" field(s) modified");
            }
        }
        
        return description.toString();
    }

    public boolean isCreation() {
        return "CREATE".equalsIgnoreCase(action) || "CREATED".equalsIgnoreCase(action);
    }

    public boolean isUpdate() {
        return "UPDATE".equalsIgnoreCase(action) || "UPDATED".equalsIgnoreCase(action);
    }

    public boolean isDeletion() {
        return "DELETE".equalsIgnoreCase(action) || "DELETED".equalsIgnoreCase(action);
    }

    // Common audit log creation methods
    public static AuditLog createForUser(String entityType, UUID entityId, String action, User user) {
        return new AuditLog(entityType, entityId, action, user);
    }

    public static AuditLog createForApiKey(String entityType, UUID entityId, String action, ApiKey apiKey) {
        return new AuditLog(entityType, entityId, action, apiKey);
    }

    public static AuditLog createSystemLog(String entityType, UUID entityId, String action) {
        return new AuditLog(entityType, entityId, action);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AuditLog auditLog = (AuditLog) obj;
        return id != null && id.equals(auditLog.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return String.format("AuditLog{id=%s, entityType=%s, entityId=%s, action=%s, createdAt=%s}", 
                id, entityType, entityId, action, createdAt);
    }
}
