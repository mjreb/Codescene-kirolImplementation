package com.agent.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a usage report containing analytics and statistics.
 */
public class UsageReport {
    private String userId;
    private DateRange dateRange;
    private int totalTokens;
    private int totalConversations;
    private double totalCost;
    private Map<String, Integer> tokensByProvider;
    private Map<String, Integer> tokensByModel;
    private List<DailyUsage> dailyBreakdown;
    private Instant generatedAt;
    
    public UsageReport() {
        this.generatedAt = Instant.now();
    }
    
    public UsageReport(String userId, DateRange dateRange) {
        this();
        this.userId = userId;
        this.dateRange = dateRange;
    }
    
    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public DateRange getDateRange() { return dateRange; }
    public void setDateRange(DateRange dateRange) { this.dateRange = dateRange; }
    
    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
    
    public int getTotalConversations() { return totalConversations; }
    public void setTotalConversations(int totalConversations) { this.totalConversations = totalConversations; }
    
    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
    
    public Map<String, Integer> getTokensByProvider() { return tokensByProvider; }
    public void setTokensByProvider(Map<String, Integer> tokensByProvider) { this.tokensByProvider = tokensByProvider; }
    
    public Map<String, Integer> getTokensByModel() { return tokensByModel; }
    public void setTokensByModel(Map<String, Integer> tokensByModel) { this.tokensByModel = tokensByModel; }
    
    public List<DailyUsage> getDailyBreakdown() { return dailyBreakdown; }
    public void setDailyBreakdown(List<DailyUsage> dailyBreakdown) { this.dailyBreakdown = dailyBreakdown; }
    
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}