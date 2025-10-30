package com.agent.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for collecting and managing custom metrics for the agent application.
 */
@Service
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicLong> gaugeValues;
    
    // Counters
    private final Counter conversationStartedCounter;
    private final Counter conversationCompletedCounter;
    private final Counter conversationFailedCounter;
    private final Counter toolExecutionCounter;
    private final Counter llmRequestCounter;
    private final Counter tokenUsageCounter;
    
    // Timers
    private final Timer conversationDurationTimer;
    private final Timer llmResponseTimer;
    private final Timer toolExecutionTimer;
    private final Timer reactCycleTimer;
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.gaugeValues = new ConcurrentHashMap<>();
        
        // Initialize counters
        this.conversationStartedCounter = Counter.builder("agent.conversations.started")
                .description("Number of conversations started")
                .register(meterRegistry);
        
        this.conversationCompletedCounter = Counter.builder("agent.conversations.completed")
                .description("Number of conversations completed successfully")
                .register(meterRegistry);
        
        this.conversationFailedCounter = Counter.builder("agent.conversations.failed")
                .description("Number of conversations that failed")
                .register(meterRegistry);
        
        this.toolExecutionCounter = Counter.builder("agent.tools.executions")
                .description("Number of tool executions")
                .tag("tool", "unknown")
                .register(meterRegistry);
        
        this.llmRequestCounter = Counter.builder("agent.llm.requests")
                .description("Number of LLM requests")
                .tag("provider", "unknown")
                .register(meterRegistry);
        
        this.tokenUsageCounter = Counter.builder("agent.tokens.used")
                .description("Total tokens consumed")
                .tag("type", "unknown")
                .register(meterRegistry);
        
        // Initialize timers
        this.conversationDurationTimer = Timer.builder("agent.conversations.duration")
                .description("Duration of conversations")
                .register(meterRegistry);
        
        this.llmResponseTimer = Timer.builder("agent.llm.response.time")
                .description("LLM response time")
                .tag("provider", "unknown")
                .register(meterRegistry);
        
        this.toolExecutionTimer = Timer.builder("agent.tools.execution.time")
                .description("Tool execution time")
                .tag("tool", "unknown")
                .register(meterRegistry);
        
        this.reactCycleTimer = Timer.builder("agent.react.cycle.time")
                .description("ReAct cycle execution time")
                .register(meterRegistry);
        
        // Initialize gauges
        initializeGauges();
    }
    
    // Conversation metrics
    public void recordConversationStarted() {
        conversationStartedCounter.increment();
        incrementGauge("active.conversations");
    }
    
    public void recordConversationCompleted(Duration duration) {
        conversationCompletedCounter.increment();
        conversationDurationTimer.record(duration);
        decrementGauge("active.conversations");
    }
    
    public void recordConversationFailed(Duration duration) {
        conversationFailedCounter.increment();
        conversationDurationTimer.record(duration);
        decrementGauge("active.conversations");
    }
    
    // Tool execution metrics
    public void recordToolExecution(String toolName, Duration duration, boolean success) {
        Counter.builder("agent.tools.executions")
                .description("Number of tool executions")
                .tag("tool", toolName)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();
        
        Timer.builder("agent.tools.execution.time")
                .description("Tool execution time")
                .tag("tool", toolName)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .record(duration);
    }
    
    // LLM metrics
    public void recordLlmRequest(String provider, Duration responseTime, boolean success) {
        Counter.builder("agent.llm.requests")
                .description("Number of LLM requests")
                .tag("provider", provider)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();
        
        Timer.builder("agent.llm.response.time")
                .description("LLM response time")
                .tag("provider", provider)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .record(responseTime);
    }
    
    // Token usage metrics
    public void recordTokenUsage(String type, long tokens) {
        Counter.builder("agent.tokens.used")
                .description("Total tokens consumed")
                .tag("type", type)
                .register(meterRegistry)
                .increment(tokens);
    }
    
    // ReAct cycle metrics
    public void recordReactCycle(Duration duration, int iterations, boolean success) {
        reactCycleTimer.record(duration);
        
        Counter.builder("agent.react.cycles")
                .description("Number of ReAct cycles")
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();
        
        // Note: For iterations, we'll use a counter instead of gauge for this metric
        Counter.builder("agent.react.iterations.total")
                .description("Total iterations across all ReAct cycles")
                .register(meterRegistry)
                .increment(iterations);
    }
    
    // Memory metrics
    public void recordMemoryOperation(String operation, Duration duration, boolean success) {
        Counter.builder("agent.memory.operations")
                .description("Number of memory operations")
                .tag("operation", operation)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();
        
        Timer.builder("agent.memory.operation.time")
                .description("Memory operation time")
                .tag("operation", operation)
                .register(meterRegistry)
                .record(duration);
    }
    
    // System metrics
    public void updateActiveConversations(long count) {
        setGaugeValue("active.conversations", count);
    }
    
    public void updateMemoryUsage(long bytes) {
        setGaugeValue("memory.usage.bytes", bytes);
    }
    
    public void updateCacheHitRate(double rate) {
        setGaugeValue("cache.hit.rate", (long) (rate * 100));
    }
    
    // Helper methods for gauges
    private void initializeGauges() {
        // Active conversations
        gaugeValues.put("active.conversations", new AtomicLong(0));
        Gauge.builder("agent.conversations.active", gaugeValues.get("active.conversations"), AtomicLong::doubleValue)
                .description("Number of active conversations")
                .register(meterRegistry);
        
        // Memory usage
        gaugeValues.put("memory.usage.bytes", new AtomicLong(0));
        Gauge.builder("agent.memory.usage.bytes", gaugeValues.get("memory.usage.bytes"), AtomicLong::doubleValue)
                .description("Memory usage in bytes")
                .register(meterRegistry);
        
        // Cache hit rate
        gaugeValues.put("cache.hit.rate", new AtomicLong(0));
        Gauge.builder("agent.cache.hit.rate", gaugeValues.get("cache.hit.rate"), AtomicLong::doubleValue)
                .description("Cache hit rate percentage")
                .register(meterRegistry);
    }
    
    private void incrementGauge(String name) {
        gaugeValues.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    private void decrementGauge(String name) {
        gaugeValues.computeIfAbsent(name, k -> new AtomicLong(0)).decrementAndGet();
    }
    
    private void setGaugeValue(String name, long value) {
        gaugeValues.computeIfAbsent(name, k -> new AtomicLong(0)).set(value);
    }
}