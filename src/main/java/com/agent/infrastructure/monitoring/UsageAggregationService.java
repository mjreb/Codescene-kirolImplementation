package com.agent.infrastructure.monitoring;

import com.agent.domain.model.TokenUsage;
import com.agent.infrastructure.monitoring.entity.UsageHistoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for aggregating token usage data into daily summaries for analytics.
 */
@Service
public class UsageAggregationService {
    
    private final TokenUsageRepository tokenUsageRepository;
    private final UsageHistoryRepository usageHistoryRepository;
    
    @Autowired
    public UsageAggregationService(TokenUsageRepository tokenUsageRepository,
                                  UsageHistoryRepository usageHistoryRepository) {
        this.tokenUsageRepository = tokenUsageRepository;
        this.usageHistoryRepository = usageHistoryRepository;
    }
    
    /**
     * Aggregate yesterday's usage data into daily summaries.
     * Runs daily at 1 AM.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void aggregateDailyUsage() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        aggregateUsageForDate(yesterday);
    }
    
    /**
     * Aggregate usage data for a specific date.
     */
    @Transactional
    public void aggregateUsageForDate(LocalDate date) {
        Instant startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        // Get all token usage records for the date
        List<TokenUsage> dailyUsage = tokenUsageRepository.findByUserIdAndDateRange(
            null, startOfDay, endOfDay); // This query needs to be modified to get all users
        
        if (dailyUsage.isEmpty()) {
            return;
        }
        
        // Group by user, provider, and model
        Map<String, Map<String, Map<String, List<TokenUsage>>>> groupedUsage = dailyUsage.stream()
            .collect(Collectors.groupingBy(
                this::getUserIdFromUsage,
                Collectors.groupingBy(
                    usage -> usage.getProviderId() != null ? usage.getProviderId() : "unknown",
                    Collectors.groupingBy(
                        usage -> usage.getModel() != null ? usage.getModel() : "unknown"
                    )
                )
            ));
        
        // Create aggregated records
        groupedUsage.forEach((userId, providerMap) -> {
            providerMap.forEach((providerId, modelMap) -> {
                modelMap.forEach((model, usageList) -> {
                    createOrUpdateUsageHistory(userId, date, providerId, model, usageList);
                });
            });
        });
    }
    
    /**
     * Clean up old usage history records.
     * Runs monthly on the 1st at 2 AM.
     */
    @Scheduled(cron = "0 0 2 1 * *")
    @Transactional
    public void cleanupOldUsageHistory() {
        // Keep 2 years of history
        LocalDate cutoffDate = LocalDate.now().minusYears(2);
        usageHistoryRepository.deleteByDateBefore(cutoffDate);
    }
    
    /**
     * Manually trigger aggregation for a date range (for backfilling).
     */
    @Transactional
    public void aggregateUsageForDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            aggregateUsageForDate(current);
            current = current.plusDays(1);
        }
    }
    
    // Private helper methods
    
    private void createOrUpdateUsageHistory(String userId, LocalDate date, String providerId, 
                                          String model, List<TokenUsage> usageList) {
        
        // Calculate aggregated values
        int totalTokens = usageList.stream().mapToInt(TokenUsage::getTotalTokens).sum();
        double totalCost = usageList.stream().mapToDouble(TokenUsage::getEstimatedCost).sum();
        long totalConversations = usageList.stream()
            .map(TokenUsage::getConversationId)
            .distinct()
            .count();
        
        // Check if record already exists
        UsageHistoryEntity existingRecord = usageHistoryRepository
            .findByUserIdAndDate(userId, date)
            .orElse(null);
        
        if (existingRecord != null) {
            // Update existing record
            existingRecord.setTotalTokens(existingRecord.getTotalTokens() + totalTokens);
            existingRecord.setTotalConversations(existingRecord.getTotalConversations() + (int) totalConversations);
            existingRecord.setTotalCost(existingRecord.getTotalCost() + totalCost);
            existingRecord.setUpdatedAt(Instant.now());
            usageHistoryRepository.save(existingRecord);
        } else {
            // Create new record
            UsageHistoryEntity newRecord = new UsageHistoryEntity(
                userId, date, totalTokens, (int) totalConversations, totalCost);
            newRecord.setProviderId(providerId);
            newRecord.setModel(model);
            usageHistoryRepository.save(newRecord);
        }
    }
    
    private String getUserIdFromUsage(TokenUsage usage) {
        // This would typically be retrieved from conversation context
        // For now, return a placeholder - in real implementation,
        // this would need to join with conversation table
        return "default-user";
    }
}