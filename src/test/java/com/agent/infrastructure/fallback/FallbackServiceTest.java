package com.agent.infrastructure.fallback;

import com.agent.domain.model.AgentResponse;
import com.agent.domain.model.LLMResponse;
import com.agent.domain.model.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FallbackServiceTest {
    
    @Mock
    private SystemStatusService systemStatusService;
    
    @Mock
    private SystemStatus systemStatus;
    
    private FallbackService fallbackService;
    
    @BeforeEach
    void setUp() {
        fallbackService = new FallbackService(systemStatusService);
    }
    
    @Test
    void testGetFallbackLLMResponse() {
        // Given
        String prompt = "Test prompt";
        String providerId = "openai";
        
        // When
        LLMResponse response = fallbackService.getFallbackLLMResponse(prompt, providerId);
        
        // Then
        assertNotNull(response);
        assertEquals("fallback", response.getProviderId());
        assertNotNull(response.getContent());
        assertTrue(response.getContent().contains("technical difficulties") || 
                  response.getContent().contains("unavailable") ||
                  response.getContent().contains("limited mode"));
        // Token usage is not tracked in fallback responses
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testGetFallbackAgentResponse() {
        // Given
        String conversationId = "conv-123";
        String userMessage = "Hello";
        
        when(systemStatusService.getCurrentStatus()).thenReturn(systemStatus);
        when(systemStatus.isFullyOperational()).thenReturn(false);
        when(systemStatus.isLLMProvidersDown()).thenReturn(true);
        
        // When
        AgentResponse response = fallbackService.getFallbackAgentResponse(conversationId, userMessage);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getContent());
        assertEquals(AgentResponse.ResponseType.ERROR, response.getType());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testGetFallbackToolResult_Calculator() {
        // Given
        String toolName = "calculator";
        Map<String, Object> parameters = Map.of("expression", "2+2");
        
        // When
        ToolResult result = fallbackService.getFallbackToolResult(toolName, parameters);
        
        // Then
        assertNotNull(result);
        assertEquals(toolName, result.getToolName());
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getResult().toString().contains("calculation"));
        assertNotNull(result.getExecutionTime());
    }
    
    @Test
    void testGetFallbackToolResult_WebSearch() {
        // Given
        String toolName = "websearch";
        Map<String, Object> parameters = Map.of("query", "test query");
        
        // When
        ToolResult result = fallbackService.getFallbackToolResult(toolName, parameters);
        
        // Then
        assertNotNull(result);
        assertEquals(toolName, result.getToolName());
        assertFalse(result.isSuccess());
        assertTrue(result.getResult().toString().contains("Search service"));
    }
    
    @Test
    void testGetFallbackToolResult_FileSystem() {
        // Given
        String toolName = "filesystem";
        Map<String, Object> parameters = Map.of("path", "/test/path");
        
        // When
        ToolResult result = fallbackService.getFallbackToolResult(toolName, parameters);
        
        // Then
        assertNotNull(result);
        assertEquals(toolName, result.getToolName());
        assertFalse(result.isSuccess());
        assertTrue(result.getResult().toString().contains("File system"));
    }
    
    @Test
    void testGetFallbackToolResult_UnknownTool() {
        // Given
        String toolName = "unknown";
        Map<String, Object> parameters = Map.of();
        
        // When
        ToolResult result = fallbackService.getFallbackToolResult(toolName, parameters);
        
        // Then
        assertNotNull(result);
        assertEquals(toolName, result.getToolName());
        assertFalse(result.isSuccess());
        assertTrue(result.getResult().toString().contains("Tool service"));
    }
    
    @Test
    void testCacheResponse() {
        // Given
        String key = "test-key";
        String content = "test content";
        String type = "llm_response";
        
        // When
        fallbackService.cacheResponse(key, content, type);
        
        // Then - Should not throw exception
        // Cache is internal, so we can't directly verify, but method should complete successfully
        assertDoesNotThrow(() -> fallbackService.cacheResponse(key, content, type));
    }
    
    @Test
    void testGetSystemStatusMessage_FullyOperational() {
        // Given
        when(systemStatusService.getCurrentStatus()).thenReturn(systemStatus);
        when(systemStatus.isFullyOperational()).thenReturn(true);
        
        // When
        String message = fallbackService.getSystemStatusMessage();
        
        // Then
        assertNull(message); // No message needed when fully operational
    }
    
    @Test
    void testGetSystemStatusMessage_LLMProvidersDown() {
        // Given
        when(systemStatusService.getCurrentStatus()).thenReturn(systemStatus);
        when(systemStatus.isFullyOperational()).thenReturn(false);
        when(systemStatus.isLLMProvidersDown()).thenReturn(true);
        when(systemStatus.isToolsDown()).thenReturn(false);
        when(systemStatus.isMemoryIssues()).thenReturn(false);
        
        // When
        String message = fallbackService.getSystemStatusMessage();
        
        // Then
        assertNotNull(message);
        assertTrue(message.contains("AI services"));
        assertTrue(message.contains("restore full functionality"));
    }
    
    @Test
    void testGetSystemStatusMessage_MultipleIssues() {
        // Given
        when(systemStatusService.getCurrentStatus()).thenReturn(systemStatus);
        when(systemStatus.isFullyOperational()).thenReturn(false);
        when(systemStatus.isLLMProvidersDown()).thenReturn(true);
        when(systemStatus.isToolsDown()).thenReturn(true);
        when(systemStatus.isMemoryIssues()).thenReturn(true);
        
        // When
        String message = fallbackService.getSystemStatusMessage();
        
        // Then
        assertNotNull(message);
        assertTrue(message.contains("AI services"));
        assertTrue(message.contains("tools"));
        assertTrue(message.contains("history"));
    }
    
    @Test
    void testIsSystemDegraded_True() {
        // Given
        when(systemStatusService.getCurrentStatus()).thenReturn(systemStatus);
        when(systemStatus.isFullyOperational()).thenReturn(false);
        
        // When
        boolean degraded = fallbackService.isSystemDegraded();
        
        // Then
        assertTrue(degraded);
    }
    
    @Test
    void testIsSystemDegraded_False() {
        // Given
        when(systemStatusService.getCurrentStatus()).thenReturn(systemStatus);
        when(systemStatus.isFullyOperational()).thenReturn(true);
        
        // When
        boolean degraded = fallbackService.isSystemDegraded();
        
        // Then
        assertFalse(degraded);
    }
}