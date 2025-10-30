package com.agent.infrastructure.monitoring.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Entity for storing aggregated usage history for analytics.
 */
@Entity
@Table(name = "usage_history")
public class UsageHistoryEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "date", nullable = false)
    private LocalDate date;
    
    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;
    
    @Column(name = "total_conversations", nullable = false)
    private int totalConversations;
    
    @Column(name = "total_cost", nullable = false)
    private double totalCost;
    
    @Column(name = "provider_id")
    private String providerId;
    
    @Column(name = "model")
    private String model;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    public UsageHistoryEntity() {
        this.createdAt = Instant.now();
    }
    
    public UsageHistoryEntity(String userId, LocalDate date, int totalTokens, 
                             int totalConversations, double totalCost) {
        this();
        this.userId = userId;
        this.date = date;
        this.totalTokens = totalTokens;
        this.totalConversations = totalConversations;
        this.totalCost = totalCost;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
    
    public int getTotalConversations() { return totalConversations; }
    public void setTotalConversations(int totalConversations) { this.totalConversations = totalConversations; }
    
    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
    
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}