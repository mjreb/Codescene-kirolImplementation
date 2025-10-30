package com.agent.domain.interfaces;

import com.agent.domain.model.ConversationContext;
import com.agent.domain.model.MemoryMetadata;

import java.util.Optional;

/**
 * Interface for managing conversation context and persistent knowledge storage.
 */
public interface MemoryManager {
    
    /**
     * Store conversation context for a specific conversation.
     * 
     * @param conversationId The unique identifier for the conversation
     * @param context The conversation context to store
     */
    void storeConversationContext(String conversationId, ConversationContext context);
    
    /**
     * Retrieve conversation context for a specific conversation.
     * 
     * @param conversationId The unique identifier for the conversation
     * @return The conversation context, or null if not found
     */
    ConversationContext retrieveConversationContext(String conversationId);
    
    /**
     * Store long-term memory with associated metadata.
     * 
     * @param key The key to store the memory under
     * @param value The value to store
     * @param metadata Additional metadata about the memory
     */
    void storeLongTermMemory(String key, Object value, MemoryMetadata metadata);
    
    /**
     * Retrieve long-term memory by key.
     * 
     * @param key The key to retrieve memory for
     * @return The stored value, if present
     */
    Optional<Object> retrieveLongTermMemory(String key);
    
    /**
     * Clean up expired memory entries to optimize storage.
     */
    void cleanupExpiredMemory();
}