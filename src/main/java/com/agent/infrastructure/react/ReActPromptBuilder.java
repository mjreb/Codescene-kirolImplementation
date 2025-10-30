package com.agent.infrastructure.react;

import com.agent.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds prompts for different phases of the ReAct reasoning pattern.
 */
@Component
public class ReActPromptBuilder {
    
    private static final String REACT_SYSTEM_PROMPT = """
        You are an intelligent agent that uses the ReAct (Reasoning and Acting) pattern to solve problems.
        
        You should follow this pattern:
        1. Thought: Think about what you need to do
        2. Action: Take an action using available tools (if needed)
        3. Observation: Observe the results of your action
        4. Repeat until you have a final answer
        
        Available tools and their descriptions will be provided in the context.
        
        When you want to use a tool, format your response as:
        Action: [tool_name]
        Parameters: {"param1": "value1", "param2": "value2"}
        
        When you have a final answer, start your response with:
        Final Answer: [your answer]
        
        Always think step by step and be explicit about your reasoning.
        """;
    
    /**
     * Build a prompt for the thinking phase with provider-specific optimizations.
     */
    public String buildThinkingPrompt(String userMessage, ReActState state, AgentContext context) {
        return buildThinkingPrompt(userMessage, state, context, null);
    }
    
    /**
     * Build a prompt for the thinking phase with provider-specific optimizations.
     */
    public String buildThinkingPrompt(String userMessage, ReActState state, AgentContext context, String providerId) {
        return buildThinkingPrompt(userMessage, state, context, providerId, null);
    }
    
    /**
     * Build a prompt for the thinking phase with tool information.
     */
    public String buildThinkingPrompt(String userMessage, ReActState state, AgentContext context, 
                                    String providerId, String availableToolsDescription) {
        StringBuilder prompt = new StringBuilder();
        
        // Add provider-specific system prompt
        String systemPrompt = getProviderSpecificSystemPrompt(providerId);
        prompt.append(systemPrompt).append("\n\n");
        
        // Add available tools information
        if (availableToolsDescription != null && !availableToolsDescription.trim().isEmpty()) {
            prompt.append(availableToolsDescription).append("\n");
        } else {
            // Fallback if no tool description provided
            prompt.append("Available Tools:\n");
            prompt.append("- calculator: Perform mathematical calculations\n");
            prompt.append("- web_search: Search the web for information\n");
            prompt.append("- file_system: Read and write files\n\n");
        }
        
        // Add conversation history/context
        if (state.getObservations() != null && !state.getObservations().isEmpty()) {
            prompt.append("Previous observations:\n");
            for (int i = 0; i < state.getObservations().size(); i++) {
                ToolResult observation = state.getObservations().get(i);
                prompt.append(String.format("Observation %d: Tool '%s' %s. Result: %s\n", 
                    i + 1, 
                    observation.getToolName(),
                    observation.isSuccess() ? "succeeded" : "failed",
                    observation.isSuccess() ? observation.getResult() : observation.getErrorMessage()));
            }
            prompt.append("\n");
        }
        
        // Add current iteration info
        prompt.append(String.format("Current iteration: %d/%d\n\n", 
            state.getIterationCount(), state.getMaxIterations()));
        
        // Add the user message or continuation context
        if (userMessage != null) {
            prompt.append("User: ").append(userMessage).append("\n\n");
        } else {
            prompt.append("Continue reasoning based on the previous observations.\n\n");
        }
        
        prompt.append("Thought: ");
        
        return optimizePromptForProvider(prompt.toString(), providerId);
    }
    
    /**
     * Build a prompt for the observing phase with provider-specific optimizations.
     */
    public String buildObservingPrompt(ReActState state, ToolResult observation, AgentContext context) {
        return buildObservingPrompt(state, observation, context, null);
    }
    
    /**
     * Build a prompt for the observing phase with provider-specific optimizations.
     */
    public String buildObservingPrompt(ReActState state, ToolResult observation, AgentContext context, String providerId) {
        StringBuilder prompt = new StringBuilder();
        
        String systemPrompt = getProviderSpecificSystemPrompt(providerId);
        prompt.append(systemPrompt).append("\n\n");
        
        // Add context about what we were trying to do
        if (state.getCurrentThought() != null) {
            prompt.append("Previous thought: ").append(state.getCurrentThought()).append("\n\n");
        }
        
        // Add the observation
        prompt.append("Observation: ");
        if (observation.isSuccess()) {
            prompt.append(String.format("Tool '%s' executed successfully. Result: %s\n\n", 
                observation.getToolName(), observation.getResult()));
        } else {
            prompt.append(String.format("Tool '%s' failed. Error: %s\n\n", 
                observation.getToolName(), observation.getErrorMessage()));
        }
        
        // Add all previous observations for context
        if (state.getObservations() != null && state.getObservations().size() > 1) {
            prompt.append("Previous observations:\n");
            List<ToolResult> previousObservations = state.getObservations().subList(0, state.getObservations().size() - 1);
            for (int i = 0; i < previousObservations.size(); i++) {
                ToolResult prevObs = previousObservations.get(i);
                prompt.append(String.format("%d. Tool '%s' %s. Result: %s\n", 
                    i + 1,
                    prevObs.getToolName(),
                    prevObs.isSuccess() ? "succeeded" : "failed",
                    prevObs.isSuccess() ? prevObs.getResult() : prevObs.getErrorMessage()));
            }
            prompt.append("\n");
        }
        
        prompt.append("Based on this observation, what should you do next? ");
        prompt.append("If you have enough information to provide a final answer, start with 'Final Answer:'. ");
        prompt.append("Otherwise, continue with 'Thought:' to plan your next action.\n\n");
        
        return optimizePromptForProvider(prompt.toString(), providerId);
    }
    
