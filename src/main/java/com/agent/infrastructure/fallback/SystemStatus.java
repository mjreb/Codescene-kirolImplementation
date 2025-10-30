package com.agent.infrastructure.fallback;

import java.time.Instant;

/**
 * System status information.
 */
public class SystemStatus {
    private final boolean llmProvidersOperational;
    private final boolean externalServicesOperational;
    private final boolean databaseOperational;
    private final boolean toolsOperational;
    private final Instant lastUpdated;
    
    public SystemStatus(boolean llmProvidersOperational, 
                       boolean externalServicesOperational, 
                       boolean databaseOperational, 
                       boolean toolsOperational, 
                       Instant lastUpdated) {
        this.llmProvidersOperational = llmProvidersOperational;
        this.externalServicesOperational = externalServicesOperational;
        this.databaseOperational = databaseOperational;
        this.toolsOperational = toolsOperational;
        this.lastUpdated = lastUpdated;
    }
    
    public boolean isLLMProvidersOperational() { return llmProvidersOperational; }
    public boolean isExternalServicesOperational() { return externalServicesOperational; }
    public boolean isDatabaseOperational() { return databaseOperational; }
    public boolean isToolsOperational() { return toolsOperational; }
    public Instant getLastUpdated() { return lastUpdated; }
    
    public boolean isFullyOperational() {
        return llmProvidersOperational && externalServicesOperational && 
               databaseOperational && toolsOperational;
    }
    
    public boolean isLLMProvidersDown() { return !llmProvidersOperational; }
    public boolean isToolsDown() { return !toolsOperational; }
    public boolean isMemoryIssues() { return !databaseOperational; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SystemStatus that = (SystemStatus) obj;
        return llmProvidersOperational == that.llmProvidersOperational &&
               externalServicesOperational == that.externalServicesOperational &&
               databaseOperational == that.databaseOperational &&
               toolsOperational == that.toolsOperational;
    }
    
    @Override
    public String toString() {
        return String.format("SystemStatus{llm=%s, external=%s, db=%s, tools=%s, updated=%s}",
                llmProvidersOperational, externalServicesOperational, 
                databaseOperational, toolsOperational, lastUpdated);
    }
}