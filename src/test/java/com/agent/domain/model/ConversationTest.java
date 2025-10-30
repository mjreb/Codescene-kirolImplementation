package com.agent.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ConversationTest {
    
    private Conversation conversation;
    private Message message;
    
    @BeforeEach
    void setUp() {
        conversation = new Conversation();
        message = new Message("conv-1", Message.MessageType.USER, "Hello");
    }
    
    @Test
    void testConversationCreation() {
        Conversation newConversation = new Conversation("conv-1", "agent-1", "user-1");
        
        assertEquals("conv-1", newConversation.getId());
        assertEquals("agent-1", newConversation.getAgentId());
        assertEquals("user-1", newConversation.getUserId());
        assertEquals(ConversationState.ConversationStatus.ACTIVE, newConversation.getStatus());
        assertNotNull(newConversation.getMessages());
        assertEquals(0, newConversation.getMessageCount());
        assertNotNull(newConversation.getCreatedAt());
        assertNotNull(newConversation.getLastActivity());
    }
    
    @Test
    void testDefaultConstructor() {
        Conversation defaultConversation = new Conversation();
        
        assertEquals(ConversationState.ConversationStatus.ACTIVE, defaultConversation.getStatus());
        assertNotNull(defaultConversation.getMessages());
        assertEquals(0, defaultConversation.getMessageCount());
        assertNotNull(defaultConversation.getCreatedAt());
        assertNotNull(defaultConversation.getLastActivity());
    }
    
    @Test
    void testAddMessage() {
        conversation.setId("conv-1");
        Instant initialActivity = conversation.getLastActivity();
        
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        conversation.addMessage(message);
        
        assertEquals(1, conversation.getMessages().size());
        assertEquals(1, conversation.getMessageCount());
        assertEquals(message, conversation.getMessages().get(0));
        assertEquals("conv-1", message.getConversationId());
        assertTrue(conversation.getLastActivity().isAfter(initialActivity));
    }
    
    @Test
    void testAddMessageWithNullList() {
        conversation.setMessages(null);
        conversation.setId("conv-1");
        
        conversation.addMessage(message);
        
        assertNotNull(conversation.getMessages());
        assertEquals(1, conversation.getMessages().size());
        assertEquals(1, conversation.getMessageCount());
    }
    
    @Test
    void testUpdateStatus() {
        Instant initialActivity = conversation.getLastActivity();
        
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        conversation.updateStatus(ConversationState.ConversationStatus.COMPLETED);
        
        assertEquals(ConversationState.ConversationStatus.COMPLETED, conversation.getStatus());
        assertTrue(conversation.getLastActivity().isAfter(initialActivity));
    }
    
    @Test
    void testIsActive() {
        assertTrue(conversation.isActive());
        
        conversation.setStatus(ConversationState.ConversationStatus.PAUSED);
        assertFalse(conversation.isActive());
        
        conversation.setStatus(ConversationState.ConversationStatus.COMPLETED);
        assertFalse(conversation.isActive());
        
        conversation.setStatus(ConversationState.ConversationStatus.ERROR);
        assertFalse(conversation.isActive());
        
        conversation.setStatus(ConversationState.ConversationStatus.ACTIVE);
        assertTrue(conversation.isActive());
    }
    
    @Test
    void testSetMessages() {
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(message);
        messages.add(new Message("conv-1", Message.MessageType.ASSISTANT, "Hi there"));
        
        conversation.setMessages(messages);
        
        assertEquals(messages, conversation.getMessages());
        assertEquals(2, conversation.getMessageCount());
    }
    
    @Test
    void testSetMessagesWithNull() {
        conversation.setMessages(null);
        
        assertNull(conversation.getMessages());
        assertEquals(0, conversation.getMessageCount());
    }
    
    @Test
    void testStatusUpdateTimestamp() {
        Instant initialActivity = conversation.getLastActivity();
        
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        conversation.setStatus(ConversationState.ConversationStatus.PAUSED);
        
        assertTrue(conversation.getLastActivity().isAfter(initialActivity));
    }
}