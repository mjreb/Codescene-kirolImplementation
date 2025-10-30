package com.agent.presentation.controller;

import com.agent.infrastructure.monitoring.AlertingService;
import com.agent.infrastructure.monitoring.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for monitoring and alerting endpoints.
 */
@RestController
@RequestMapping("/monitoring")
public class MonitoringController {
    
    private final AlertingService alertingService;
    private final MetricsService metricsService;
    private final MeterRegistry meterRegistry;
    private final HealthEndpoint healthEndpoint;
    
    public MonitoringController(AlertingService alertingService,
                              MetricsService metricsService,
                              MeterRegistry meterRegistry,
                              HealthEndpoint healthEndpoint) {
        this.alertingService = alertingService;
        this.metricsService = metricsService;
        this.meterRegistry = meterRegistry;
        this.healthEndpoint = healthEndpoint;
    }
    
    /**
     * Get system health summary
     */
    @GetMapping("/health/summary")
    public ResponseEntity<Map<String, Object>> getHealthSummary() {
        var health = healthEndpoint.health();
        
        Map<String, Object> components = Map.of();
        if (health instanceof org.springframework.boot.actuate.health.CompositeHealth compositeHealth) {
            components = compositeHealth.getComponents().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> {
                                var componentHealth = entry.getValue();
                                if (componentHealth instanceof org.springframework.boot.actuate.health.Health healthComponent) {
                                    return Map.of(
                                            "status", healthComponent.getStatus().getCode(),
                                            "details", healthComponent.getDetails()
                                    );
                                }
                                return Map.of("status", componentHealth.getStatus().getCode());
                            }
                    ));
        }
        
        Map<String, Object> summary = Map.of(
                "status", health.getStatus().getCode(),
                "components", components,
                "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Get active alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getActiveAlerts() {
        var alerts = alertingService.getActiveAlerts();
        
        Map<String, Object> response = Map.of(
                "totalAlerts", alerts.size(),
                "alerts", alerts.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> {
                                    var state = entry.getValue();
                                    return Map.of(
                                            "severity", state.getSeverity(),
                                            "firstOccurrence", state.getFirstOccurrence(),
                                            "lastOccurrence", state.getLastOccurrence(),
                                            "occurrenceCount", state.getOccurrenceCount(),
                                            "acknowledged", state.isAcknowledged()
                                    );
                                }
                        ))
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Acknowledge an alert
     */
    @PostMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeAlert(@PathVariable String alertId) {
        boolean acknowledged = alertingService.acknowledgeAlert(alertId);
        
        Map<String, Object> response = Map.of(
                "alertId", alertId,
                "acknowledged", acknowledged,
                "timestamp", System.currentTimeMillis()
        );
        
        return acknowledged ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    /**
     * Clear resolved alerts
     */
    @PostMapping("/alerts/clear-resolved")
    public ResponseEntity<Map<String, String>> clearResolvedAlerts() {
        alertingService.clearResolvedAlerts();
        
        return ResponseEntity.ok(Map.of(
                "message", "Resolved alerts cleared",
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }
    
    /**
     * Get custom metrics summary
     */
    @GetMapping("/metrics/summary")
    public ResponseEntity<Map<String, Object>> getMetricsSummary() {
        Map<String, Object> metrics = Map.of(
                "conversations", Map.of(
                        "started", getCounterValue("agent.conversations.started"),
                        "completed", getCounterValue("agent.conversations.completed"),
                        "failed", getCounterValue("agent.conversations.failed"),
                        "active", getGaugeValue("agent.conversations.active")
                ),
                "llm", Map.of(
                        "requests", getCounterValue("agent.llm.requests"),
                        "averageResponseTime", getTimerMean("agent.llm.response.time")
                ),
                "tools", Map.of(
                        "executions", getCounterValue("agent.tools.executions"),
                        "averageExecutionTime", getTimerMean("agent.tools.execution.time")
                ),
                "tokens", Map.of(
                        "totalUsed", getCounterValue("agent.tokens.used")
                ),
                "memory", Map.of(
                        "usageBytes", getGaugeValue("agent.memory.usage.bytes"),
                        "cacheHitRate", getGaugeValue("agent.cache.hit.rate")
                )
        );
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Trigger a test alert (for testing purposes)
     */
    @PostMapping("/alerts/test")
    public ResponseEntity<Map<String, String>> triggerTestAlert(@RequestParam String severity) {
        AlertingService.AlertSeverity alertSeverity;
        try {
            alertSeverity = AlertingService.AlertSeverity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid severity level. Use: LOW, MEDIUM, HIGH, CRITICAL"
            ));
        }
        
        alertingService.triggerAlert(
                "test.alert." + System.currentTimeMillis(),
                alertSeverity,
                "Test alert triggered via API",
                Map.of("source", "monitoring-controller", "timestamp", System.currentTimeMillis())
        );
        
        return ResponseEntity.ok(Map.of(
                "message", "Test alert triggered",
                "severity", severity
        ));
    }
    
    private double getCounterValue(String meterName) {
        return meterRegistry.find(meterName).counter() != null ? 
               meterRegistry.find(meterName).counter().count() : 0.0;
    }
    
    private double getGaugeValue(String meterName) {
        return meterRegistry.find(meterName).gauge() != null ? 
               meterRegistry.find(meterName).gauge().value() : 0.0;
    }
    
    private double getTimerMean(String meterName) {
        return meterRegistry.find(meterName).timer() != null ? 
               meterRegistry.find(meterName).timer().mean(java.util.concurrent.TimeUnit.MILLISECONDS) : 0.0;
    }
}