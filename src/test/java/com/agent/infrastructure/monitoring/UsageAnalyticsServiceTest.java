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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageAnalyticsServiceTest {
    
    @Mock
    private TokenUsageRepository tokenUsageRepository;
    
    @Mock
    private TokenBudgetRepository tokenBudgetRepository;
    
    @Mock
    private TokenCountingService tokenCountingService;
    
    private UsageAnalyticsService analyticsService;
    
    @BeforeEach
    void setUp() {
        analyticsService = new UsageAnalyticsService(tokenUsageRepository, tokenBudgetRepository, tokenCountingService);
    }
    
    @Test
    void generateDetailedUsageReport_WithUsageData_ShouldCreateComprehensiveReport() {
        // Given
        String userId = "user-123";
        DateRange dateRange = new DateRange(LocalDate.now().minusDays(7), LocalDate.now());
        List<TokenUsage> mockUsageData = createMockUsageData();
        
        when(tokenUsageRepository.findByUserIdAndDateRange(eq(userId), any(Instant.class), any(Instant.class)))
            .thenReturn(mockUsageData);
        
        // When
        UsageReport result = analyticsService.generateDetailedUsageReport(userId, dateRange);
        
        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(dateRange, result.getDateRange());
        assertEquals(750, result.getTotalTokens()); // Sum of mock data
        assertEquals(2, result.getTotalConversations()); // Unique conversations
        assertEquals(0.05, result.getTotalCost(), 0.001);
        
        // Check provider breakdown
        assertNotNull(result.getTokensByProvider());
        assertEquals(350, result.getTokensByProvider().get("openai"));
        assertEquals(400, result.getTokensByProvider().get("anthropic"));
        
        // Check model breakdown
        assertNotNull(result.getTokensByModel());
        assertEquals(350, result.getTokensByModel().get("gpt-3.5-turbo"));
        assertEquals(400, result.getTokensByModel().get("claude-3-sonnet"));
        
        // Check daily breakdown
        assertNotNull(result.getDailyBreakdown());
        assertFalse(result.getDailyBreakdown().isEmpty());
    }
    
    @Test
    void generateDetailedUsageReport_WithNoUsageData_ShouldReturnEmptyReport() {
        // Given
        String userId = "user-123";
        DateRange dateRange = new DateRange(LocalDate.now().minusDays(7), LocalDate.now());
        
        when(tokenUsageRepository.findByUserIdAndDateRange(eq(userId), any(Instant.class), any(Instant.class)))
            .thenReturn(Arrays.asList());
        
        // When
        UsageReport result = analyticsService.generateDetailedUsageReport(userId, dateRange);
        
        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(0, result.getTotalTokens());
        assertEquals(0, result.getTotalConversations());
        assertEquals(0.0, result.getTotalCost());
    }
    
    @Test
    void generateOptimizationRecommendations_WithMultipleProviders_ShouldSuggestCheaperProvider() {
        // Given
        String userId = "user-123";
        DateRange dateRange = new DateRange(LocalDate.now().minusDays(7), LocalDate.now());
        List<TokenUsage> mockUsageData = createMockUsageDataWithDifferentCosts();
        
        when(tokenUsageRepository.findByUserIdAndDateRange(eq(userId), any(Instant.class), any(Instant.class)))
            .thenReturn(mockUsageData);
        
        // When
        List<String> recommendations = analyticsService.generateOptimizationRecommendations(userId, dateRange);
        
        // Then
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.stream().anyMatch(rec -> rec.contains("Consider using")));
    }
    
    @Test
    void generateOptimizationRecommendations_WithNoUsageData_ShouldReturnEmptyList() {
        // Given
        String userId = "user-123";
        DateRange dateRange = new DateRange(LocalDate.now().minusDays(7), LocalDate.now());
        
        when(tokenUsageRepository.findByUserIdAndDateRange(eq(userId), any(Instant.class), any(Instant.class)))
            .thenReturn(Arrays.asList());
        
        // When
        List<String> recommendations = analyticsService.generateOptimizationRecommendations(userId, dateRange);
        
        // Then
        assertNotNull(recommendations);
        assertTrue(recommendations.isEmpty());
    }
    
    @Test
    void calculateUsageTrends_WithSufficientData_ShouldCalculateTrends() {
        // Given
        String userId = "user-123";
        DateRange dateRange = new DateRange(LocalDate.now().minusDays(14), LocalDate.now());
        List<TokenUsage> mockUsageData = createMockUsageDataForTrends();
        
        when(tokenUsageRepository.findByUserIdAndDateRange(eq(userId), any(Instant.class), any(Instant.class)))
            .thenReturn(mockUsageData);
        
        // When
        Map<String, Object> trends = analyticsService.calculateUsageTrends(userId, dateRange);
        
        // Then
        assertNotNull(trends);
        assertTrue(trends.containsKey("averageDailyUsage"));
        assertTrue(trends.containsKey("maxDailyUsage"));
        assertTrue(trends.containsKey("minDailyUsage"));
        assertTrue(trends.containsKey("usageVariability"));
        
        assertTrue((Double) trends.get("averageDailyUsage") > 0);
        assertTrue((Integer) trends.get("maxDailyUsage") > 0);
        assertTrue((Integer) trends.get("minDailyUsage") >= 0);
    }
    
    @Test
    void calculateUsageTrends_WithInsufficientData_ShouldReturnEmptyTrends() {
        // Given
        String userId = "user-123";
        DateRange dateRange = new DateRange(LocalDate.now().minusDays(7), LocalDate.now());
        
        when(tokenUsageRepository.findByUserIdAndDateRange(eq(userId), any(Instant.class), any(Instant.class)))
            .thenReturn(Arrays.asList(createSingleTokenUsage()));
        
        // When
        Map<String, Object> trends = analyticsService.calculateUsageTrends(userId, dateRange);
        
        // Then
        assertNotNull(trends);
        assertTrue(trends.isEmpty());
    }
    
    @Test
    void getUsageStatistics_WithUsageData_ShouldCalculateCorrectStatistics() {
        // Given
        String userId = "user-123";
        DateRange dateRange = new DateRange(LocalDate.now().minusDays(7), LocalDate.now());
        List<TokenUsage> mockUsageData = createMockUsageData();
        
        when(tokenUsageRepository.findByUserIdAndDateRange(eq(userId), any(Instant.class), any(Instant.class)))
            .thenReturn(mockUsageData);
        
        // When
        Map<String, Object> stats = analyticsService.getUsageStatistics(userId, dateRange);
        
        // Then
        assertNotNull(stats);
        assertEquals(750, stats.get("totalTokens"));
        assertEquals(0.05, (Double) stats.get("totalCost"), 0.001);
        assertEquals(2L, stats.get("uniqueConversations"));
        assertEquals(375.0, (Double) stats.get("averageTokensPerConversation"), 0.1);
        assertTrue((Double) stats.get("averageTokensPerDay") > 0);
        assertTrue((Double) stats.get("averageCostPerDay") > 0);
    }
    
    @Test
    void getUsageStatistics_WithNoUsageData_ShouldReturnEmptyStatistics() {
        // Given
        String userId = "user-123";
        DateRange dateRange = new DateRange(LocalDate.now().minusDays(7), LocalDate.now());
        
        when(tokenUsageRepository.findByUserIdAndDateRange(eq(userId), any(Instant.class), any(Instant.class)))
            .thenReturn(Arrays.asList());
        
        // When
        Map<String, Object> stats = analyticsService.getUsageStatistics(userId, dateRange);
        
        // Then
        assertNotNull(stats);
        assertTrue(stats.isEmpty());
    }
    
    // Helper methods
    
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
    
    private List<TokenUsage> createMockUsageDataWithDifferentCosts() {
        TokenUsage expensiveUsage = new TokenUsage("conv-1", 1000, 500);
        expensiveUsage.setProviderId("expensive-provider");
        expensiveUsage.setModel("expensive-model");
        expensiveUsage.setEstimatedCost(1.50);
        expensiveUsage.setTimestamp(Instant.now().minusSeconds(3600));
        
        TokenUsage cheapUsage = new TokenUsage("conv-2", 1000, 500);
        cheapUsage.setProviderId("cheap-provider");
        cheapUsage.setModel("cheap-model");
        cheapUsage.setEstimatedCost(0.10);
        cheapUsage.setTimestamp(Instant.now().minusSeconds(1800));
        
        return Arrays.asList(expensiveUsage, cheapUsage);
    }
    
    private List<TokenUsage> createMockUsageDataForTrends() {
        List<TokenUsage> usageData = Arrays.asList(
            createTokenUsageForDay(0, 100),
            createTokenUsageForDay(1, 150),
            createTokenUsageForDay(2, 200),
            createTokenUsageForDay(3, 180),
            createTokenUsageForDay(4, 220),
            createTokenUsageForDay(5, 250),
            createTokenUsageForDay(6, 300),
            createTokenUsageForDay(7, 280),
            createTokenUsageForDay(8, 320),
            createTokenUsageForDay(9, 350)
        );
        return usageData;
    }
    
    private TokenUsage createTokenUsageForDay(int daysAgo, int tokens) {
        TokenUsage usage = new TokenUsage("conv-" + daysAgo, tokens / 2, tokens / 2);
        usage.setProviderId("openai");
        usage.setModel("gpt-3.5-turbo");
        usage.setEstimatedCost(tokens * 0.0001);
        usage.setTimestamp(Instant.now().minusSeconds(daysAgo * 24 * 3600L));
        return usage;
    }
    
    private TokenUsage createSingleTokenUsage() {
        TokenUsage usage = new TokenUsage("conv-1", 100, 50);
        usage.setProviderId("openai");
        usage.setModel("gpt-3.5-turbo");
        usage.setEstimatedCost(0.01);
        usage.setTimestamp(Instant.now());
        return usage;
    }
}