package com.agent.presentation.websocket;

import com.agent.application.service.ConversationService;
import com.agent.domain.model.AgentResponse;
import com.agent.domain.model.Conversation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time conversation updates.
 */
@Component
public class ConversationWebSocketHandler implements WebSocketHandler {
    
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToConversation = new ConcurrentHashMap<>();
    
    public ConversationWebSocketHandler(ConversationService conversationService,
                                       ObjectMapper objectMapper) {
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        
        // Send connection confirmation
        ConversationWebSocketMessage message = new ConversationWebSocketMessage("connection", "Connected to conversation WebSocket", null);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage textMessage) {
            handleTextMessage(session, textMessage);
        }
    }
    
    private void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String action = (String) payload.get("action");
            
            switch (action) {
                case "subscribe":
                    handleSubscribe(session, payload);
                    break;
                case "send_message":
                    handleSendMessage(session, payload);
                    break;
                case "unsubscribe":
                    handleUnsubscribe(session);
                    break;
                default:
                    sendError(session, "Unknown action: " + action);
            }
        } catch (Exception e) {
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }
    
    private void handleSubscribe(WebSocketSession session, Map<String, Object> payload) throws IOException {
        String conversationId = (String) payload.get("conversationId");
        String userId = (String) payload.get("userId");
        
        if (conversationId == null || userId == null) {
            sendError(session, "conversationId and userId are required for subscription");
            return;
        }
        
        // Validate access to conversation
        try {
            Conversation conversation = conversationService.getConversation(conversationId, userId);
            if (conversation == null) {
                sendError(session, "Conversation not found or access denied");
                return;
            }
            
            sessionToConversation.put(session.getId(), conversationId);
            
            ConversationWebSocketMessage response = new ConversationWebSocketMessage("subscribed", 
                    "Subscribed to conversation: " + conversationId, 
                    Map.of("conversationId", conversationId));
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            
        } catch (SecurityException e) {
            sendError(session, "Access denied to conversation");
        }
    }
    
    private void handleSendMessage(WebSocketSession session, Map<String, Object> payload) throws IOException {
        String conversationId = sessionToConversation.get(session.getId());
        if (conversationId == null) {
            sendError(session, "Not subscribed to any conversation");
            return;
        }
        
        String content = (String) payload.get("content");
        String userId = (String) payload.get("userId");
        
        if (content == null || userId == null) {
            sendError(session, "content and userId are required");
            return;
        }
        
        try {
            // Send message processing notification
            ConversationWebSocketMessage processingMessage = new ConversationWebSocketMessage("processing", 
                    "Processing your message...", 
                    Map.of("conversationId", conversationId));
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(processingMessage)));
            
            // Process message
            AgentResponse response = conversationService.sendMessage(conversationId, content, userId);
            
            // Send response
            ConversationWebSocketMessage responseMessage = new ConversationWebSocketMessage("agent_response", 
                    response.getContent(), 
                    Map.of(
                            "conversationId", conversationId,
                            "responseType", response.getType().toString(),
                            "timestamp", response.getTimestamp().toEpochMilli(),
                            "tokenUsage", response.getTokenUsage() != null ? response.getTokenUsage() : Map.of()
                    ));
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(responseMessage)));
            
        } catch (Exception e) {
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }
    
    private void handleUnsubscribe(WebSocketSession session) throws IOException {
        sessionToConversation.remove(session.getId());
        
        ConversationWebSocketMessage response = new ConversationWebSocketMessage("unsubscribed", 
                "Unsubscribed from conversation", null);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }
    
    private void sendError(WebSocketSession session, String errorMessage) throws IOException {
        ConversationWebSocketMessage error = new ConversationWebSocketMessage("error", errorMessage, null);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessions.remove(session.getId());
        sessionToConversation.remove(session.getId());
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        sessions.remove(session.getId());
        sessionToConversation.remove(session.getId());
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * Broadcast a message to all sessions subscribed to a conversation.
     */
    public void broadcastToConversation(String conversationId, ConversationWebSocketMessage message) {
        sessionToConversation.entrySet().stream()
                .filter(entry -> conversationId.equals(entry.getValue()))
                .forEach(entry -> {
                    WebSocketSession session = sessions.get(entry.getKey());
                    if (session != null && session.isOpen()) {
                        try {
                            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                        } catch (IOException e) {
                            // Log error and remove session
                            sessions.remove(entry.getKey());
                            sessionToConversation.remove(entry.getKey());
                        }
                    }
                });
    }
    
    /**
     * Custom WebSocket message structure.
     */
    public static class ConversationWebSocketMessage {
        private String type;
        private String message;
        private Map<String, Object> data;
        private long timestamp;
        
        public ConversationWebSocketMessage(String type, String message, Map<String, Object> data) {
            this.type = type;
            this.message = message;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}