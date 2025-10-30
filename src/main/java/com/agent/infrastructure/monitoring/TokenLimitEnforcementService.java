package com.agent.infrastructure.monitoring;

import com.agent.domain.model.TokenBudget;
import com.agent.domain.model.TokenLimits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for enforcing token limits and validating token usage before requests.
 */
@Service
public class TokenLimitEnforcementService {
    
    private final TokenMonitorImpl tokenMonitor;
    private final TokenCountingService tokenCountingService;
    
    @Autowired
    public TokenLimitEnforcementService(TokenMonitorImpl tokenMonitor,
                                       TokenCountingService tokenCountingService) {
        this.tokenMonitor = tokenMonitor;
        this.tokenCountingService = tokenCountingService;
    }
    
    /**
     * Validate if a request can proceed based on token limits.
     * Throws TokenLimitException if limits would be exceeded.
     */
    public void validateTokenLimits(String conversationId, String userId, String text, 
                                   String providerId, String model) {
        // Estimate token count for the request
        int estimatedTokens = tokenCountingService.estimateTokens(text, providerId, model);
        
        // Check various limits
        validateDailyLimit(userId, estimatedTokens);
        validateMonthlyLimit(userId, estimatedTokens);
        validateConversationLimit(conversationId, userId, estimatedTokens);
        validateRequestLimit(estimatedTokens, userId);
    }
    
    /**
     * Pre-validate a request and return estimated token usage.
     */
    public TokenValidationResult preValidateRequest(String conversationId, String userId, 
                                                   String text, String providerId, String model) {
        int estimatedTokens = tokenCountingService.estimateTokens(text, providerId, model);
        double estimatedCost = tokenCountingService.calculateCost(providerId, model, estimatedTokens, 0);
        
        TokenValidationResult result = new TokenValidationResult();
        result.setEstimatedInputTokens(estimatedTokens);
        result.setEstimatedCost(estimatedCost);
        result.setValid(true);
        
        try {
            validateTokenLimits(conversationId, userId, text, providerId, model);
        } catch (TokenLimitException e) {
            result.setValid(false);
            result.setErrorMessage(e.getMessage());
            result.setLimitType(e.getLimitType());
            result.setCurrentUsage(e.getCurrentUsage());
            result.setLimit(e.getLimit());
        }
        
        return result;
    }
    
    /**
     * Check if a user is approaching their token limits (warning threshold).
     */
    public TokenLimitWarning checkWarningThresholds(String userId) {
        TokenBudget budget = tokenMonitor.getTokenBudget(userId);
        TokenLimitWarning warning = new TokenLimitWarning();
        
        if (budget.isUnlimited()) {
            warning.setWarningLevel(TokenLimitWarning.WarningLevel.NONE);
            return warning;
        }
        
        // Check daily limit warning (80% threshold)
        double dailyUsagePercent = (double) budget.getDailyUsed() / budget.getDailyLimit();
        if (dailyUsagePercent >= 0.9) {
            warning.setWarningLevel(TokenLimitWarning.WarningLevel.CRITICAL);
            warning.addWarning("Daily token limit is 90% used (" + budget.getDailyUsed() + "/" + budget.getDailyLimit() + ")");
        } else if (dailyUsagePercent >= 0.8) {
            warning.setWarningLevel(TokenLimitWarning.WarningLevel.HIGH);
            warning.addWarning("Daily token limit is 80% used (" + budget.getDailyUsed() + "/" + budget.getDailyLimit() + ")");
        }
        
        // Check monthly limit warning
        double monthlyUsagePercent = (double) budget.getMonthlyUsed() / budget.getMonthlyLimit();
        if (monthlyUsagePercent >= 0.9) {
            warning.setWarningLevel(TokenLimitWarning.WarningLevel.CRITICAL);
            warning.addWarning("Monthly token limit is 90% used (" + budget.getMonthlyUsed() + "/" + budget.getMonthlyLimit() + ")");
        } else if (monthlyUsagePercent >= 0.8) {
            if (warning.getWarningLevel() == TokenLimitWarning.WarningLevel.NONE) {
                warning.setWarningLevel(TokenLimitWarning.WarningLevel.HIGH);
            }
            warning.addWarning("Monthly token limit is 80% used (" + budget.getMonthlyUsed() + "/" + budget.getMonthlyLimit() + ")");
        }
        
        return warning;
    }
    
    /**
     * Get remaining token allowance for a user.
     */
    public TokenAllowance getRemainingAllowance(String userId) {
        TokenBudget budget = tokenMonitor.getTokenBudget(userId);
        TokenAllowance allowance = new TokenAllowance();
        
        if (budget.isUnlimited()) {
            allowance.setUnlimited(true);
            return allowance;
        }
        
        allowance.setDailyRemaining(Math.max(0, budget.getDailyLimit() - budget.getDailyUsed()));
        allowance.setMonthlyRemaining(Math.max(0, budget.getMonthlyLimit() - budget.getMonthlyUsed()));
        allowance.setDailyUsed(budget.getDailyUsed());
        allowance.setMonthlyUsed(budget.getMonthlyUsed());
        allowance.setDailyLimit(budget.getDailyLimit());
        allowance.setMonthlyLimit(budget.getMonthlyLimit());
        
        return allowance;
    }
    
    // Private validation methods
    
