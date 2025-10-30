package com.agent.infrastructure.monitoring;

import com.agent.infrastructure.monitoring.entity.UsageHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UsageHistoryEntity.
 */
@Repository
public interface UsageHistoryRepository extends JpaRepository<UsageHistoryEntity, Long> {
    
    /**
     * Find usage history for a user within a date range.
     */
    List<UsageHistoryEntity> findByUserIdAndDateBetween(String userId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Find usage history for a specific user and date.
     */
    Optional<UsageHistoryEntity> findByUserIdAndDate(String userId, LocalDate date);
    
    /**
     * Find usage history by provider within a date range.
     */
    List<UsageHistoryEntity> findByProviderIdAndDateBetween(String providerId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Get total usage for a user within a date range.
     */
    @Query("SELECT COALESCE(SUM(uh.totalTokens), 0) FROM UsageHistoryEntity uh " +
           "WHERE uh.userId = :userId AND uh.date BETWEEN :startDate AND :endDate")
    Integer getTotalTokensForUserInDateRange(@Param("userId") String userId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
    
    /**
     * Get total cost for a user within a date range.
     */
    @Query("SELECT COALESCE(SUM(uh.totalCost), 0.0) FROM UsageHistoryEntity uh " +
           "WHERE uh.userId = :userId AND uh.date BETWEEN :startDate AND :endDate")
    Double getTotalCostForUserInDateRange(@Param("userId") String userId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
    
    /**
     * Get usage statistics by provider for a user.
     */
    @Query("SELECT uh.providerId, SUM(uh.totalTokens), SUM(uh.totalCost) " +
           "FROM UsageHistoryEntity uh " +
           "WHERE uh.userId = :userId AND uh.date BETWEEN :startDate AND :endDate " +
           "GROUP BY uh.providerId")
    List<Object[]> getUsageByProviderForUser(@Param("userId") String userId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
    
    /**
     * Delete old usage history records (for cleanup).
     */
    void deleteByDateBefore(LocalDate cutoffDate);
}