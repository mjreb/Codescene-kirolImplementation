package com.agent.infrastructure.monitoring.health;

import com.agent.domain.interfaces.MemoryManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Health indicator for memory management components.
 */
@Component
public class MemoryHealthIndicator implements HealthIndicator {
    
    private final MemoryManager memoryManager;
    private final RedisConnectionFactory redisConnectionFactory;
    
    public MemoryHealthIndicator(MemoryManager memoryManager, 
                               RedisConnectionFactory redisConnectionFactory) {
        this.memoryManager = memoryManager;
        this.redisConnectionFactory = redisConnectionFactory;
    }
    
    @Override
    public Health health() {
        Health.Builder healthBuilder = Health.up();
        
        try {
            // Check Redis connection
            checkRedisHealth(healthBuilder);
            
            // Check memory manager functionality
            checkMemoryManagerHealth(healthBuilder);
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
    
    private void checkRedisHealth(Health.Builder healthBuilder) {
        try {
            RedisConnection connection = redisConnectionFactory.getConnection();
            
            // Test basic Redis operations
            long startTime = System.currentTimeMillis();
            String pong = connection.ping();
            long responseTime = System.currentTimeMillis() - startTime;
            
            if ("PONG".equals(pong)) {
                healthBuilder.withDetail("redis", Map.of(
                        "status", "UP",
                        "responseTime", responseTime + "ms"
                ));
            } else {
                healthBuilder.withDetail("redis", Map.of(
                        "status", "DOWN",
                        "reason", "Unexpected ping response: " + pong
                ));
            }
            
            connection.close();
            
        } catch (Exception e) {
            healthBuilder.withDetail("redis", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
            ));
        }
    }
    
    private void checkMemoryManagerHealth(Health.Builder healthBuilder) {
        try {
            // Test memory manager basic functionality
            String testKey = "health-check-" + System.currentTimeMillis();
            String testValue = "test-value";
            
            long startTime = System.currentTimeMillis();
            
            // Test store and retrieve
            memoryManager.storeLongTermMemory(testKey, testValue, null);
            var retrieved = memoryManager.retrieveLongTermMemory(testKey);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (retrieved.isPresent() && testValue.equals(retrieved.get())) {
                healthBuilder.withDetail("memoryManager", Map.of(
                        "status", "UP",
                        "responseTime", responseTime + "ms",
                        "operations", "store/retrieve successful"
                ));
            } else {
                healthBuilder.withDetail("memoryManager", Map.of(
                        "status", "DOWN",
                        "reason", "Store/retrieve operation failed"
                ));
            }
            
        } catch (Exception e) {
            healthBuilder.withDetail("memoryManager", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
            ));
        }
    }
}