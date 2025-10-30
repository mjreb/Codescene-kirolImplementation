package com.agent.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class MetricsServiceTest {
    
    private MeterRegistry meterRegistry;
    private MetricsService metricsService;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }
    
    @Test
    void testRecordConversationStarted() {
        // When
        metricsService.recordConversationStarted();
        
        // Then
        Counter counter = meterRegistry.find("agent.conversations.started").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
        
        Gauge activeGauge = meterRegistry.find("agent.conversations.active").gauge();
        assertNotNull(activeGauge);
        assertEquals(1.0, activeGauge.value());
    }
    
    @Test
    void testRecordConversationCompleted() {
        // Given
        metricsService.recordConversationStarted(); // Start a conversation first
        Duration duration = Duration.ofSeconds(30);
        
        // When
        metricsService.recordConversationCompleted(duration);
        
        // Then
        Counter completedCounter = meterRegistry.find("agent.conversations.completed").counter();
        assertNotNull(completedCounter);
        assertEquals(1.0, completedCounter.count());
        
        Timer durationTimer = meterRegistry.find("agent.conversations.duration").timer();
        assertNotNull(durationTimer);
        assertEquals(1, durationTimer.count());
        
        Gauge activeGauge = meterRegistry.find("agent.conversations.active").gauge();
        assertNotNull(activeGauge);
        assertEquals(0.0, activeGauge.value());
    }
    
    @Test
    void testRecordConversationFailed() {
        // Given
        metricsService.recordConversationStarted(); // Start a conversation first
        Duration duration = Duration.ofSeconds(15);
        
        // When
        metricsService.recordConversationFailed(duration);
        
        // Then
        Counter failedCounter = meterRegistry.find("agent.conversations.failed").counter();
        assertNotNull(failedCounter);
        assertEquals(1.0, failedCounter.count());
        
        Timer durationTimer = meterRegistry.find("agent.conversations.duration").timer();
        assertNotNull(durationTimer);
        assertEquals(1, durationTimer.count());
        
        Gauge activeGauge = meterRegistry.find("agent.conversations.active").gauge();
        assertNotNull(activeGauge);
        assertEquals(0.0, activeGauge.value());
    }
    
    @Test
    void testRecordToolExecution() {
        // Given
        String toolName = "calculator";
        Duration duration = Duration.ofMillis(500);
        
        // When
        metricsService.recordToolExecution(toolName, duration, true);
        
        // Then
        Counter executionCounter = meterRegistry.find("agent.tools.executions")
                .tag("tool", toolName)
                .tag("status", "success")
                .counter();
        assertNotNull(executionCounter);
        assertEquals(1.0, executionCounter.count());
        
        Timer executionTimer = meterRegistry.find("agent.tools.execution.time")
                .tag("tool", toolName)
                .tag("status", "success")
                .timer();
        assertNotNull(executionTimer);
        assertEquals(1, executionTimer.count());
    }
    
    @Test
    void testRecordLlmRequest() {
        // Given
        String provider = "openai";
        Duration responseTime = Duration.ofMillis(1500);
        
        // When
        metricsService.recordLlmRequest(provider, responseTime, true);
        
        // Then
        Counter requestCounter = meterRegistry.find("agent.llm.requests")
                .tag("provider", provider)
                .tag("status", "success")
                .counter();
        assertNotNull(requestCounter);
        assertEquals(1.0, requestCounter.count());
        
        Timer responseTimer = meterRegistry.find("agent.llm.response.time")
                .tag("provider", provider)
                .tag("status", "success")
                .timer();
        assertNotNull(responseTimer);
        assertEquals(1, responseTimer.count());
    }
    
    @Test
    void testRecordTokenUsage() {
        // Given
        String type = "input";
        long tokens = 150;
        
        // When
        metricsService.recordTokenUsage(type, tokens);
        
        // Then
        Counter tokenCounter = meterRegistry.find("agent.tokens.used")
                .tag("type", type)
                .counter();
        assertNotNull(tokenCounter);
        assertEquals(150.0, tokenCounter.count());
    }
    
    @Test
    void testRecordReactCycle() {
        // Given
        Duration duration = Duration.ofSeconds(5);
        int iterations = 3;
        
        // When
        metricsService.recordReactCycle(duration, iterations, true);
        
        // Then
        Timer cycleTimer = meterRegistry.find("agent.react.cycle.time").timer();
        assertNotNull(cycleTimer);
        assertEquals(1, cycleTimer.count());
        
        Counter cycleCounter = meterRegistry.find("agent.react.cycles")
                .tag("status", "success")
                .counter();
        assertNotNull(cycleCounter);
        assertEquals(1.0, cycleCounter.count());
    }
    
    @Test
    void testRecordMemoryOperation() {
        // Given
        String operation = "store";
        Duration duration = Duration.ofMillis(100);
        
        // When
        metricsService.recordMemoryOperation(operation, duration, true);
        
        // Then
        Counter operationCounter = meterRegistry.find("agent.memory.operations")
                .tag("operation", operation)
                .tag("status", "success")
                .counter();
        assertNotNull(operationCounter);
        assertEquals(1.0, operationCounter.count());
        
        Timer operationTimer = meterRegistry.find("agent.memory.operation.time")
                .tag("operation", operation)
                .timer();
        assertNotNull(operationTimer);
        assertEquals(1, operationTimer.count());
    }
    
    @Test
    void testUpdateActiveConversations() {
        // When
        metricsService.updateActiveConversations(5);
        
        // Then
        Gauge activeGauge = meterRegistry.find("agent.conversations.active").gauge();
        assertNotNull(activeGauge);
        assertEquals(5.0, activeGauge.value());
    }
    
    @Test
    void testUpdateMemoryUsage() {
        // When
        metricsService.updateMemoryUsage(1024 * 1024); // 1MB
        
        // Then
        Gauge memoryGauge = meterRegistry.find("agent.memory.usage.bytes").gauge();
        assertNotNull(memoryGauge);
        assertEquals(1024.0 * 1024.0, memoryGauge.value());
    }
    
    @Test
    void testUpdateCacheHitRate() {
        // When
        metricsService.updateCacheHitRate(0.85); // 85%
        
        // Then
        Gauge cacheGauge = meterRegistry.find("agent.cache.hit.rate").gauge();
        assertNotNull(cacheGauge);
        assertEquals(85.0, cacheGauge.value());
    }
}