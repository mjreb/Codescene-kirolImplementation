package com.agent.infrastructure.react;

import com.agent.domain.interfaces.MemoryManager;
import com.agent.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConversationStateManager.
 */
@ExtendWith(MockitoExtension.class)
class ConversationStateManagerTest {
    
    private ConversationStateManager stateManager;
    
    @Mock
    private MemoryManager memoryManager;
    
    private String testConversationId;
    private AgentContext testContext;
    
    @BeforeEach
    void setUp() {
        stateManager = new ConversationStateManager(memoryManager);
        testConversationId = "test-conversation-123";
        testContext = createTestAgentContext();
    }
    
    @Test
    void testInitializeConversationState_NewConversation() {
        // Given
        when(memoryManager.retrieveConversationContext(testConversationId)).thenReturn(null);
        
        // When
        ConversationState state = stateManager.initializeConversationState(testConversationId, testContext);
        
        // Then
        assertNotNull(state);
        assertEquals(testConversationId, state.getConversationId());
        assertEquals(ConversationState.ConversationStatus.ACTIVE, state.getStatus());
        assertEquals(ReActPhase.THINKING, state.getCurrentPhase());
        assertNotNull(state.getContext());
        assertEquals("test-agent", state.getContext().getAgentId());
        assertEquals("test-user", state.getContext().getUserId());
        assertNotNull(state.getContext().getReActState());
    }
    
    @Test
    void testInitializeConversationState_ExistingConversation() {
        // Given
        ConversationContext existingContext = createTestConversationContext();
        when(memoryManager.retrieveConversationContext(testConversationId)).thenReturn(existingContext);
        
        // When
        ConversationState state = stateManager.initializeConversationState(testConversationId, testContext);
        
        // Then
        assertNotNull(state);
        assertEquals(testConversationId, state.getConversationId());
        assertEquals(existingContext, state.getContext());
        assertEquals(ReActPhase.THINKING, state.getCurrentPhase()); // From ReActState
    }
    
    @Test
    void testUpdateConversationState() {
        // Given
        ConversationState state = createTestConversationState();
        
        // When
        stateManager.updateConversationState(testConversationId, state);
        
        // Then
        assertNotNull(state.getLastActivity());
        verify(memoryManager).storeConversationContext(eq(testConversationId), any(ConversationContext.class));
    }
    
    @Test
    void testGetConversationState_FromCache() {
        // Given: Initialize state first (puts it in cache)
        when(memoryManager.retrieveConversationContext(testConversationId)).thenReturn(null);
        ConversationState initializedState = stateManager.initializeConversationState(testConversationId, testContext);
        
        // When
        ConversationState retrievedState = stateManager.getConversationState(testConversationId);
        
        // Then
        assertEquals(initializedState, retrievedState);
    }
    
    @Test
    void testGetConversationState_FromPersistentMemory() {
        // Given
        ConversationContext persistentContext = createTestConversationContext();
        when(memoryManager.retrieveConversationContext(testConversationId)).thenReturn(persistentContext);
        
        // When
        ConversationState state = stateManager.getConversationState(testConversationId);
        
        // Then
        assertNotNull(state);
        assertEquals(testConversationId, state.getConversationId());
        assertEquals(persistentContext, state.getContext());
    }
    
    @Test
    void testTransitionState() {
        // Given
        ConversationState state = createTestConversationState();
        when(memoryManager.retrieveConversationContext(testConversationId)).thenReturn(state.getContext());
        
        // When
        stateManager.transitionState(testConversationId, ReActPhase.ACTING);
        
        // Then
        ConversationState updatedState = stateManager.getConversationState(testConversationId);
        assertEquals(ReActPhase.ACTING, updatedState.getCurrentPhase());
        verify(memoryManager, atLeast(1)).storeConversationContext(eq(testConversationId), any(ConversationContext.class));
    }
    
    @Test
    void testTransitionState_ConversationNotFound() {
        // Given
        when(memoryManager.retrieveConversationContext(testConversationId)).thenReturn(null);
        
        // When
        stateManager.transitionState(testConversationId, ReActPhase.ACTING);
        
        // Then
        // Should not throw exception, just log warning
        verify(memoryManager, never()).storeConversationContext(any(), any());
    }
    
    @Test
    void testHandleConversationError() {
        // Given
        ConversationState state = createTestConversationState();
        when(memoryManager.retrieveConversationContext(testConversationId)).thenReturn(state.getContext());
        Exception testException = new RuntimeException("Test error");
        
        // When
        stateManager.handleConversationError(testConversationId, "Test error message", testException);
        
        // Then
        ConversationState updatedState = stateManager.getConversationState(testConversationId);
        assertEquals(ConversationState.ConversationStatus.ERROR, updatedState.getStatus());
        
        // Check error information in context
        assertNotNull(updatedState.getContext().getContextData());
        assertEquals("Test error message", updatedState.getContext().getContextData().get("last_error"));
        assertEquals(1, updatedState.getContext().getContextData().get("error_count"));
        
        verify(memoryManager, atLeast(1)).storeConversationContext(eq(testConversationId), any(ConversationContext.class));
    }
    
