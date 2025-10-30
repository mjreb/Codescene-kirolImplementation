package com.agent.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentTest {
    
    private Agent agent;
    private AgentConfiguration configuration;
    private MemoryConfiguration memoryConfig;
    private TokenLimits tokenLimits;
    
    @BeforeEach
    void setUp() {
        agent = new Agent();
        configuration = new AgentConfiguration();
        memoryConfig = new MemoryConfiguration();
        tokenLimits = new TokenLimits(1000, 10000, 50000);
    }
    
    @Test
    void testAgentCreation() {
        Agent newAgent = new Agent("agent-1", "Test Agent", "A test agent");
        
        assertEquals("agent-1", newAgent.getId());
        assertEquals("Test Agent", newAgent.getName());
        assertEquals("A test agent", newAgent.getDescription());
        assertTrue(newAgent.isActive());
        assertNotNull(newAgent.getCreatedAt());
        assertNotNull(newAgent.getUpdatedAt());
    }
    
    @Test
    void testDefaultConstructor() {
        Agent defaultAgent = new Agent();
        
        assertTrue(defaultAgent.isActive());
        assertNotNull(defaultAgent.getCreatedAt());
        assertNotNull(defaultAgent.getUpdatedAt());
    }
    
    @Test
    void testConfigurationUpdate() {
        Instant initialUpdatedAt = agent.getUpdatedAt();
        
        // Small delay to ensure timestamp difference
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        agent.setConfiguration(configuration);
        
        assertEquals(configuration, agent.getConfiguration());
        assertTrue(agent.getUpdatedAt().isAfter(initialUpdatedAt));
    }
    
    @Test
    void testAvailableToolsUpdate() {
        List<String> tools = Arrays.asList("calculator", "web-search", "file-reader");
        Instant initialUpdatedAt = agent.getUpdatedAt();
        
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        agent.setAvailableTools(tools);
        
        assertEquals(tools, agent.getAvailableTools());
        assertTrue(agent.getUpdatedAt().isAfter(initialUpdatedAt));
    }
    
    @Test
    void testMemoryConfigUpdate() {
        Instant initialUpdatedAt = agent.getUpdatedAt();
        
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        agent.setMemoryConfig(memoryConfig);
        
        assertEquals(memoryConfig, agent.getMemoryConfig());
        assertTrue(agent.getUpdatedAt().isAfter(initialUpdatedAt));
    }
    
    @Test
    void testTokenLimitsUpdate() {
        Instant initialUpdatedAt = agent.getUpdatedAt();
        
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        agent.setTokenLimits(tokenLimits);
        
        assertEquals(tokenLimits, agent.getTokenLimits());
        assertTrue(agent.getUpdatedAt().isAfter(initialUpdatedAt));
    }
    
    @Test
    void testActiveStatusUpdate() {
        Instant initialUpdatedAt = agent.getUpdatedAt();
        
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        agent.setActive(false);
        
        assertFalse(agent.isActive());
        assertTrue(agent.getUpdatedAt().isAfter(initialUpdatedAt));
    }
}