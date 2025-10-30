package com.agent.infrastructure.security.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Service for implementing rate limiting using Redis
 */
@Service
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;

    // Default rate limits
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;
    private static final int DEFAULT_REQUESTS_PER_HOUR = 1000;
    private static final int DEFAULT_REQUESTS_PER_DAY = 10000;

    // API key rate limits (higher)
    private static final int API_KEY_REQUESTS_PER_MINUTE = 300;
    private static final int API_KEY_REQUESTS_PER_HOUR = 5000;
    private static final int API_KEY_REQUESTS_PER_DAY = 50000;

    public RateLimitingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Check if user is within rate limits
     */
    public RateLimitResult checkRateLimit(String userId, RateLimitType type) {
        String key = buildRateLimitKey(userId, type);
        
        int limit = getLimit(type, false);
        Duration window = getWindow(type);
        
        return checkLimit(key, limit, window);
    }

    /**
     * Check if API key is within rate limits
     */
    public RateLimitResult checkApiKeyRateLimit(String apiKeyId, RateLimitType type) {
        String key = buildApiKeyRateLimitKey(apiKeyId, type);
        
        int limit = getLimit(type, true);
        Duration window = getWindow(type);
        
        return checkLimit(key, limit, window);
    }

    /**
     * Check rate limit for conversation-specific operations
     */
    public RateLimitResult checkConversationRateLimit(String userId, String conversationId) {
        String key = "rate_limit:conversation:" + userId + ":" + conversationId + ":minute";
        
        // Allow 10 messages per minute per conversation
        return checkLimit(key, 10, Duration.ofMinutes(1));
    }

    /**
     * Increment rate limit counter
     */
    public void incrementRateLimit(String userId, RateLimitType type) {
        String key = buildRateLimitKey(userId, type);
        Duration window = getWindow(type);
        
        increment(key, window);
    }

    /**
     * Increment API key rate limit counter
     */
    public void incrementApiKeyRateLimit(String apiKeyId, RateLimitType type) {
        String key = buildApiKeyRateLimitKey(apiKeyId, type);
        Duration window = getWindow(type);
        
        increment(key, window);
    }

    /**
     * Increment conversation rate limit counter
     */
    public void incrementConversationRateLimit(String userId, String conversationId) {
        String key = "rate_limit:conversation:" + userId + ":" + conversationId + ":minute";
        increment(key, Duration.ofMinutes(1));
    }

    /**
     * Reset rate limits for user (admin function)
     */
    public void resetRateLimits(String userId) {
        for (RateLimitType type : RateLimitType.values()) {
            String key = buildRateLimitKey(userId, type);
            redisTemplate.delete(key);
        }
    }

    /**
     * Get current usage for user
     */
    public RateLimitUsage getCurrentUsage(String userId) {
        int minuteUsage = getCurrentCount(buildRateLimitKey(userId, RateLimitType.PER_MINUTE));
        int hourUsage = getCurrentCount(buildRateLimitKey(userId, RateLimitType.PER_HOUR));
        int dayUsage = getCurrentCount(buildRateLimitKey(userId, RateLimitType.PER_DAY));
        
        return new RateLimitUsage(minuteUsage, hourUsage, dayUsage);
    }

    private RateLimitResult checkLimit(String key, int limit, Duration window) {
        int currentCount = getCurrentCount(key);
        
        if (currentCount >= limit) {
            long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return RateLimitResult.exceeded(limit, currentCount, ttl);
        }
        
        return RateLimitResult.allowed(limit, currentCount);
    }

    private void increment(String key, Duration window) {
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, window);
    }

    private int getCurrentCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    private String buildRateLimitKey(String userId, RateLimitType type) {
        return "rate_limit:user:" + userId + ":" + type.name().toLowerCase();
    }

    private String buildApiKeyRateLimitKey(String apiKeyId, RateLimitType type) {
        return "rate_limit:api_key:" + apiKeyId + ":" + type.name().toLowerCase();
    }

    private int getLimit(RateLimitType type, boolean isApiKey) {
        return switch (type) {
            case PER_MINUTE -> isApiKey ? API_KEY_REQUESTS_PER_MINUTE : DEFAULT_REQUESTS_PER_MINUTE;
            case PER_HOUR -> isApiKey ? API_KEY_REQUESTS_PER_HOUR : DEFAULT_REQUESTS_PER_HOUR;
            case PER_DAY -> isApiKey ? API_KEY_REQUESTS_PER_DAY : DEFAULT_REQUESTS_PER_DAY;
        };
    }

    private Duration getWindow(RateLimitType type) {
        return switch (type) {
            case PER_MINUTE -> Duration.ofMinutes(1);
            case PER_HOUR -> Duration.ofHours(1);
            case PER_DAY -> Duration.ofDays(1);
        };
    }

    public enum RateLimitType {
        PER_MINUTE,
        PER_HOUR,
        PER_DAY
    }

    public static class RateLimitResult {
        private final boolean allowed;
        private final int limit;
        private final int currentCount;
        private final long resetTimeSeconds;

        private RateLimitResult(boolean allowed, int limit, int currentCount, long resetTimeSeconds) {
            this.allowed = allowed;
            this.limit = limit;
            this.currentCount = currentCount;
            this.resetTimeSeconds = resetTimeSeconds;
        }

        public static RateLimitResult allowed(int limit, int currentCount) {
            return new RateLimitResult(true, limit, currentCount, 0);
        }

        public static RateLimitResult exceeded(int limit, int currentCount, long resetTimeSeconds) {
            return new RateLimitResult(false, limit, currentCount, resetTimeSeconds);
        }

        public boolean isAllowed() { return allowed; }
        public int getLimit() { return limit; }
        public int getCurrentCount() { return currentCount; }
        public long getResetTimeSeconds() { return resetTimeSeconds; }
        public int getRemainingRequests() { return Math.max(0, limit - currentCount); }
    }

    public static class RateLimitUsage {
        private final int minuteUsage;
        private final int hourUsage;
        private final int dayUsage;

        public RateLimitUsage(int minuteUsage, int hourUsage, int dayUsage) {
            this.minuteUsage = minuteUsage;
            this.hourUsage = hourUsage;
            this.dayUsage = dayUsage;
        }

        public int getMinuteUsage() { return minuteUsage; }
        public int getHourUsage() { return hourUsage; }
        public int getDayUsage() { return dayUsage; }
    }
}