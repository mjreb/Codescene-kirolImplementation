package com.agent.domain.model;

import java.time.LocalDate;

/**
 * Represents a date range for reporting and filtering.
 */
public class DateRange {
    private LocalDate startDate;
    private LocalDate endDate;
    
    public DateRange() {}
    
    public DateRange(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Getters and setters
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}