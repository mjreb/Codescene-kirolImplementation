package com.agent.infrastructure.monitoring.health;

import com.agent.domain.interfaces.ToolFramework;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Health indicator for tool framework.
 */
@Component
public class ToolFrameworkHealthIndicator implements HealthIndicator {
    
    private final ToolFramework toolFramework;
    
    public ToolFrameworkHealthIndicator(ToolFramework toolFramework) {
        this.toolFramework = toolFramework;
    }
    
    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Get available tools
            var availableTools = toolFramework.getAvailableTools();
            
            // Test calculator tool if available
            boolean calculatorTest = testCalculatorTool();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            Health.Builder healthBuilder = Health.up();
            
            healthBuilder.withDetail("toolFramework", Map.of(
                    "status", "UP",
                    "responseTime", responseTime + "ms",
                    "availableTools", availableTools.size(),
                    "toolNames", availableTools.stream()
                            .map(tool -> tool.getName())
                            .toList(),
                    "calculatorTest", calculatorTest ? "PASSED" : "FAILED"
            ));
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("toolFramework", Map.of(
                            "status", "DOWN",
                            "error", e.getMessage()
                    ))
                    .withException(e)
                    .build();
        }
    }
    
    private boolean testCalculatorTool() {
        try {
            // Test basic calculator functionality
            Map<String, Object> parameters = Map.of(
                    "expression", "2 + 2"
            );
            
            var result = toolFramework.executeTool("calculator", parameters);
            return result != null && result.isSuccess();
            
        } catch (Exception e) {
            return false;
        }
    }
}