    /**
     * Build a prompt for error recovery.
     */
    public String buildErrorRecoveryPrompt(ReActState state, String errorMessage, AgentContext context) {
        return buildErrorRecoveryPrompt(state, errorMessage, context, null);
    }
    
    /**
     * Build a prompt for error recovery with provider-specific optimizations.
     */
    public String buildErrorRecoveryPrompt(ReActState state, String errorMessage, AgentContext context, String providerId) {
        StringBuilder prompt = new StringBuilder();
        
        String systemPrompt = getProviderSpecificSystemPrompt(providerId);
        prompt.append(systemPrompt).append("\n\n");
        
        prompt.append("An error occurred during the reasoning process:\n");
        prompt.append("Error: ").append(errorMessage).append("\n\n");
        
        if (state.getCurrentThought() != null) {
            prompt.append("Previous thought: ").append(state.getCurrentThought()).append("\n\n");
        }
        
        prompt.append("Please analyze the error and decide how to proceed. ");
        prompt.append("You can either try a different approach or provide a final answer based on what you know so far.\n\n");
        
        prompt.append("Thought: ");
        
        return optimizePromptForProvider(prompt.toString(), providerId);
    }
    
    /**
     * Optimize prompt for specific LLM provider.
     */
    private String optimizePromptForProvider(String prompt, String providerId) {
        if (providerId == null) {
            return prompt;
        }
        
        switch (providerId.toLowerCase()) {
            case "openai":
                return optimizeForOpenAI(prompt);
            case "anthropic":
                return optimizeForAnthropic(prompt);
            case "ollama":
                return optimizeForOllama(prompt);
            default:
                return prompt;
        }
    }
    
    /**
     * Optimize prompt for OpenAI models.
     */
    private String optimizeForOpenAI(String prompt) {
        // OpenAI models work well with structured prompts and clear instructions
        // Add system message formatting if not already present
        if (!prompt.startsWith("System:") && !prompt.contains("You are")) {
            return "System: " + prompt;
        }
        return prompt;
    }
    
    /**
     * Optimize prompt for Anthropic models.
     */
    private String optimizeForAnthropic(String prompt) {
        // Anthropic models prefer more conversational and detailed prompts
        // Add human/assistant formatting
        if (!prompt.contains("Human:") && !prompt.contains("Assistant:")) {
            return "Human: " + prompt + "\n\nAssistant: I'll help you with that. Let me think through this step by step.\n\n";
        }
        return prompt;
    }
    
    /**
     * Optimize prompt for Ollama (local) models.
     */
    private String optimizeForOllama(String prompt) {
        // Local models often benefit from shorter, more direct prompts
        // Remove excessive formatting and keep it concise
        return prompt.replaceAll("\\n\\n+", "\n\n").trim();
    }
    
    /**
     * Get provider-specific system prompt.
     */
    private String getProviderSpecificSystemPrompt(String providerId) {
        if (providerId == null) {
            return REACT_SYSTEM_PROMPT;
        }
        
        switch (providerId.toLowerCase()) {
            case "anthropic":
                return """
                    You are Claude, an intelligent agent that uses the ReAct (Reasoning and Acting) pattern to solve problems.
                    
                    Please follow this structured approach:
                    1. Thought: Carefully reason about what you need to do
                    2. Action: Take an action using available tools (if needed)
                    3. Observation: Observe and analyze the results of your action
                    4. Repeat until you have a complete answer
                    
                    When using tools, format your response exactly as:
                    Action: [tool_name]
                    Parameters: {"param1": "value1", "param2": "value2"}
                    
                    When you have a final answer, format it as:
                    Final Answer: [your complete answer]
                    
                    Always be thorough in your reasoning and explicit about your thought process.
                    """;
                    
            case "ollama":
                return """
                    You are an AI agent using ReAct pattern. Think, act, observe, repeat.
                    
                    Format:
                    - Thought: [reasoning]
                    - Action: [tool_name] with Parameters: {json}
                    - Final Answer: [answer]
                    
                    Be concise but thorough.
                    """;
                    
            default:
                return REACT_SYSTEM_PROMPT;
        }
    }
}