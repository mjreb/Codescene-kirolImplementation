package com.agent.infrastructure.config;

import com.agent.domain.interfaces.SecretsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Environment-based fallback implementation of SecretsManager.
 * Uses environment variables and in-memory storage for secrets.
 */
@Component
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", havingValue = "false", matchIfMissing = true)
public class EnvironmentSecretsManager implements SecretsManager {
    
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentSecretsManager.class);
    private static final String SECRET_PREFIX = "SECRET_";
    
    private final Environment environment;
    private final Map<String, String> runtimeSecrets;
    private final SecureRandom secureRandom;
    
    public EnvironmentSecretsManager(Environment environment) {
        this.environment = environment;
        this.runtimeSecrets = new ConcurrentHashMap<>();
        this.secureRandom = new SecureRandom();
    }
    
    @Override
    public Optional<String> getSecret(String key) {
        try {
            // First check runtime secrets
            if (runtimeSecrets.containsKey(key)) {
                return Optional.of(runtimeSecrets.get(key));
            }
            
            // Then check environment variables
            String envKey = SECRET_PREFIX + key.toUpperCase().replace(".", "_");
            String value = environment.getProperty(envKey);
            
            return Optional.ofNullable(value);
        } catch (Exception e) {
            logger.error("Failed to retrieve secret: {}", key, e);
            return Optional.empty();
        }
    }
    
    @Override
    public boolean storeSecret(String key, String value) {
        try {
            runtimeSecrets.put(key, value);
            logger.info("Successfully stored secret in runtime storage: {}", key);
            return true;
        } catch (Exception e) {
            logger.error("Failed to store secret: {}", key, e);
            return false;
        }
    }
    
    @Override
    public boolean deleteSecret(String key) {
        try {
            runtimeSecrets.remove(key);
            logger.info("Successfully deleted secret from runtime storage: {}", key);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete secret: {}", key, e);
            return false;
        }
    }
    
    @Override
    public boolean hasSecret(String key) {
        return getSecret(key).isPresent();
    }
    
    @Override
    public Optional<String> rotateSecret(String key) {
        try {
            // Generate new secret value
            byte[] randomBytes = new byte[32];
            secureRandom.nextBytes(randomBytes);
            String newSecret = Base64.getEncoder().encodeToString(randomBytes);
            
            // Store the new secret
            if (storeSecret(key, newSecret)) {
                logger.info("Successfully rotated secret: {}", key);
                return Optional.of(newSecret);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to rotate secret: {}", key, e);
            return Optional.empty();
        }
    }
}