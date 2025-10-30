package com.agent.application.service;

import com.agent.domain.interfaces.ReActEngine;
import com.agent.domain.interfaces.StreamingReActEngine;
import com.agent.domain.model.AgentContext;
import com.agent.domain.model.AgentResponse;
import com.agent.domain.model.Conversation;
import com.agent.domain.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for handling streaming responses in conversations.
 */
@Service
public class StreamingService {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamingService.class);
    
    private final ConversationService conversationService;
    private final ReActEngine reActEngine;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private final Map<String, SseEmitter> activeStreams = new ConcurrentHashMap<>();
    
    public StreamingService(ConversationService conversationService,
                           ReActEngine reActEngine,
                           ObjectMapper objectMapper) {
        this.conversationService = conversationService;
        this.reActEngine = reActEngine;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * Send a message with streaming response using Server-Sent Events.
     */
    public SseEmitter sendMessageWithStreaming(String conversationId, String content, String userId) {
        SseEmitter emitter = new SseEmitter(30000L); // 30 second timeout
        
        // Store the emitter for this conversation
        activeStreams.put(conversationId, emitter);
        
        // Handle emitter completion and timeout
        emitter.onCompletion(() -> activeStreams.remove(conversationId));
        emitter.onTimeout(() -> {
            activeStreams.remove(conversationId);
            emitter.complete();
        });
        emitter.onError((ex) -> {
            activeStreams.remove(conversationId);
            emitter.completeWithError(ex);
        });
        
        // Process message asynchronously with streaming
        CompletableFuture.runAsync(() -> {
            try {
                processMessageWithStreaming(conversationId, content, userId, emitter);
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("Error processing message: " + e.getMessage()));
                    emitter.completeWithError(e);
                } catch (IOException ioException) {
                    emitter.completeWithError(ioException);
                }
            }
        }, executorService);
        
        return emitter;
    }
    
    /**
     * Check if a conversation is currently streaming.
     */
    public boolean isConversationStreaming(String conversationId) {
        return activeStreams.containsKey(conversationId);
    }
    
    /**
     * Cancel streaming for a conversation.
     */
    public void cancelStreaming(String conversationId, String userId) {
        SseEmitter emitter = activeStreams.get(conversationId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("cancelled")
                        .data("Streaming cancelled by user"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            activeStreams.remove(conversationId);
        }
    }
    
    /**
     * Process message with streaming updates.
     */
    private void processMessageWithStreaming(String conversationId, String content, 
                                           String userId, SseEmitter emitter) throws IOException {
        
        // Get conversation and validate access
        Conversation conversation = conversationService.getConversation(conversationId, userId);
        if (conversation == null) {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("Conversation not found"));
            emitter.completeWithError(new IllegalArgumentException("Conversation not found"));
            return;
        }
        
        // Send start event
        emitter.send(SseEmitter.event()
                .name("start")
                .data(createStreamEvent("processing", "Processing your message...")));
        
        // Add user message to conversation
        Message userMessage = new Message(conversationId, Message.MessageType.USER, content);
        userMessage.setId(UUID.randomUUID().toString());
        conversation.addMessage(userMessage);
        
        // Send user message event
        emitter.send(SseEmitter.event()
                .name("user_message")
                .data(createMessageEvent(userMessage)));
        
        // Create agent context
        AgentContext context = new AgentContext();
        context.setUserId(userId);
        context.setAgentId(conversation.getAgentId());
        
        // Send thinking event
        emitter.send(SseEmitter.event()
                .name("thinking")
                .data(createStreamEvent("thinking", "Agent is thinking...")));
        
        // Process with ReAct engine using streaming if supported
        if (reActEngine instanceof StreamingReActEngine streamingEngine && streamingEngine.isStreamingSupported()) {
            streamingEngine.processMessageWithStreaming(
                conversationId, 
                content, 
                context,
                // onThinking callback
                thinking -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("thinking")
                                .data(createStreamEvent("thinking", thinking)));
                    } catch (IOException e) {
                        logger.error("Error sending thinking event", e);
                    }
                },
                // onAction callback
                action -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("action")
                                .data(createStreamEvent("action", action)));
                    } catch (IOException e) {
                        logger.error("Error sending action event", e);
                    }
                },
                // onObservation callback
                observation -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("observation")
                                .data(createStreamEvent("observation", observation)));
                    } catch (IOException e) {
                        logger.error("Error sending observation event", e);
                    }
                },
                // onComplete callback
                response -> {
                    try {
                        // Add agent message to conversation
                        Message agentMessage = new Message(conversationId, Message.MessageType.ASSISTANT, response.getContent());
                        agentMessage.setId(UUID.randomUUID().toString());
                        agentMessage.setTimestamp(response.getTimestamp());
                        conversation.addMessage(agentMessage);
                        
                        // Send completion event
                        emitter.send(SseEmitter.event()
                                .name("complete")
                                .data(createCompletionEvent(response)));
                        
                        emitter.complete();
                    } catch (IOException e) {
                        logger.error("Error sending completion event", e);
                        emitter.completeWithError(e);
                    }
                }
            );
        } else {
            // Fallback to non-streaming processing
            AgentResponse response = reActEngine.processMessage(conversationId, content, context);
            
            // Send agent response in chunks (simulating streaming)
            String responseContent = response.getContent();
            int chunkSize = 50; // Characters per chunk
            
            for (int i = 0; i < responseContent.length(); i += chunkSize) {
                int endIndex = Math.min(i + chunkSize, responseContent.length());
                String chunk = responseContent.substring(i, endIndex);
                
                emitter.send(SseEmitter.event()
                        .name("chunk")
                        .data(createChunkEvent(chunk, i == 0)));
                
                // Small delay to simulate streaming
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // Add agent message to conversation
            Message agentMessage = new Message(conversationId, Message.MessageType.ASSISTANT, response.getContent());
            agentMessage.setId(UUID.randomUUID().toString());
            agentMessage.setTimestamp(response.getTimestamp());
            conversation.addMessage(agentMessage);
            
            // Send completion event
            emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(createCompletionEvent(response)));
            
            emitter.complete();
        }
    }
    
    private String createStreamEvent(String type, String message) throws IOException {
        Map<String, Object> event = Map.of(
                "type", type,
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
        return objectMapper.writeValueAsString(event);
    }
    
    private String createMessageEvent(Message message) throws IOException {
        Map<String, Object> event = Map.of(
                "id", message.getId(),
                "type", message.getType().toString(),
                "content", message.getContent(),
                "timestamp", message.getTimestamp().toEpochMilli()
        );
        return objectMapper.writeValueAsString(event);
    }
    
    private String createChunkEvent(String chunk, boolean isFirst) throws IOException {
        Map<String, Object> event = Map.of(
                "chunk", chunk,
                "isFirst", isFirst,
                "timestamp", System.currentTimeMillis()
        );
        return objectMapper.writeValueAsString(event);
    }
    
    private String createCompletionEvent(AgentResponse response) throws IOException {
        Map<String, Object> event = Map.of(
                "type", "completion",
                "content", response.getContent(),
                "responseType", response.getType().toString(),
                "tokenUsage", response.getTokenUsage() != null ? response.getTokenUsage() : Map.of(),
                "timestamp", response.getTimestamp().toEpochMilli()
        );
        return objectMapper.writeValueAsString(event);
    }
}