package com.agent.domain.model;

/**
 * Represents memory configuration settings for an agent.
 */
public class MemoryConfiguration {
    private boolean shortTermEnabled;
    private boolean longTermEnabled;
    private int shortTermTtlMinutes;
    private int maxConversationHistory;
    private boolean semanticSearchEnabled;
    private double similarityThreshold;
    
    public MemoryConfiguration() {
        this.shortTermEnabled = true;
        this.longTermEnabled = false;
        this.shortTermTtlMinutes = 60; // 1 hour default
        this.maxConversationHistory = 100;
        this.semanticSearchEnabled = false;
        this.similarityThreshold = 0.8;
    }
    
    // Getters and setters
    public boolean isShortTermEnabled() { return shortTermEnabled; }
    public void setShortTermEnabled(boolean shortTermEnabled) { this.shortTermEnabled = shortTermEnabled; }
    
    public boolean isLongTermEnabled() { return longTermEnabled; }
    public void setLongTermEnabled(boolean longTermEnabled) { this.longTermEnabled = longTermEnabled; }
    
    public int getShortTermTtlMinutes() { return shortTermTtlMinutes; }
    public void setShortTermTtlMinutes(int shortTermTtlMinutes) { this.shortTermTtlMinutes = shortTermTtlMinutes; }
    
    public int getMaxConversationHistory() { return maxConversationHistory; }
    public void setMaxConversationHistory(int maxConversationHistory) { this.maxConversationHistory = maxConversationHistory; }
    
    public boolean isSemanticSearchEnabled() { return semanticSearchEnabled; }
    public void setSemanticSearchEnabled(boolean semanticSearchEnabled) { this.semanticSearchEnabled = semanticSearchEnabled; }
    
    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
}