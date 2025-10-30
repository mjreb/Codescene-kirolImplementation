package com.agent.infrastructure.react;

import com.agent.domain.interfaces.LLMProviderManager;
import com.agent.domain.interfaces.MemoryManager;
import com.agent.domain.interfaces.ToolFramework;
import com.agent.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the ReAct Engine implementation.
 * Tests complete reasoning loops with mock LLM and tools.
 */
@ExtendWith(MockitoExtension.class)
class ReActEngineIntegrationTest {
    
    private ReActEngineImpl reactEngine;
    
    @Mock
    private LLMProviderManager llmProviderManager;
    
    @Mock
    private ToolFramework toolFramework;
    
    @Mock
    private MemoryManager memoryManager;
    
    @Mock
    private ToolIntegrationService toolIntegrationService;
    
    @Mock
    private ConversationStateManager stateManager;
    
    private AgentContext testContext;
    private String testConversationId;
    
    @BeforeEach
    void setUp() {
        reactEngine = new ReActEngineImpl(
            llmProviderManager,
            toolFramework,
            memoryManager,
            toolIntegrationService,
            stateManager
        );
        
        testConversationId = "test-conversation-123";
        testContext = createTestAgentContext();
        
        // Setup default mocks
        setupDefaultMocks();
    }
    
    @Test
    void testProcessMessage_SimpleThoughtResponse() {
        // Given: LLM returns a simple thought without action
        String userMessage = "What is 2 + 2?";
        String llmResponse = "Thought: I can calculate this simple math problem directly. 2 + 2 equals 4.\n\nFinal Answer: 4";
        
        setupLLMResponse(llmResponse);
        setupConversationState();
        
        // When
        AgentResponse response = reactEngine.processMessage(testConversationId, userMessage, testContext);
        
        // Then
        assertNotNull(response);
        assertEquals(AgentResponse.ResponseType.TEXT, response.getType());
        assertEquals("4", response.getContent());
        
        verify(stateManager).initializeConversationState(testConversationId, testContext);
        verify(stateManager).updateConversationState(eq(testConversationId), any(ConversationState.class));
    }
    
    @Test
    void testProcessMessage_WithToolExecution() {
        // Given: LLM returns thought with tool action
        String userMessage = "Calculate 15 * 23";
        String thinkingResponse = "Thought: I need to calculate 15 * 23. I'll use the calculator tool.\n\n" +
                                "Action: calculator\n" +
                                "Parameters: {\"expression\": \"15 * 23\"}";
        
        String observingResponse = "Final Answer: The result of 15 * 23 is 345.";
        
        setupLLMResponses(thinkingResponse, observingResponse);
        setupConversationState();
        setupToolExecution("calculator", Map.of("expression", "15 * 23"), 345);
        
        // When
        AgentResponse response = reactEngine.processMessage(testConversationId, userMessage, testContext);
        
        // Then
        assertNotNull(response);
        assertEquals(AgentResponse.ResponseType.TEXT, response.getType());
        assertEquals("The result of 15 * 23 is 345.", response.getContent());
        
        verify(toolIntegrationService).executeToolSafely("calculator", Map.of("expression", "15 * 23"));
        verify(stateManager, atLeast(1)).updateConversationState(eq(testConversationId), any(ConversationState.class));
    }
    
    @Test
    void testProcessMessage_MultipleIterations() {
        // Given: Multiple reasoning iterations
        String userMessage = "Find information about Java programming and then calculate 10 + 5";
        
        String[] llmResponses = {
            "Thought: I need to search for information about Java programming first.\n\n" +
            "Action: web_search\n" +
            "Parameters: {\"query\": \"Java programming language\"}",
            
            "Thought: Now I have information about Java. Next, I need to calculate 10 + 5.\n\n" +
            "Action: calculator\n" +
            "Parameters: {\"expression\": \"10 + 5\"}",
            
            "Final Answer: Java is a popular programming language. The calculation 10 + 5 equals 15."
        };
        
        setupMultipleLLMResponses(llmResponses);
        setupConversationState();
        setupToolExecution("web_search", Map.of("query", "Java programming language"), "Java is a popular programming language...");
        setupToolExecution("calculator", Map.of("expression", "10 + 5"), 15);
        
        // When
        AgentResponse response = reactEngine.processMessage(testConversationId, userMessage, testContext);
        
        // Then
        assertNotNull(response);
        assertEquals(AgentResponse.ResponseType.TEXT, response.getType());
        assertTrue(response.getContent().contains("Java is a popular programming language"));
        assertTrue(response.getContent().contains("15"));
        
        verify(toolIntegrationService).executeToolSafely("web_search", Map.of("query", "Java programming language"));
        verify(toolIntegrationService).executeToolSafely("calculator", Map.of("expression", "10 + 5"));
    }
    
