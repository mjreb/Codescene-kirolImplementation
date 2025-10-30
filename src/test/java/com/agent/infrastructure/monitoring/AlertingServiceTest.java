package com.agent.infrastructure.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlertingServiceTest {
    
    private AlertingService alertingService;
    
    @BeforeEach
    void setUp() {
        alertingService = new AlertingService();
        // Clear any existing alerts from previous tests
        alertingService.clearAllAlerts();
    }
    
    @Test
    void testTriggerAlert() {
        // Given
        String alertId = "test.alert";
        AlertingService.AlertSeverity severity = AlertingService.AlertSeverity.HIGH;
        String message = "Test alert message";
        Map<String, Object> context = Map.of("key", "value");
        
        // When
        alertingService.triggerAlert(alertId, severity, message, context);
        
        // Then
        var activeAlerts = alertingService.getActiveAlerts();
        assertTrue(activeAlerts.containsKey(alertId));
        
        var alertState = activeAlerts.get(alertId);
        assertEquals(severity, alertState.getSeverity());
        assertEquals(1, alertState.getOccurrenceCount());
        assertFalse(alertState.isAcknowledged());
    }
    
    @Test
    void testTriggerAlert_MultipleOccurrences() {
        // Given
        String alertId = "test.alert";
        AlertingService.AlertSeverity severity = AlertingService.AlertSeverity.MEDIUM;
        String message = "Test alert message";
        Map<String, Object> context = Map.of("key", "value");
        
        // When
        alertingService.triggerAlert(alertId, severity, message, context);
        alertingService.triggerAlert(alertId, severity, message, context);
        alertingService.triggerAlert(alertId, severity, message, context);
        
        // Then
        var activeAlerts = alertingService.getActiveAlerts();
        var alertState = activeAlerts.get(alertId);
        assertEquals(3, alertState.getOccurrenceCount());
    }
    
    @Test
    void testHandleHealthCheckFailure() {
        // Given
        String component = "database";
        Health health = Health.down()
                .withDetail("error", "Connection failed")
                .build();
        
        // When
        alertingService.handleHealthCheckFailure(component, health);
        
        // Then
        var activeAlerts = alertingService.getActiveAlerts();
        String expectedAlertId = "health.check.failure." + component;
        assertTrue(activeAlerts.containsKey(expectedAlertId));
        
        var alertState = activeAlerts.get(expectedAlertId);
        assertEquals(AlertingService.AlertSeverity.CRITICAL, alertState.getSeverity());
    }
    
    @Test
    void testHandleHighErrorRate() {
        // Given
        String operation = "conversation.processing";
        double errorRate = 0.15; // 15%
        double threshold = 0.05; // 5%
        
        // When
        alertingService.handleHighErrorRate(operation, errorRate, threshold);
        
        // Then
        var activeAlerts = alertingService.getActiveAlerts();
        String expectedAlertId = "high.error.rate." + operation;
        assertTrue(activeAlerts.containsKey(expectedAlertId));
        
        var alertState = activeAlerts.get(expectedAlertId);
        assertEquals(AlertingService.AlertSeverity.CRITICAL, alertState.getSeverity());
    }
    
    @Test
    void testHandleHighResponseTime() {
        // Given
        String operation = "llm.request";
        long responseTimeMs = 8000;
        long thresholdMs = 3000;
        
        // When
        alertingService.handleHighResponseTime(operation, responseTimeMs, thresholdMs);
        
        // Then
        var activeAlerts = alertingService.getActiveAlerts();
        String expectedAlertId = "high.response.time." + operation;
        assertTrue(activeAlerts.containsKey(expectedAlertId));
        
        var alertState = activeAlerts.get(expectedAlertId);
        assertEquals(AlertingService.AlertSeverity.HIGH, alertState.getSeverity());
    }
    
    @Test
    void testHandleTokenLimitExceeded() {
        // Given
        String userId = "user123";
        String conversationId = "conv456";
        long tokensUsed = 5000;
        long limit = 4000;
        
        // When
        alertingService.handleTokenLimitExceeded(userId, conversationId, tokensUsed, limit);
        
        // Then
        var activeAlerts = alertingService.getActiveAlerts();
        String expectedAlertId = "token.limit.exceeded." + userId;
        assertTrue(activeAlerts.containsKey(expectedAlertId));
        
        var alertState = activeAlerts.get(expectedAlertId);
        assertEquals(AlertingService.AlertSeverity.MEDIUM, alertState.getSeverity());
    }
    
    @Test
    void testHandleHighMemoryUsage() {
        // Given
        double memoryUsagePercent = 92.5;
        double threshold = 85.0;
        
        // When
        alertingService.handleHighMemoryUsage(memoryUsagePercent, threshold);
        
        // Then
        var activeAlerts = alertingService.getActiveAlerts();
        String expectedAlertId = "high.memory.usage";
        assertTrue(activeAlerts.containsKey(expectedAlertId));
        
        var alertState = activeAlerts.get(expectedAlertId);
        assertEquals(AlertingService.AlertSeverity.CRITICAL, alertState.getSeverity());
    }
    
    @Test
    void testAcknowledgeAlert() {
        // Given
        String alertId = "test.alert";
        alertingService.triggerAlert(alertId, AlertingService.AlertSeverity.LOW, "Test", Map.of());
        
        // When
        boolean acknowledged = alertingService.acknowledgeAlert(alertId);
        
        // Then
        assertTrue(acknowledged);
        
        var activeAlerts = alertingService.getActiveAlerts();
        var alertState = activeAlerts.get(alertId);
        assertTrue(alertState.isAcknowledged());
    }
    
    @Test
    void testAcknowledgeAlert_NonExistent() {
        // When
        boolean acknowledged = alertingService.acknowledgeAlert("non.existent.alert");
        
        // Then
        assertFalse(acknowledged);
    }
    
    @Test
    void testClearResolvedAlerts() {
        // Given
        String alertId = "test.alert";
        alertingService.triggerAlert(alertId, AlertingService.AlertSeverity.LOW, "Test", Map.of());
        alertingService.acknowledgeAlert(alertId);
        
        // When
        alertingService.clearResolvedAlerts();
        
        // Then
        // Note: In the current implementation, alerts are only cleared if they're older than 1 hour
        // For this test, we just verify the method doesn't throw an exception
        var activeAlerts = alertingService.getActiveAlerts();
        // The alert should still be there since it's not old enough
        assertTrue(activeAlerts.containsKey(alertId));
    }
    
    @Test
    void testGetActiveAlerts() {
        // Given
        alertingService.triggerAlert("alert1", AlertingService.AlertSeverity.HIGH, "Test 1", Map.of());
        alertingService.triggerAlert("alert2", AlertingService.AlertSeverity.MEDIUM, "Test 2", Map.of());
        
        // When
        var activeAlerts = alertingService.getActiveAlerts();
        
        // Then
        assertEquals(2, activeAlerts.size());
        assertTrue(activeAlerts.containsKey("alert1"));
        assertTrue(activeAlerts.containsKey("alert2"));
    }
}