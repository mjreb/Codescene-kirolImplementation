package com.agent.infrastructure.monitoring;

import com.agent.domain.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating usage analytics and optimization recommendations.
 */
@Service
public class UsageAnalyticsService {
    
    private final TokenUsageRepository tokenUsageRepository;
    private final TokenBudgetRepository tokenBudgetRepository;
    private final TokenCountingService tokenCountingService;
    
    @Autowired
    public UsageAnalyticsService(TokenUsageRepository tokenUsageRepository,
                                TokenBudgetRepository tokenBudgetRepository,
                                TokenCountingService tokenCountingService) {
        this.tokenUsageRepository = tokenUsageRepository;
        this.tokenBudgetRepository = tokenBudgetRepository;
        this.tokenCountingService = tokenCountingService;
    }
    
    /**
     * Generate comprehensive usage report with analytics.
     */
    public UsageReport generateDetailedUsageReport(String userId, DateRange dateRange) {
        UsageReport report = new UsageReport(userId, dateRange);
        
        // Get usage data for the date range
        Instant startInstant = dateRange.getStartDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        Instant endInstant = dateRange.getEndDate().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        List<TokenUsage> usageData = tokenUsageRepository.findByUserIdAndDateRange(
            userId, startInstant, endInstant);
        
        if (usageData.isEmpty()) {
            return report; // Return empty report
        }
        
        // Calculate basic metrics
        calculateBasicMetrics(report, usageData);
        
        // Generate provider and model breakdowns
        generateProviderBreakdown(report, usageData);
        generateModelBreakdown(report, usageData);
        
        // Generate daily breakdown with trends
        generateDailyBreakdown(report, usageData, dateRange);
        
        return report;
    }
    
    /**
     * Generate cost optimization recommendations.
     */
    public List<String> generateOptimizationRecommendations(String userId, DateRange dateRange) {
        List<String> recommendations = new ArrayList<>();
        
        Instant startInstant = dateRange.getStartDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        Instant endInstant = dateRange.getEndDate().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        List<TokenUsage> usageData = tokenUsageRepository.findByUserIdAndDateRange(
            userId, startInstant, endInstant);
        
        if (usageData.isEmpty()) {
            return recommendations;
        }
        
        // Analyze provider costs
        analyzeProviderCosts(recommendations, usageData);
        
        // Analyze usage patterns
        analyzeUsagePatterns(recommendations, usageData);
        
        // Analyze token efficiency
        analyzeTokenEfficiency(recommendations, usageData);
        
        return recommendations;
    }
    
    /**
     * Calculate usage trends and predictions.
     */
    public Map<String, Object> calculateUsageTrends(String userId, DateRange dateRange) {
        Map<String, Object> trends = new HashMap<>();
        
        Instant startInstant = dateRange.getStartDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        Instant endInstant = dateRange.getEndDate().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        List<TokenUsage> usageData = tokenUsageRepository.findByUserIdAndDateRange(
            userId, startInstant, endInstant);
        
        if (usageData.size() < 2) {
            return trends; // Not enough data for trends
        }
        
        // Calculate daily averages
        Map<LocalDate, Integer> dailyUsage = calculateDailyUsage(usageData);
        List<Integer> dailyValues = new ArrayList<>(dailyUsage.values());
        
        // Calculate trend metrics
        double averageDaily = dailyValues.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        int maxDaily = dailyValues.stream().mapToInt(Integer::intValue).max().orElse(0);
        int minDaily = dailyValues.stream().mapToInt(Integer::intValue).min().orElse(0);
        
        trends.put("averageDailyUsage", averageDaily);
        trends.put("maxDailyUsage", maxDaily);
        trends.put("minDailyUsage", minDaily);
        trends.put("usageVariability", calculateVariability(dailyValues));
        
        // Calculate growth rate
        if (dailyValues.size() >= 7) {
            double growthRate = calculateGrowthRate(dailyValues);
            trends.put("weeklyGrowthRate", growthRate);
            
            // Predict next week usage
            double predictedUsage = averageDaily * (1 + growthRate) * 7;
            trends.put("predictedWeeklyUsage", predictedUsage);
        }
        
        return trends;
    }
    