    @Test
    void testProcessMessage_ToolExecutionFailure() {
        // Given: Tool execution fails
        String userMessage = "Calculate something complex";
        String thinkingResponse = "Thought: I'll use the calculator.\n\n" +
                                "Action: calculator\n" +
                                "Parameters: {\"expression\": \"invalid expression\"}";
        
        String recoveryResponse = "Final Answer: I encountered an error with the calculation. Please provide a valid mathematical expression.";
        
        setupLLMResponses(thinkingResponse, recoveryResponse);
        setupConversationState();
        setupFailedToolExecution("calculator", "Invalid mathematical expression");
        
        // When
        AgentResponse response = reactEngine.processMessage(testConversationId, userMessage, testContext);
        
        // Then
        assertNotNull(response);
        assertEquals(AgentResponse.ResponseType.TEXT, response.getType());
        assertTrue(response.getContent().contains("error"));
        
        verify(toolIntegrationService).executeToolSafely(eq("calculator"), any());
    }
    
    @Test
    void testContinueReasoning() {
        // Given: Existing conversation state and tool result
        ToolResult toolResult = createSuccessfulToolResult("calculator", 42);
        setupConversationStateForContinuation();
        
        String observingResponse = "Final Answer: The calculation result is 42.";
        setupLLMResponse(observingResponse);
        
        // When
        reactEngine.continueReasoning(testConversationId, toolResult);
        
        // Then
        verify(stateManager).getConversationState(testConversationId);
        verify(stateManager).transitionState(testConversationId, ReActPhase.OBSERVING);
        verify(stateManager, atLeast(1)).updateConversationState(eq(testConversationId), any(ConversationState.class));
    }
    
    @Test
    void testGetConversationState() {
        // Given: Existing conversation state
        ConversationState expectedState = createTestConversationState();
        when(stateManager.getConversationState(testConversationId)).thenReturn(expectedState);
        
        // When
        ConversationState actualState = reactEngine.getConversationState(testConversationId);
        
        // Then
        assertEquals(expectedState, actualState);
        verify(stateManager).getConversationState(testConversationId);
    }
    
    @Test
    void testErrorHandling_LLMProviderFailure() {
        // Given: LLM provider fails
        String userMessage = "Test message";
        setupConversationState();
        when(llmProviderManager.generateResponse(any(LLMRequest.class), anyString()))
            .thenThrow(new RuntimeException("LLM provider unavailable"));
        
        // When
        AgentResponse response = reactEngine.processMessage(testConversationId, userMessage, testContext);
        
        // Then
        assertNotNull(response);
        assertEquals(AgentResponse.ResponseType.ERROR, response.getType());
        assertTrue(response.getContent().contains("error occurred"));
        
        verify(stateManager).handleConversationError(eq(testConversationId), anyString(), any(Exception.class));
    }
    
    @Test
    void testErrorHandling_StateManagerFailure() {
        // Given: State manager fails
        String userMessage = "Test message";
        when(stateManager.initializeConversationState(testConversationId, testContext))
            .thenThrow(new RuntimeException("State manager failure"));
        
        // When
        AgentResponse response = reactEngine.processMessage(testConversationId, userMessage, testContext);
        
        // Then
        assertNotNull(response);
        assertEquals(AgentResponse.ResponseType.ERROR, response.getType());
        assertTrue(response.getContent().contains("error occurred"));
        
        verify(stateManager).handleConversationError(eq(testConversationId), anyString(), any(Exception.class));
    }
    
