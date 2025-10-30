package com.agent.infrastructure.config;

import com.agent.domain.interfaces.SecretsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * HashiCorp Vault implementation of SecretsManager.
 */
@Component
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", havingValue = "true")
public class VaultSecretsManager implements SecretsManager {
    
    private static final Logger logger = LoggerFactory.getLogger(VaultSecretsManager.class);
    private static final String SECRET_PATH_PREFIX = "secret/agent/";
    
    private final VaultTemplate vaultTemplate;
    private final SecureRandom secureRandom;
    
    public VaultSecretsManager(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
        this.secureRandom = new SecureRandom();
    }
    
    @Override
    public Optional<String> getSecret(String key) {
        try {
            String path = SECRET_PATH_PREFIX + key;
            VaultResponse response = vaultTemplate.read(path);
            
            if (response != null && response.getData() != null) {
                Object value = response.getData().get("value");
                return Optional.ofNullable(value != null ? value.toString() : null);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to retrieve secret: {}", key, e);
            return Optional.empty();
        }
    }
    
    @Override
    public boolean storeSecret(String key, String value) {
        try {
            String path = SECRET_PATH_PREFIX + key;
            Map<String, Object> data = Map.of("value", value);
            
            vaultTemplate.write(path, data);
            logger.info("Successfully stored secret: {}", key);
            return true;
        } catch (Exception e) {
            logger.error("Failed to store secret: {}", key, e);
            return false;
        }
    }
    
    @Override
    public boolean deleteSecret(String key) {
        try {
            String path = SECRET_PATH_PREFIX + key;
            vaultTemplate.delete(path);
            logger.info("Successfully deleted secret: {}", key);
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