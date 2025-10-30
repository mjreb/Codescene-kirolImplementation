package com.agent.infrastructure.react;

import com.agent.domain.interfaces.ToolFramework;
import com.agent.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ToolIntegrationService.
 */
@ExtendWith(MockitoExtension.class)
class ToolIntegrationServiceTest {
    
    private ToolIntegrationService toolIntegrationService;
    
    @Mock
    private ToolFramework toolFramework;
    
    @BeforeEach
    void setUp() {
        toolIntegrationService = new ToolIntegrationService(toolFramework);
    }
    
    @Test
    void testGetAvailableToolsDescription() {
        // Given
        List<ToolDefinition> tools = Arrays.asList(
            createToolDefinition("calculator", "Perform calculations"),
            createToolDefinition("web_search", "Search the web")
        );
        when(toolFramework.getAvailableTools()).thenReturn(tools);
        
        // When
        String description = toolIntegrationService.getAvailableToolsDescription();
        
        // Then
        assertNotNull(description);
        assertTrue(description.contains("calculator"));
        assertTrue(description.contains("web_search"));
        assertTrue(description.contains("Perform calculations"));
        assertTrue(description.contains("Search the web"));
    }
    
    @Test
    void testGetAvailableToolsDescription_NoTools() {
        // Given
        when(toolFramework.getAvailableTools()).thenReturn(Collections.emptyList());
        
        // When
        String description = toolIntegrationService.getAvailableToolsDescription();
        
        // Then
        assertEquals("No tools are currently available.", description);
    }
    
    @Test
    void testIsToolAvailable() {
        // Given
        List<ToolDefinition> tools = Arrays.asList(
            createToolDefinition("calculator", "Perform calculations")
        );
        when(toolFramework.getAvailableTools()).thenReturn(tools);
        
        // When & Then
        assertTrue(toolIntegrationService.isToolAvailable("calculator"));
        assertFalse(toolIntegrationService.isToolAvailable("non_existent"));
        assertFalse(toolIntegrationService.isToolAvailable(null));
        assertFalse(toolIntegrationService.isToolAvailable(""));
    }
    
    @Test
    void testGetToolDefinition() {
        // Given
        ToolDefinition calculatorTool = createToolDefinition("calculator", "Perform calculations");
        List<ToolDefinition> tools = Arrays.asList(calculatorTool);
        when(toolFramework.getAvailableTools()).thenReturn(tools);
        
        // When
        Optional<ToolDefinition> result = toolIntegrationService.getToolDefinition("calculator");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(calculatorTool, result.get());
        
        // Test non-existent tool
        Optional<ToolDefinition> nonExistent = toolIntegrationService.getToolDefinition("non_existent");
        assertFalse(nonExistent.isPresent());
    }
    
    @Test
    void testExecuteToolSafely_Success() {
        // Given
        String toolName = "calculator";
        Map<String, Object> parameters = Map.of("expression", "2 + 2");
        ToolResult expectedResult = new ToolResult(toolName, true, 4);
        
        when(toolFramework.getAvailableTools()).thenReturn(Arrays.asList(
            createToolDefinition(toolName, "Perform calculations")
        ));
        when(toolFramework.executeTool(toolName, parameters)).thenReturn(expectedResult);
        
        // When
        ToolResult result = toolIntegrationService.executeToolSafely(toolName, parameters);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(4, result.getResult());
        verify(toolFramework).executeTool(toolName, parameters);
    }
    
