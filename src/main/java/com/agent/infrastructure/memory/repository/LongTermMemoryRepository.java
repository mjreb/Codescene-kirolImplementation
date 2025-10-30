package com.agent.infrastructure.memory.repository;

import com.agent.infrastructure.memory.entity.LongTermMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for long-term memory operations.
 */
@Repository
public interface LongTermMemoryRepository extends JpaRepository<LongTermMemoryEntity, Long> {
    
    /**
     * Find memory entry by key.
     */
    Optional<LongTermMemoryEntity> findByKey(String key);
    
    /**
     * Check if memory entry exists by key.
     */
    boolean existsByKey(String key);
    
    /**
     * Delete memory entry by key.
     */
    void deleteByKey(String key);
    
    /**
     * Find all entries by source.
     */
    List<LongTermMemoryEntity> findBySource(String source);
    
    /**
     * Find entries that contain specific tags.
     */
    @Query("SELECT m FROM LongTermMemoryEntity m WHERE m.tagsJson LIKE %:tag%")
    List<LongTermMemoryEntity> findByTagsContaining(@Param("tag") String tag);
    
    /**
     * Find expired entries.
     */
    @Query("SELECT m FROM LongTermMemoryEntity m WHERE m.expiresAt IS NOT NULL AND m.expiresAt < :now")
    List<LongTermMemoryEntity> findExpiredEntries(@Param("now") Instant now);
    
    /**
     * Delete expired entries.
     */
    @Modifying
    @Query("DELETE FROM LongTermMemoryEntity m WHERE m.expiresAt IS NOT NULL AND m.expiresAt < :now")
    int deleteExpiredEntries(@Param("now") Instant now);
    
    /**
     * Find entries with low access count for cleanup.
     */
    @Query("SELECT m FROM LongTermMemoryEntity m WHERE m.accessCount < :threshold ORDER BY m.lastAccessed ASC")
    List<LongTermMemoryEntity> findLowAccessEntries(@Param("threshold") int threshold);
    
    /**
     * Update access count and last accessed time.
     */
    @Modifying
    @Query("UPDATE LongTermMemoryEntity m SET m.accessCount = m.accessCount + 1, m.lastAccessed = :now WHERE m.key = :key")
    int updateAccessInfo(@Param("key") String key, @Param("now") Instant now);
}