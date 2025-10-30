package com.agent.infrastructure.memory.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for long-term memory storage.
 */
@Entity
@Table(name = "long_term_memory", indexes = {
    @Index(name = "idx_memory_key", columnList = "memory_key"),
    @Index(name = "idx_source", columnList = "source"),
    @Index(name = "idx_expires_at", columnList = "expires_at"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class LongTermMemoryEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "memory_key", nullable = false, unique = true, length = 255)
    private String key;
    
    @Lob
    @Column(name = "memory_value", nullable = false)
    private String value;
    
    @Column(name = "source", length = 255)
    private String source;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Column(name = "access_count", nullable = false)
    private Integer accessCount = 0;
    
    @Column(name = "last_accessed")
    private Instant lastAccessed;
    
    @Lob
    @Column(name = "tags_json")
    private String tagsJson;
    
    // Constructors
    public LongTermMemoryEntity() {
        this.createdAt = Instant.now();
        this.accessCount = 0;
    }
    
    public LongTermMemoryEntity(String key, String value) {
        this();
        this.key = key;
        this.value = value;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    
    public Integer getAccessCount() { return accessCount; }
    public void setAccessCount(Integer accessCount) { this.accessCount = accessCount; }
    
    public Instant getLastAccessed() { return lastAccessed; }
    public void setLastAccessed(Instant lastAccessed) { this.lastAccessed = lastAccessed; }
    
    public String getTagsJson() { return tagsJson; }
    public void setTagsJson(String tagsJson) { this.tagsJson = tagsJson; }
    
    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessed = Instant.now();
    }
}