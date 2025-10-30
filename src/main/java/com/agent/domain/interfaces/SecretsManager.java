package com.agent.domain.interfaces;

import java.util.Optional;

/**
 * Interface for managing secrets and sensitive configuration data.
 */
public interface SecretsManager {
    
    /**
     * Retrieve a secret value by key
     * @param key the secret key
     * @return the secret value if present
     */
    Optional<String> getSecret(String key);
    
    /**
     * Store a secret value
     * @param key the secret key
     * @param value the secret value
     * @return true if storage was successful
     */
    boolean storeSecret(String key, String value);
    
    /**
     * Delete a secret
     * @param key the secret key
     * @return true if deletion was successful
     */
    boolean deleteSecret(String key);
    
    /**
     * Check if a secret exists
     * @param key the secret key
     * @return true if the secret exists
     */
    boolean hasSecret(String key);
    
    /**
     * Rotate a secret (generate new value)
     * @param key the secret key
     * @return the new secret value if rotation was successful
     */
    Optional<String> rotateSecret(String key);
}