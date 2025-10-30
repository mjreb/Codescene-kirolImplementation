package com.agent.domain.model;

/**
 * Enumeration representing the different phases of the ReAct (Reasoning and Acting) pattern.
 */
public enum ReActPhase {
    /**
     * The thinking phase where the agent reasons about the problem and plans actions.
     */
    THINKING,
    
    /**
     * The acting phase where the agent executes tools or takes actions.
     */
    ACTING,
    
    /**
     * The observing phase where the agent processes the results of actions.
     */
    OBSERVING
}