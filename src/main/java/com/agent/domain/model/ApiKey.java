package com.agent.domain.model;

import java.time.Instant;
import java.util.Set;

/**
 * API Key entity for service-to-service authentication
 */
public class ApiKey {
    private String id;
    private String keyHash;
    private String name;
    private String description;
    private String userId;
    private Set<String> scopes;
    private boolean enabled;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant lastUsedAt;
    private long usageCount;

    public ApiKey() {}

    public ApiKey(String id, String keyHash, String name, String userId) {
        this.id = id;
        this.keyHash = keyHash;
        this.name = name;
        this.userId = userId;
        this.enabled = true;
        this.createdAt = Instant.now();
        this.usageCount = 0;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Set<String> getScopes() { return scopes; }
    public void setScopes(Set<String> scopes) { this.scopes = scopes; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public long getUsageCount() { return usageCount; }
    public void setUsageCount(long usageCount) { this.usageCount = usageCount; }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}