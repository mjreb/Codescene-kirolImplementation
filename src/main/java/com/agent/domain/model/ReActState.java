package com.agent.domain.model;

import java.util.List;

/**
 * Represents the current state of the ReAct reasoning process.
 */
public class ReActState {
    private String conversationId;
    private ReActPhase currentPhase;
    private String currentThought;
    private ToolCall pendingAction;
    private List<ToolResult> observations;
    private int iterationCount;
    private int maxIterations;
    
    public ReActState() {
        this.currentPhase = ReActPhase.THINKING;
        this.iterationCount = 0;
        this.maxIterations = 10; // Default max iterations
    }
    
    public ReActState(String conversationId) {
        this();
        this.conversationId = conversationId;
    }
    
    // Getters and setters
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    
    public ReActPhase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(ReActPhase currentPhase) { this.currentPhase = currentPhase; }
    
    public String getCurrentThought() { return currentThought; }
    public void setCurrentThought(String currentThought) { this.currentThought = currentThought; }
    
    public ToolCall getPendingAction() { return pendingAction; }
    public void setPendingAction(ToolCall pendingAction) { this.pendingAction = pendingAction; }
    
    public List<ToolResult> getObservations() { return observations; }
    public void setObservations(List<ToolResult> observations) { this.observations = observations; }
    
    public int getIterationCount() { return iterationCount; }
    public void setIterationCount(int iterationCount) { this.iterationCount = iterationCount; }
    
    public int getMaxIterations() { return maxIterations; }
    public void setMaxIterations(int maxIterations) { this.maxIterations = maxIterations; }
}