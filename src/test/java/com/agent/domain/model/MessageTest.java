package com.agent.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {
    
    private Message message;
    
    @BeforeEach
    void setUp() {
        message = new Message();
    }
    
    @Test
    void testMessageCreation() {
        Message newMessage = new Message("conv-1", Message.MessageType.USER, "Hello world");
        
        assertEquals("conv-1", newMessage.getConversationId());
        assertEquals(Message.MessageType.USER, newMessage.getType());
        assertEquals("Hello world", newMessage.getContent());
        assertNotNull(newMessage.getTimestamp());
    }
    
    @Test
    void testDefaultConstructor() {
        Message defaultMessage = new Message();
        
        assertNotNull(defaultMessage.getTimestamp());
    }
    
    @Test
    void testMessageTypes() {
        // Test all message types
        message.setType(Message.MessageType.USER);
        assertEquals(Message.MessageType.USER, message.getType());
        
        message.setType(Message.MessageType.ASSISTANT);
        assertEquals(Message.MessageType.ASSISTANT, message.getType());
        
        message.setType(Message.MessageType.SYSTEM);
        assertEquals(Message.MessageType.SYSTEM, message.getType());
        
        message.setType(Message.MessageType.TOOL_CALL);
        assertEquals(Message.MessageType.TOOL_CALL, message.getType());
        
        message.setType(Message.MessageType.TOOL_RESULT);
        assertEquals(Message.MessageType.TOOL_RESULT, message.getType());
    }
    
    @Test
    void testMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "api");
        metadata.put("priority", 1);
        metadata.put("processed", true);
        
        message.setMetadata(metadata);
        
        assertEquals(metadata, message.getMetadata());
        assertEquals("api", message.getMetadata().get("source"));
        assertEquals(1, message.getMetadata().get("priority"));
        assertEquals(true, message.getMetadata().get("processed"));
    }
    
    @Test
    void testSettersAndGetters() {
        String id = "msg-123";
        String conversationId = "conv-456";
        String content = "Test message content";
        Instant timestamp = Instant.now();
        
        message.setId(id);
        message.setConversationId(conversationId);
        message.setContent(content);
        message.setTimestamp(timestamp);
        
        assertEquals(id, message.getId());
        assertEquals(conversationId, message.getConversationId());
        assertEquals(content, message.getContent());
        assertEquals(timestamp, message.getTimestamp());
    }
    
    @Test
    void testMessageTypeEnumValues() {
        // Verify all expected enum values exist
        Message.MessageType[] types = Message.MessageType.values();
        assertEquals(5, types.length);
        
        assertTrue(containsType(types, Message.MessageType.USER));
        assertTrue(containsType(types, Message.MessageType.ASSISTANT));
        assertTrue(containsType(types, Message.MessageType.SYSTEM));
        assertTrue(containsType(types, Message.MessageType.TOOL_CALL));
        assertTrue(containsType(types, Message.MessageType.TOOL_RESULT));
    }
    
    private boolean containsType(Message.MessageType[] types, Message.MessageType target) {
        for (Message.MessageType type : types) {
            if (type == target) {
                return true;
            }
        }
        return false;
    }
}