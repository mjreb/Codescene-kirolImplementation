package com.agent.infrastructure.tools;

import com.agent.domain.model.*;
import com.agent.infrastructure.tools.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ToolFrameworkImplTest {
    
    private ToolFrameworkImpl toolFramework;
    private TestTool testTool;
    private SlowTool slowTool;
    
    @BeforeEach
    void setUp() {
        toolFramework = new ToolFrameworkImpl();
        testTool = new TestTool();
        slowTool = new SlowTool();
    }
    
    @Test
    void testRegisterTool() {
        toolFramework.registerTool(testTool);
        
        List<ToolDefinition> availableTools = toolFramework.getAvailableTools();
        assertEquals(1, availableTools.size());
        assertEquals("test_tool", availableTools.get(0).getName());
    }
    
    @Test
    void testRegisterNullTool() {
        assertThrows(IllegalArgumentException.class, () -> toolFramework.registerTool(null));
    }
    
    @Test
    void testRegisterToolWithInvalidDefinition() {
        Tool invalidTool = new Tool() {
            @Override
            public ToolDefinition getDefinition() {
                return null;
            }
            
            @Override
            public ToolResult execute(Map<String, Object> parameters) {
                return null;
            }
            
            @Override
            public boolean validateParameters(Map<String, Object> parameters) {
                return true;
            }
        };
        
        assertThrows(IllegalArgumentException.class, () -> toolFramework.registerTool(invalidTool));
    }
    
    @Test
    void testExecuteToolSuccess() {
        toolFramework.registerTool(testTool);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("input", "test value");
        
        ToolResult result = toolFramework.executeTool("test_tool", parameters);
        
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("test_tool", result.getToolName());
        assertEquals("Processed: test value", result.getResult());
        assertTrue(result.getDurationMs() >= 0);
    }
    
    @Test
    void testExecuteToolNotFound() {
        assertThrows(ToolNotFoundException.class, () -> 
            toolFramework.executeTool("nonexistent_tool", new HashMap<>()));
    }
    
    @Test
    void testExecuteToolWithNullName() {
        assertThrows(ToolExecutionException.class, () -> 
            toolFramework.executeTool(null, new HashMap<>()));
    }
    
    @Test
    void testExecuteToolWithEmptyName() {
        assertThrows(ToolExecutionException.class, () -> 
            toolFramework.executeTool("", new HashMap<>()));
    }
    
    @Test
    void testExecuteToolWithMissingRequiredParameter() {
        toolFramework.registerTool(testTool);
        
        // Don't provide the required "input" parameter
        Map<String, Object> parameters = new HashMap<>();
        
        assertThrows(ToolParameterException.class, () -> 
            toolFramework.executeTool("test_tool", parameters));
    }
    
    @Test
    void testExecuteToolWithInvalidParameterType() {
        toolFramework.registerTool(testTool);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("input", 123); // Should be string
        
        // This should work because parameter validator converts numbers to strings
        ToolResult result = toolFramework.executeTool("test_tool", parameters);
        assertTrue(result.isSuccess());
        assertEquals("Processed: 123", result.getResult());
    }
    
    @Test
    void testExecuteToolWithDefaultParameter() {
        toolFramework.registerTool(testTool);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("input", "test");
        // Don't provide optional "count" parameter, should use default value
        
        ToolResult result = toolFramework.executeTool("test_tool", parameters);
        assertTrue(result.isSuccess());
        assertEquals("Processed: test", result.getResult()); // Default count=1 not shown
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testExecuteToolTimeout() {
        // Register slow tool with short timeout
        toolFramework.registerTool(slowTool, Duration.ofMillis(100));
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("delay", 500); // 500ms delay, but timeout is 100ms
        
        assertThrows(ToolTimeoutException.class, () -> 
            toolFramework.executeTool("slow_tool", parameters));
    }
    
    @Test
    void testExecuteToolAsync() throws Exception {
        toolFramework.registerTool(testTool);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("input", "async test");
        
        CompletableFuture<ToolResult> future = toolFramework.executeToolAsync("test_tool", parameters);
        ToolResult result = future.get(5, TimeUnit.SECONDS);
        
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Processed: async test", result.getResult());
    }
    
    @Test
    void testExecuteToolAsyncWithError() throws Exception {
        toolFramework.registerTool(testTool);
        
        Map<String, Object> parameters = new HashMap<>();
        // Missing required parameter should cause error
        
        CompletableFuture<ToolResult> future = toolFramework.executeToolAsync("test_tool", parameters);
        ToolResult result = future.get(5, TimeUnit.SECONDS);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }
    
    @Test
    void testGetAvailableTools() {
        toolFramework.registerTool(testTool);
        toolFramework.registerTool(slowTool);
        
        List<ToolDefinition> availableTools = toolFramework.getAvailableTools();
        assertEquals(2, availableTools.size());
        
        // Should be sorted by name
        assertEquals("slow_tool", availableTools.get(0).getName());
        assertEquals("test_tool", availableTools.get(1).getName());
    }
    
    @Test
    void testToolExecutionWithException() {
        Tool errorTool = new Tool() {
            @Override
            public ToolDefinition getDefinition() {
                ToolDefinition def = new ToolDefinition();
                def.setName("error_tool");
                def.setDescription("Tool that throws exceptions");
                return def;
            }
            
            @Override
            public ToolResult execute(Map<String, Object> parameters) {
                throw new RuntimeException("Simulated tool error");
            }
            
            @Override
            public boolean validateParameters(Map<String, Object> parameters) {
                return true;
            }
        };
        
        toolFramework.registerTool(errorTool);
        
        assertThrows(ToolExecutionException.class, () -> 
            toolFramework.executeTool("error_tool", new HashMap<>()));
    }
    
    // Test tool implementation
    private static class TestTool implements Tool {
        private final ToolDefinition definition;
        
        public TestTool() {
            definition = new ToolDefinition();
            definition.setName("test_tool");
            definition.setDescription("A test tool for unit testing");
            definition.setAsync(false);
            
            Map<String, ParameterDefinition> parameters = new HashMap<>();
            
            ParameterDefinition input = new ParameterDefinition();
            input.setName("input");
            input.setType("string");
            input.setDescription("Input string to process");
            input.setRequired(true);
            parameters.put("input", input);
            
            ParameterDefinition count = new ParameterDefinition();
            count.setName("count");
            count.setType("integer");
            count.setDescription("Number of times to process");
            count.setRequired(false);
            count.setDefaultValue(1);
            parameters.put("count", count);
            
            definition.setParameters(parameters);
        }
        
        @Override
        public ToolDefinition getDefinition() {
            return definition;
        }
        
        @Override
        public ToolResult execute(Map<String, Object> parameters) {
            String input = (String) parameters.get("input");
            Integer count = (Integer) parameters.getOrDefault("count", 1);
            
            String result = "Processed: " + input;
            if (count != 1) {
                result += " (count=" + count + ")";
            }
            return new ToolResult("test_tool", true, result);
        }
        
        @Override
        public boolean validateParameters(Map<String, Object> parameters) {
            return parameters.containsKey("input");
        }
    }
    
    // Slow tool for timeout testing
    private static class SlowTool implements Tool {
        private final ToolDefinition definition;
        
        public SlowTool() {
            definition = new ToolDefinition();
            definition.setName("slow_tool");
            definition.setDescription("A slow tool for timeout testing");
            definition.setAsync(false);
            
            Map<String, ParameterDefinition> parameters = new HashMap<>();
            
            ParameterDefinition delay = new ParameterDefinition();
            delay.setName("delay");
            delay.setType("integer");
            delay.setDescription("Delay in milliseconds");
            delay.setRequired(false);
            delay.setDefaultValue(1000);
            parameters.put("delay", delay);
            
            definition.setParameters(parameters);
        }
        
        @Override
        public ToolDefinition getDefinition() {
            return definition;
        }
        
        @Override
        public ToolResult execute(Map<String, Object> parameters) {
            Integer delay = (Integer) parameters.getOrDefault("delay", 1000);
            
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted", e);
            }
            
            return new ToolResult("slow_tool", true, "Completed after " + delay + "ms");
        }
        
        @Override
        public boolean validateParameters(Map<String, Object> parameters) {
            return true;
        }
    }
}