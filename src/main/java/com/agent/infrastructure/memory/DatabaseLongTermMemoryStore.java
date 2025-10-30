package com.agent.infrastructure.memory;

import com.agent.domain.model.MemoryMetadata;
import com.agent.infrastructure.memory.entity.LongTermMemoryEntity;
import com.agent.infrastructure.memory.repository.LongTermMemoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Database-based implementation of long-term memory storage.
 */
@Component
public class DatabaseLongTermMemoryStore implements LongTermMemoryStore {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseLongTermMemoryStore.class);
    
    private final LongTermMemoryRepository repository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public DatabaseLongTermMemoryStore(LongTermMemoryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Transactional
    public void store(String key, Object value, MemoryMetadata metadata) {
        try {
            String serializedValue = objectMapper.writeValueAsString(value);
            String serializedTags = metadata.getTags() != null ? 
                objectMapper.writeValueAsString(metadata.getTags()) : null;
            
            Optional<LongTermMemoryEntity> existingEntity = repository.findByKey(key);
            LongTermMemoryEntity entity;
            
            if (existingEntity.isPresent()) {
                entity = existingEntity.get();
                entity.setValue(serializedValue);
                entity.setTagsJson(serializedTags);
            } else {
                entity = new LongTermMemoryEntity(key, serializedValue);
                entity.setTagsJson(serializedTags);
            }
            
            entity.setSource(metadata.getSource());
            entity.setExpiresAt(metadata.getExpiresAt());
            
            repository.save(entity);
            logger.debug("Stored long-term memory for key: {}", key);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize value or metadata for key: {}", key, e);
            throw new MemoryException("Failed to serialize value or metadata", e);
        } catch (Exception e) {
            logger.error("Failed to store long-term memory for key: {}", key, e);
            throw new MemoryException("Failed to store long-term memory", e);
        }
    }
    
    @Override
    @Transactional
    public Optional<Object> retrieve(String key) {
        try {
            Optional<LongTermMemoryEntity> entityOpt = repository.findByKey(key);
            if (entityOpt.isEmpty()) {
                logger.debug("No long-term memory found for key: {}", key);
                return Optional.empty();
            }
            
            LongTermMemoryEntity entity = entityOpt.get();
            
            // Check if expired
            if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(Instant.now())) {
                logger.debug("Long-term memory expired for key: {}, removing", key);
                repository.delete(entity);
                return Optional.empty();
            }
            
            // Update access information
            entity.incrementAccessCount();
            repository.save(entity);
            
            Object value = objectMapper.readValue(entity.getValue(), Object.class);
            logger.debug("Retrieved long-term memory for key: {}", key);
            return Optional.of(value);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize value for key: {}", key, e);
            throw new MemoryException("Failed to deserialize value", e);
        } catch (Exception e) {
            logger.error("Failed to retrieve long-term memory for key: {}", key, e);
            throw new MemoryException("Failed to retrieve long-term memory", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<MemoryMetadata> retrieveMetadata(String key) {
        try {
            Optional<LongTermMemoryEntity> entityOpt = repository.findByKey(key);
            if (entityOpt.isEmpty()) {
                return Optional.empty();
            }
            
            LongTermMemoryEntity entity = entityOpt.get();
            MemoryMetadata metadata = new MemoryMetadata(entity.getSource());
            metadata.setCreatedAt(entity.getCreatedAt());
            metadata.setExpiresAt(entity.getExpiresAt());
            metadata.setAccessCount(entity.getAccessCount());
            metadata.setLastAccessed(entity.getLastAccessed());
            
            if (entity.getTagsJson() != null) {
                Map<String, Object> tags = objectMapper.readValue(
                    entity.getTagsJson(), 
                    new TypeReference<Map<String, Object>>() {}
                );
                metadata.setTags(tags);
            }
            
            return Optional.of(metadata);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize metadata for key: {}", key, e);
            throw new MemoryException("Failed to deserialize metadata", e);
        } catch (Exception e) {
            logger.error("Failed to retrieve metadata for key: {}", key, e);
            throw new MemoryException("Failed to retrieve metadata", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> searchByTags(String... tags) {
        try {
            Set<String> matchingKeys = new HashSet<>();
            
            for (String tag : tags) {
                List<LongTermMemoryEntity> entities = repository.findByTagsContaining(tag);
                matchingKeys.addAll(entities.stream()
                    .map(LongTermMemoryEntity::getKey)
                    .collect(Collectors.toList()));
            }
            
            logger.debug("Found {} keys matching tags: {}", matchingKeys.size(), Arrays.toString(tags));
            return new ArrayList<>(matchingKeys);
            
        } catch (Exception e) {
            logger.error("Failed to search by tags: {}", Arrays.toString(tags), e);
            throw new MemoryException("Failed to search by tags", e);
        }
    }
    
    @Override
    @Transactional
    public void remove(String key) {
        try {
            if (repository.existsByKey(key)) {
                repository.deleteByKey(key);
                logger.debug("Removed long-term memory for key: {}", key);
            } else {
                logger.debug("No long-term memory found to remove for key: {}", key);
            }
        } catch (Exception e) {
            logger.error("Failed to remove long-term memory for key: {}", key, e);
            throw new MemoryException("Failed to remove long-term memory", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean exists(String key) {
        try {
            boolean exists = repository.existsByKey(key);
            logger.debug("Long-term memory exists check for key: {} = {}", key, exists);
            return exists;
        } catch (Exception e) {
            logger.error("Failed to check if long-term memory exists for key: {}", key, e);
            throw new MemoryException("Failed to check long-term memory existence", e);
        }
    }
    
    @Override
    @Transactional
    public void cleanup() {
        try {
            Instant now = Instant.now();
            
            // Remove expired entries
            int expiredCount = repository.deleteExpiredEntries(now);
            logger.debug("Removed {} expired long-term memory entries", expiredCount);
            
            // Optionally remove low-access entries (implement based on business rules)
            // This is a simple example - in practice, you might want more sophisticated cleanup logic
            List<LongTermMemoryEntity> lowAccessEntries = repository.findLowAccessEntries(1);
            if (lowAccessEntries.size() > 1000) { // Only cleanup if we have too many entries
                // Remove oldest 10% of low-access entries
                int toRemove = Math.min(100, lowAccessEntries.size() / 10);
                for (int i = 0; i < toRemove; i++) {
                    repository.delete(lowAccessEntries.get(i));
                }
                logger.debug("Removed {} low-access long-term memory entries", toRemove);
            }
            
            logger.info("Long-term memory cleanup completed");
            
        } catch (Exception e) {
            logger.error("Error during long-term memory cleanup", e);
            throw new MemoryException("Failed to cleanup long-term memory", e);
        }
    }
}