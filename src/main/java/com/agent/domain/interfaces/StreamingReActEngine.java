package com.agent.domain.interfaces;

import com.agent.domain.model.AgentContext;
import com.agent.domain.model.AgentResponse;

import java.util.function.Consumer;

/**
 * Extended interface for ReAct engine with streaming capabilities.
 */
public interface StreamingReActEngine extends ReActEngine {
    
    /**
     * Process a message with streaming response callbacks.
     * 
     * @param conversationId The unique identifier for the conversation
     * @param message The input message to process
     * @param context The current agent context
     * @param onThinking Callback for thinking phase updates
     * @param onAction Callback for action execution updates
     * @param onObservation Callback for observation updates
     * @param onComplete Callback for final response
     */
    void processMessageWithStreaming(String conversationId, String message, AgentContext context,
                                   Consumer<String> onThinking,
                                   Consumer<String> onAction,
                                   Consumer<String> onObservation,
                                   Consumer<AgentResponse> onComplete);
    
    /**
     * Check if streaming is supported for the current configuration.
     */
    boolean isStreamingSupported();
}