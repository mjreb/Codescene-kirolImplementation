package com.agent.infrastructure.memory;

import com.agent.domain.model.MemoryMetadata;
import com.agent.infrastructure.memory.repository.LongTermMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for database-based long-term memory store.
 */
@SpringBootTest(classes = com.agent.application.AgentApplication.class)
@ActiveProfiles("test")
class DatabaseLongTermMemoryStoreTest {
    
    @Autowired
    private DatabaseLongTermMemoryStore longTermMemoryStore;
    
    @Autowired
    private LongTermMemoryRepository repository;
    
    @BeforeEach
    void setUp() {
        // Clean up database before each test
        repository.deleteAll();
    }
    
    @Test
    void testStoreAndRetrieve() {
        // Given
        String key = "test-key";
        Map<String, Object> value = new HashMap<>();
        value.put("data", "test-data");
        value.put("number", 123);
        
        MemoryMetadata metadata = new MemoryMetadata("test-source");
        Map<String, Object> tags = new HashMap<>();
        tags.put("category", "test");
        metadata.setTags(tags);
        
        // When
        longTermMemoryStore.store(key, value, metadata);
        Optional<Object> retrievedValue = longTermMemoryStore.retrieve(key);
        
        // Then
        assertTrue(retrievedValue.isPresent());
        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedMap = (Map<String, Object>) retrievedValue.get();
        assertEquals("test-data", retrievedMap.get("data"));
        assertEquals(123, retrievedMap.get("number"));
    }
    
    @Test
    void testRetrieveNonExistent() {
        // When
        Optional<Object> result = longTermMemoryStore.retrieve("non-existent");
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testExists() {
        // Given
        String key = "exists-test";
        MemoryMetadata metadata = new MemoryMetadata("test-source");
        
        // When - before storing
        boolean existsBefore = longTermMemoryStore.exists(key);
        
        // Store
        longTermMemoryStore.store(key, "test-value", metadata);
        
        // When - after storing
        boolean existsAfter = longTermMemoryStore.exists(key);
        
        // Then
        assertFalse(existsBefore);
        assertTrue(existsAfter);
    }
    
    @Test
    void testRemove() {
        // Given
        String key = "remove-test";
        MemoryMetadata metadata = new MemoryMetadata("test-source");
        longTermMemoryStore.store(key, "test-value", metadata);
        
        // Verify it exists
        assertTrue(longTermMemoryStore.exists(key));
        
        // When
        longTermMemoryStore.remove(key);
        
        // Then
        assertFalse(longTermMemoryStore.exists(key));
        assertTrue(longTermMemoryStore.retrieve(key).isEmpty());
    }
    
    @Test
    void testRemoveNonExistent() {
        // When/Then - should not throw exception
        assertDoesNotThrow(() -> longTermMemoryStore.remove("non-existent"));
    }
    
    @Test
    void testRetrieveMetadata() {
        // Given
        String key = "metadata-test";
        Map<String, Object> tags = new HashMap<>();
        tags.put("category", "test");
        tags.put("priority", "high");
        
        MemoryMetadata originalMetadata = new MemoryMetadata("test-source");
        originalMetadata.setTags(tags);
        
        longTermMemoryStore.store(key, "test-value", originalMetadata);
        
        // When
        Optional<MemoryMetadata> retrievedMetadata = longTermMemoryStore.retrieveMetadata(key);
        
        // Then
        assertTrue(retrievedMetadata.isPresent());
        MemoryMetadata metadata = retrievedMetadata.get();
        assertEquals("test-source", metadata.getSource());
        assertNotNull(metadata.getCreatedAt());
        assertEquals("test", metadata.getTags().get("category"));
        assertEquals("high", metadata.getTags().get("priority"));
    }
    
    @Test
    void testSearchByTags() {
        // Given
        MemoryMetadata metadata1 = new MemoryMetadata("source1");
        Map<String, Object> tags1 = new HashMap<>();
        tags1.put("category", "test");
        tags1.put("type", "data");
        metadata1.setTags(tags1);
        
        MemoryMetadata metadata2 = new MemoryMetadata("source2");
        Map<String, Object> tags2 = new HashMap<>();
        tags2.put("category", "production");
        tags2.put("type", "data");
        metadata2.setTags(tags2);
        
        MemoryMetadata metadata3 = new MemoryMetadata("source3");
        Map<String, Object> tags3 = new HashMap<>();
        tags3.put("category", "test");
        tags3.put("type", "config");
        metadata3.setTags(tags3);
        
        longTermMemoryStore.store("key1", "value1", metadata1);
        longTermMemoryStore.store("key2", "value2", metadata2);
        longTermMemoryStore.store("key3", "value3", metadata3);
        
        // When
        List<String> testKeys = longTermMemoryStore.searchByTags("test");
        List<String> dataKeys = longTermMemoryStore.searchByTags("data");
        
        // Then
        assertEquals(2, testKeys.size());
        assertTrue(testKeys.contains("key1"));
        assertTrue(testKeys.contains("key3"));
        
        assertEquals(2, dataKeys.size());
        assertTrue(dataKeys.contains("key1"));
        assertTrue(dataKeys.contains("key2"));
    }
    
    @Test
    void testExpiredMemoryHandling() {
        // Given
        String key = "expired-test";
        MemoryMetadata metadata = new MemoryMetadata("test-source");
        metadata.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS)); // Already expired
        
