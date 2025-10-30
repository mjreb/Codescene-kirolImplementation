package com.agent.infrastructure.memory;

import com.agent.domain.interfaces.MemoryManager;
import com.agent.domain.model.ConversationContext;
import com.agent.domain.model.MemoryConfiguration;
import com.agent.domain.model.MemoryMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Implementation of MemoryManager that coordinates between short-term and long-term memory storage.
 */
@Service
public class MemoryManagerImpl implements MemoryManager {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryManagerImpl.class);
    
    private final ShortTermMemoryStore shortTermMemoryStore;
    private final LongTermMemoryStore longTermMemoryStore;
    private final MemoryConfiguration memoryConfiguration;
    
    @Autowired
    public MemoryManagerImpl(ShortTermMemoryStore shortTermMemoryStore, 
                           LongTermMemoryStore longTermMemoryStore,
                           MemoryConfiguration memoryConfiguration) {
        this.shortTermMemoryStore = shortTermMemoryStore;
        this.longTermMemoryStore = longTermMemoryStore;
        this.memoryConfiguration = memoryConfiguration;
    }
    
    @Override
    public void storeConversationContext(String conversationId, ConversationContext context) {
        if (!memoryConfiguration.isShortTermEnabled()) {
            logger.debug("Short-term memory is disabled, skipping conversation context storage");
            return;
        }
        
        try {
            context.setLastUpdated(Instant.now());
            shortTermMemoryStore.storeContext(conversationId, context, memoryConfiguration.getShortTermTtlMinutes());
            logger.debug("Stored conversation context for conversation: {}", conversationId);
        } catch (Exception e) {
            logger.error("Failed to store conversation context for conversation: {}", conversationId, e);
            throw new MemoryException("Failed to store conversation context", e);
        }
    }
    
    @Override
    public ConversationContext retrieveConversationContext(String conversationId) {
        if (!memoryConfiguration.isShortTermEnabled()) {
            logger.debug("Short-term memory is disabled, returning null for conversation context");
            return null;
        }
        
        try {
            ConversationContext context = shortTermMemoryStore.retrieveContext(conversationId);
            if (context != null) {
                logger.debug("Retrieved conversation context for conversation: {}", conversationId);
            } else {
                logger.debug("No conversation context found for conversation: {}", conversationId);
            }
            return context;
        } catch (Exception e) {
            logger.error("Failed to retrieve conversation context for conversation: {}", conversationId, e);
            throw new MemoryException("Failed to retrieve conversation context", e);
        }
    }
    
    @Override
    public void storeLongTermMemory(String key, Object value, MemoryMetadata metadata) {
        if (!memoryConfiguration.isLongTermEnabled()) {
            logger.debug("Long-term memory is disabled, skipping storage");
            return;
        }
        
        try {
            longTermMemoryStore.store(key, value, metadata);
            logger.debug("Stored long-term memory for key: {}", key);
        } catch (Exception e) {
            logger.error("Failed to store long-term memory for key: {}", key, e);
            throw new MemoryException("Failed to store long-term memory", e);
        }
    }
    
    @Override
    public Optional<Object> retrieveLongTermMemory(String key) {
        if (!memoryConfiguration.isLongTermEnabled()) {
            logger.debug("Long-term memory is disabled, returning empty optional");
            return Optional.empty();
        }
        
        try {
            Optional<Object> result = longTermMemoryStore.retrieve(key);
            if (result.isPresent()) {
                logger.debug("Retrieved long-term memory for key: {}", key);
            } else {
                logger.debug("No long-term memory found for key: {}", key);
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to retrieve long-term memory for key: {}", key, e);
            throw new MemoryException("Failed to retrieve long-term memory", e);
        }
    }
    
    @Override
    public void cleanupExpiredMemory() {
        logger.info("Starting memory cleanup process");
        
        try {
            if (memoryConfiguration.isShortTermEnabled()) {
                shortTermMemoryStore.cleanup();
                logger.debug("Completed short-term memory cleanup");
            }
            
            if (memoryConfiguration.isLongTermEnabled()) {
                longTermMemoryStore.cleanup();
                logger.debug("Completed long-term memory cleanup");
            }
            
            logger.info("Memory cleanup process completed successfully");
        } catch (Exception e) {
            logger.error("Error during memory cleanup process", e);
            throw new MemoryException("Failed to cleanup expired memory", e);
        }
    }
}