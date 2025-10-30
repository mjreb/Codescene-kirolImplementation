package com.agent.presentation.controller;

import com.agent.application.service.ConversationService;
import com.agent.domain.model.AgentResponse;
import com.agent.domain.model.Conversation;
import com.agent.infrastructure.security.annotation.RequireConversationAccess;
import com.agent.infrastructure.security.annotation.RequirePermission;
import com.agent.infrastructure.security.service.AuthorizationService;
import com.agent.presentation.dto.*;
import com.agent.presentation.mapper.ConversationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for agent conversation management.
 */
@RestController
@RequestMapping("/conversations")
@Tag(name = "Conversations", description = "Agent conversation management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {
    
    private final ConversationService conversationService;
    private final ConversationMapper conversationMapper;
    private final AuthorizationService authorizationService;
    
    public AgentController(ConversationService conversationService, 
                          ConversationMapper conversationMapper,
                          AuthorizationService authorizationService) {
        this.conversationService = conversationService;
        this.conversationMapper = conversationMapper;
        this.authorizationService = authorizationService;
    }
    
    /**
     * Create a new conversation with an initial message.
     * POST /conversations
     */
    @Operation(
        summary = "Create a new conversation",
        description = "Creates a new conversation with an agent and optionally sends an initial message. " +
                     "The conversation will be associated with the authenticated user.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Conversation creation request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateConversationRequest.class),
                examples = @ExampleObject(
                    name = "Create conversation example",
                    value = """
                        {
                          "agentId": "default-agent",
                          "title": "Help with coding",
                          "initialMessage": "I need help writing a Java function to calculate fibonacci numbers"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Conversation created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ConversationResponse.class),
                examples = @ExampleObject(
                    name = "Successful creation",
                    value = """
                        {
                          "id": "conv-123",
                          "agentId": "default-agent",
                          "title": "Help with coding",
                          "status": "ACTIVE",
                          "createdAt": "2024-01-01T10:00:00Z",
                          "lastActivity": "2024-01-01T10:00:00Z",
                          "messageCount": 1,
                          "tokenUsage": {
                            "totalTokens": 25,
                            "inputTokens": 15,
                            "outputTokens": 10
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping
    @RequirePermission("conversation:write")
    public ResponseEntity<ConversationResponse> createConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        
        String userId = authorizationService.getCurrentUserId();
        
        Conversation conversation = conversationService.createConversation(
                request.getAgentId(),
                userId,
                request.getTitle(),
                request.getInitialMessage()
        );
        
        ConversationResponse response = conversationMapper.toConversationResponse(conversation);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Send a message to an existing conversation.
     * POST /conversations/{id}/messages
     */
    @Operation(
        summary = "Send a message to a conversation",
        description = "Sends a message to an existing conversation and receives the agent's response. " +
                     "The agent will process the message using the ReAct pattern, potentially using tools " +
                     "and external LLM providers to generate a comprehensive response.",
        parameters = @Parameter(
            name = "conversationId",
            description = "Unique identifier of the conversation",
            required = true,
            example = "conv-123"
        ),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Message to send to the agent",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SendMessageRequest.class),
                examples = @ExampleObject(
                    name = "Send message example",
                    value = """
                        {
                          "content": "Can you help me optimize this code for better performance?",
                          "metadata": {
                            "priority": "high",
                            "context": "code-review"
                          }
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Message processed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AgentMessageResponse.class),
                examples = @ExampleObject(
                    name = "Successful response",
                    value = """
                        {
                          "conversationId": "conv-123",
                          "messageId": "msg-456",
                          "content": "I can help you optimize your code. Let me analyze it and suggest improvements...",
                          "reasoning": [
                            {
                              "phase": "THINKING",
                              "content": "The user wants code optimization help. I should analyze the code structure first."
                            },
                            {
                              "phase": "ACTING",
                              "content": "Using code analysis tool to identify performance bottlenecks."
                            }
                          ],
                          "toolCalls": [
                            {
                              "toolName": "code-analyzer",
                              "parameters": {"language": "java"},
                              "result": "Found 3 optimization opportunities"
                            }
                          ],
                          "tokenUsage": {
                            "inputTokens": 45,
                            "outputTokens": 120,
                            "totalTokens": 165
                          },
                          "timestamp": "2024-01-01T10:05:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid message content or conversation state",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Conversation not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Token limit exceeded or rate limit reached",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/{conversationId}/messages")
    @RequireConversationAccess(requireWrite = true)
    public ResponseEntity<AgentMessageResponse> sendMessage(
            @Parameter(description = "Conversation ID", required = true, example = "conv-123")
            @PathVariable String conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        
        String userId = authorizationService.getCurrentUserId();
        
        // Check if user can send message (token limits, etc.)
        if (!conversationService.canSendMessage(conversationId, userId, estimateTokens(request.getContent()))) {
            throw new IllegalStateException("Token limit exceeded for conversation");
        }
        
        AgentResponse agentResponse = conversationService.sendMessage(
                conversationId, 
                request.getContent(), 
                userId
        );
        
        AgentMessageResponse response = conversationMapper.toAgentMessageResponse(conversationId, agentResponse);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get conversation history by ID.
     * GET /conversations/{id}
     */
    @Operation(
        summary = "Get conversation details",
        description = "Retrieves the complete conversation history including all messages, " +
                     "agent responses, tool calls, and metadata. Only accessible by the conversation owner.",
        parameters = @Parameter(
            name = "conversationId",
            description = "Unique identifier of the conversation",
            required = true,
            example = "conv-123"
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conversation retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ConversationResponse.class),
                examples = @ExampleObject(
                    name = "Complete conversation",
                    value = """
                        {
                          "id": "conv-123",
                          "agentId": "default-agent",
                          "title": "Help with coding",
                          "status": "ACTIVE",
                          "createdAt": "2024-01-01T10:00:00Z",
                          "lastActivity": "2024-01-01T10:05:00Z",
                          "messageCount": 3,
                          "messages": [
                            {
                              "id": "msg-1",
                              "type": "USER",
                              "content": "I need help with Java",
                              "timestamp": "2024-01-01T10:00:00Z"
                            },
                            {
                              "id": "msg-2",
                              "type": "ASSISTANT",
                              "content": "I'd be happy to help you with Java...",
                              "timestamp": "2024-01-01T10:01:00Z"
                            }
                          ],
                          "tokenUsage": {
                            "totalTokens": 190,
                            "inputTokens": 60,
                            "outputTokens": 130
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Conversation not found or access denied",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/{conversationId}")
    @RequireConversationAccess
    public ResponseEntity<ConversationResponse> getConversation(
            @Parameter(description = "Conversation ID", required = true, example = "conv-123")
            @PathVariable String conversationId) {
        
        String userId = authorizationService.getCurrentUserId();
        
        Conversation conversation = conversationService.getConversation(conversationId, userId);
        
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        
        ConversationResponse response = conversationMapper.toConversationResponse(conversation);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Terminate a conversation.
     * DELETE /conversations/{id}
     */
    @Operation(
        summary = "Terminate a conversation",
        description = "Permanently terminates a conversation and cleans up associated resources. " +
                     "This action cannot be undone. The conversation history will be archived " +
                     "but no longer accessible through the API.",
        parameters = @Parameter(
            name = "conversationId",
            description = "Unique identifier of the conversation to terminate",
            required = true,
            example = "conv-123"
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Conversation terminated successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Conversation not found or access denied",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conversation cannot be terminated in current state",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @DeleteMapping("/{conversationId}")
    @RequireConversationAccess(requireWrite = true)
    public ResponseEntity<Void> terminateConversation(
            @Parameter(description = "Conversation ID", required = true, example = "conv-123")
            @PathVariable String conversationId) {
        
        String userId = authorizationService.getCurrentUserId();
        
        conversationService.terminateConversation(conversationId, userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Simple token estimation for validation.
     * In a real implementation, this would use the actual tokenizer.
     */
    private int estimateTokens(String content) {
        // Rough estimation: ~4 characters per token
        return content.length() / 4;
    }
}