    @Test
    void testMaxIterationsReached() {
        // Given: ReAct state with max iterations reached
        String userMessage = "Complex task";
        ConversationState conversationState = createTestConversationState();
        ReActState reactState = conversationState.getContext().getReActState();
        reactState.setIterationCount(10);
        reactState.setMaxIterations(10);
        
        when(stateManager.initializeConversationState(testConversationId, testContext))
            .thenReturn(conversationState);
        
        // When
        AgentResponse response = reactEngine.processMessage(testConversationId, userMessage, testContext);
        
        // Then
        assertNotNull(response);
        assertEquals(AgentResponse.ResponseType.TEXT, response.getType());
        assertTrue(response.getContent().contains("maximum number of reasoning steps"));
    }
    
    // Helper methods
    
    private void setupDefaultMocks() {
        when(llmProviderManager.getAvailableProviders()).thenReturn(Arrays.asList("openai", "anthropic"));
        when(toolIntegrationService.getAvailableToolsDescription()).thenReturn("Available tools: calculator, web_search");
        when(toolIntegrationService.formatToolResultForObservation(any())).thenReturn("Tool executed successfully");
    }
    
    private void setupLLMResponse(String response) {
        LLMResponse llmResponse = new LLMResponse(response, "gpt-3.5-turbo", "openai");
        when(llmProviderManager.generateResponse(any(LLMRequest.class), anyString()))
            .thenReturn(llmResponse);
    }
    
    private void setupLLMResponses(String... responses) {
        LLMResponse[] llmResponses = Arrays.stream(responses)
            .map(content -> new LLMResponse(content, "gpt-3.5-turbo", "openai"))
            .toArray(LLMResponse[]::new);
        
        when(llmProviderManager.generateResponse(any(LLMRequest.class), anyString()))
            .thenReturn(llmResponses[0], Arrays.copyOfRange(llmResponses, 1, llmResponses.length));
    }
    
    private void setupMultipleLLMResponses(String[] responses) {
        setupLLMResponses(responses);
    }
    
    private void setupConversationState() {
        ConversationState conversationState = createTestConversationState();
        when(stateManager.initializeConversationState(testConversationId, testContext))
            .thenReturn(conversationState);
        when(stateManager.getConversationState(testConversationId))
            .thenReturn(conversationState);
    }
    
    private void setupConversationStateForContinuation() {
        ConversationState conversationState = createTestConversationState();
        conversationState.setCurrentPhase(ReActPhase.ACTING);
        
        when(stateManager.getConversationState(testConversationId))
            .thenReturn(conversationState);
        when(memoryManager.retrieveConversationContext(testConversationId))
            .thenReturn(conversationState.getContext());
    }
    
    private void setupToolExecution(String toolName, Map<String, Object> parameters, Object result) {
        ToolResult toolResult = createSuccessfulToolResult(toolName, result);
        when(toolIntegrationService.executeToolSafely(toolName, parameters))
            .thenReturn(toolResult);
    }
    
    private void setupFailedToolExecution(String toolName, String errorMessage) {
        ToolResult toolResult = createFailedToolResult(toolName, errorMessage);
        when(toolIntegrationService.executeToolSafely(eq(toolName), any()))
            .thenReturn(toolResult);
    }
    
    private AgentContext createTestAgentContext() {
        AgentConfiguration config = new AgentConfiguration();
        return new AgentContext("test-agent", "test-user", new HashMap<>(), config);
    }
    
    private ConversationState createTestConversationState() {
        ConversationState state = new ConversationState(testConversationId, ConversationState.ConversationStatus.ACTIVE);
        state.setCurrentPhase(ReActPhase.THINKING);
        state.setLastActivity(Instant.now());
        
        ConversationContext context = new ConversationContext(testConversationId, "test-agent", "test-user");
        ReActState reactState = new ReActState(testConversationId);
        reactState.setCurrentPhase(ReActPhase.THINKING);
        context.setReActState(reactState);
        
        state.setContext(context);
        return state;
    }
    
    private ToolResult createSuccessfulToolResult(String toolName, Object result) {
        ToolResult toolResult = new ToolResult(toolName, true, result);
        toolResult.setDurationMs(100);
        return toolResult;
    }
    
    private ToolResult createFailedToolResult(String toolName, String errorMessage) {
        ToolResult toolResult = new ToolResult(toolName, false, null);
        toolResult.setErrorMessage(errorMessage);
        return toolResult;
    }
}