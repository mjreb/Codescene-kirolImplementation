package com.agent.domain.model;

/**
 * Represents token limits for conversations and requests.
 */
public class TokenLimits {
    private int maxTokensPerRequest;
    private int maxTokensPerConversation;
    private int maxTokensPerDay;
    private int warningThreshold;
    
    public TokenLimits() {}
    
    public TokenLimits(int maxTokensPerRequest, int maxTokensPerConversation, int maxTokensPerDay) {
        this.maxTokensPerRequest = maxTokensPerRequest;
        this.maxTokensPerConversation = maxTokensPerConversation;
        this.maxTokensPerDay = maxTokensPerDay;
        this.warningThreshold = (int) (maxTokensPerDay * 0.8); // 80% warning threshold
    }
    
    // Getters and setters
    public int getMaxTokensPerRequest() { return maxTokensPerRequest; }
    public void setMaxTokensPerRequest(int maxTokensPerRequest) { this.maxTokensPerRequest = maxTokensPerRequest; }
    
    public int getMaxTokensPerConversation() { return maxTokensPerConversation; }
    public void setMaxTokensPerConversation(int maxTokensPerConversation) { this.maxTokensPerConversation = maxTokensPerConversation; }
    
    public int getMaxTokensPerDay() { return maxTokensPerDay; }
    public void setMaxTokensPerDay(int maxTokensPerDay) { this.maxTokensPerDay = maxTokensPerDay; }
    
    public int getWarningThreshold() { return warningThreshold; }
    public void setWarningThreshold(int warningThreshold) { this.warningThreshold = warningThreshold; }
}