    /**
     * Get usage statistics by time period.
     */
    public Map<String, Object> getUsageStatistics(String userId, DateRange dateRange) {
        Map<String, Object> stats = new HashMap<>();
        
        Instant startInstant = dateRange.getStartDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        Instant endInstant = dateRange.getEndDate().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        List<TokenUsage> usageData = tokenUsageRepository.findByUserIdAndDateRange(
            userId, startInstant, endInstant);
        
        if (usageData.isEmpty()) {
            return stats;
        }
        
        // Basic statistics
        int totalTokens = usageData.stream().mapToInt(TokenUsage::getTotalTokens).sum();
        double totalCost = usageData.stream().mapToDouble(TokenUsage::getEstimatedCost).sum();
        long uniqueConversations = usageData.stream()
            .map(TokenUsage::getConversationId)
            .distinct()
            .count();
        
        stats.put("totalTokens", totalTokens);
        stats.put("totalCost", totalCost);
        stats.put("uniqueConversations", uniqueConversations);
        stats.put("averageTokensPerConversation", totalTokens / (double) uniqueConversations);
        stats.put("averageCostPerConversation", totalCost / uniqueConversations);
        
        // Time-based statistics
        long daysCovered = ChronoUnit.DAYS.between(dateRange.getStartDate(), dateRange.getEndDate()) + 1;
        stats.put("averageTokensPerDay", totalTokens / (double) daysCovered);
        stats.put("averageCostPerDay", totalCost / daysCovered);
        
        return stats;
    }
    
    // Private helper methods
    
    private void calculateBasicMetrics(UsageReport report, List<TokenUsage> usageData) {
        int totalTokens = usageData.stream().mapToInt(TokenUsage::getTotalTokens).sum();
        double totalCost = usageData.stream().mapToDouble(TokenUsage::getEstimatedCost).sum();
        long totalConversations = usageData.stream()
            .map(TokenUsage::getConversationId)
            .distinct()
            .count();
        
        report.setTotalTokens(totalTokens);
        report.setTotalCost(totalCost);
        report.setTotalConversations((int) totalConversations);
    }
    
    private void generateProviderBreakdown(UsageReport report, List<TokenUsage> usageData) {
        Map<String, Integer> tokensByProvider = usageData.stream()
            .collect(Collectors.groupingBy(
                usage -> usage.getProviderId() != null ? usage.getProviderId() : "unknown",
                Collectors.summingInt(TokenUsage::getTotalTokens)
            ));
        report.setTokensByProvider(tokensByProvider);
    }
    
    private void generateModelBreakdown(UsageReport report, List<TokenUsage> usageData) {
        Map<String, Integer> tokensByModel = usageData.stream()
            .collect(Collectors.groupingBy(
                usage -> usage.getModel() != null ? usage.getModel() : "unknown",
                Collectors.summingInt(TokenUsage::getTotalTokens)
            ));
        report.setTokensByModel(tokensByModel);
    }
    
    private void generateDailyBreakdown(UsageReport report, List<TokenUsage> usageData, DateRange dateRange) {
        Map<LocalDate, DailyUsage> dailyMap = new HashMap<>();
        
        // Initialize all days in range
        LocalDate current = dateRange.getStartDate();
        while (!current.isAfter(dateRange.getEndDate())) {
            dailyMap.put(current, new DailyUsage(current, 0, 0, 0.0));
            current = current.plusDays(1);
        }
        
        // Aggregate usage by day
        Map<LocalDate, List<TokenUsage>> usageByDay = usageData.stream()
            .collect(Collectors.groupingBy(usage -> 
                usage.getTimestamp().atZone(ZoneId.systemDefault()).toLocalDate()));
        
        usageByDay.forEach((date, dayUsage) -> {
            DailyUsage daily = dailyMap.get(date);
            if (daily != null) {
                int tokens = dayUsage.stream().mapToInt(TokenUsage::getTotalTokens).sum();
                double cost = dayUsage.stream().mapToDouble(TokenUsage::getEstimatedCost).sum();
                long conversations = dayUsage.stream()
                    .map(TokenUsage::getConversationId)
                    .distinct()
                    .count();
                
                daily.setTokens(tokens);
                daily.setCost(cost);
                daily.setConversations((int) conversations);
            }
        });
        
        List<DailyUsage> dailyBreakdown = dailyMap.values().stream()
            .sorted(Comparator.comparing(DailyUsage::getDate))
            .collect(Collectors.toList());
        
        report.setDailyBreakdown(dailyBreakdown);
    }
    
