package com.agent.domain.interfaces;

import com.agent.domain.model.AgentContext;
import com.agent.domain.model.AgentResponse;
import com.agent.domain.model.ConversationState;
import com.agent.domain.model.ToolResult;

/**
 * Core interface for the ReAct (Reasoning and Acting) engine.
 * Implements the reasoning loop pattern for intelligent agent decision-making.
 */
public interface ReActEngine {
    
    /**
     * Process a new message in a conversation and generate an agent response.
     * 
     * @param conversationId The unique identifier for the conversation
     * @param message The input message to process
     * @param context The current agent context
     * @return The agent's response after reasoning and potential actions
     */
    AgentResponse processMessage(String conversationId, String message, AgentContext context);
    
    /**
     * Continue the reasoning loop after a tool execution completes.
     * 
     * @param conversationId The unique identifier for the conversation
     * @param toolResult The result from the executed tool
     */
    void continueReasoning(String conversationId, ToolResult toolResult);
    
    /**
     * Retrieve the current state of a conversation's reasoning process.
     * 
     * @param conversationId The unique identifier for the conversation
     * @return The current conversation state
     */
    ConversationState getConversationState(String conversationId);
}