    private void validateDailyLimit(String userId, int estimatedTokens) {
        TokenBudget budget = tokenMonitor.getTokenBudget(userId);
        if (budget.isUnlimited()) {
            return;
        }
        
        if (budget.getDailyUsed() + estimatedTokens > budget.getDailyLimit()) {
            throw new TokenLimitException(
                "Daily token limit would be exceeded. Current usage: " + budget.getDailyUsed() + 
                ", Limit: " + budget.getDailyLimit() + ", Requested: " + estimatedTokens,
                "DAILY",
                budget.getDailyUsed(),
                budget.getDailyLimit(),
                userId,
                null
            );
        }
    }
    
    private void validateMonthlyLimit(String userId, int estimatedTokens) {
        TokenBudget budget = tokenMonitor.getTokenBudget(userId);
        if (budget.isUnlimited()) {
            return;
        }
        
        if (budget.getMonthlyUsed() + estimatedTokens > budget.getMonthlyLimit()) {
            throw new TokenLimitException(
                "Monthly token limit would be exceeded. Current usage: " + budget.getMonthlyUsed() + 
                ", Limit: " + budget.getMonthlyLimit() + ", Requested: " + estimatedTokens,
                "MONTHLY",
                budget.getMonthlyUsed(),
                budget.getMonthlyLimit(),
                userId,
                null
            );
        }
    }
    
    private void validateConversationLimit(String conversationId, String userId, int estimatedTokens) {
        TokenLimits limits = getTokenLimitsForUser(userId);
        if (limits == null) {
            return;
        }
        
        int currentConversationTokens = tokenMonitor.getConversationTokenUsage(conversationId);
        if (currentConversationTokens + estimatedTokens > limits.getMaxTokensPerConversation()) {
            throw new TokenLimitException(
                "Conversation token limit would be exceeded. Current usage: " + currentConversationTokens + 
                ", Limit: " + limits.getMaxTokensPerConversation() + ", Requested: " + estimatedTokens,
                "CONVERSATION",
                currentConversationTokens,
                limits.getMaxTokensPerConversation(),
                userId,
                conversationId
            );
        }
    }
    
    private void validateRequestLimit(int estimatedTokens, String userId) {
        TokenLimits limits = getTokenLimitsForUser(userId);
        if (limits == null) {
            return;
        }
        
        if (estimatedTokens > limits.getMaxTokensPerRequest()) {
            throw new TokenLimitException(
                "Request token limit exceeded. Requested: " + estimatedTokens + 
                ", Limit: " + limits.getMaxTokensPerRequest(),
                "REQUEST",
                estimatedTokens,
                limits.getMaxTokensPerRequest(),
                userId,
                null
            );
        }
    }
    
    private TokenLimits getTokenLimitsForUser(String userId) {
        // This would typically be retrieved from user configuration
        // For now, return default limits
        return new TokenLimits(4000, 50000, 10000);
    }
    
    // Inner classes for result objects
    
    public static class TokenValidationResult {
        private boolean valid;
        private int estimatedInputTokens;
        private double estimatedCost;
        private String errorMessage;
        private String limitType;
        private int currentUsage;
        private int limit;
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public int getEstimatedInputTokens() { return estimatedInputTokens; }
        public void setEstimatedInputTokens(int estimatedInputTokens) { this.estimatedInputTokens = estimatedInputTokens; }
        
        public double getEstimatedCost() { return estimatedCost; }
        public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getLimitType() { return limitType; }
        public void setLimitType(String limitType) { this.limitType = limitType; }
        
        public int getCurrentUsage() { return currentUsage; }
        public void setCurrentUsage(int currentUsage) { this.currentUsage = currentUsage; }
        
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }
    
    public static class TokenLimitWarning {
        public enum WarningLevel { NONE, LOW, HIGH, CRITICAL }
        
        private WarningLevel warningLevel = WarningLevel.NONE;
        private java.util.List<String> warnings = new java.util.ArrayList<>();
        
        public WarningLevel getWarningLevel() { return warningLevel; }
        public void setWarningLevel(WarningLevel warningLevel) { this.warningLevel = warningLevel; }
        
        public java.util.List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
    }
    
    public static class TokenAllowance {
        private boolean unlimited = false;
        private int dailyRemaining;
        private int monthlyRemaining;
        private int dailyUsed;
        private int monthlyUsed;
        private int dailyLimit;
        private int monthlyLimit;
        
        // Getters and setters
        public boolean isUnlimited() { return unlimited; }
        public void setUnlimited(boolean unlimited) { this.unlimited = unlimited; }
        
        public int getDailyRemaining() { return dailyRemaining; }
        public void setDailyRemaining(int dailyRemaining) { this.dailyRemaining = dailyRemaining; }
        
        public int getMonthlyRemaining() { return monthlyRemaining; }
        public void setMonthlyRemaining(int monthlyRemaining) { this.monthlyRemaining = monthlyRemaining; }
        
        public int getDailyUsed() { return dailyUsed; }
        public void setDailyUsed(int dailyUsed) { this.dailyUsed = dailyUsed; }
        
        public int getMonthlyUsed() { return monthlyUsed; }
        public void setMonthlyUsed(int monthlyUsed) { this.monthlyUsed = monthlyUsed; }
        
        public int getDailyLimit() { return dailyLimit; }
        public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }
        
        public int getMonthlyLimit() { return monthlyLimit; }
        public void setMonthlyLimit(int monthlyLimit) { this.monthlyLimit = monthlyLimit; }
    }
}