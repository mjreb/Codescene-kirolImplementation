package com.agent.infrastructure.memory;

import com.agent.domain.model.ConversationContext;

/**
 * Interface for short-term memory storage operations.
 */
public interface ShortTermMemoryStore {
    
    /**
     * Store conversation context with TTL.
     * 
     * @param conversationId The conversation identifier
     * @param context The conversation context to store
     * @param ttlMinutes Time to live in minutes
     */
    void storeContext(String conversationId, ConversationContext context, int ttlMinutes);
    
    /**
     * Retrieve conversation context.
     * 
     * @param conversationId The conversation identifier
     * @return The conversation context, or null if not found
     */
    ConversationContext retrieveContext(String conversationId);
    
    /**
     * Remove conversation context.
     * 
     * @param conversationId The conversation identifier
     */
    void removeContext(String conversationId);
    
    /**
     * Check if conversation context exists.
     * 
     * @param conversationId The conversation identifier
     * @return true if context exists, false otherwise
     */
    boolean existsContext(String conversationId);
    
    /**
     * Clean up expired entries.
     */
    void cleanup();
}