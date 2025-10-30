package com.agent.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Represents token budget allocation and limits for a user.
 */
@Entity
@Table(name = "token_budget")
public class TokenBudget {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;
    
    @Column(name = "daily_limit", nullable = false)
    private int dailyLimit;
    
    @Column(name = "monthly_limit", nullable = false)
    private int monthlyLimit;
    
    @Column(name = "daily_used", nullable = false)
    private int dailyUsed;
    
    @Column(name = "monthly_used", nullable = false)
    private int monthlyUsed;
    
    @Column(name = "reset_date")
    private Instant resetDate;
    
    @Column(name = "unlimited", nullable = false)
    private boolean unlimited;
    
    public TokenBudget() {}
    
    public TokenBudget(String userId, int dailyLimit, int monthlyLimit) {
        this.userId = userId;
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
        this.dailyUsed = 0;
        this.monthlyUsed = 0;
        this.unlimited = false;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public int getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }
    
    public int getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(int monthlyLimit) { this.monthlyLimit = monthlyLimit; }
    
    public int getDailyUsed() { return dailyUsed; }
    public void setDailyUsed(int dailyUsed) { this.dailyUsed = dailyUsed; }
    
    public int getMonthlyUsed() { return monthlyUsed; }
    public void setMonthlyUsed(int monthlyUsed) { this.monthlyUsed = monthlyUsed; }
    
    public Instant getResetDate() { return resetDate; }
    public void setResetDate(Instant resetDate) { this.resetDate = resetDate; }
    
    public boolean isUnlimited() { return unlimited; }
    public void setUnlimited(boolean unlimited) { this.unlimited = unlimited; }
}