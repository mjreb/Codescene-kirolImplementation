package com.agent.application.service;

import com.agent.domain.interfaces.MemoryManager;
import com.agent.domain.interfaces.ReActEngine;
import com.agent.domain.interfaces.TokenMonitor;
import com.agent.domain.model.*;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application service for managing conversations and agent interactions.
 */
@Service
public class ConversationService {
    
    private final ReActEngine reActEngine;
    private final MemoryManager memoryManager;
    private final TokenMonitor tokenMonitor;
    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();
    
    public ConversationService(ReActEngine reActEngine, 
                              MemoryManager memoryManager, 
                              TokenMonitor tokenMonitor) {
        this.reActEngine = reActEngine;
        this.memoryManager = memoryManager;
        this.tokenMonitor = tokenMonitor;
    }
    
    /**
     * Create a new conversation with an initial message.
     */
    public Conversation createConversation(String agentId, String userId, String title, String initialMessage) {
        String conversationId = UUID.randomUUID().toString();
        
        Conversation conversation = new Conversation(conversationId, agentId, userId);
        conversation.setTitle(title);
        
        // Add initial user message
        Message userMessage = new Message(conversationId, Message.MessageType.USER, initialMessage);
        userMessage.setId(UUID.randomUUID().toString());
        conversation.addMessage(userMessage);
        
        // Store conversation
        conversations.put(conversationId, conversation);
        
        // Process initial message with ReAct engine
        AgentContext context = createAgentContext(userId, agentId);
        AgentResponse response = reActEngine.processMessage(conversationId, initialMessage, context);
        
        // Add agent response as message
        Message agentMessage = new Message(conversationId, Message.MessageType.ASSISTANT, response.getContent());
        agentMessage.setId(UUID.randomUUID().toString());
        agentMessage.setTimestamp(response.getTimestamp());
        conversation.addMessage(agentMessage);
        
        // Update token usage
        if (response.getTokenUsage() != null) {
            conversation.setTokenUsage(response.getTokenUsage());
        }
        
        return conversation;
    }
    
    /**
     * Send a message to an existing conversation.
     */
    public AgentResponse sendMessage(String conversationId, String content, String userId) {
        Conversation conversation = getConversation(conversationId);
        
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation not found: " + conversationId);
        }
        
        if (!conversation.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to conversation: " + conversationId);
        }
        
        if (!conversation.isActive()) {
            throw new IllegalStateException("Conversation is not active: " + conversationId);
        }
        
        // Add user message
        Message userMessage = new Message(conversationId, Message.MessageType.USER, content);
        userMessage.setId(UUID.randomUUID().toString());
        conversation.addMessage(userMessage);
        
        // Process message with ReAct engine
        AgentContext context = createAgentContext(userId, conversation.getAgentId());
        AgentResponse response = reActEngine.processMessage(conversationId, content, context);
        
        // Add agent response as message
        Message agentMessage = new Message(conversationId, Message.MessageType.ASSISTANT, response.getContent());
        agentMessage.setId(UUID.randomUUID().toString());
        agentMessage.setTimestamp(response.getTimestamp());
        conversation.addMessage(agentMessage);
        
        // Update token usage
        if (response.getTokenUsage() != null) {
            TokenUsage currentUsage = conversation.getTokenUsage();
            if (currentUsage != null) {
                // Aggregate token usage
                TokenUsage updatedUsage = new TokenUsage();
                updatedUsage.setInputTokens(currentUsage.getInputTokens() + response.getTokenUsage().getInputTokens());
                updatedUsage.setOutputTokens(currentUsage.getOutputTokens() + response.getTokenUsage().getOutputTokens());
                updatedUsage.setTotalTokens(updatedUsage.getInputTokens() + updatedUsage.getOutputTokens());
                conversation.setTokenUsage(updatedUsage);
            } else {
                conversation.setTokenUsage(response.getTokenUsage());
            }
        }
        
        return response;
    }
    
    /**
     * Get conversation by ID.
     */
    public Conversation getConversation(String conversationId) {
        return conversations.get(conversationId);
    }
    
    /**
     * Get conversation by ID with user access check.
     */
    public Conversation getConversation(String conversationId, String userId) {
        Conversation conversation = getConversation(conversationId);
        
        if (conversation == null) {
            return null;
        }
        
        if (!conversation.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to conversation: " + conversationId);
        }
        
        return conversation;
    }
    
    /**
     * Terminate a conversation.
     */
    public void terminateConversation(String conversationId, String userId) {
        Conversation conversation = getConversation(conversationId, userId);
        
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation not found: " + conversationId);
        }
        
        conversation.updateStatus(ConversationState.ConversationStatus.COMPLETED);
    }
    
    /**
     * Check if user can send message (token limits, etc.).
     */
    public boolean canSendMessage(String conversationId, String userId, int estimatedTokens) {
        return tokenMonitor.checkTokenLimit(conversationId, estimatedTokens);
    }
    
    private AgentContext createAgentContext(String userId, String agentId) {
        AgentContext context = new AgentContext();
        context.setUserId(userId);
        context.setAgentId(agentId);
        // Add any additional context setup here
        return context;
    }
}