package com.agent.infrastructure.memory;

import com.agent.domain.model.ConversationContext;
import com.agent.domain.model.MemoryConfiguration;
import com.agent.domain.model.MemoryMetadata;
import com.agent.infrastructure.memory.repository.LongTermMemoryRepository;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for memory management components.
 */
@SpringBootTest(classes = com.agent.application.AgentApplication.class)
@ActiveProfiles("test")
@Testcontainers
class MemoryManagerIntegrationTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired
    private MemoryManagerImpl memoryManager;
    
    @Autowired
    private RedisShortTermMemoryStore shortTermMemoryStore;
    
    @Autowired
    private DatabaseLongTermMemoryStore longTermMemoryStore;
    
    @Autowired
    private LongTermMemoryRepository longTermMemoryRepository;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private MemoryConfiguration memoryConfiguration;
    
    @BeforeEach
    void setUp() {
        // Clean up Redis
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        
        // Clean up database
        longTermMemoryRepository.deleteAll();
        
        // Enable both short-term and long-term memory for tests
        memoryConfiguration.setShortTermEnabled(true);
        memoryConfiguration.setLongTermEnabled(true);
    }
    
    @Test
    void testStoreAndRetrieveConversationContext() {
        // Given
        String conversationId = "test-conversation-1";
        ConversationContext context = new ConversationContext(conversationId, "agent-1", "user-1");
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("key1", "value1");
        contextData.put("key2", 42);
        context.setContextData(contextData);
        
        // When
        memoryManager.storeConversationContext(conversationId, context);
        ConversationContext retrievedContext = memoryManager.retrieveConversationContext(conversationId);
        
        // Then
        assertNotNull(retrievedContext);
        assertEquals(conversationId, retrievedContext.getConversationId());
        assertEquals("agent-1", retrievedContext.getAgentId());
        assertEquals("user-1", retrievedContext.getUserId());
        assertEquals("value1", retrievedContext.getContextData().get("key1"));
        assertEquals(42, retrievedContext.getContextData().get("key2"));
    }
    
    @Test
    void testConversationContextNotFoundReturnsNull() {
        // When
        ConversationContext context = memoryManager.retrieveConversationContext("non-existent");
        
        // Then
        assertNull(context);
    }
    
    @Test
    void testStoreAndRetrieveLongTermMemory() {
        // Given
        String key = "test-key-1";
        Map<String, Object> value = new HashMap<>();
        value.put("data", "test-data");
        value.put("number", 123);
        
        MemoryMetadata metadata = new MemoryMetadata("test-source");
        Map<String, Object> tags = new HashMap<>();
        tags.put("category", "test");
        tags.put("priority", "high");
        metadata.setTags(tags);
        
        // When
        memoryManager.storeLongTermMemory(key, value, metadata);
        Optional<Object> retrievedValue = memoryManager.retrieveLongTermMemory(key);
        
        // Then
        assertTrue(retrievedValue.isPresent());
        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedMap = (Map<String, Object>) retrievedValue.get();
        assertEquals("test-data", retrievedMap.get("data"));
        assertEquals(123, retrievedMap.get("number"));
    }
    
    @Test
    void testLongTermMemoryNotFoundReturnsEmpty() {
        // When
        Optional<Object> result = memoryManager.retrieveLongTermMemory("non-existent");
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testMemoryCleanup() {
        // Given - store some test data
        ConversationContext context = new ConversationContext("cleanup-test", "agent-1", "user-1");
        memoryManager.storeConversationContext("cleanup-test", context);
        
        MemoryMetadata metadata = new MemoryMetadata("cleanup-source");
        memoryManager.storeLongTermMemory("cleanup-key", "cleanup-value", metadata);
        
        // When
        assertDoesNotThrow(() -> memoryManager.cleanupExpiredMemory());
        
        // Then - verify data still exists (not expired)
        assertNotNull(memoryManager.retrieveConversationContext("cleanup-test"));
        assertTrue(memoryManager.retrieveLongTermMemory("cleanup-key").isPresent());
    }
    
    @Test
    void testShortTermMemoryDisabled() {
        // Given
        memoryConfiguration.setShortTermEnabled(false);
        ConversationContext context = new ConversationContext("disabled-test", "agent-1", "user-1");
        
        // When
        memoryManager.storeConversationContext("disabled-test", context);
        ConversationContext retrieved = memoryManager.retrieveConversationContext("disabled-test");
        
        // Then
        assertNull(retrieved);
    }
    
    @Test
    void testLongTermMemoryDisabled() {
        // Given
        memoryConfiguration.setLongTermEnabled(false);
        MemoryMetadata metadata = new MemoryMetadata("disabled-source");
        
        // When
        memoryManager.storeLongTermMemory("disabled-key", "disabled-value", metadata);
        Optional<Object> retrieved = memoryManager.retrieveLongTermMemory("disabled-key");
        
        // Then
        assertTrue(retrieved.isEmpty());
    }
}