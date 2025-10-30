package com.agent.domain.exception;

/**
 * Exception for business logic and domain rule violations.
 */
public class BusinessLogicException extends AgentException {
    
    private final String businessRule;
    private final String entityType;
    private final String entityId;
    
    public BusinessLogicException(String businessRule, String message) {
        super("BUSINESS_LOGIC_ERROR", ErrorCategory.BUSINESS_LOGIC, message, false);
        this.businessRule = businessRule;
        this.entityType = null;
        this.entityId = null;
    }
    
    public BusinessLogicException(String businessRule, String entityType, String entityId, String message) {
        super("BUSINESS_LOGIC_ERROR", ErrorCategory.BUSINESS_LOGIC, message, false);
        this.businessRule = businessRule;
        this.entityType = entityType;
        this.entityId = entityId;
    }
    
    public String getBusinessRule() {
        return businessRule;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    @Override
    public String getUserMessage() {
        return getMessage();
    }
    
    /**
     * Invalid conversation state exception.
     */
    public static class InvalidConversationStateException extends BusinessLogicException {
        public InvalidConversationStateException(String conversationId, String currentState, String requiredState) {
            super("conversation_state", "conversation", conversationId, 
                    String.format("Conversation is in state '%s' but operation requires state '%s'", 
                            currentState, requiredState));
        }
        
        @Override
        public String getUserMessage() {
            return "This operation cannot be performed on the current conversation state.";
        }
    }
    
    /**
     * Conversation not found exception.
     */
    public static class ConversationNotFoundException extends BusinessLogicException {
        public ConversationNotFoundException(String conversationId) {
            super("conversation_exists", "conversation", conversationId, 
                    "Conversation not found: " + conversationId);
        }
        
        @Override
        public String getUserMessage() {
            return "The requested conversation was not found.";
        }
    }
    
    /**
     * Agent not available exception.
     */
    public static class AgentNotAvailableException extends BusinessLogicException {
        public AgentNotAvailableException(String agentId, String reason) {
            super("agent_availability", "agent", agentId, 
                    String.format("Agent '%s' is not available: %s", agentId, reason));
        }
        
        @Override
        public String getUserMessage() {
            return "The requested agent is currently not available. Please try again later.";
        }
    }
    
    /**
     * Maximum conversation limit exceeded exception.
     */
    public static class ConversationLimitExceededException extends BusinessLogicException {
        private final int currentCount;
        private final int maxAllowed;
        
        public ConversationLimitExceededException(String userId, int currentCount, int maxAllowed) {
            super("conversation_limit", "user", userId, 
                    String.format("User has %d conversations but maximum allowed is %d", currentCount, maxAllowed));
            this.currentCount = currentCount;
            this.maxAllowed = maxAllowed;
        }
        
        public int getCurrentCount() {
            return currentCount;
        }
        
        public int getMaxAllowed() {
            return maxAllowed;
        }
        
        @Override
        public String getUserMessage() {
            return String.format("You have reached the maximum number of conversations (%d). Please close some conversations before starting new ones.", maxAllowed);
        }
    }
}