    private void analyzeProviderCosts(List<String> recommendations, List<TokenUsage> usageData) {
        Map<String, Double> costByProvider = usageData.stream()
            .collect(Collectors.groupingBy(
                usage -> usage.getProviderId() != null ? usage.getProviderId() : "unknown",
                Collectors.summingDouble(TokenUsage::getEstimatedCost)
            ));
        
        if (costByProvider.size() > 1) {
            String mostExpensive = costByProvider.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
            
            String cheapest = costByProvider.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
            
            if (!mostExpensive.equals(cheapest)) {
                double savings = costByProvider.get(mostExpensive) - costByProvider.get(cheapest);
                if (savings > 0.01) { // More than 1 cent difference
                    recommendations.add(String.format(
                        "Consider using %s instead of %s to save approximately $%.2f",
                        cheapest, mostExpensive, savings));
                }
            }
        }
    }
    
    private void analyzeUsagePatterns(List<String> recommendations, List<TokenUsage> usageData) {
        Map<LocalDate, Integer> dailyUsage = calculateDailyUsage(usageData);
        
        if (dailyUsage.size() >= 7) {
            double averageUsage = dailyUsage.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            long highUsageDays = dailyUsage.values().stream()
                .mapToInt(Integer::intValue)
                .filter(usage -> usage > averageUsage * 1.5)
                .count();
            
            if (highUsageDays > dailyUsage.size() * 0.3) {
                recommendations.add("Consider implementing token budgets to control high usage days");
            }
        }
    }
    
    private void analyzeTokenEfficiency(List<String> recommendations, List<TokenUsage> usageData) {
        Map<String, List<TokenUsage>> usageByConversation = usageData.stream()
            .collect(Collectors.groupingBy(TokenUsage::getConversationId));
        
        double averageTokensPerConversation = usageByConversation.values().stream()
            .mapToInt(conversation -> conversation.stream().mapToInt(TokenUsage::getTotalTokens).sum())
            .average()
            .orElse(0.0);
        
        long longConversations = usageByConversation.values().stream()
            .mapToInt(conversation -> conversation.stream().mapToInt(TokenUsage::getTotalTokens).sum())
            .filter(total -> total > averageTokensPerConversation * 2)
            .count();
        
        if (longConversations > usageByConversation.size() * 0.2) {
            recommendations.add("Consider implementing conversation length limits to improve token efficiency");
        }
    }
    
    private Map<LocalDate, Integer> calculateDailyUsage(List<TokenUsage> usageData) {
        return usageData.stream()
            .collect(Collectors.groupingBy(
                usage -> usage.getTimestamp().atZone(ZoneId.systemDefault()).toLocalDate(),
                Collectors.summingInt(TokenUsage::getTotalTokens)
            ));
    }
    
    private double calculateVariability(List<Integer> values) {
        if (values.size() < 2) return 0.0;
        
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = values.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average()
            .orElse(0.0);
        
        return Math.sqrt(variance) / mean; // Coefficient of variation
    }
    
    private double calculateGrowthRate(List<Integer> values) {
        if (values.size() < 2) return 0.0;
        
        int firstHalf = values.subList(0, values.size() / 2).stream().mapToInt(Integer::intValue).sum();
        int secondHalf = values.subList(values.size() / 2, values.size()).stream().mapToInt(Integer::intValue).sum();
        
        if (firstHalf == 0) return 0.0;
        
        return (double) (secondHalf - firstHalf) / firstHalf;
    }
}