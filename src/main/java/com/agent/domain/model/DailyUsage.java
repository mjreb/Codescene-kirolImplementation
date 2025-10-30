package com.agent.domain.model;

import java.time.LocalDate;

/**
 * Represents daily usage statistics.
 */
public class DailyUsage {
    private LocalDate date;
    private int tokens;
    private int conversations;
    private double cost;
    
    public DailyUsage() {}
    
    public DailyUsage(LocalDate date, int tokens, int conversations, double cost) {
        this.date = date;
        this.tokens = tokens;
        this.conversations = conversations;
        this.cost = cost;
    }
    
    // Getters and setters
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public int getTokens() { return tokens; }
    public void setTokens(int tokens) { this.tokens = tokens; }
    
    public int getConversations() { return conversations; }
    public void setConversations(int conversations) { this.conversations = conversations; }
    
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
}