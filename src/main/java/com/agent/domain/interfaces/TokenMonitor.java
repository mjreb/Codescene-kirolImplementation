package com.agent.domain.interfaces;

import com.agent.domain.model.DateRange;
import com.agent.domain.model.TokenBudget;
import com.agent.domain.model.TokenUsage;
import com.agent.domain.model.UsageReport;

/**
 * Interface for tracking and managing token consumption across conversations.
 */
public interface TokenMonitor {
    
    /**
     * Track token usage for a conversation.
     * 
     * @param conversationId The unique identifier for the conversation
     * @param inputTokens The number of input tokens consumed
     * @param outputTokens The number of output tokens generated
     * @return The updated token usage information
     */
    TokenUsage trackTokenUsage(String conversationId, int inputTokens, int outputTokens);
    
    /**
     * Check if a conversation can consume the estimated number of tokens.
     * 
     * @param conversationId The unique identifier for the conversation
     * @param estimatedTokens The estimated number of tokens to be consumed
     * @return true if the tokens can be consumed within limits, false otherwise
     */
    boolean checkTokenLimit(String conversationId, int estimatedTokens);
    
    /**
     * Get the token budget for a specific user.
     * 
     * @param userId The unique identifier for the user
     * @return The user's token budget information
     */
    TokenBudget getTokenBudget(String userId);
    
    /**
     * Generate a usage report for a user within a date range.
     * 
     * @param userId The unique identifier for the user
     * @param dateRange The date range for the report
     * @return The usage report containing analytics and statistics
     */
    UsageReport generateUsageReport(String userId, DateRange dateRange);
}