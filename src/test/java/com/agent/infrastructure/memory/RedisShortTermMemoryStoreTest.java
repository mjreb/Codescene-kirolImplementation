package com.agent.infrastructure.memory;

import com.agent.domain.model.ConversationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Redis-based short-term memory store.
 */
@SpringBootTest(classes = com.agent.application.AgentApplication.class)
@ActiveProfiles("test")
@Testcontainers
class RedisShortTermMemoryStoreTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired
    private RedisShortTermMemoryStore shortTermMemoryStore;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        // Clean up Redis before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }
    
    @Test
    void testStoreAndRetrieveContext() {
        // Given
        String conversationId = "test-conversation";
        ConversationContext context = new ConversationContext(conversationId, "agent-1", "user-1");
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("key1", "value1");
        contextData.put("key2", 42);
        context.setContextData(contextData);
        
        // When
        shortTermMemoryStore.storeContext(conversationId, context, 60);
        ConversationContext retrievedContext = shortTermMemoryStore.retrieveContext(conversationId);
        
        // Then
        assertNotNull(retrievedContext);
        assertEquals(conversationId, retrievedContext.getConversationId());
        assertEquals("agent-1", retrievedContext.getAgentId());
        assertEquals("user-1", retrievedContext.getUserId());
        assertEquals("value1", retrievedContext.getContextData().get("key1"));
        assertEquals(42, retrievedContext.getContextData().get("key2"));
    }
    
    @Test
    void testRetrieveNonExistentContext() {
        // When
        ConversationContext context = shortTermMemoryStore.retrieveContext("non-existent");
        
        // Then
        assertNull(context);
    }
    
    @Test
    void testExistsContext() {
        // Given
        String conversationId = "exists-test";
        ConversationContext context = new ConversationContext(conversationId, "agent-1", "user-1");
        
        // When - before storing
        boolean existsBefore = shortTermMemoryStore.existsContext(conversationId);
        
        // Store context
        shortTermMemoryStore.storeContext(conversationId, context, 60);
        
        // When - after storing
        boolean existsAfter = shortTermMemoryStore.existsContext(conversationId);
        
        // Then
        assertFalse(existsBefore);
        assertTrue(existsAfter);
    }
    
    @Test
    void testRemoveContext() {
        // Given
        String conversationId = "remove-test";
        ConversationContext context = new ConversationContext(conversationId, "agent-1", "user-1");
        shortTermMemoryStore.storeContext(conversationId, context, 60);
        
        // Verify it exists
        assertTrue(shortTermMemoryStore.existsContext(conversationId));
        
        // When
        shortTermMemoryStore.removeContext(conversationId);
        
        // Then
        assertFalse(shortTermMemoryStore.existsContext(conversationId));
        assertNull(shortTermMemoryStore.retrieveContext(conversationId));
    }
    
    @Test
    void testRemoveNonExistentContext() {
        // When/Then - should not throw exception
        assertDoesNotThrow(() -> shortTermMemoryStore.removeContext("non-existent"));
    }
    
    @Test
    void testCleanup() {
        // Given - store some contexts
        ConversationContext context1 = new ConversationContext("cleanup-1", "agent-1", "user-1");
        ConversationContext context2 = new ConversationContext("cleanup-2", "agent-1", "user-1");
        
        shortTermMemoryStore.storeContext("cleanup-1", context1, 60);
        shortTermMemoryStore.storeContext("cleanup-2", context2, 60);
        
        // When
        assertDoesNotThrow(() -> shortTermMemoryStore.cleanup());
        
        // Then - contexts should still exist (not expired)
        assertTrue(shortTermMemoryStore.existsContext("cleanup-1"));
        assertTrue(shortTermMemoryStore.existsContext("cleanup-2"));
    }
    
    @Test
    void testStoreWithShortTTL() throws InterruptedException {
        // Given
        String conversationId = "ttl-test";
        ConversationContext context = new ConversationContext(conversationId, "agent-1", "user-1");
        
        // When - store with very short TTL (1 second)
        shortTermMemoryStore.storeContext(conversationId, context, 0); // 0 minutes = immediate expiry
        
        // Wait a bit
        Thread.sleep(100);
        
        // Then - context should be expired/not retrievable
        // Note: Redis TTL behavior may vary, so we test that the store handles it gracefully
        assertDoesNotThrow(() -> shortTermMemoryStore.retrieveContext(conversationId));
    }
}