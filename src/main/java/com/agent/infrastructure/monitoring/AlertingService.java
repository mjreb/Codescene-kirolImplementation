package com.agent.infrastructure.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for handling alerts and notifications based on system health and metrics.
 */
@Service
public class AlertingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertingService.class);
    private static final Logger alertLogger = LoggerFactory.getLogger("ALERT");
    
    private final Map<String, AlertState> alertStates = new ConcurrentHashMap<>();
    private final AtomicLong alertCounter = new AtomicLong(0);
    
    /**
     * Alert severity levels
     */
    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Alert state tracking
     */
    public static class AlertState {
        private final String alertId;
        private final AlertSeverity severity;
        private final Instant firstOccurrence;
        private Instant lastOccurrence;
        private int occurrenceCount;
        private boolean acknowledged;
        
        public AlertState(String alertId, AlertSeverity severity) {
            this.alertId = alertId;
            this.severity = severity;
            this.firstOccurrence = Instant.now();
            this.lastOccurrence = Instant.now();
            this.occurrenceCount = 1;
            this.acknowledged = false;
        }
        
        public void recordOccurrence() {
            this.lastOccurrence = Instant.now();
            this.occurrenceCount++;
        }
        
        // Getters
        public String getAlertId() { return alertId; }
        public AlertSeverity getSeverity() { return severity; }
        public Instant getFirstOccurrence() { return firstOccurrence; }
        public Instant getLastOccurrence() { return lastOccurrence; }
        public int getOccurrenceCount() { return occurrenceCount; }
        public boolean isAcknowledged() { return acknowledged; }
        public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    }
    
    /**
     * Trigger an alert
     */
    public void triggerAlert(String alertId, AlertSeverity severity, String message, Map<String, Object> context) {
        boolean isNewAlert = !alertStates.containsKey(alertId);
        AlertState alertState = alertStates.computeIfAbsent(alertId, 
                id -> new AlertState(id, severity));
        
        // Only record occurrence if this is not a new alert
        if (!isNewAlert) {
            alertState.recordOccurrence();
        }
        
        // Log the alert
        alertLogger.warn("ALERT [{}] [{}] [{}]: {} - Context: {}", 
                alertCounter.incrementAndGet(),
                alertId, 
                severity, 
                message, 
                context);
        
        // Send notifications based on severity
        sendNotification(alertState, message, context);
    }
    
    /**
     * Handle health check failures
     */
    public void handleHealthCheckFailure(String component, Health health) {
        String alertId = "health.check.failure." + component;
        AlertSeverity severity = determineHealthAlertSeverity(component, health);
        
        String message = String.format("Health check failed for component: %s", component);
        Map<String, Object> context = Map.of(
                "component", component,
                "status", health.getStatus().getCode(),
                "details", health.getDetails()
        );
        
        triggerAlert(alertId, severity, message, context);
    }
    
    /**
     * Handle high error rates
     */
    public void handleHighErrorRate(String operation, double errorRate, double threshold) {
        String alertId = "high.error.rate." + operation;
        AlertSeverity severity = errorRate > threshold * 2 ? AlertSeverity.CRITICAL : AlertSeverity.HIGH;
        
        String message = String.format("High error rate detected for %s: %.2f%% (threshold: %.2f%%)", 
                operation, errorRate * 100, threshold * 100);
        Map<String, Object> context = Map.of(
                "operation", operation,
                "errorRate", errorRate,
                "threshold", threshold
        );
        
        triggerAlert(alertId, severity, message, context);
    }
    
    /**
     * Handle high response times
     */
    public void handleHighResponseTime(String operation, long responseTimeMs, long thresholdMs) {
        String alertId = "high.response.time." + operation;
        AlertSeverity severity = responseTimeMs > thresholdMs * 2 ? AlertSeverity.HIGH : AlertSeverity.MEDIUM;
        
        String message = String.format("High response time detected for %s: %dms (threshold: %dms)", 
                operation, responseTimeMs, thresholdMs);
        Map<String, Object> context = Map.of(
                "operation", operation,
                "responseTime", responseTimeMs,
                "threshold", thresholdMs
        );
        
        triggerAlert(alertId, severity, message, context);
    }
    
    /**
     * Handle token limit exceeded
     */
    public void handleTokenLimitExceeded(String userId, String conversationId, long tokensUsed, long limit) {
        String alertId = "token.limit.exceeded." + userId;
        AlertSeverity severity = AlertSeverity.MEDIUM;
        
        String message = String.format("Token limit exceeded for user %s: %d tokens used (limit: %d)", 
                userId, tokensUsed, limit);
        Map<String, Object> context = Map.of(
                "userId", userId,
                "conversationId", conversationId,
                "tokensUsed", tokensUsed,
                "limit", limit
        );
        
        triggerAlert(alertId, severity, message, context);
    }
    
    /**
     * Handle memory usage alerts
     */
    public void handleHighMemoryUsage(double memoryUsagePercent, double threshold) {
        String alertId = "high.memory.usage";
        AlertSeverity severity = memoryUsagePercent > 90 ? AlertSeverity.CRITICAL : AlertSeverity.HIGH;
        
        String message = String.format("High memory usage detected: %.1f%% (threshold: %.1f%%)", 
                memoryUsagePercent, threshold);
        Map<String, Object> context = Map.of(
                "memoryUsage", memoryUsagePercent,
                "threshold", threshold
        );
        
        triggerAlert(alertId, severity, message, context);
    }
    
    /**
     * Acknowledge an alert
     */
    public boolean acknowledgeAlert(String alertId) {
        AlertState alertState = alertStates.get(alertId);
        if (alertState != null) {
            alertState.setAcknowledged(true);
            alertLogger.info("Alert acknowledged: {}", alertId);
            return true;
        }
        return false;
    }
    
    /**
     * Get all active alerts
     */
    public Map<String, AlertState> getActiveAlerts() {
        return Map.copyOf(alertStates);
    }
    
    /**
     * Clear resolved alerts
     */
    public void clearResolvedAlerts() {
        // In a real implementation, this would check if the underlying issue is resolved
        // For now, we'll clear acknowledged alerts older than 1 hour
        Instant cutoff = Instant.now().minusSeconds(3600);
        
        alertStates.entrySet().removeIf(entry -> {
            AlertState state = entry.getValue();
            return state.isAcknowledged() && state.getLastOccurrence().isBefore(cutoff);
        });
    }
    
    /**
     * Clear all alerts (for testing purposes)
     */
    public void clearAllAlerts() {
        alertStates.clear();
        alertCounter.set(0);
    }
    
    private AlertSeverity determineHealthAlertSeverity(String component, Health health) {
        Status status = health.getStatus();
        
        if (Status.DOWN.equals(status)) {
            // Critical components
            if (component.contains("database") || component.contains("llm")) {
                return AlertSeverity.CRITICAL;
            }
            return AlertSeverity.HIGH;
        } else if (Status.OUT_OF_SERVICE.equals(status)) {
            return AlertSeverity.HIGH;
        } else {
            return AlertSeverity.MEDIUM;
        }
    }
    
    private void sendNotification(AlertState alertState, String message, Map<String, Object> context) {
        // In a real implementation, this would integrate with notification systems
        // like Slack, PagerDuty, email, etc.
        
        switch (alertState.getSeverity()) {
            case CRITICAL:
                logger.error("CRITICAL ALERT: {} - Occurrences: {}", message, alertState.getOccurrenceCount());
                // Send immediate notification to on-call team
                break;
            case HIGH:
                logger.warn("HIGH ALERT: {} - Occurrences: {}", message, alertState.getOccurrenceCount());
                // Send notification to team
                break;
            case MEDIUM:
                logger.warn("MEDIUM ALERT: {} - Occurrences: {}", message, alertState.getOccurrenceCount());
                // Send notification during business hours
                break;
            case LOW:
                logger.info("LOW ALERT: {} - Occurrences: {}", message, alertState.getOccurrenceCount());
                // Log only, no immediate notification
                break;
        }
    }
}