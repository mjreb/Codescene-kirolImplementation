package com.agent.infrastructure.monitoring;

import com.agent.domain.model.TokenBudget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenLimitEnforcementServiceTest {
    
    @Mock
    private TokenMonitorImpl tokenMonitor;
    
    @Mock
    private TokenCountingService tokenCountingService;
    
    private TokenLimitEnforcementService enforcementService;
    
    @BeforeEach
    void setUp() {
        enforcementService = new TokenLimitEnforcementService(tokenMonitor, tokenCountingService);
    }
    
    @Test
    void validateTokenLimits_WithinAllLimits_ShouldNotThrowException() {
        // Given
        String conversationId = "conv-123";
        String userId = "user-123";
        String text = "Hello world";
        String providerId = "openai";
        String model = "gpt-3.5-turbo";
        
        TokenBudget budget = createBudgetWithLimits(1000, 10000, 100, 500);
        
        when(tokenCountingService.estimateTokens(text, providerId, model)).thenReturn(50);
        when(tokenMonitor.getTokenBudget(userId)).thenReturn(budget);
        when(tokenMonitor.getConversationTokenUsage(conversationId)).thenReturn(200);
        
        // When & Then
        assertDoesNotThrow(() -> 
            enforcementService.validateTokenLimits(conversationId, userId, text, providerId, model));
    }
    
    @Test
    void validateTokenLimits_ExceedsDailyLimit_ShouldThrowException() {
        // Given
        String conversationId = "conv-123";
        String userId = "user-123";
        String text = "Hello world";
        String providerId = "openai";
        String model = "gpt-3.5-turbo";
        
        TokenBudget budget = createBudgetWithLimits(1000, 10000, 950, 500);
        
        when(tokenCountingService.estimateTokens(text, providerId, model)).thenReturn(100);
        when(tokenMonitor.getTokenBudget(userId)).thenReturn(budget);
        
        // When & Then
        TokenLimitException exception = assertThrows(TokenLimitException.class, () ->
            enforcementService.validateTokenLimits(conversationId, userId, text, providerId, model));
        
        assertEquals("DAILY", exception.getLimitType());
        assertEquals(950, exception.getCurrentUsage());
        assertEquals(1000, exception.getLimit());
    }
    
    @Test
    void validateTokenLimits_ExceedsMonthlyLimit_ShouldThrowException() {
        // Given
        String conversationId = "conv-123";
        String userId = "user-123";
        String text = "Hello world";
        String providerId = "openai";
        String model = "gpt-3.5-turbo";
        
        TokenBudget budget = createBudgetWithLimits(1000, 10000, 100, 9950);
        
        when(tokenCountingService.estimateTokens(text, providerId, model)).thenReturn(100);
        when(tokenMonitor.getTokenBudget(userId)).thenReturn(budget);
        
        // When & Then
        TokenLimitException exception = assertThrows(TokenLimitException.class, () ->
            enforcementService.validateTokenLimits(conversationId, userId, text, providerId, model));
        
        assertEquals("MONTHLY", exception.getLimitType());
        assertEquals(9950, exception.getCurrentUsage());
        assertEquals(10000, exception.getLimit());
    }
    
    @Test
    void validateTokenLimits_ExceedsConversationLimit_ShouldThrowException() {
        // Given
        String conversationId = "conv-123";
        String userId = "user-123";
        String text = "Hello world";
        String providerId = "openai";
        String model = "gpt-3.5-turbo";
        
        TokenBudget budget = createBudgetWithLimits(10000, 100000, 100, 500);
        
        when(tokenCountingService.estimateTokens(text, providerId, model)).thenReturn(100);
        when(tokenMonitor.getTokenBudget(userId)).thenReturn(budget);
        when(tokenMonitor.getConversationTokenUsage(conversationId)).thenReturn(49950); // Close to 50K limit
        
        // When & Then
        TokenLimitException exception = assertThrows(TokenLimitException.class, () ->
            enforcementService.validateTokenLimits(conversationId, userId, text, providerId, model));
        
        assertEquals("CONVERSATION", exception.getLimitType());
        assertEquals(49950, exception.getCurrentUsage());
        assertEquals(50000, exception.getLimit());
    }
    
    @Test
    void preValidateRequest_WithValidRequest_ShouldReturnValidResult() {
        // Given
        String conversationId = "conv-123";
        String userId = "user-123";
        String text = "Hello world";
        String providerId = "openai";
        String model = "gpt-3.5-turbo";
        
        TokenBudget budget = createBudgetWithLimits(1000, 10000, 100, 500);
        
        when(tokenCountingService.estimateTokens(text, providerId, model)).thenReturn(50);
        when(tokenCountingService.calculateCost(providerId, model, 50, 0)).thenReturn(0.01);
        when(tokenMonitor.getTokenBudget(userId)).thenReturn(budget);
        when(tokenMonitor.getConversationTokenUsage(conversationId)).thenReturn(200);
        
        // When
        TokenLimitEnforcementService.TokenValidationResult result = 
            enforcementService.preValidateRequest(conversationId, userId, text, providerId, model);
        
        // Then
        assertTrue(result.isValid());
        assertEquals(50, result.getEstimatedInputTokens());
        assertEquals(0.01, result.getEstimatedCost(), 0.001);
        assertNull(result.getErrorMessage());
    }
    
    @Test
    void preValidateRequest_WithInvalidRequest_ShouldReturnInvalidResult() {
        // Given
        String conversationId = "conv-123";
        String userId = "user-123";
        String text = "Hello world";
        String providerId = "openai";
        String model = "gpt-3.5-turbo";
        
        TokenBudget budget = createBudgetWithLimits(1000, 10000, 950, 500);
        
        when(tokenCountingService.estimateTokens(text, providerId, model)).thenReturn(100);
        when(tokenCountingService.calculateCost(providerId, model, 100, 0)).thenReturn(0.02);
        when(tokenMonitor.getTokenBudget(userId)).thenReturn(budget);
        
        // When
        TokenLimitEnforcementService.TokenValidationResult result = 
            enforcementService.preValidateRequest(conversationId, userId, text, providerId, model);
        
        // Then
        assertFalse(result.isValid());
        assertEquals(100, result.getEstimatedInputTokens());
        assertEquals(0.02, result.getEstimatedCost(), 0.001);
        assertNotNull(result.getErrorMessage());
        assertEquals("DAILY", result.getLimitType());
    }
    
    @Test
    void checkWarningThresholds_WithLowUsage_ShouldReturnNoWarning() {
        // Given
        String userId = "user-123";
        TokenBudget budget = createBudgetWithLimits(1000, 10000, 100, 500);
        
        when(tokenMonitor.getTokenBudget(userId)).thenReturn(budget);
        
        // When
        TokenLimitEnforcementService.TokenLimitWarning warning = 
            enforcementService.checkWarningThresholds(userId);
        
        // Then
        assertEquals(TokenLimitEnforcementService.TokenLimitWarning.WarningLevel.NONE, warning.getWarningLevel());
        assertTrue(warning.getWarnings().isEmpty());
    }
    
    @Test
    void checkWarningThresholds_WithHighDailyUsage_ShouldReturnHighWarning() {
        // Given
        String userId = "user-123";
        TokenBudget budget = createBudgetWithLimits(1000, 10000, 850, 500); // 85% daily usage
        
        when(tokenMonitor.getTokenBudget(userId)).thenReturn(budget);
        
        // When
        TokenLimitEnforcementService.TokenLimitWarning warning = 
            enforcementService.checkWarningThresholds(userId);
        
        // Then
        assertEquals(TokenLimitEnforcementService.TokenLimitWarning.WarningLevel.HIGH, warning.getWarningLevel());
        assertFalse(warning.getWarnings().isEmpty());
        assertTrue(warning.getWarnings().get(0).contains("Daily token limit is 80% used"));
    }
    
    @Test
    void checkWarningThresholds_WithCriticalDailyUsage_ShouldReturnCriticalWarning() {
        // Given
        String userId = "user-123";
        TokenBudget budget = createBudgetWithLimits(1000, 10000, 950, 500); // 95% daily usage
        
        when(tokenMonitor.getTokenBudget(userId)).thenReturn(budget);
        
        // When
        TokenLimitEnforcementService.TokenLimitWarning warning = 
            enforcementService.checkWarningThresholds(userId);
        
        // Then
        assertEquals(TokenLimitEnforcementService.TokenLimitWarning.WarningLevel.CRITICAL, warning.getWarningLevel());
        assertFalse(warning.getWarnings().isEmpty());
        assertTrue(warning.getWarnings().get(0).contains("Daily token limit is 90% used"));
    }
    
    @Test
    void checkWarningThresholds_WithUnlimitedBudget_ShouldReturnNoWarning() {
        // Given
        String userId = "user-123";
        TokenBudget budget = createBudgetWithLimits(1000, 10000, 950, 500);
        budget.setUnlimited(true);
        
        when(tokenMonitor.getTokenBudget(userId)).thenReturn(budget);
        
        // When
        TokenLimitEnforcementService.TokenLimitWarning warning = 
            enforcementService.checkWarningThresholds(userId);
        
        // Then
        assertEquals(TokenLimitEnforcementService.TokenLimitWarning.WarningLevel.NONE, warning.getWarningLevel());
    }
    
    @Test
    void getRemainingAllowance_WithNormalBudget_ShouldReturnCorrectAllowance() {
        // Given
        String userId = "user-123";
        TokenBudget budget = createBudgetWithLimits(1000, 10000, 300, 2000);
        
        when(tokenMonitor.getTokenBudget(userId)).thenReturn(budget);
        
        // When
        TokenLimitEnforcementService.TokenAllowance allowance = 
            enforcementService.getRemainingAllowance(userId);
        
        // Then
        assertFalse(allowance.isUnlimited());
        assertEquals(700, allowance.getDailyRemaining()); // 1000 - 300
        assertEquals(8000, allowance.getMonthlyRemaining()); // 10000 - 2000
        assertEquals(300, allowance.getDailyUsed());
        assertEquals(2000, allowance.getMonthlyUsed());
        assertEquals(1000, allowance.getDailyLimit());
        assertEquals(10000, allowance.getMonthlyLimit());
    }
    
    @Test
    void getRemainingAllowance_WithUnlimitedBudget_ShouldReturnUnlimitedAllowance() {
        // Given
        String userId = "user-123";
        TokenBudget budget = createBudgetWithLimits(1000, 10000, 300, 2000);
        budget.setUnlimited(true);
        
        when(tokenMonitor.getTokenBudget(userId)).thenReturn(budget);
        
        // When
        TokenLimitEnforcementService.TokenAllowance allowance = 
            enforcementService.getRemainingAllowance(userId);
        
        // Then
        assertTrue(allowance.isUnlimited());
    }
    
    @Test
    void getRemainingAllowance_WithExceededLimits_ShouldReturnZeroRemaining() {
        // Given
        String userId = "user-123";
        TokenBudget budget = createBudgetWithLimits(1000, 10000, 1200, 12000); // Exceeded both limits
        
        when(tokenMonitor.getTokenBudget(userId)).thenReturn(budget);
        
        // When
        TokenLimitEnforcementService.TokenAllowance allowance = 
            enforcementService.getRemainingAllowance(userId);
        
        // Then
        assertEquals(0, allowance.getDailyRemaining());
        assertEquals(0, allowance.getMonthlyRemaining());
    }
    
    // Helper methods
    
    private TokenBudget createBudgetWithLimits(int dailyLimit, int monthlyLimit, int dailyUsed, int monthlyUsed) {
        TokenBudget budget = new TokenBudget("user-123", dailyLimit, monthlyLimit);
        budget.setDailyUsed(dailyUsed);
        budget.setMonthlyUsed(monthlyUsed);
        return budget;
    }
}