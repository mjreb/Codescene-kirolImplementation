package com.agent.infrastructure.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldAllowRequestWhenUnderLimit() {
        // Given
        String userId = "user-123";
        when(valueOperations.get(anyString())).thenReturn("5"); // Current count is 5
        
        // When
        RateLimitingService.RateLimitResult result = rateLimitingService.checkRateLimit(
                userId, RateLimitingService.RateLimitType.PER_MINUTE);

        // Then
        assertTrue(result.isAllowed());
        assertEquals(60, result.getLimit()); // Default limit per minute
        assertEquals(5, result.getCurrentCount());
        assertEquals(55, result.getRemainingRequests());
    }

    @Test
    void shouldDenyRequestWhenOverLimit() {
        // Given
        String userId = "user-123";
        when(valueOperations.get(anyString())).thenReturn("60"); // At limit
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(30L);
        
        // When
        RateLimitingService.RateLimitResult result = rateLimitingService.checkRateLimit(
                userId, RateLimitingService.RateLimitType.PER_MINUTE);

        // Then
        assertFalse(result.isAllowed());
        assertEquals(60, result.getLimit());
        assertEquals(60, result.getCurrentCount());
        assertEquals(0, result.getRemainingRequests());
        assertEquals(30L, result.getResetTimeSeconds());
    }

    @Test
    void shouldAllowHigherLimitsForApiKeys() {
        // Given
        String apiKeyId = "api-key-123";
        when(valueOperations.get(anyString())).thenReturn("100"); // Current count is 100
        
        // When
        RateLimitingService.RateLimitResult result = rateLimitingService.checkApiKeyRateLimit(
                apiKeyId, RateLimitingService.RateLimitType.PER_MINUTE);

        // Then
        assertTrue(result.isAllowed());
        assertEquals(300, result.getLimit()); // API key limit per minute
        assertEquals(100, result.getCurrentCount());
        assertEquals(200, result.getRemainingRequests());
    }

    @Test
    void shouldIncrementCounter() {
        // Given
        String userId = "user-123";
        
        // When
        rateLimitingService.incrementRateLimit(userId, RateLimitingService.RateLimitType.PER_MINUTE);

        // Then
        verify(valueOperations).increment("rate_limit:user:" + userId + ":per_minute");
        verify(redisTemplate).expire(eq("rate_limit:user:" + userId + ":per_minute"), any());
    }

    @Test
    void shouldCheckConversationRateLimit() {
        // Given
        String userId = "user-123";
        String conversationId = "conv-456";
        when(valueOperations.get(anyString())).thenReturn("5");
        
        // When
        RateLimitingService.RateLimitResult result = rateLimitingService.checkConversationRateLimit(
                userId, conversationId);

        // Then
        assertTrue(result.isAllowed());
        assertEquals(10, result.getLimit()); // Conversation limit is 10 per minute
    }

    @Test
    void shouldResetRateLimits() {
        // Given
        String userId = "user-123";
        
        // When
        rateLimitingService.resetRateLimits(userId);

        // Then
        verify(redisTemplate, times(3)).delete(anyString()); // 3 rate limit types
    }

    @Test
    void shouldReturnCurrentUsage() {
        // Given
        String userId = "user-123";
        when(valueOperations.get(contains("per_minute"))).thenReturn("10");
        when(valueOperations.get(contains("per_hour"))).thenReturn("100");
        when(valueOperations.get(contains("per_day"))).thenReturn("500");
        
        // When
        RateLimitingService.RateLimitUsage usage = rateLimitingService.getCurrentUsage(userId);

        // Then
        assertEquals(10, usage.getMinuteUsage());
        assertEquals(100, usage.getHourUsage());
        assertEquals(500, usage.getDayUsage());
    }

    @Test
    void shouldHandleNullRedisValues() {
        // Given
        String userId = "user-123";
        when(valueOperations.get(anyString())).thenReturn(null); // No previous count
        
        // When
        RateLimitingService.RateLimitResult result = rateLimitingService.checkRateLimit(
                userId, RateLimitingService.RateLimitType.PER_MINUTE);

        // Then
        assertTrue(result.isAllowed());
        assertEquals(0, result.getCurrentCount());
        assertEquals(60, result.getRemainingRequests());
    }
}