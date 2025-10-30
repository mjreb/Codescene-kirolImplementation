package com.agent.domain.exception;

/**
 * Exception for token limit and budget related errors.
 */
public class TokenLimitException extends AgentException {
    
    private final String limitType;
    private final int currentUsage;
    private final int limit;
    private final String userId;
    private final String conversationId;
    
    public TokenLimitException(String limitType, int currentUsage, int limit, String message) {
        super("TOKEN_LIMIT_EXCEEDED", ErrorCategory.TOKEN_LIMIT, message, false);
        this.limitType = limitType;
        this.currentUsage = currentUsage;
        this.limit = limit;
        this.userId = null;
        this.conversationId = null;
    }
    
    public TokenLimitException(String limitType, int currentUsage, int limit, String userId, String conversationId, String message) {
        super("TOKEN_LIMIT_EXCEEDED", ErrorCategory.TOKEN_LIMIT, message, false);
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
    
    @Override
    public String getUserMessage() {
        switch (limitType.toLowerCase()) {
            case "daily":
                return "You have reached your daily token limit. Please try again tomorrow.";
            case "monthly":
                return "You have reached your monthly token limit. Please upgrade your plan or try again next month.";
            case "conversation":
                return "This conversation has reached its token limit. Please start a new conversation.";
            case "request":
                return "This request is too large. Please try with a shorter message.";
            default:
                return "Token limit exceeded. Please try again later or contact support.";
        }
    }
    
    /**
     * Daily token limit exceeded exception.
     */
    public static class DailyLimitExceededException extends TokenLimitException {
        public DailyLimitExceededException(int currentUsage, int limit, String userId) {
            super("daily", currentUsage, limit, userId, null, 
                    String.format("Daily token limit exceeded: %d/%d tokens", currentUsage, limit));
        }
    }
    
    /**
     * Monthly token limit exceeded exception.
     */
    public static class MonthlyLimitExceededException extends TokenLimitException {
        public MonthlyLimitExceededException(int currentUsage, int limit, String userId) {
            super("monthly", currentUsage, limit, userId, null, 
                    String.format("Monthly token limit exceeded: %d/%d tokens", currentUsage, limit));
        }
    }
    
    /**
     * Conversation token limit exceeded exception.
     */
    public static class ConversationLimitExceededException extends TokenLimitException {
        public ConversationLimitExceededException(int currentUsage, int limit, String conversationId) {
            super("conversation", currentUsage, limit, null, conversationId, 
                    String.format("Conversation token limit exceeded: %d/%d tokens", currentUsage, limit));
        }
    }
    
    /**
     * Request size limit exceeded exception.
     */
    public static class RequestSizeLimitExceededException extends TokenLimitException {
        public RequestSizeLimitExceededException(int requestSize, int limit) {
            super("request", requestSize, limit, 
                    String.format("Request size limit exceeded: %d/%d tokens", requestSize, limit));
        }
    }
}