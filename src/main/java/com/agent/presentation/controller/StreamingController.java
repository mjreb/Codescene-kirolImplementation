package com.agent.presentation.controller;

import com.agent.application.service.ConversationService;
import com.agent.application.service.StreamingService;
import com.agent.domain.model.AgentResponse;
import com.agent.presentation.dto.SendMessageRequest;
import com.agent.presentation.mapper.ConversationMapper;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

/**
 * REST controller for streaming conversation responses.
 */
@RestController
@RequestMapping("/conversations")
public class StreamingController {
    
    private final ConversationService conversationService;
    private final StreamingService streamingService;
    private final ConversationMapper conversationMapper;
    
    public StreamingController(ConversationService conversationService,
                              StreamingService streamingService,
                              ConversationMapper conversationMapper) {
        this.conversationService = conversationService;
        this.streamingService = streamingService;
        this.conversationMapper = conversationMapper;
    }
    
    /**
     * Send a message with streaming response using Server-Sent Events.
     * POST /conversations/{id}/messages/stream
     */
    @PostMapping(value = "/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(
            @PathVariable String conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "default-user") String userId) {
        
        // Check if user can send message (token limits, etc.)
        if (!conversationService.canSendMessage(conversationId, userId, estimateTokens(request.getContent()))) {
            throw new IllegalStateException("Token limit exceeded for conversation");
        }
        
        return streamingService.sendMessageWithStreaming(conversationId, request.getContent(), userId);
    }
    
    /**
     * Get streaming status for a conversation.
     * GET /conversations/{id}/stream/status
     */
    @GetMapping("/{conversationId}/stream/status")
    public ResponseEntity<StreamingStatusResponse> getStreamingStatus(
            @PathVariable String conversationId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "default-user") String userId) {
        
        boolean isStreaming = streamingService.isConversationStreaming(conversationId);
        StreamingStatusResponse response = new StreamingStatusResponse(conversationId, isStreaming);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancel streaming for a conversation.
     * DELETE /conversations/{id}/stream
     */
    @DeleteMapping("/{conversationId}/stream")
    public ResponseEntity<Void> cancelStreaming(
            @PathVariable String conversationId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "default-user") String userId) {
        
        streamingService.cancelStreaming(conversationId, userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Simple token estimation for validation.
     */
    private int estimateTokens(String content) {
        return content.length() / 4;
    }
    
    /**
     * Response DTO for streaming status.
     */
    public static class StreamingStatusResponse {
        private String conversationId;
        private boolean streaming;
        
        public StreamingStatusResponse(String conversationId, boolean streaming) {
            this.conversationId = conversationId;
            this.streaming = streaming;
        }
        
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        
        public boolean isStreaming() { return streaming; }
        public void setStreaming(boolean streaming) { this.streaming = streaming; }
    }
}