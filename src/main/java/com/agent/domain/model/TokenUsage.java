package com.agent.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Represents token usage information for a conversation or request.
 */
@Entity
@Table(name = "token_usage")
public class TokenUsage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "conversation_id", nullable = false)
    private String conversationId;
    
    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;
    
    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;
    
    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;
    
    @Column(name = "estimated_cost")
    private double estimatedCost;
    
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    @Column(name = "provider_id")
    private String providerId;
    
    @Column(name = "model")
    private String model;
    
    public TokenUsage() {
        this.timestamp = Instant.now();
    }
    
    public TokenUsage(String conversationId, int inputTokens, int outputTokens) {
        this();
        this.conversationId = conversationId;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = inputTokens + outputTokens;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    
    public int getInputTokens() { return inputTokens; }
    public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }
    
    public int getOutputTokens() { return outputTokens; }
    public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }
    
    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
    
    public double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}