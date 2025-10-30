package com.agent.presentation.controller;

import com.agent.infrastructure.monitoring.AlertingService;
import com.agent.infrastructure.monitoring.MetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MonitoringController.class)
@Import({SimpleMeterRegistry.class})
class MonitoringControllerTest {
    
    private MockMvc mockMvc;
    
    @MockBean
    private AlertingService alertingService;
    
    @MockBean
    private MetricsService metricsService;
    
    @MockBean
    private HealthEndpoint healthEndpoint;
    
    private MeterRegistry meterRegistry;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        objectMapper = new ObjectMapper();
        
        MonitoringController controller = new MonitoringController(
                alertingService, metricsService, meterRegistry, healthEndpoint);
        
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }
    
    @Test
    void testGetHealthSummary() throws Exception {
        // Given
        Health health = Health.up()
                .withDetail("database", Health.up().build())
                .withDetail("redis", Health.up().build())
                .build();
        
        when(healthEndpoint.health()).thenReturn(health);
        
        // When & Then
        mockMvc.perform(get("/monitoring/health/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
    
    @Test
    void testGetActiveAlerts() throws Exception {
        // Given
        Map<String, Object> mockAlerts = Map.of(
                "test.alert", Map.of(
                        "severity", "HIGH",
                        "occurrenceCount", 2,
                        "acknowledged", false
                )
        );
        
        when(alertingService.getActiveAlerts()).thenReturn(Map.of());
        
        // When & Then
        mockMvc.perform(get("/monitoring/alerts"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalAlerts").value(0))
                .andExpect(jsonPath("$.alerts").exists());
    }
    
    @Test
    void testAcknowledgeAlert_Success() throws Exception {
        // Given
        String alertId = "test.alert";
        when(alertingService.acknowledgeAlert(alertId)).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/monitoring/alerts/{alertId}/acknowledge", alertId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.alertId").value(alertId))
                .andExpect(jsonPath("$.acknowledged").value(true))
                .andExpect(jsonPath("$.timestamp").exists());
    }
    
    @Test
    void testAcknowledgeAlert_NotFound() throws Exception {
        // Given
        String alertId = "non.existent.alert";
        when(alertingService.acknowledgeAlert(alertId)).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/monitoring/alerts/{alertId}/acknowledge", alertId))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testClearResolvedAlerts() throws Exception {
        // Given
        doNothing().when(alertingService).clearResolvedAlerts();
        
        // When & Then
        mockMvc.perform(post("/monitoring/alerts/clear-resolved"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Resolved alerts cleared"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
    
    @Test
    void testGetMetricsSummary() throws Exception {
        // When & Then
        mockMvc.perform(get("/monitoring/metrics/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.conversations").exists())
                .andExpect(jsonPath("$.llm").exists())
                .andExpect(jsonPath("$.tools").exists())
                .andExpect(jsonPath("$.tokens").exists())
                .andExpect(jsonPath("$.memory").exists());
    }
    
    @Test
    void testTriggerTestAlert_ValidSeverity() throws Exception {
        // Given
        String severity = "HIGH";
        doNothing().when(alertingService).triggerAlert(anyString(), any(), anyString(), any());
        
        // When & Then
        mockMvc.perform(post("/monitoring/alerts/test")
                        .param("severity", severity))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Test alert triggered"))
                .andExpect(jsonPath("$.severity").value(severity));
    }
    
    @Test
    void testTriggerTestAlert_InvalidSeverity() throws Exception {
        // Given
        String severity = "INVALID";
        
        // When & Then
        mockMvc.perform(post("/monitoring/alerts/test")
                        .param("severity", severity))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid severity level. Use: LOW, MEDIUM, HIGH, CRITICAL"));
    }
}