package com.agent.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationManagerImplTest {
    
    @Mock
    private Environment environment;
    
    @Mock
    private ConfigurableEnvironment configurableEnvironment;
    
    @Mock
    private ContextRefresher contextRefresher;
    
    @Mock
    private MutablePropertySources mutablePropertySources;
    
    private ConfigurationManagerImpl configurationManager;
    
    @BeforeEach
    void setUp() {
        when(configurableEnvironment.getPropertySources()).thenReturn(mutablePropertySources);
        when(mutablePropertySources.contains(anyString())).thenReturn(false);
        
        configurationManager = new ConfigurationManagerImpl(
                environment, configurableEnvironment, contextRefresher);
    }
    
    @Test
    void testGetConfigValue_FromEnvironment() {
        // Given
        when(environment.getProperty("test.key")).thenReturn("test.value");
        
        // When
        var result = configurationManager.getConfigValue("test.key");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals("test.value", result.get());
    }
    
    @Test
    void testGetConfigValue_WithDefault() {
        // Given
        when(environment.getProperty("missing.key")).thenReturn(null);
        
        // When
        String result = configurationManager.getConfigValue("missing.key", "default.value");
        
        // Then
        assertEquals("default.value", result);
    }
    
    @Test
    void testUpdateConfigValue() {
        // When
        boolean result = configurationManager.updateConfigValue("runtime.key", "runtime.value");
        
        // Then
        assertTrue(result);
        
        // Verify the value can be retrieved
        var retrievedValue = configurationManager.getConfigValue("runtime.key");
        assertTrue(retrievedValue.isPresent());
        assertEquals("runtime.value", retrievedValue.get());
    }
    
    @Test
    void testGetConfigProperties() {
        // Given - Add runtime properties to test the functionality
        configurationManager.updateConfigValue("app.test.key1", "value1");
        configurationManager.updateConfigValue("app.test.key2", "value2");
        configurationManager.updateConfigValue("other.key", "other.value");
        
        // When
        Map<String, String> result = configurationManager.getConfigProperties("app.test");
        
        // Then
        assertEquals(2, result.size());
        assertEquals("value1", result.get("app.test.key1"));
        assertEquals("value2", result.get("app.test.key2"));
        assertNull(result.get("other.key"));
    }
    
    @Test
    void testRefreshConfiguration() {
        // Given
        when(contextRefresher.refresh()).thenReturn(java.util.Set.of("refreshed.key"));
        
        // When
        boolean result = configurationManager.refreshConfiguration();
        
        // Then
        assertTrue(result);
        verify(contextRefresher).refresh();
    }
    
    @Test
    void testRefreshConfiguration_Exception() {
        // Given
        when(contextRefresher.refresh()).thenThrow(new RuntimeException("Refresh failed"));
        
        // When
        boolean result = configurationManager.refreshConfiguration();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testValidateConfiguration_Success() {
        // Given
        when(environment.containsProperty("spring.application.name")).thenReturn(true);
        when(environment.containsProperty("server.port")).thenReturn(true);
        when(environment.containsProperty("app.security.jwt.secret")).thenReturn(true);
        when(environment.getProperty("app.security.jwt.secret")).thenReturn("a-very-long-secret-key-that-is-at-least-32-characters-long");
        
        // When
        boolean result = configurationManager.validateConfiguration();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testValidateConfiguration_MissingKey() {
        // Given
        when(environment.containsProperty("spring.application.name")).thenReturn(false);
        
        // When
        boolean result = configurationManager.validateConfiguration();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testValidateConfiguration_ShortJwtSecret() {
        // Given
        when(environment.containsProperty("spring.application.name")).thenReturn(true);
        when(environment.containsProperty("server.port")).thenReturn(true);
        when(environment.containsProperty("app.security.jwt.secret")).thenReturn(true);
        when(environment.getProperty("app.security.jwt.secret")).thenReturn("short");
        
        // When
        boolean result = configurationManager.validateConfiguration();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testGetEnvironmentProfile() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test", "integration"});
        
        // When
        String result = configurationManager.getEnvironmentProfile();
        
        // Then
        assertEquals("test", result);
    }
    
    @Test
    void testGetEnvironmentProfile_Default() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        
        // When
        String result = configurationManager.getEnvironmentProfile();
        
        // Then
        assertEquals("default", result);
    }
    
    @Test
    void testHasConfigKey() {
        // Given
        when(environment.containsProperty("existing.key")).thenReturn(true);
        when(environment.containsProperty("missing.key")).thenReturn(false);
        
        // When & Then
        assertTrue(configurationManager.hasConfigKey("existing.key"));
        assertFalse(configurationManager.hasConfigKey("missing.key"));
    }
}