        longTermMemoryStore.store(key, "expired-value", metadata);
        
        // When
        Optional<Object> result = longTermMemoryStore.retrieve(key);
        
        // Then
        assertTrue(result.isEmpty()); // Should be empty because it's expired
        assertFalse(longTermMemoryStore.exists(key)); // Should be removed after retrieval attempt
    }
    
    @Test
    void testAccessCountIncrement() {
        // Given
        String key = "access-test";
        MemoryMetadata metadata = new MemoryMetadata("test-source");
        longTermMemoryStore.store(key, "test-value", metadata);
        
        // When - retrieve multiple times
        longTermMemoryStore.retrieve(key);
        longTermMemoryStore.retrieve(key);
        longTermMemoryStore.retrieve(key);
        
        Optional<MemoryMetadata> retrievedMetadata = longTermMemoryStore.retrieveMetadata(key);
        
        // Then
        assertTrue(retrievedMetadata.isPresent());
        assertEquals(3, retrievedMetadata.get().getAccessCount());
        assertNotNull(retrievedMetadata.get().getLastAccessed());
    }
    
    @Test
    void testCleanup() {
        // Given - store some test data
        MemoryMetadata metadata1 = new MemoryMetadata("source1");
        MemoryMetadata metadata2 = new MemoryMetadata("source2");
        metadata2.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS)); // Expired
        
        longTermMemoryStore.store("cleanup-key1", "value1", metadata1);
        longTermMemoryStore.store("cleanup-key2", "value2", metadata2);
        
        // When
        assertDoesNotThrow(() -> longTermMemoryStore.cleanup());
        
        // Then - non-expired should exist, expired should be removed
        assertTrue(longTermMemoryStore.exists("cleanup-key1"));
        assertFalse(longTermMemoryStore.exists("cleanup-key2"));
    }
    
    @Test
    void testUpdateExistingEntry() {
        // Given
        String key = "update-test";
        MemoryMetadata metadata1 = new MemoryMetadata("source1");
        longTermMemoryStore.store(key, "original-value", metadata1);
        
        // When - store again with different value and metadata
        MemoryMetadata metadata2 = new MemoryMetadata("source2");
        longTermMemoryStore.store(key, "updated-value", metadata2);
        
        // Then
        Optional<Object> retrievedValue = longTermMemoryStore.retrieve(key);
        assertTrue(retrievedValue.isPresent());
        assertEquals("updated-value", retrievedValue.get());
        
        Optional<MemoryMetadata> retrievedMetadata = longTermMemoryStore.retrieveMetadata(key);
        assertTrue(retrievedMetadata.isPresent());
        assertEquals("source2", retrievedMetadata.get().getSource());
    }
}