package com.agent.infrastructure.memory;

import com.agent.domain.model.ConversationContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Redis-based implementation of short-term memory storage.
 */
@Component
public class RedisShortTermMemoryStore implements ShortTermMemoryStore {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisShortTermMemoryStore.class);
    private static final String CONVERSATION_KEY_PREFIX = "conversation:context:";
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public RedisShortTermMemoryStore(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void storeContext(String conversationId, ConversationContext context, int ttlMinutes) {
        String key = buildKey(conversationId);
        
        try {
            String serializedContext = objectMapper.writeValueAsString(context);
            redisTemplate.opsForValue().set(key, serializedContext, Duration.ofMinutes(ttlMinutes));
            logger.debug("Stored conversation context in Redis with key: {} and TTL: {} minutes", key, ttlMinutes);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize conversation context for key: {}", key, e);
            throw new MemoryException("Failed to serialize conversation context", e);
        } catch (Exception e) {
            logger.error("Failed to store conversation context in Redis for key: {}", key, e);
            throw new MemoryException("Failed to store conversation context in Redis", e);
        }
    }
    
    @Override
    public ConversationContext retrieveContext(String conversationId) {
        String key = buildKey(conversationId);
        
        try {
            String serializedContext = redisTemplate.opsForValue().get(key);
            if (serializedContext == null) {
                logger.debug("No conversation context found in Redis for key: {}", key);
                return null;
            }
            
            ConversationContext context = objectMapper.readValue(serializedContext, ConversationContext.class);
            logger.debug("Retrieved conversation context from Redis for key: {}", key);
            return context;
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize conversation context for key: {}", key, e);
            throw new MemoryException("Failed to deserialize conversation context", e);
        } catch (Exception e) {
            logger.error("Failed to retrieve conversation context from Redis for key: {}", key, e);
            throw new MemoryException("Failed to retrieve conversation context from Redis", e);
        }
    }
    
    @Override
    public void removeContext(String conversationId) {
        String key = buildKey(conversationId);
        
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                logger.debug("Removed conversation context from Redis for key: {}", key);
            } else {
                logger.debug("No conversation context found to remove for key: {}", key);
            }
        } catch (Exception e) {
            logger.error("Failed to remove conversation context from Redis for key: {}", key, e);
            throw new MemoryException("Failed to remove conversation context from Redis", e);
        }
    }
    
    @Override
    public boolean existsContext(String conversationId) {
        String key = buildKey(conversationId);
        
        try {
            Boolean exists = redisTemplate.hasKey(key);
            boolean result = Boolean.TRUE.equals(exists);
            logger.debug("Conversation context exists check for key: {} = {}", key, result);
            return result;
        } catch (Exception e) {
            logger.error("Failed to check if conversation context exists in Redis for key: {}", key, e);
            throw new MemoryException("Failed to check conversation context existence in Redis", e);
        }
    }
    
    @Override
    public void cleanup() {
        try {
            // Redis automatically handles TTL-based cleanup, but we can manually clean up expired keys
            Set<String> keys = redisTemplate.keys(CONVERSATION_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                logger.debug("Found {} conversation context keys in Redis for potential cleanup", keys.size());
                
                // Redis TTL will handle automatic cleanup, but we can log the current state
                int expiredCount = 0;
                for (String key : keys) {
                    Long ttl = redisTemplate.getExpire(key);
                    if (ttl != null && ttl == -2) { // -2 means key doesn't exist (expired)
                        expiredCount++;
                    }
                }
                
                logger.debug("Redis cleanup completed. Found {} expired keys that were automatically removed", expiredCount);
            } else {
                logger.debug("No conversation context keys found in Redis for cleanup");
            }
        } catch (Exception e) {
            logger.error("Error during Redis short-term memory cleanup", e);
            throw new MemoryException("Failed to cleanup Redis short-term memory", e);
        }
    }
    
    private String buildKey(String conversationId) {
        return CONVERSATION_KEY_PREFIX + conversationId;
    }
}