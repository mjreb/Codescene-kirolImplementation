package com.agent.infrastructure.monitoring;

import com.agent.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenMonitorImplTest {
    
    @Mock
    private TokenUsageRepository tokenUsageRepository;
    
    @Mock
    private TokenBudgetRepository tokenBudgetRepository;
    
    @Mock
    private TokenCountingService tokenCountingService;
    
    private TokenMonitorImpl tokenMonitor;
    
    @BeforeEach
    void setUp() {
        tokenMonitor = new TokenMonitorImpl(tokenUsageRepository, tokenBudgetRepository, tokenCountingService);
    }
    
    @Test
    void trackTokenUsage_ShouldCreateAndPersistTokenUsage() {
        // Given
        String conversationId = "conv-123";
        int inputTokens = 100;
        int outputTokens = 150;
        double expectedCost = 0.05;
        
        when(tokenCountingService.calculateCost(anyString(), anyString(), eq(inputTokens), eq(outputTokens)))
            .thenReturn(expectedCost);
        when(tokenUsageRepository.save(any(TokenUsage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenBudgetRepository.findByUserId(anyString())).thenReturn(Optional.of(createDefaultBudget()));
        when(tokenBudgetRepository.save(any(TokenBudget.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TokenUsage result = tokenMonitor.trackTokenUsage(conversationId, inputTokens, outputTokens);
        
        // Then
        assertNotNull(result);
        assertEquals(conversationId, result.getConversationId());
        assertEquals(inputTokens, result.getInputTokens());
        assertEquals(outputTokens, result.getOutputTokens());
        assertEquals(inputTokens + outputTokens, result.getTotalTokens());
        assertEquals(expectedCost, result.getEstimatedCost());
        
        verify(tokenUsageRepository).save(any(TokenUsage.class));
        verify(tokenBudgetRepository).save(any(TokenBudget.class));
    }
    
    @Test
    void checkTokenLimit_WithUnlimitedBudget_ShouldReturnTrue() {
        // Given
        String conversationId = "conv-123";
        int estimatedTokens = 1000;
        TokenBudget unlimitedBudget = createDefaultBudget();
        unlimitedBudget.setUnlimited(true);
        
        when(tokenBudgetRepository.findByUserId(anyString())).thenReturn(Optional.of(unlimitedBudget));
        
        // When
        boolean result = tokenMonitor.checkTokenLimit(conversationId, estimatedTokens);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void checkTokenLimit_WithinDailyLimit_ShouldReturnTrue() {
        // Given
        String conversationId = "conv-123";
        int estimatedTokens = 100;
        TokenBudget budget = createDefaultBudget();
        budget.setDailyUsed(500);
        budget.setDailyLimit(1000);
        budget.setMonthlyUsed(2000);
        budget.setMonthlyLimit(10000);
        
        when(tokenBudgetRepository.findByUserId(anyString())).thenReturn(Optional.of(budget));
        
        // When
        boolean result = tokenMonitor.checkTokenLimit(conversationId, estimatedTokens);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void checkTokenLimit_ExceedsDailyLimit_ShouldReturnFalse() {
        // Given
        String conversationId = "conv-123";
        int estimatedTokens = 600;
        TokenBudget budget = createDefaultBudget();
        budget.setDailyUsed(500);
        budget.setDailyLimit(1000);
        budget.setMonthlyUsed(2000);
        budget.setMonthlyLimit(10000);
        
        when(tokenBudgetRepository.findByUserId(anyString())).thenReturn(Optional.of(budget));
        
        // When
        boolean result = tokenMonitor.checkTokenLimit(conversationId, estimatedTokens);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void checkTokenLimit_ExceedsMonthlyLimit_ShouldReturnFalse() {
        // Given
        String conversationId = "conv-123";
        int estimatedTokens = 100;
        TokenBudget budget = createDefaultBudget();
        budget.setDailyUsed(500);
        budget.setDailyLimit(1000);
        budget.setMonthlyUsed(9950);
        budget.setMonthlyLimit(10000);
        
        when(tokenBudgetRepository.findByUserId(anyString())).thenReturn(Optional.of(budget));
        
        // When
        boolean result = tokenMonitor.checkTokenLimit(conversationId, estimatedTokens);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void getTokenBudget_ExistingUser_ShouldReturnBudget() {
        // Given
        String userId = "user-123";
        TokenBudget expectedBudget = createDefaultBudget();
        expectedBudget.setUserId(userId);
        
        when(tokenBudgetRepository.findByUserId(userId)).thenReturn(Optional.of(expectedBudget));
        
        // When
        TokenBudget result = tokenMonitor.getTokenBudget(userId);
        
        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(expectedBudget.getDailyLimit(), result.getDailyLimit());
        assertEquals(expectedBudget.getMonthlyLimit(), result.getMonthlyLimit());
    }
    
    @Test
    void getTokenBudget_NewUser_ShouldCreateDefaultBudget() {
        // Given
        String userId = "new-user-123";
        TokenBudget defaultBudget = new TokenBudget(userId, 10000, 100000);
        
        when(tokenBudgetRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(tokenBudgetRepository.save(any(TokenBudget.class))).thenReturn(defaultBudget);
        
        // When
        TokenBudget result = tokenMonitor.getTokenBudget(userId);
        
        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(10000, result.getDailyLimit());
        assertEquals(100000, result.getMonthlyLimit());
        verify(tokenBudgetRepository).save(any(TokenBudget.class));
    }
    
    @Test
    void generateUsageReport_ShouldCreateComprehensiveReport() {
        // Given
        String userId = "user-123";
        DateRange dateRange = new DateRange(LocalDate.now().minusDays(7), LocalDate.now());
        List<TokenUsage> mockUsageData = createMockUsageData();
        
        when(tokenUsageRepository.findByUserIdAndDateRange(eq(userId), any(Instant.class), any(Instant.class)))
            .thenReturn(mockUsageData);
        
        // When
        UsageReport result = tokenMonitor.generateUsageReport(userId, dateRange);
        
        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(dateRange, result.getDateRange());
        assertEquals(750, result.getTotalTokens()); // Sum of mock data
        assertEquals(2, result.getTotalConversations()); // Unique conversations
        assertTrue(result.getTotalCost() > 0);
        assertNotNull(result.getTokensByProvider());
        assertNotNull(result.getTokensByModel());
        assertNotNull(result.getDailyBreakdown());
    }
    
    @Test
    void estimateTokenCount_ShouldUseTokenCountingService() {
        // Given
        String text = "Hello, how are you?";
        String providerId = "openai";
        String model = "gpt-3.5-turbo";
        int expectedTokens = 5;
        
        when(tokenCountingService.estimateTokens(text, providerId, model)).thenReturn(expectedTokens);
        
        // When
        int result = tokenMonitor.estimateTokenCount(text, providerId, model);
        
        // Then
        assertEquals(expectedTokens, result);
        verify(tokenCountingService).estimateTokens(text, providerId, model);
    }
    
    @Test
    void getConversationTokenUsage_ShouldReturnCachedValue() {
        // Given
        String conversationId = "conv-123";
        
        // First track some usage to populate cache
        when(tokenCountingService.calculateCost(anyString(), anyString(), anyInt(), anyInt())).thenReturn(0.01);
        when(tokenUsageRepository.save(any(TokenUsage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenBudgetRepository.findByUserId(anyString())).thenReturn(Optional.of(createDefaultBudget()));
        when(tokenBudgetRepository.save(any(TokenBudget.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        tokenMonitor.trackTokenUsage(conversationId, 100, 50);
        
        // When
        int result = tokenMonitor.getConversationTokenUsage(conversationId);
        
        // Then
        assertEquals(150, result);
    }
    
    @Test
    void clearConversationCache_ShouldRemoveFromCache() {
        // Given
        String conversationId = "conv-123";
        
        // First track some usage to populate cache
        when(tokenCountingService.calculateCost(anyString(), anyString(), anyInt(), anyInt())).thenReturn(0.01);
        when(tokenUsageRepository.save(any(TokenUsage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenBudgetRepository.findByUserId(anyString())).thenReturn(Optional.of(createDefaultBudget()));
        when(tokenBudgetRepository.save(any(TokenBudget.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        tokenMonitor.trackTokenUsage(conversationId, 100, 50);
        assertEquals(150, tokenMonitor.getConversationTokenUsage(conversationId));
        
        // When
        tokenMonitor.clearConversationCache(conversationId);
        
        // Then
        assertEquals(0, tokenMonitor.getConversationTokenUsage(conversationId));
    }
    
    // Helper methods
    
    private TokenBudget createDefaultBudget() {
        TokenBudget budget = new TokenBudget("default-user", 10000, 100000);
        budget.setResetDate(Instant.now());
        return budget;
    }
    
    private List<TokenUsage> createMockUsageData() {
        TokenUsage usage1 = new TokenUsage("conv-1", 200, 150);
        usage1.setProviderId("openai");
        usage1.setModel("gpt-3.5-turbo");
        usage1.setEstimatedCost(0.02);
        usage1.setTimestamp(Instant.now().minusSeconds(3600));
        
        TokenUsage usage2 = new TokenUsage("conv-2", 300, 100);
        usage2.setProviderId("anthropic");
        usage2.setModel("claude-3-sonnet");
        usage2.setEstimatedCost(0.03);
        usage2.setTimestamp(Instant.now().minusSeconds(1800));
        
        return Arrays.asList(usage1, usage2);
    }
}