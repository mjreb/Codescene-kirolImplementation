package com.agent.infrastructure.monitoring;

import com.agent.domain.interfaces.TokenMonitor;
import com.agent.domain.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of TokenMonitor that provides real-time token tracking,
 * budget enforcement, and usage analytics.
 */
@Service
public class TokenMonitorImpl implements TokenMonitor {
    
    private final TokenUsageRepository tokenUsageRepository;
    private final TokenBudgetRepository tokenBudgetRepository;
    private final TokenCountingService tokenCountingService;
    
    // In-memory cache for conversation token usage
    private final Map<String, Integer> conversationTokenCache = new ConcurrentHashMap<>();
    
    @Autowired
    public TokenMonitorImpl(TokenUsageRepository tokenUsageRepository,
                           TokenBudgetRepository tokenBudgetRepository,
                           TokenCountingService tokenCountingService) {
        this.tokenUsageRepository = tokenUsageRepository;
        this.tokenBudgetRepository = tokenBudgetRepository;
        this.tokenCountingService = tokenCountingService;
    }
    
    @Override
    public TokenUsage trackTokenUsage(String conversationId, int inputTokens, int outputTokens) {
        TokenUsage usage = new TokenUsage(conversationId, inputTokens, outputTokens);
        
        // Set provider and model information if available
        // This would typically come from the LLM request context
        usage.setProviderId(getCurrentProviderId(conversationId));
        usage.setModel(getCurrentModel(conversationId));
        
        // Calculate estimated cost
        double cost = tokenCountingService.calculateCost(usage.getProviderId(), 
                                                        usage.getModel(), 
                                                        inputTokens, 
                                                        outputTokens);
        usage.setEstimatedCost(cost);
        
        // Update conversation token cache
        conversationTokenCache.merge(conversationId, usage.getTotalTokens(), Integer::sum);
        
        // Persist usage record
        tokenUsageRepository.save(usage);
        
        // Update user's token budget
        String userId = getUserIdFromConversation(conversationId);
        if (userId != null) {
            updateTokenBudget(userId, usage.getTotalTokens());
        }
        
        return usage;
    }
    
    @Override
    public boolean checkTokenLimit(String conversationId, int estimatedTokens) {
        String userId = getUserIdFromConversation(conversationId);
        if (userId == null) {
            return false; // Cannot validate without user context
        }
        
        try {
            TokenBudget budget = getTokenBudget(userId);
            if (budget.isUnlimited()) {
                return true;
            }
            
            // Check daily limit
            if (budget.getDailyUsed() + estimatedTokens > budget.getDailyLimit()) {
                return false;
            }
            
            // Check monthly limit
            if (budget.getMonthlyUsed() + estimatedTokens > budget.getMonthlyLimit()) {
                return false;
            }
            
            // Check conversation-level limit if configured
            int conversationTokens = conversationTokenCache.getOrDefault(conversationId, 0);
            TokenLimits limits = getTokenLimitsForUser(userId);
            if (limits != null && conversationTokens + estimatedTokens > limits.getMaxTokensPerConversation()) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            // Log error and return false for safety
            return false;
        }
    }
    
