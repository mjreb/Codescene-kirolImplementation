package com.agent.infrastructure.monitoring;

/**
 * Exception thrown when token limits are exceeded.
 */
public class TokenLimitException extends RuntimeException {
    
    private final String limitType;
    private final int currentUsage;
    private final int limit;
    private final String userId;
    private final String conversationId;
    
    public TokenLimitException(String message, String limitType, int currentUsage, int limit) {
        super(message);
        this.limitType = limitType;
        this.currentUsage = currentUsage;
        this.limit = limit;
        this.userId = null;
        this.conversationId = null;
    }
    
    public TokenLimitException(String message, String limitType, int currentUsage, int limit, 
                              String userId, String conversationId) {
        super(message);
        this.limitType = limitType;
        this.currentUsage = currentUsage;
        this.limit = limit;
        this.userId = userId;
        this.conversationId = conversationId;
    }
    
    public String getLimitType() {
        return limitType;
    }
    
    public int getCurrentUsage() {
        return currentUsage;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getConversationId() {
        return conversationId;
    }
}