package com.agent.infrastructure.security.service;

import com.agent.domain.model.ApiKey;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing API keys
 */
@Service
public class ApiKeyService {

    private final Map<String, ApiKey> apiKeys = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a new API key
     */
    public String generateApiKey(String userId, String name, Set<String> scopes, Instant expiresAt) {
        // Generate random API key
        byte[] keyBytes = new byte[32];
        secureRandom.nextBytes(keyBytes);
        String apiKey = Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        
        // Hash the API key for storage
        String keyHash = hashApiKey(apiKey);
        
        // Create API key entity
        ApiKey apiKeyEntity = new ApiKey(UUID.randomUUID().toString(), keyHash, name, userId);
        apiKeyEntity.setScopes(scopes);
        apiKeyEntity.setExpiresAt(expiresAt);
        
        // Store in memory (in production, this would be stored in database)
        apiKeys.put(keyHash, apiKeyEntity);
        
        return apiKey;
    }

    /**
     * Validate API key and return associated ApiKey entity
     */
    public Optional<ApiKey> validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Optional.empty();
        }

        String keyHash = hashApiKey(apiKey);
        ApiKey apiKeyEntity = apiKeys.get(keyHash);
        
        if (apiKeyEntity == null) {
            return Optional.empty();
        }

        // Check if API key is enabled
        if (!apiKeyEntity.isEnabled()) {
            return Optional.empty();
        }

        // Check if API key is expired
        if (apiKeyEntity.isExpired()) {
            return Optional.empty();
        }

        return Optional.of(apiKeyEntity);
    }

    /**
     * Update last used timestamp for API key
     */
    public void updateLastUsed(String apiKeyId) {
        apiKeys.values().stream()
                .filter(apiKey -> apiKey.getId().equals(apiKeyId))
                .findFirst()
                .ifPresent(apiKey -> {
                    apiKey.setLastUsedAt(Instant.now());
                    apiKey.setUsageCount(apiKey.getUsageCount() + 1);
                });
    }

    /**
     * Revoke API key
     */
    public boolean revokeApiKey(String apiKeyId) {
        return apiKeys.values().stream()
                .filter(apiKey -> apiKey.getId().equals(apiKeyId))
                .findFirst()
                .map(apiKey -> {
                    apiKey.setEnabled(false);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Get API keys for user
     */
    public List<ApiKey> getApiKeysForUser(String userId) {
        return apiKeys.values().stream()
                .filter(apiKey -> apiKey.getUserId().equals(userId))
                .toList();
    }

    /**
     * Hash API key using SHA-256
     */
    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Initialize default API keys for testing
     */
    public void initializeDefaultApiKeys() {
        // Create a default API key for testing
        Set<String> scopes = Set.of("conversation:read", "conversation:write", "tool:execute");
        generateApiKey("default-user", "Default API Key", scopes, null);
    }
}