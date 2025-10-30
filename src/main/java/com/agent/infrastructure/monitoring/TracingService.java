package com.agent.infrastructure.monitoring;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Service for managing distributed tracing and correlation IDs.
 */
@Service
public class TracingService {
    
    private static final Logger logger = LoggerFactory.getLogger(TracingService.class);
    
    private final Tracer tracer;
    
    public TracingService(Tracer tracer) {
        this.tracer = tracer;
    }
    
    /**
     * Execute a block of code within a new span
     */
    public <T> T executeInSpan(String spanName, Supplier<T> operation) {
        return executeInSpan(spanName, Map.of(), operation);
    }
    
    /**
     * Execute a block of code within a new span with tags
     */
    public <T> T executeInSpan(String spanName, Map<String, String> tags, Supplier<T> operation) {
        Span span = tracer.nextSpan().name(spanName);
        
        // Add tags to span
        tags.forEach(span::tag);
        
        try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
            // Add correlation ID to MDC for logging
            String traceId = span.context().traceId();
            String spanId = span.context().spanId();
            
            MDC.put("traceId", traceId);
            MDC.put("spanId", spanId);
            
            logger.debug("Starting span: {} with traceId: {}, spanId: {}", spanName, traceId, spanId);
            
            return operation.get();
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            span.tag("error.type", e.getClass().getSimpleName());
            logger.error("Error in span: {}", spanName, e);
            throw e;
        } finally {
            span.end();
            MDC.remove("traceId");
            MDC.remove("spanId");
        }
    }
    
    /**
     * Execute a block of code within a new span (void return)
     */
    public void executeInSpan(String spanName, Runnable operation) {
        executeInSpan(spanName, Map.of(), operation);
    }
    
    /**
     * Execute a block of code within a new span with tags (void return)
     */
    public void executeInSpan(String spanName, Map<String, String> tags, Runnable operation) {
        executeInSpan(spanName, tags, () -> {
            operation.run();
            return null;
        });
    }
    
    /**
     * Add a tag to the current span
     */
    public void addTagToCurrentSpan(String key, String value) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.tag(key, value);
        }
    }
    
    /**
     * Add an event to the current span
     */
    public void addEventToCurrentSpan(String event) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.event(event);
        }
    }
    
    /**
     * Get the current trace ID
     */
    public String getCurrentTraceId() {
        Span currentSpan = tracer.currentSpan();
        return currentSpan != null ? currentSpan.context().traceId() : null;
    }
    
    /**
     * Get the current span ID
     */
    public String getCurrentSpanId() {
        Span currentSpan = tracer.currentSpan();
        return currentSpan != null ? currentSpan.context().spanId() : null;
    }
    
    /**
     * Create a child span for async operations
     */
    public Span createChildSpan(String spanName) {
        return tracer.nextSpan().name(spanName);
    }
    
    /**
     * Trace conversation operations
     */
    public <T> T traceConversation(String conversationId, String operation, Supplier<T> supplier) {
        return executeInSpan("conversation." + operation, 
                           Map.of("conversation.id", conversationId,
                                 "operation.type", "conversation"), 
                           supplier);
    }
    
    /**
     * Trace LLM operations
     */
    public <T> T traceLlmOperation(String provider, String operation, Supplier<T> supplier) {
        return executeInSpan("llm." + operation,
                           Map.of("llm.provider", provider,
                                 "operation.type", "llm"),
                           supplier);
    }
    
    /**
     * Trace tool operations
     */
    public <T> T traceToolOperation(String toolName, String operation, Supplier<T> supplier) {
        return executeInSpan("tool." + operation,
                           Map.of("tool.name", toolName,
                                 "operation.type", "tool"),
                           supplier);
    }
    
    /**
     * Trace memory operations
     */
    public <T> T traceMemoryOperation(String operation, Supplier<T> supplier) {
        return executeInSpan("memory." + operation,
                           Map.of("operation.type", "memory"),
                           supplier);
    }
    
    /**
     * Trace ReAct operations
     */
    public <T> T traceReactOperation(String conversationId, String phase, Supplier<T> supplier) {
        return executeInSpan("react." + phase,
                           Map.of("conversation.id", conversationId,
                                 "react.phase", phase,
                                 "operation.type", "react"),
                           supplier);
    }
}