    @Test
    void testAttemptConversationRecovery_Success() {
        // Given
        ConversationState errorState = createTestConversationState();
        errorState.setStatus(ConversationState.ConversationStatus.ERROR);
        when(memoryManager.retrieveConversationContext(testConversationId)).thenReturn(errorState.getContext());
        
        // When
        boolean recovered = stateManager.attemptConversationRecovery(testConversationId);
        
        // Then
        assertTrue(recovered);
        ConversationState recoveredState = stateManager.getConversationState(testConversationId);
        assertEquals(ConversationState.ConversationStatus.ACTIVE, recoveredState.getStatus());
        assertEquals(ReActPhase.THINKING, recoveredState.getCurrentPhase());
        
        // Check recovery timestamp
        assertNotNull(recoveredState.getContext().getContextData());
        assertTrue(recoveredState.getContext().getContextData().containsKey("recovery_timestamp"));
        
        verify(memoryManager, atLeast(1)).storeConversationContext(eq(testConversationId), any(ConversationContext.class));
    }
    
    @Test
    void testAttemptConversationRecovery_NotInErrorState() {
        // Given
        ConversationState activeState = createTestConversationState();
        activeState.setStatus(ConversationState.ConversationStatus.ACTIVE);
        when(memoryManager.retrieveConversationContext(testConversationId)).thenReturn(activeState.getContext());
        
        // When
        boolean recovered = stateManager.attemptConversationRecovery(testConversationId);
        
        // Then
        assertTrue(recovered); // Should return true as no recovery needed
    }
    
    @Test
    void testAttemptConversationRecovery_ConversationNotFound() {
        // Given
        when(memoryManager.retrieveConversationContext(testConversationId)).thenReturn(null);
        
        // When
        boolean recovered = stateManager.attemptConversationRecovery(testConversationId);
        
        // Then
        assertFalse(recovered);
    }
    
    @Test
    void testTerminateConversation() {
        // Given
        ConversationState state = createTestConversationState();
        when(memoryManager.retrieveConversationContext(testConversationId)).thenReturn(state.getContext());
        
        // When
        stateManager.terminateConversation(testConversationId);
        
        // Then
        // State should be updated to completed
        verify(memoryManager, atLeast(1)).storeConversationContext(eq(testConversationId), any(ConversationContext.class));
        
        // Should be removed from cache
        ConversationState retrievedState = stateManager.getConversationState(testConversationId);
        // Will be retrieved from memory manager again since removed from cache
        verify(memoryManager, atLeast(2)).retrieveConversationContext(testConversationId);
    }
    
    @Test
    void testGetConversationFlow() {
        // Given
        stateManager.initializeConversationState(testConversationId, testContext);
        
        // When
        ConversationStateManager.ConversationFlow flow = stateManager.getConversationFlow(testConversationId);
        
        // Then
        assertNotNull(flow);
        assertEquals(testConversationId, flow.getConversationId());
        assertEquals("test-agent", flow.getAgentId());
        assertEquals("test-user", flow.getUserId());
        assertNotNull(flow.getStartTime());
    }
    
    @Test
    void testConversationFlow_RecordTransition() {
        // Given
        stateManager.initializeConversationState(testConversationId, testContext);
        ConversationStateManager.ConversationFlow flow = stateManager.getConversationFlow(testConversationId);
        
        // When
        flow.recordTransition(ReActPhase.THINKING, ReActPhase.ACTING);
        
        // Then
        assertEquals(1, flow.getTransitions().size());
        ConversationStateManager.PhaseTransition transition = flow.getTransitions().get(0);
        assertEquals(ReActPhase.THINKING, transition.getFromPhase());
        assertEquals(ReActPhase.ACTING, transition.getToPhase());
        assertNotNull(transition.getTimestamp());
    }
    
    @Test
    void testConversationFlow_RecordError() {
        // Given
        stateManager.initializeConversationState(testConversationId, testContext);
        ConversationStateManager.ConversationFlow flow = stateManager.getConversationFlow(testConversationId);
        
        // When
        flow.recordError("Test error");
        
        // Then
        assertEquals(1, flow.getErrors().size());
        assertEquals("Test error", flow.getErrors().get(0));
    }
    
    // Helper methods
    
    private AgentContext createTestAgentContext() {
        AgentConfiguration config = new AgentConfiguration();
        return new AgentContext("test-agent", "test-user", new HashMap<>(), config);
    }
    
    private ConversationContext createTestConversationContext() {
        ConversationContext context = new ConversationContext(testConversationId, "test-agent", "test-user");
        ReActState reactState = new ReActState(testConversationId);
        reactState.setCurrentPhase(ReActPhase.THINKING);
        context.setReActState(reactState);
        context.setContextData(new HashMap<>());
        return context;
    }
    
    private ConversationState createTestConversationState() {
        ConversationState state = new ConversationState(testConversationId, ConversationState.ConversationStatus.ACTIVE);
        state.setCurrentPhase(ReActPhase.THINKING);
        state.setLastActivity(Instant.now());
        state.setContext(createTestConversationContext());
        return state;
    }
}