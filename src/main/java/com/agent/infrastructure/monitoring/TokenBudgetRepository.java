package com.agent.infrastructure.monitoring;

import com.agent.domain.model.TokenBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for TokenBudget entities.
 */
@Repository
public interface TokenBudgetRepository extends JpaRepository<TokenBudget, Long> {
    
    /**
     * Find token budget by user ID.
     */
    Optional<TokenBudget> findByUserId(String userId);
    
    /**
     * Check if a user has unlimited token budget.
     */
    boolean existsByUserIdAndUnlimitedTrue(String userId);
}