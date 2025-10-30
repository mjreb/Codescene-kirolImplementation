package com.agent.infrastructure.react;

import com.agent.domain.interfaces.MemoryManager;
import com.agent.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing conversation state and flow control in the ReAct Engine.
 * Handles state persistence, recovery, and transitions.
 */
@Service
public class ConversationStateManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationStateManager.class);
    
    private final MemoryManager memoryManager;
    
    // In-memory cache for active conversation states
    private final Map<String, ConversationState> activeStates = new ConcurrentHashMap<>();
    
    // Conversation flow control
    private final Map<String, ConversationFlow> conversationFlows = new ConcurrentHashMap<>();
    
    @Autowired
    public ConversationStateManager(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }
    
    /**
     * Initialize or retrieve conversation state.
     */
    public ConversationState initializeConversationState(String conversationId, AgentContext context) {
        logger.debug("Initializing conversation state for: {}", conversationId);
        
        // Check if state exists in memory cache
        ConversationState state = activeStates.get(conversationId);
        if (state != null) {
            logger.debug("Found active conversation state for: {}", conversationId);
            return state;
        }
        
        // Try to retrieve from persistent memory
        ConversationContext persistentContext = memoryManager.retrieveConversationContext(conversationId);
        
        if (persistentContext != null) {
            // Restore state from persistent context
            state = restoreStateFromContext(persistentContext);
            logger.debug("Restored conversation state from persistent memory for: {}", conversationId);
        } else {
            // Create new state
            state = createNewConversationState(conversationId, context);
            logger.debug("Created new conversation state for: {}", conversationId);
        }
        
        // Cache the state
        activeStates.put(conversationId, state);
        
        // Initialize conversation flow
        initializeConversationFlow(conversationId, context);
        
        return state;
    }
    
    /**
     * Update conversation state and persist changes.
     */
    public void updateConversationState(String conversationId, ConversationState state) {
        logger.debug("Updating conversation state for: {}", conversationId);
        
        try {
            // Update timestamp
            state.setLastActivity(Instant.now());
            
            // Update in-memory cache
            activeStates.put(conversationId, state);
            
            // Persist to memory manager
            ConversationContext context = convertStateToContext(state);
            memoryManager.storeConversationContext(conversationId, context);
            
            // Update conversation flow
            updateConversationFlow(conversationId, state);
            
            logger.debug("Successfully updated conversation state for: {}", conversationId);
            
        } catch (Exception e) {
            logger.error("Failed to update conversation state for {}: {}", conversationId, e.getMessage(), e);
            throw new RuntimeException("Failed to update conversation state", e);
        }
    }
    
    /**
     * Get current conversation state.
     */
    public ConversationState getConversationState(String conversationId) {
        ConversationState state = activeStates.get(conversationId);
        
        if (state == null) {
            // Try to retrieve from persistent memory
            ConversationContext context = memoryManager.retrieveConversationContext(conversationId);
            if (context != null) {
                state = restoreStateFromContext(context);
                activeStates.put(conversationId, state);
            }
        }
        
        return state;
    }
    
    /**
     * Handle conversation state transitions.
     */
    public void transitionState(String conversationId, ReActPhase newPhase) {
        logger.debug("Transitioning conversation {} to phase: {}", conversationId, newPhase);
        
        ConversationState state = getConversationState(conversationId);
        if (state == null) {
            logger.warn("Cannot transition state - conversation not found: {}", conversationId);
            return;
        }
        
        ReActPhase oldPhase = state.getCurrentPhase();
        state.setCurrentPhase(newPhase);
        
        // Update conversation flow
        ConversationFlow flow = conversationFlows.get(conversationId);
        if (flow != null) {
            flow.recordTransition(oldPhase, newPhase);
        }
        
        updateConversationState(conversationId, state);
        
        logger.debug("Successfully transitioned conversation {} from {} to {}", 
                    conversationId, oldPhase, newPhase);
    }
    
    /**
     * Handle conversation errors and recovery.
     */
    public void handleConversationError(String conversationId, String errorMessage, Exception exception) {
        logger.error("Handling conversation error for {}: {}", conversationId, errorMessage, exception);
        
        ConversationState state = getConversationState(conversationId);
        if (state == null) {
            logger.warn("Cannot handle error - conversation not found: {}", conversationId);
            return;
        }
        
        // Update state to error status
        state.setStatus(ConversationState.ConversationStatus.ERROR);
        
        // Add error information to context
        if (state.getContext() != null) {
            Map<String, Object> contextData = state.getContext().getContextData();
            if (contextData == null) {
                contextData = new HashMap<>();
                state.getContext().setContextData(contextData);
            }
            
            contextData.put("last_error", errorMessage);
            contextData.put("error_timestamp", Instant.now().toString());
            contextData.put("error_count", (Integer) contextData.getOrDefault("error_count", 0) + 1);
        }
        
        // Update conversation flow
        ConversationFlow flow = conversationFlows.get(conversationId);
        if (flow != null) {
            flow.recordError(errorMessage);
        }
        
        updateConversationState(conversationId, state);
    }
    
    /**
     * Attempt to recover conversation from error state.
     */
    public boolean attemptConversationRecovery(String conversationId) {
        logger.info("Attempting conversation recovery for: {}", conversationId);
        
        ConversationState state = getConversationState(conversationId);
        if (state == null) {
            logger.warn("Cannot recover - conversation not found: {}", conversationId);
            return false;
        }
        
        if (state.getStatus() != ConversationState.ConversationStatus.ERROR) {
            logger.debug("Conversation {} is not in error state, no recovery needed", conversationId);
            return true;
        }
        
        try {
            // Reset to active status
            state.setStatus(ConversationState.ConversationStatus.ACTIVE);
            
            // Reset to thinking phase
            state.setCurrentPhase(ReActPhase.THINKING);
            
            // Clear error information from context
            if (state.getContext() != null && state.getContext().getContextData() != null) {
                Map<String, Object> contextData = state.getContext().getContextData();
                contextData.remove("last_error");
                contextData.put("recovery_timestamp", Instant.now().toString());
            }
            
            updateConversationState(conversationId, state);
            
            logger.info("Successfully recovered conversation: {}", conversationId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to recover conversation {}: {}", conversationId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Terminate conversation and cleanup resources.
     */
    public void terminateConversation(String conversationId) {
        logger.info("Terminating conversation: {}", conversationId);
        
        ConversationState state = getConversationState(conversationId);
        if (state != null) {
            state.setStatus(ConversationState.ConversationStatus.COMPLETED);
            updateConversationState(conversationId, state);
        }
        
        // Remove from active cache
        activeStates.remove(conversationId);
        conversationFlows.remove(conversationId);
        
        logger.info("Successfully terminated conversation: {}", conversationId);
    }
    
    /**
     * Get conversation flow information.
     */
    public ConversationFlow getConversationFlow(String conversationId) {
        return conversationFlows.get(conversationId);
    }
    
    /**
     * Create a new conversation state.
     */
    private ConversationState createNewConversationState(String conversationId, AgentContext context) {
        ConversationState state = new ConversationState(conversationId, ConversationState.ConversationStatus.ACTIVE);
        state.setCurrentPhase(ReActPhase.THINKING);
        state.setMessages(new ArrayList<>());
        
        // Create conversation context
        ConversationContext conversationContext = new ConversationContext(
            conversationId, 
            context.getAgentId(), 
            context.getUserId()
        );
        
        // Initialize ReAct state
        ReActState reactState = new ReActState(conversationId);
        conversationContext.setReActState(reactState);
        
        state.setContext(conversationContext);
        
        return state;
    }
    
    /**
     * Restore state from persistent context.
     */
    private ConversationState restoreStateFromContext(ConversationContext context) {
        ConversationState state = new ConversationState(
            context.getConversationId(), 
            ConversationState.ConversationStatus.ACTIVE
        );
        
        if (context.getReActState() != null) {
            state.setCurrentPhase(context.getReActState().getCurrentPhase());
        } else {
            state.setCurrentPhase(ReActPhase.THINKING);
        }
        
        state.setContext(context);
        state.setLastActivity(context.getLastUpdated());
        
        return state;
    }
    
    /**
     * Convert state to context for persistence.
     */
    private ConversationContext convertStateToContext(ConversationState state) {
        ConversationContext context = state.getContext();
        if (context == null) {
            context = new ConversationContext(
                state.getConversationId(),
                null, // Will be set from agent context
                null  // Will be set from agent context
            );
        }
        
        context.setLastUpdated(Instant.now());
        
        return context;
    }
    
    /**
     * Initialize conversation flow tracking.
     */
    private void initializeConversationFlow(String conversationId, AgentContext context) {
        ConversationFlow flow = new ConversationFlow(conversationId);
        flow.setStartTime(Instant.now());
        flow.setAgentId(context.getAgentId());
        flow.setUserId(context.getUserId());
        
        conversationFlows.put(conversationId, flow);
    }
    
    /**
     * Update conversation flow with current state.
     */
    private void updateConversationFlow(String conversationId, ConversationState state) {
        ConversationFlow flow = conversationFlows.get(conversationId);
        if (flow != null) {
            flow.setCurrentPhase(state.getCurrentPhase());
            flow.setLastActivity(state.getLastActivity());
            
            if (state.getContext() != null && state.getContext().getReActState() != null) {
                flow.setIterationCount(state.getContext().getReActState().getIterationCount());
            }
        }
    }
    
    /**
     * Inner class to track conversation flow.
     */
    public static class ConversationFlow {
        private final String conversationId;
        private String agentId;
        private String userId;
        private Instant startTime;
        private Instant lastActivity;
        private ReActPhase currentPhase;
        private int iterationCount;
        private final List<PhaseTransition> transitions = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        
        public ConversationFlow(String conversationId) {
            this.conversationId = conversationId;
        }
        
        public void recordTransition(ReActPhase from, ReActPhase to) {
            transitions.add(new PhaseTransition(from, to, Instant.now()));
        }
        
        public void recordError(String errorMessage) {
            errors.add(errorMessage);
        }
        
        // Getters and setters
        public String getConversationId() { return conversationId; }
        public String getAgentId() { return agentId; }
        public void setAgentId(String agentId) { this.agentId = agentId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public Instant getStartTime() { return startTime; }
        public void setStartTime(Instant startTime) { this.startTime = startTime; }
        public Instant getLastActivity() { return lastActivity; }
        public void setLastActivity(Instant lastActivity) { this.lastActivity = lastActivity; }
        public ReActPhase getCurrentPhase() { return currentPhase; }
        public void setCurrentPhase(ReActPhase currentPhase) { this.currentPhase = currentPhase; }
        public int getIterationCount() { return iterationCount; }
        public void setIterationCount(int iterationCount) { this.iterationCount = iterationCount; }
        public List<PhaseTransition> getTransitions() { return new ArrayList<>(transitions); }
        public List<String> getErrors() { return new ArrayList<>(errors); }
    }
    
    /**
     * Inner class to track phase transitions.
     */
    public static class PhaseTransition {
        private final ReActPhase fromPhase;
        private final ReActPhase toPhase;
        private final Instant timestamp;
        
        public PhaseTransition(ReActPhase fromPhase, ReActPhase toPhase, Instant timestamp) {
            this.fromPhase = fromPhase;
            this.toPhase = toPhase;
            this.timestamp = timestamp;
        }
        
        public ReActPhase getFromPhase() { return fromPhase; }
        public ReActPhase getToPhase() { return toPhase; }
        public Instant getTimestamp() { return timestamp; }
    }
}