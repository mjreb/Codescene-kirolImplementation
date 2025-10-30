package com.agent.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolConfigTest {
    
    private ToolConfig toolConfig;
    
    @BeforeEach
    void setUp() {
        toolConfig = new ToolConfig();
    }
    
    @Test
    void testToolConfigCreation() {
        ToolConfig newConfig = new ToolConfig("calculator", "com.agent.tools.CalculatorTool");
        
        assertEquals("calculator", newConfig.getToolName());
        assertEquals("com.agent.tools.CalculatorTool", newConfig.getImplementationClass());
        assertTrue(newConfig.isEnabled());
        assertEquals(30, newConfig.getTimeoutSeconds());
        assertFalse(newConfig.isAsyncSupported());
    }
    
    @Test
    void testDefaultConstructor() {
        ToolConfig defaultConfig = new ToolConfig();
        
        assertTrue(defaultConfig.isEnabled());
        assertEquals(30, defaultConfig.getTimeoutSeconds());
        assertFalse(defaultConfig.isAsyncSupported());
    }
    
    @Test
    void testPermissionManagement() {
        List<String> permissions = new ArrayList<>();
        permissions.add("file.read");
        permissions.add("network.access");
        
        toolConfig.setRequiredPermissions(permissions);
        
        assertTrue(toolConfig.hasPermission("file.read"));
        assertTrue(toolConfig.hasPermission("network.access"));
        assertFalse(toolConfig.hasPermission("file.write"));
    }
    
    @Test
    void testAddRequiredPermission() {
        List<String> permissions = new ArrayList<>();
        permissions.add("file.read");
        toolConfig.setRequiredPermissions(permissions);
        
        toolConfig.addRequiredPermission("file.write");
        
        assertTrue(toolConfig.hasPermission("file.read"));
        assertTrue(toolConfig.hasPermission("file.write"));
        assertEquals(2, toolConfig.getRequiredPermissions().size());
    }
    
    @Test
    void testAddDuplicatePermission() {
        List<String> permissions = new ArrayList<>();
        permissions.add("file.read");
        toolConfig.setRequiredPermissions(permissions);
        
        toolConfig.addRequiredPermission("file.read");
        
        assertEquals(1, toolConfig.getRequiredPermissions().size());
        assertTrue(toolConfig.hasPermission("file.read"));
    }
    
    @Test
    void testConfigurationValues() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxResults", 10);
        config.put("timeout", 5000);
        config.put("enabled", true);
        
        toolConfig.setConfiguration(config);
        
        assertEquals(10, toolConfig.getConfigurationValue("maxResults"));
        assertEquals(5000, toolConfig.getConfigurationValue("timeout"));
        assertEquals(true, toolConfig.getConfigurationValue("enabled"));
        assertNull(toolConfig.getConfigurationValue("nonexistent"));
    }
    
    @Test
    void testSetConfigurationValue() {
        Map<String, Object> config = new HashMap<>();
        toolConfig.setConfiguration(config);
        
        toolConfig.setConfigurationValue("newKey", "newValue");
        
        assertEquals("newValue", toolConfig.getConfigurationValue("newKey"));
        assertEquals("newValue", config.get("newKey"));
    }
    
    @Test
    void testSetConfigurationValueWithNullConfig() {
        toolConfig.setConfiguration(null);
        
        // Should not throw exception
        toolConfig.setConfigurationValue("key", "value");
        
        assertNull(toolConfig.getConfigurationValue("key"));
    }
    
    @Test
    void testHasPermissionWithNullPermissions() {
        toolConfig.setRequiredPermissions(null);
        
        assertFalse(toolConfig.hasPermission("any.permission"));
    }
    
    @Test
    void testToolProperties() {
        toolConfig.setToolName("web-search");
        toolConfig.setImplementationClass("com.agent.tools.WebSearchTool");
        toolConfig.setDescription("Search the web for information");
        toolConfig.setVersion("1.0.0");
        toolConfig.setEnabled(false);
        toolConfig.setTimeoutSeconds(60);
        toolConfig.setAsyncSupported(true);
        
        assertEquals("web-search", toolConfig.getToolName());
        assertEquals("com.agent.tools.WebSearchTool", toolConfig.getImplementationClass());
        assertEquals("Search the web for information", toolConfig.getDescription());
        assertEquals("1.0.0", toolConfig.getVersion());
        assertFalse(toolConfig.isEnabled());
        assertEquals(60, toolConfig.getTimeoutSeconds());
        assertTrue(toolConfig.isAsyncSupported());
    }
}