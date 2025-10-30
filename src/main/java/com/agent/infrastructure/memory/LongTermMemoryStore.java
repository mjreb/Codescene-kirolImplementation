package com.agent.infrastructure.memory;

import com.agent.domain.model.MemoryMetadata;

import java.util.List;
import java.util.Optional;

/**
 * Interface for long-term memory storage operations.
 */
public interface LongTermMemoryStore {
    
    /**
     * Store a value with metadata.
     * 
     * @param key The key to store under
     * @param value The value to store
     * @param metadata Associated metadata
     */
    void store(String key, Object value, MemoryMetadata metadata);
    
    /**
     * Retrieve a value by key.
     * 
     * @param key The key to retrieve
     * @return The stored value, if present
     */
    Optional<Object> retrieve(String key);
    
    /**
     * Retrieve metadata for a key.
     * 
     * @param key The key to get metadata for
     * @return The metadata, if present
     */
    Optional<MemoryMetadata> retrieveMetadata(String key);
    
    /**
     * Search for keys by tags.
     * 
     * @param tags The tags to search for
     * @return List of matching keys
     */
    List<String> searchByTags(String... tags);
    
    /**
     * Remove a stored value.
     * 
     * @param key The key to remove
     */
    void remove(String key);
    
    /**
     * Check if a key exists.
     * 
     * @param key The key to check
     * @return true if key exists, false otherwise
     */
    boolean exists(String key);
    
    /**
     * Clean up expired entries.
     */
    void cleanup();
}