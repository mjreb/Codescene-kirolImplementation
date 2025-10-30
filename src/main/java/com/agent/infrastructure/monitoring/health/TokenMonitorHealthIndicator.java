package com.agent.infrastructure.monitoring.health;

import com.agent.domain.interfaces.TokenMonitor;
import com.agent.domain.model.DateRange;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Health indicator for token monitoring system.
 */
@Component
public class TokenMonitorHealthIndicator implements HealthIndicator {
    
    private final TokenMonitor tokenMonitor;
    
    public TokenMonitorHealthIndicator(TokenMonitor tokenMonitor) {
        this.tokenMonitor = tokenMonitor;
    }
    
    @Override
    public Health health() {
        try {
            // Test token monitoring functionality
            String testConversationId = "health-check-" + System.currentTimeMillis();
            
            long startTime = System.currentTimeMillis();
            
            // Test token usage tracking
            var tokenUsage = tokenMonitor.trackTokenUsage(testConversationId, 10, 20);
            
            // Test token limit checking
            boolean limitCheck = tokenMonitor.checkTokenLimit(testConversationId, 100);
            
            // Test usage report generation
            DateRange dateRange = new DateRange(
                    LocalDate.now().minusDays(1),
                    LocalDate.now()
            );
            var usageReport = tokenMonitor.generateUsageReport("health-check-user", dateRange);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (tokenUsage != null && usageReport != null) {
                return Health.up()
                        .withDetail("tokenMonitor", Map.of(
                                "status", "UP",
                                "responseTime", responseTime + "ms",
                                "operations", "tracking/limits/reporting successful",
                                "limitCheckResult", limitCheck
                        ))
                        .build();
            } else {
                return Health.down()
                        .withDetail("tokenMonitor", Map.of(
                                "status", "DOWN",
                                "reason", "Token monitoring operations failed"
                        ))
                        .build();
            }
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("tokenMonitor", Map.of(
                            "status", "DOWN",
                            "error", e.getMessage()
                    ))
                    .withException(e)
                    .build();
        }
    }
}