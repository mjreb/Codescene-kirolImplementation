package com.agent.infrastructure.react;

import com.agent.domain.interfaces.LLMProviderManager;
import com.agent.domain.interfaces.MemoryManager;
import com.agent.domain.interfaces.ReActEngine;
import com.agent.domain.interfaces.StreamingReActEngine;
import com.agent.domain.interfaces.ToolFramework;
import com.agent.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the ReAct (Reasoning and Acting) engine.
 * Manages the reasoning loop pattern for intelligent agent decision-making.
 */
@Service
public class ReActEngineImpl implements ReActEngine, StreamingReActEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(ReActEngineImpl.class);
    
    private final LLMProviderManager llmProviderManager;
    private final ToolFramework toolFramework;
    private final MemoryManager memoryManager;
    private final ReActPromptBuilder promptBuilder;
    private final ReActResponseParser responseParser;
    private final ToolIntegrationService toolIntegrationService;
    private final ConversationStateManager stateManager;
    
    // In-memory storage for active conversation states
    private final Map<String, ReActState> activeStates = new ConcurrentHashMap<>();
    
    @Autowired
    public ReActEngineImpl(LLMProviderManager llmProviderManager, 
                          ToolFramework toolFramework,
                          MemoryManager memoryManager,
                          ToolIntegrationService toolIntegrationService,
                          ConversationStateManager stateManager) {
        this.llmProviderManager = llmProviderManager;
        this.toolFramework = toolFramework;
        this.memoryManager = memoryManager;
        this.toolIntegrationService = toolIntegrationService;
        this.stateManager = stateManager;
        this.promptBuilder = new ReActPromptBuilder();
        this.responseParser = new ReActResponseParser();
    }
    
    @Override
    public AgentResponse processMessage(String conversationId, String message, AgentContext context) {
        logger.info("Processing message for conversation: {}", conversationId);
        
        try {
            // Initialize or retrieve conversation state
            ConversationState conversationState = stateManager.initializeConversationState(conversationId, context);
            ReActState reactState = getReActStateFromConversation(conversationState);
            
            // Start the reasoning loop
            AgentResponse response = executeReasoningLoop(conversationId, message, context, reactState);
            
            // Update conversation state
            updateConversationStateFromReAct(conversationState, reactState);
            stateManager.updateConversationState(conversationId, conversationState);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing message for conversation {}: {}", conversationId, e.getMessage(), e);
            stateManager.handleConversationError(conversationId, e.getMessage(), e);
            return createErrorResponse("An error occurred while processing your message: " + e.getMessage());
        }
    }
    
    @Override
    public void continueReasoning(String conversationId, ToolResult toolResult) {
        logger.info("Continuing reasoning for conversation: {} after tool execution", conversationId);
        
        try {
            ConversationState conversationState = stateManager.getConversationState(conversationId);
            if (conversationState == null) {
                logger.warn("No conversation state found for: {}", conversationId);
                return;
            }
            
            ReActState reactState = getReActStateFromConversation(conversationState);
            
            // Add the tool result as an observation
            if (reactState.getObservations() == null) {
                reactState.setObservations(new ArrayList<>());
            }
            reactState.getObservations().add(toolResult);
            
            // Move to observing phase
            stateManager.transitionState(conversationId, ReActPhase.OBSERVING);
            
            // Continue the reasoning loop
            AgentContext context = retrieveContextFromMemory(conversationId);
            if (context != null) {
                AgentResponse response = executeReasoningLoop(conversationId, null, context, reactState);
                
                // Update conversation state
                updateConversationStateFromReAct(conversationState, reactState);
                stateManager.updateConversationState(conversationId, conversationState);
            }
            
        } catch (Exception e) {
            logger.error("Error continuing reasoning for conversation {}: {}", conversationId, e.getMessage(), e);
            stateManager.handleConversationError(conversationId, e.getMessage(), e);
        }
    }
    
    @Override
    public ConversationState getConversationState(String conversationId) {
        logger.debug("Retrieving conversation state for: {}", conversationId);
        return stateManager.getConversationState(conversationId);
    }
    
    /**
     * Execute the main reasoning loop for the ReAct pattern.
     */
    private AgentResponse executeReasoningLoop(String conversationId, String userMessage, 
                                             AgentContext context, ReActState state) {
        
        while (state.getIterationCount() < state.getMaxIterations()) {
            state.setIterationCount(state.getIterationCount() + 1);
            
            switch (state.getCurrentPhase()) {
                case THINKING:
                    return executeThinkingPhase(conversationId, userMessage, context, state);
                    
                case ACTING:
                    return executeActingPhase(conversationId, context, state);
                    
                case OBSERVING:
                    return executeObservingPhase(conversationId, context, state);
                    
                default:
                    logger.warn("Unknown ReAct phase: {}", state.getCurrentPhase());
                    return createErrorResponse("Unknown reasoning phase encountered");
            }
        }
        
        // Max iterations reached
        logger.warn("Max iterations ({}) reached for conversation: {}", state.getMaxIterations(), conversationId);
        return createResponse("I've reached the maximum number of reasoning steps. Let me provide what I've learned so far.", 
                            AgentResponse.ResponseType.TEXT);
    }
    
    /**
     * Execute the thinking phase - generate thoughts and plan actions.
     */
    private AgentResponse executeThinkingPhase(String conversationId, String userMessage, 
                                             AgentContext context, ReActState state) {
        logger.debug("Executing thinking phase for conversation: {}", conversationId);
        
        try {
            // Get preferred provider
            String providerId = getPreferredProvider(context);
            
            // Get available tools description
            String toolsDescription = toolIntegrationService.getAvailableToolsDescription();
            
            // Build the prompt for thinking
            String prompt = promptBuilder.buildThinkingPrompt(userMessage, state, context, providerId, toolsDescription);
            
            // Get LLM response
            LLMRequest request = createLLMRequest(prompt, context);
            LLMResponse llmResponse = llmProviderManager.generateResponse(request, providerId);
            
            // Parse the response to extract thought and potential action
            ReActResponseParser.ReActThought thought = responseParser.parseThinkingResponse(llmResponse.getContent());
            
            state.setCurrentThought(thought.getThought());
            
            if (thought.hasAction()) {
                // Move to acting phase
                state.setPendingAction(thought.getAction());
                state.setCurrentPhase(ReActPhase.ACTING);
                
                // Continue to acting phase
                return executeActingPhase(conversationId, context, state);
            } else {
                // No action needed, return the thought as final response
                return createResponse(thought.getThought(), AgentResponse.ResponseType.TEXT);
            }
            
        } catch (Exception e) {
            logger.error("Error in thinking phase for conversation {}: {}", conversationId, e.getMessage(), e);
            return createErrorResponse("Error during thinking phase: " + e.getMessage());
        }
    }
    
    /**
     * Execute the acting phase - perform the planned action.
     */
    private AgentResponse executeActingPhase(String conversationId, AgentContext context, ReActState state) {
        logger.debug("Executing acting phase for conversation: {}", conversationId);
        
        try {
            ToolCall action = state.getPendingAction();
            if (action == null) {
                logger.warn("No pending action found in acting phase for conversation: {}", conversationId);
                state.setCurrentPhase(ReActPhase.THINKING);
                return executeThinkingPhase(conversationId, null, context, state);
            }
            
            // Execute the tool using the integration service
            ToolResult result = toolIntegrationService.executeToolSafely(action.getToolName(), action.getParameters());
            
            // Add result to observations
            if (state.getObservations() == null) {
                state.setObservations(new ArrayList<>());
            }
            state.getObservations().add(result);
            
            // Move to observing phase
            state.setCurrentPhase(ReActPhase.OBSERVING);
            state.setPendingAction(null);
            
            // Continue to observing phase
            return executeObservingPhase(conversationId, context, state);
            
        } catch (Exception e) {
            logger.error("Error in acting phase for conversation {}: {}", conversationId, e.getMessage(), e);
            
            // Create error observation and move to observing phase
            ToolResult errorResult = new ToolResult();
            errorResult.setToolName(state.getPendingAction() != null ? state.getPendingAction().getToolName() : "unknown");
            errorResult.setSuccess(false);
            errorResult.setErrorMessage(e.getMessage());
            
            if (state.getObservations() == null) {
                state.setObservations(new ArrayList<>());
            }
            state.getObservations().add(errorResult);
            
            state.setCurrentPhase(ReActPhase.OBSERVING);
            state.setPendingAction(null);
            
            return executeObservingPhase(conversationId, context, state);
        }
    }
    
    /**
     * Execute the observing phase - process action results and decide next steps.
     */
    private AgentResponse executeObservingPhase(String conversationId, AgentContext context, ReActState state) {
        logger.debug("Executing observing phase for conversation: {}", conversationId);
        
        try {
            // Get the latest observation
            List<ToolResult> observations = state.getObservations();
            if (observations == null || observations.isEmpty()) {
                logger.warn("No observations found in observing phase for conversation: {}", conversationId);
                state.setCurrentPhase(ReActPhase.THINKING);
                return executeThinkingPhase(conversationId, null, context, state);
            }
            
            ToolResult latestObservation = observations.get(observations.size() - 1);
            
            // Get preferred provider
            String providerId = getPreferredProvider(context);
            
            // Format the observation using the tool integration service
            String formattedObservation = toolIntegrationService.formatToolResultForObservation(latestObservation);
            
            // Build prompt for observation processing
            String prompt = promptBuilder.buildObservingPrompt(state, latestObservation, context, providerId);
            
            // Get LLM response
            LLMRequest request = createLLMRequest(prompt, context);
            LLMResponse llmResponse = llmProviderManager.generateResponse(request, providerId);
            
            // Parse the response to determine next action
            ReActResponseParser.ReActObservation observation = responseParser.parseObservingResponse(llmResponse.getContent());
            
            if (observation.isComplete()) {
                // Task is complete, return final response
                return createResponse(observation.getFinalAnswer(), AgentResponse.ResponseType.TEXT);
            } else {
                // Continue reasoning - move back to thinking phase
                state.setCurrentPhase(ReActPhase.THINKING);
                return executeThinkingPhase(conversationId, null, context, state);
            }
            
        } catch (Exception e) {
            logger.error("Error in observing phase for conversation {}: {}", conversationId, e.getMessage(), e);
            return createErrorResponse("Error during observation phase: " + e.getMessage());
        }
    }
    

    
    /**
     * Retrieve context from memory.
     */
    private AgentContext retrieveContextFromMemory(String conversationId) {
        try {
            ConversationContext conversationContext = memoryManager.retrieveConversationContext(conversationId);
            if (conversationContext != null) {
                return new AgentContext(conversationContext.getAgentId(), conversationContext.getUserId(), 
                                      conversationContext.getContextData(), null);
            }
        } catch (Exception e) {
            logger.error("Error retrieving context from memory for conversation {}: {}", conversationId, e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Create an LLM request with appropriate parameters.
     */
    private LLMRequest createLLMRequest(String prompt, AgentContext context) {
        LLMRequest request = new LLMRequest();
        request.setPrompt(prompt);
        
        // Get provider-specific configuration
        String providerId = getPreferredProvider(context);
        configureRequestForProvider(request, providerId, context);
        
        return request;
    }
    
    /**
     * Configure the LLM request based on the provider and context.
     */
    private void configureRequestForProvider(LLMRequest request, String providerId, AgentContext context) {
        // Default parameters
        request.setMaxTokens(2000);
        request.setTemperature(0.7);
        
        // Provider-specific optimizations
        switch (providerId.toLowerCase()) {
            case "openai":
                request.setModel("gpt-3.5-turbo");
                request.setTemperature(0.7);
                request.setMaxTokens(2000);
                break;
                
            case "anthropic":
                request.setModel("claude-3-sonnet-20240229");
                request.setTemperature(0.7);
                request.setMaxTokens(2000);
                break;
                
            case "ollama":
                request.setModel("llama2");
                request.setTemperature(0.7);
                request.setMaxTokens(1500); // Lower for local models
                break;
                
            default:
                request.setModel("gpt-3.5-turbo");
                break;
        }
        
        // Override with context configuration if available
        if (context.getConfiguration() != null) {
            // In a full implementation, we would read model preferences from configuration
            // For now, we'll use the defaults above
        }
    }
    
    /**
     * Get the preferred LLM provider from context.
     */
    private String getPreferredProvider(AgentContext context) {
        // Check context configuration for preferred provider
        if (context.getConfiguration() != null) {
            // In a full implementation, we would read provider preference from configuration
        }
        
        // Get available providers and select the best one
        List<String> providers = llmProviderManager.getAvailableProviders();
        if (providers.isEmpty()) {
            throw new RuntimeException("No LLM providers available");
        }
        
        // Prefer providers in this order: OpenAI, Anthropic, Ollama
        String[] preferredOrder = {"openai", "anthropic", "ollama"};
        
        for (String preferred : preferredOrder) {
            if (providers.contains(preferred)) {
                // Check provider health before using
                try {
                    ProviderHealth health = llmProviderManager.checkProviderHealth(preferred);
                    if (health.getStatus() != ProviderHealth.HealthStatus.UNHEALTHY) {
                        return preferred;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to check health for provider {}: {}", preferred, e.getMessage());
                }
            }
        }
        
        // If no preferred provider is healthy, use the first available
        return providers.get(0);
    }
    
    /**
     * Create a standard agent response.
     */
    private AgentResponse createResponse(String content, AgentResponse.ResponseType type) {
        AgentResponse response = new AgentResponse();
        response.setContent(content);
        response.setType(type);
        response.setTimestamp(Instant.now());
        return response;
    }
    
    /**
     * Create an error response.
     */
    private AgentResponse createErrorResponse(String errorMessage) {
        return createResponse(errorMessage, AgentResponse.ResponseType.ERROR);
    }
    
    /**
     * Get ReAct state from conversation state.
     */
    private ReActState getReActStateFromConversation(ConversationState conversationState) {
        if (conversationState.getContext() != null && conversationState.getContext().getReActState() != null) {
            return conversationState.getContext().getReActState();
        }
        
        // Create new ReAct state if none exists
        ReActState reactState = new ReActState(conversationState.getConversationId());
        reactState.setCurrentPhase(conversationState.getCurrentPhase());
        
        // Store it in the conversation context
        if (conversationState.getContext() != null) {
            conversationState.getContext().setReActState(reactState);
        }
        
        return reactState;
    }
    
    /**
     * Update conversation state from ReAct state.
     */
    private void updateConversationStateFromReAct(ConversationState conversationState, ReActState reactState) {
        conversationState.setCurrentPhase(reactState.getCurrentPhase());
        
        if (conversationState.getContext() != null) {
            conversationState.getContext().setReActState(reactState);
            conversationState.getContext().setLastUpdated(Instant.now());
        }
    }
    
    @Override
    public void processMessageWithStreaming(String conversationId, String message, AgentContext context,
                                          Consumer<String> onThinking,
                                          Consumer<String> onAction,
                                          Consumer<String> onObservation,
                                          Consumer<AgentResponse> onComplete) {
        logger.info("Processing message with streaming for conversation: {}", conversationId);
        
        try {
            // Initialize or retrieve conversation state
            ConversationState conversationState = stateManager.initializeConversationState(conversationId, context);
            ReActState reactState = getReActStateFromConversation(conversationState);
            
            // Execute the reasoning loop with streaming callbacks
            AgentResponse response = executeReasoningLoopWithStreaming(
                    conversationId, message, context, reactState,
                    onThinking, onAction, onObservation);
            
            // Update conversation state
            updateConversationStateFromReAct(conversationState, reactState);
            stateManager.updateConversationState(conversationId, conversationState);
            
            // Send final response
            onComplete.accept(response);
            
        } catch (Exception e) {
            logger.error("Error processing message with streaming for conversation {}: {}", conversationId, e.getMessage(), e);
            stateManager.handleConversationError(conversationId, e.getMessage(), e);
            onComplete.accept(createErrorResponse("An error occurred while processing your message: " + e.getMessage()));
        }
    }
    
    @Override
    public boolean isStreamingSupported() {
        return true;
    }
    
    /**
     * Execute the reasoning loop with streaming callbacks.
     */
    private AgentResponse executeReasoningLoopWithStreaming(String conversationId, String userMessage, 
                                                          AgentContext context, ReActState state,
                                                          Consumer<String> onThinking,
                                                          Consumer<String> onAction,
                                                          Consumer<String> onObservation) {
        
        while (state.getIterationCount() < state.getMaxIterations()) {
            state.setIterationCount(state.getIterationCount() + 1);
            
            switch (state.getCurrentPhase()) {
                case THINKING:
                    return executeThinkingPhaseWithStreaming(conversationId, userMessage, context, state, onThinking, onAction);
                    
                case ACTING:
                    return executeActingPhaseWithStreaming(conversationId, context, state, onAction, onObservation);
                    
                case OBSERVING:
                    return executeObservingPhaseWithStreaming(conversationId, context, state, onObservation, onThinking);
                    
                default:
                    logger.warn("Unknown ReAct phase: {}", state.getCurrentPhase());
                    return createErrorResponse("Unknown reasoning phase encountered");
            }
        }
        
        // Max iterations reached
        logger.warn("Max iterations ({}) reached for conversation: {}", state.getMaxIterations(), conversationId);
        return createResponse("I've reached the maximum number of reasoning steps. Let me provide what I've learned so far.", 
                            AgentResponse.ResponseType.TEXT);
    }
    
    /**
     * Execute thinking phase with streaming.
     */
    private AgentResponse executeThinkingPhaseWithStreaming(String conversationId, String userMessage, 
                                                          AgentContext context, ReActState state,
                                                          Consumer<String> onThinking,
                                                          Consumer<String> onAction) {
        logger.debug("Executing thinking phase with streaming for conversation: {}", conversationId);
        
        try {
            onThinking.accept("Analyzing your request and considering available options...");
            
            // Get preferred provider
            String providerId = getPreferredProvider(context);
            
            // Get available tools description
            String toolsDescription = toolIntegrationService.getAvailableToolsDescription();
            
            // Build the prompt for thinking
            String prompt = promptBuilder.buildThinkingPrompt(userMessage, state, context, providerId, toolsDescription);
            
            // Get LLM response
            LLMRequest request = createLLMRequest(prompt, context);
            LLMResponse llmResponse = llmProviderManager.generateResponse(request, providerId);
            
            // Parse the response to extract thought and potential action
            ReActResponseParser.ReActThought thought = responseParser.parseThinkingResponse(llmResponse.getContent());
            
            state.setCurrentThought(thought.getThought());
            onThinking.accept(thought.getThought());
            
            if (thought.hasAction()) {
                // Move to acting phase
                state.setPendingAction(thought.getAction());
                state.setCurrentPhase(ReActPhase.ACTING);
                
                onAction.accept("Executing action: " + thought.getAction().getToolName());
                
                // Continue to acting phase
                return executeActingPhaseWithStreaming(conversationId, context, state, onAction, null);
            } else {
                // No action needed, return the thought as final response
                return createResponse(thought.getThought(), AgentResponse.ResponseType.TEXT);
            }
            
        } catch (Exception e) {
            logger.error("Error in thinking phase with streaming for conversation {}: {}", conversationId, e.getMessage(), e);
            return createErrorResponse("Error during thinking phase: " + e.getMessage());
        }
    }
    
    /**
     * Execute acting phase with streaming.
     */
    private AgentResponse executeActingPhaseWithStreaming(String conversationId, AgentContext context, ReActState state,
                                                        Consumer<String> onAction,
                                                        Consumer<String> onObservation) {
        logger.debug("Executing acting phase with streaming for conversation: {}", conversationId);
        
        try {
            ToolCall action = state.getPendingAction();
            if (action == null) {
                logger.warn("No pending action found in acting phase for conversation: {}", conversationId);
                state.setCurrentPhase(ReActPhase.THINKING);
                return executeThinkingPhaseWithStreaming(conversationId, null, context, state, null, onAction);
            }
            
            if (onAction != null) {
                onAction.accept("Executing " + action.getToolName() + " with parameters: " + action.getParameters());
            }
            
            // Execute the tool using the integration service
            ToolResult result = toolIntegrationService.executeToolSafely(action.getToolName(), action.getParameters());
            
            // Add result to observations
            if (state.getObservations() == null) {
                state.setObservations(new ArrayList<>());
            }
            state.getObservations().add(result);
            
            // Move to observing phase
            state.setCurrentPhase(ReActPhase.OBSERVING);
            state.setPendingAction(null);
            
            if (onObservation != null) {
                String observationText = result.isSuccess() ? 
                    "Tool executed successfully: " + result.getResult() :
                    "Tool execution failed: " + result.getErrorMessage();
                onObservation.accept(observationText);
            }
            
            // Continue to observing phase
            return executeObservingPhaseWithStreaming(conversationId, context, state, onObservation, null);
            
        } catch (Exception e) {
            logger.error("Error in acting phase with streaming for conversation {}: {}", conversationId, e.getMessage(), e);
            
            // Create error observation and move to observing phase
            ToolResult errorResult = new ToolResult();
            errorResult.setToolName(state.getPendingAction() != null ? state.getPendingAction().getToolName() : "unknown");
            errorResult.setSuccess(false);
            errorResult.setErrorMessage(e.getMessage());
            
            if (state.getObservations() == null) {
                state.setObservations(new ArrayList<>());
            }
            state.getObservations().add(errorResult);
            
            state.setCurrentPhase(ReActPhase.OBSERVING);
            state.setPendingAction(null);
            
            if (onObservation != null) {
                onObservation.accept("Tool execution failed: " + e.getMessage());
            }
            
            return executeObservingPhaseWithStreaming(conversationId, context, state, onObservation, null);
        }
    }
    
    /**
     * Execute observing phase with streaming.
     */
    private AgentResponse executeObservingPhaseWithStreaming(String conversationId, AgentContext context, ReActState state,
                                                           Consumer<String> onObservation,
                                                           Consumer<String> onThinking) {
        logger.debug("Executing observing phase with streaming for conversation: {}", conversationId);
        
        try {
            // Get the latest observation
            List<ToolResult> observations = state.getObservations();
            if (observations == null || observations.isEmpty()) {
                logger.warn("No observations found in observing phase for conversation: {}", conversationId);
                state.setCurrentPhase(ReActPhase.THINKING);
                return executeThinkingPhaseWithStreaming(conversationId, null, context, state, onThinking, null);
            }
            
            ToolResult latestObservation = observations.get(observations.size() - 1);
            
            if (onObservation != null) {
                onObservation.accept("Processing the results and determining next steps...");
            }
            
            // Get preferred provider
            String providerId = getPreferredProvider(context);
            
            // Build prompt for observation processing
            String prompt = promptBuilder.buildObservingPrompt(state, latestObservation, context, providerId);
            
            // Get LLM response
            LLMRequest request = createLLMRequest(prompt, context);
            LLMResponse llmResponse = llmProviderManager.generateResponse(request, providerId);
            
            // Parse the response to determine next action
            ReActResponseParser.ReActObservation observation = responseParser.parseObservingResponse(llmResponse.getContent());
            
            if (observation.isComplete()) {
                // Task is complete, return final response
                return createResponse(observation.getFinalAnswer(), AgentResponse.ResponseType.TEXT);
            } else {
                // Continue reasoning - move back to thinking phase
                state.setCurrentPhase(ReActPhase.THINKING);
                if (onThinking != null) {
                    onThinking.accept("Continuing to analyze the situation...");
                }
                return executeThinkingPhaseWithStreaming(conversationId, null, context, state, onThinking, null);
            }
            
        } catch (Exception e) {
            logger.error("Error in observing phase with streaming for conversation {}: {}", conversationId, e.getMessage(), e);
            return createErrorResponse("Error during observation phase: " + e.getMessage());
        }
    }
}