    @Test
    void testExecuteToolSafely_ToolNotFound() {
        // Given
        when(toolFramework.getAvailableTools()).thenReturn(Collections.emptyList());
        
        // When
        ToolResult result = toolIntegrationService.executeToolSafely("non_existent", Map.of());
        
        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("not available"));
        assertEquals("TOOL_NOT_FOUND", result.getMetadata().get("error_code"));
    }
    
    @Test
    void testExecuteToolSafely_ExecutionError() {
        // Given
        String toolName = "calculator";
        Map<String, Object> parameters = Map.of("expression", "invalid");
        
        when(toolFramework.getAvailableTools()).thenReturn(Arrays.asList(
            createToolDefinition(toolName, "Perform calculations")
        ));
        when(toolFramework.executeTool(toolName, parameters))
            .thenThrow(new RuntimeException("Calculation error"));
        
        // When
        ToolResult result = toolIntegrationService.executeToolSafely(toolName, parameters);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Tool execution failed"));
        assertEquals("EXECUTION_ERROR", result.getMetadata().get("error_code"));
    }
    
    @Test
    void testParseToolParameters_JSON() {
        // Given
        String jsonParams = "{\"expression\": \"2 + 2\", \"precision\": 2}";
        
        // When
        Map<String, Object> result = toolIntegrationService.parseToolParameters(jsonParams);
        
        // Then
        assertEquals(2, result.size());
        assertEquals("2 + 2", result.get("expression"));
        assertEquals(2, result.get("precision"));
    }
    
    @Test
    void testParseToolParameters_KeyValue() {
        // Given
        String keyValueParams = "expression: \"2 + 2\", precision: 2";
        
        // When
        Map<String, Object> result = toolIntegrationService.parseToolParameters(keyValueParams);
        
        // Then
        assertEquals(2, result.size());
        assertEquals("2 + 2", result.get("expression"));
        assertEquals(2, result.get("precision"));
    }
    
    @Test
    void testParseToolParameters_Empty() {
        // When
        Map<String, Object> result1 = toolIntegrationService.parseToolParameters("");
        Map<String, Object> result2 = toolIntegrationService.parseToolParameters(null);
        
        // Then
        assertTrue(result1.isEmpty());
        assertTrue(result2.isEmpty());
    }
    
    @Test
    void testFormatToolResultForObservation_Success() {
        // Given
        ToolResult result = new ToolResult("calculator", true, 42);
        result.setDurationMs(150);
        
        // When
        String observation = toolIntegrationService.formatToolResultForObservation(result);
        
        // Then
        assertNotNull(observation);
        assertTrue(observation.contains("calculator"));
        assertTrue(observation.contains("successfully"));
        assertTrue(observation.contains("42"));
        assertTrue(observation.contains("150ms"));
    }
    
    @Test
    void testFormatToolResultForObservation_Failure() {
        // Given
        ToolResult result = new ToolResult("calculator", false, null);
        result.setErrorMessage("Invalid expression");
        
        // When
        String observation = toolIntegrationService.formatToolResultForObservation(result);
        
        // Then
        assertNotNull(observation);
        assertTrue(observation.contains("calculator"));
        assertTrue(observation.contains("failed"));
        assertTrue(observation.contains("Invalid expression"));
    }
    
    @Test
    void testSuggestAlternativeTools() {
        // Given
        List<ToolDefinition> tools = Arrays.asList(
            createToolDefinition("calculator", "Math calculations"),
            createToolDefinition("math_solver", "Advanced math"),
            createToolDefinition("web_search", "Search web")
        );
        when(toolFramework.getAvailableTools()).thenReturn(tools);
        
        // When
        List<String> alternatives = toolIntegrationService.suggestAlternativeTools("calc", "math context");
        
        // Then
        assertNotNull(alternatives);
        assertTrue(alternatives.size() <= 3);
        // Should suggest calculator and math_solver as they're math-related
        assertTrue(alternatives.contains("calculator") || alternatives.contains("math_solver"));
    }
    
    private ToolDefinition createToolDefinition(String name, String description) {
        ToolDefinition definition = new ToolDefinition(name, description);
        
        if ("calculator".equals(name)) {
            Map<String, ParameterDefinition> parameters = new HashMap<>();
            ParameterDefinition expressionParam = new ParameterDefinition();
            expressionParam.setName("expression");
            expressionParam.setType("string");
            expressionParam.setDescription("Mathematical expression");
            expressionParam.setRequired(true);
            parameters.put("expression", expressionParam);
            definition.setParameters(parameters);
        }
        
        return definition;
    }
}