    /**
     * Enhanced token limit checking with detailed validation.
     */
    public boolean checkTokenLimitWithValidation(String conversationId, String userId, 
                                               String text, String providerId, String model) {
        try {
            int estimatedTokens = tokenCountingService.estimateTokens(text, providerId, model);
            return checkTokenLimit(conversationId, estimatedTokens);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public TokenBudget getTokenBudget(String userId) {
        Optional<TokenBudget> budget = tokenBudgetRepository.findByUserId(userId);
        if (budget.isPresent()) {
            TokenBudget userBudget = budget.get();
            // Check if budget needs to be reset (daily/monthly)
            resetBudgetIfNeeded(userBudget);
            return userBudget;
        } else {
            // Create default budget for new user
            return createDefaultTokenBudget(userId);
        }
    }
    
    @Override
    public UsageReport generateUsageReport(String userId, DateRange dateRange) {
        UsageReport report = new UsageReport(userId, dateRange);
        
        // Get usage data for the date range
        Instant startInstant = dateRange.getStartDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        Instant endInstant = dateRange.getEndDate().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        List<TokenUsage> usageData = tokenUsageRepository.findByUserIdAndDateRange(
            userId, startInstant, endInstant);
        
        // Calculate totals
        int totalTokens = usageData.stream().mapToInt(TokenUsage::getTotalTokens).sum();
        double totalCost = usageData.stream().mapToDouble(TokenUsage::getEstimatedCost).sum();
        long totalConversations = usageData.stream()
            .map(TokenUsage::getConversationId)
            .distinct()
            .count();
        
        report.setTotalTokens(totalTokens);
        report.setTotalCost(totalCost);
        report.setTotalConversations((int) totalConversations);
        
        // Group by provider
        Map<String, Integer> tokensByProvider = new HashMap<>();
        usageData.forEach(usage -> {
            String provider = usage.getProviderId() != null ? usage.getProviderId() : "unknown";
            tokensByProvider.merge(provider, usage.getTotalTokens(), Integer::sum);
        });
        report.setTokensByProvider(tokensByProvider);
        
        // Group by model
        Map<String, Integer> tokensByModel = new HashMap<>();
        usageData.forEach(usage -> {
            String model = usage.getModel() != null ? usage.getModel() : "unknown";
            tokensByModel.merge(model, usage.getTotalTokens(), Integer::sum);
        });
        report.setTokensByModel(tokensByModel);
        
        // Generate daily breakdown
        List<DailyUsage> dailyBreakdown = generateDailyBreakdown(usageData, dateRange);
        report.setDailyBreakdown(dailyBreakdown);
        
        return report;
    }
    
    /**
     * Estimate token count for a given text using the specified provider and model.
     */
    public int estimateTokenCount(String text, String providerId, String model) {
        return tokenCountingService.estimateTokens(text, providerId, model);
    }
    
    /**
     * Get current conversation token usage from cache.
     */
    public int getConversationTokenUsage(String conversationId) {
        return conversationTokenCache.getOrDefault(conversationId, 0);
    }
    
    /**
     * Clear conversation from token cache (when conversation ends).
     */
    public void clearConversationCache(String conversationId) {
        conversationTokenCache.remove(conversationId);
    }
    
    // Private helper methods
    
    private void updateTokenBudget(String userId, int tokensUsed) {
        TokenBudget budget = getTokenBudget(userId);
        budget.setDailyUsed(budget.getDailyUsed() + tokensUsed);
        budget.setMonthlyUsed(budget.getMonthlyUsed() + tokensUsed);
        tokenBudgetRepository.save(budget);
    }
    
    private void resetBudgetIfNeeded(TokenBudget budget) {
        Instant now = Instant.now();
        LocalDate today = now.atZone(ZoneId.systemDefault()).toLocalDate();
        
        if (budget.getResetDate() == null || 
            budget.getResetDate().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(today)) {
            
            // Reset daily usage
            budget.setDailyUsed(0);
            
            // Reset monthly usage if it's a new month
            LocalDate resetDate = budget.getResetDate() != null ? 
                budget.getResetDate().atZone(ZoneId.systemDefault()).toLocalDate() : 
                today.minusDays(1);
            
            if (resetDate.getMonth() != today.getMonth() || resetDate.getYear() != today.getYear()) {
                budget.setMonthlyUsed(0);
            }
            
            budget.setResetDate(now);
            tokenBudgetRepository.save(budget);
        }
    }
    
    private TokenBudget createDefaultTokenBudget(String userId) {
        // Default limits: 10,000 tokens per day, 100,000 per month
        TokenBudget budget = new TokenBudget(userId, 10000, 100000);
        budget.setResetDate(Instant.now());
        return tokenBudgetRepository.save(budget);
    }
    
    private List<DailyUsage> generateDailyBreakdown(List<TokenUsage> usageData, DateRange dateRange) {
        Map<LocalDate, DailyUsage> dailyMap = new HashMap<>();
        
        // Initialize all days in range with zero usage
        LocalDate current = dateRange.getStartDate();
        while (!current.isAfter(dateRange.getEndDate())) {
            dailyMap.put(current, new DailyUsage(current, 0, 0, 0.0));
            current = current.plusDays(1);
        }
        
        // Aggregate usage by day
        usageData.forEach(usage -> {
            LocalDate date = usage.getTimestamp().atZone(ZoneId.systemDefault()).toLocalDate();
            DailyUsage daily = dailyMap.get(date);
            if (daily != null) {
                daily.setTokens(daily.getTokens() + usage.getTotalTokens());
                daily.setConversations(daily.getConversations() + 1);
                daily.setCost(daily.getCost() + usage.getEstimatedCost());
            }
        });
        
        return new ArrayList<>(dailyMap.values());
    }
    
    private String getCurrentProviderId(String conversationId) {
        // This would typically be retrieved from conversation context
        // For now, return a placeholder
        return "openai"; // Default provider
    }
    
    private String getCurrentModel(String conversationId) {
        // This would typically be retrieved from conversation context
        // For now, return a placeholder
        return "gpt-3.5-turbo"; // Default model
    }
    
    private String getUserIdFromConversation(String conversationId) {
        // This would typically be retrieved from conversation context
        // For now, return a placeholder - in real implementation, 
        // this would query the conversation repository
        return "default-user";
    }
    
    private TokenLimits getTokenLimitsForUser(String userId) {
        // This would typically be retrieved from user configuration
        // For now, return default limits
        return new TokenLimits(4000, 50000, 10000);
    }
}