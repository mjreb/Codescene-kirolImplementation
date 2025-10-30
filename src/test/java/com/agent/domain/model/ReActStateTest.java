package com.agent.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReActStateTest {
    
    private ReActState reActState;
    private ToolCall toolCall;
    private ToolResult toolResult;
    
    @BeforeEach
    void setUp() {
        reActState = new ReActState();
        
        Map<String, Object> params = new HashMap<>();
        params.put("query", "test");
        toolCall = new ToolCall("calculator", params);
        
        toolResult = new ToolResult("calculator", true, "42");
    }
    
    @Test
    void testReActStateCreation() {
        ReActState newState = new ReActState("conv-1");
        
        assertEquals("conv-1", newState.getConversationId());
        assertEquals(ReActPhase.THINKING, newState.getCurrentPhase());
        assertEquals(0, newState.getIterationCount());
        assertEquals(10, newState.getMaxIterations());
    }
    
    @Test
    void testDefaultConstructor() {
        ReActState defaultState = new ReActState();
        
        assertEquals(ReActPhase.THINKING, defaultState.getCurrentPhase());
        assertEquals(0, defaultState.getIterationCount());
        assertEquals(10, defaultState.getMaxIterations());
    }
    
    @Test
    void testPhaseTransitions() {
        // Test phase transitions
        reActState.setCurrentPhase(ReActPhase.THINKING);
        assertEquals(ReActPhase.THINKING, reActState.getCurrentPhase());
        
        reActState.setCurrentPhase(ReActPhase.ACTING);
        assertEquals(ReActPhase.ACTING, reActState.getCurrentPhase());
        
        reActState.setCurrentPhase(ReActPhase.OBSERVING);
        assertEquals(ReActPhase.OBSERVING, reActState.getCurrentPhase());
    }
    
    @Test
    void testIterationManagement() {
        assertEquals(0, reActState.getIterationCount());
        
        reActState.setIterationCount(5);
        assertEquals(5, reActState.getIterationCount());
        
        reActState.setMaxIterations(20);
        assertEquals(20, reActState.getMaxIterations());
    }
    
    @Test
    void testCurrentThought() {
        String thought = "I need to calculate the sum of 2 and 2";
        reActState.setCurrentThought(thought);
        
        assertEquals(thought, reActState.getCurrentThought());
    }
    
    @Test
    void testPendingAction() {
        reActState.setPendingAction(toolCall);
        
        assertEquals(toolCall, reActState.getPendingAction());
        assertEquals("calculator", reActState.getPendingAction().getToolName());
    }
    
    @Test
    void testObservations() {
        List<ToolResult> observations = new ArrayList<>();
        observations.add(toolResult);
        
        reActState.setObservations(observations);
        
        assertEquals(observations, reActState.getObservations());
        assertEquals(1, reActState.getObservations().size());
        assertEquals(toolResult, reActState.getObservations().get(0));
    }
    
    @Test
    void testConversationId() {
        String conversationId = "conv-123";
        reActState.setConversationId(conversationId);
        
        assertEquals(conversationId, reActState.getConversationId());
    }
    
    @Test
    void testCompleteReActCycle() {
        // Simulate a complete ReAct cycle
        reActState.setConversationId("conv-1");
        reActState.setCurrentPhase(ReActPhase.THINKING);
        reActState.setCurrentThought("I need to perform a calculation");
        reActState.setIterationCount(1);
        
        // Move to acting phase
        reActState.setCurrentPhase(ReActPhase.ACTING);
        reActState.setPendingAction(toolCall);
        
        // Move to observing phase
        reActState.setCurrentPhase(ReActPhase.OBSERVING);
        List<ToolResult> observations = new ArrayList<>();
        observations.add(toolResult);
        reActState.setObservations(observations);
        
        // Verify final state
        assertEquals("conv-1", reActState.getConversationId());
        assertEquals(ReActPhase.OBSERVING, reActState.getCurrentPhase());
        assertEquals("I need to perform a calculation", reActState.getCurrentThought());
        assertEquals(toolCall, reActState.getPendingAction());
        assertEquals(1, reActState.getObservations().size());
        assertEquals(1, reActState.getIterationCount());
    }
}