package com.agent.infrastructure.security;

import com.agent.domain.model.ApiKey;
import com.agent.infrastructure.security.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * API Key Authentication Filter for service-to-service authentication
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String apiKeyHeader = request.getHeader("X-API-Key");

        // Check if API Key header is present
        if (apiKeyHeader == null || apiKeyHeader.trim().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Validate API key
            Optional<ApiKey> apiKeyOpt = apiKeyService.validateApiKey(apiKeyHeader);
            
            if (apiKeyOpt.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                ApiKey apiKey = apiKeyOpt.get();
                
                // Create authorities based on API key scopes
                List<SimpleGrantedAuthority> authorities = apiKey.getScopes().stream()
                        .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                        .collect(Collectors.toList());

                // Create authentication token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        "api-key:" + apiKey.getId(),
                        null,
                        authorities
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
                // Update API key usage
                apiKeyService.updateLastUsed(apiKey.getId());
            }
        } catch (Exception e) {
            // Log the exception and continue without authentication
            logger.debug("API Key validation failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}