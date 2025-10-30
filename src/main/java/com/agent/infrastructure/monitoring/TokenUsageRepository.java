package com.agent.infrastructure.monitoring;

import com.agent.domain.model.TokenUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for TokenUsage entities.
 */
@Repository
public interface TokenUsageRepository extends JpaRepository<TokenUsage, Long> {
    
    /**
     * Find token usage records by conversation ID.
     */
    List<TokenUsage> findByConversationId(String conversationId);
    
    /**
     * Find token usage records for a user within a date range.
     */
    @Query("SELECT tu FROM TokenUsage tu WHERE tu.conversationId IN " +
           "(SELECT c.id FROM Conversation c WHERE c.userId = :userId) " +
           "AND tu.timestamp >= :startDate AND tu.timestamp <= :endDate")
    List<TokenUsage> findByUserIdAndDateRange(@Param("userId") String userId,
                                             @Param("startDate") Instant startDate,
                                             @Param("endDate") Instant endDate);
    
    /**
     * Find token usage records by provider ID.
     */
    List<TokenUsage> findByProviderId(String providerId);
    
    /**
     * Find token usage records by model.
     */
    List<TokenUsage> findByModel(String model);
    
    /**
     * Get total token usage for a conversation.
     */
    @Query("SELECT COALESCE(SUM(tu.totalTokens), 0) FROM TokenUsage tu WHERE tu.conversationId = :conversationId")
    Integer getTotalTokensForConversation(@Param("conversationId") String conversationId);
    
    /**
     * Get total token usage for a user within a date range.
     */
    @Query("SELECT COALESCE(SUM(tu.totalTokens), 0) FROM TokenUsage tu WHERE tu.conversationId IN " +
           "(SELECT c.id FROM Conversation c WHERE c.userId = :userId) " +
           "AND tu.timestamp >= :startDate AND tu.timestamp <= :endDate")
    Integer getTotalTokensForUserInDateRange(@Param("userId") String userId,
                                           @Param("startDate") Instant startDate,
                                           @Param("endDate") Instant endDate);
}