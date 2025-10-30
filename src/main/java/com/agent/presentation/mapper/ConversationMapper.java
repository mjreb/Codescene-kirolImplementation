package com.agent.presentation.mapper;

import com.agent.domain.model.AgentResponse;
import com.agent.domain.model.Conversation;
import com.agent.domain.model.Message;
import com.agent.presentation.dto.AgentMessageResponse;
import com.agent.presentation.dto.ConversationResponse;
import com.agent.presentation.dto.MessageResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper utility for converting between domain models and DTOs.
 */
@Component
public class ConversationMapper {
    
    /**
     * Convert Conversation domain model to ConversationResponse DTO.
     */
    public ConversationResponse toConversationResponse(Conversation conversation) {
        if (conversation == null) {
            return null;
        }
        
        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setAgentId(conversation.getAgentId());
        response.setUserId(conversation.getUserId());
        response.setStatus(conversation.getStatus());
        response.setTitle(conversation.getTitle());
        response.setMessageCount(conversation.getMessageCount());
        response.setTokenUsage(conversation.getTokenUsage());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setLastActivity(conversation.getLastActivity());
        
        if (conversation.getMessages() != null) {
            List<MessageResponse> messageResponses = conversation.getMessages().stream()
                    .map(this::toMessageResponse)
                    .collect(Collectors.toList());
            response.setMessages(messageResponses);
        }
        
        return response;
    }
    
    /**
     * Convert Message domain model to MessageResponse DTO.
     */
    public MessageResponse toMessageResponse(Message message) {
        if (message == null) {
            return null;
        }
        
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setType(message.getType());
        response.setContent(message.getContent());
        response.setMetadata(message.getMetadata());
        response.setTimestamp(message.getTimestamp());
        
        return response;
    }
    
    /**
     * Convert AgentResponse domain model to AgentMessageResponse DTO.
     */
    public AgentMessageResponse toAgentMessageResponse(String conversationId, AgentResponse agentResponse) {
        if (agentResponse == null) {
            return null;
        }
        
        AgentMessageResponse response = new AgentMessageResponse();
        response.setConversationId(conversationId);
        response.setContent(agentResponse.getContent());
        response.setType(agentResponse.getType());
        response.setMetadata(agentResponse.getMetadata());
        response.setTokenUsage(agentResponse.getTokenUsage());
        response.setTimestamp(agentResponse.getTimestamp());
        
        return response;
    }
    
    /**
     * Convert Conversation to ConversationResponse without messages (for list views).
     */
    public ConversationResponse toConversationSummary(Conversation conversation) {
        if (conversation == null) {
            return null;
        }
        
        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setAgentId(conversation.getAgentId());
        response.setUserId(conversation.getUserId());
        response.setStatus(conversation.getStatus());
        response.setTitle(conversation.getTitle());
        response.setMessageCount(conversation.getMessageCount());
        response.setTokenUsage(conversation.getTokenUsage());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setLastActivity(conversation.getLastActivity());
        // Intentionally not setting messages for summary view
        
        return response;
    }
}