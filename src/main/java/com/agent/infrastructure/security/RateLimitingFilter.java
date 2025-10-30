package com.agent.infrastructure.security;

import com.agent.infrastructure.security.service.AuthorizationService;
import com.agent.infrastructure.security.service.RateLimitingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter for rate limiting requests
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final AuthorizationService authorizationService;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(
            RateLimitingService rateLimitingService,
            AuthorizationService authorizationService,
            ObjectMapper objectMapper
    ) {
        this.rateLimitingService = rateLimitingService;
        this.authorizationService = authorizationService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip rate limiting for health checks and auth endpoints
        String requestPath = request.getRequestURI();
        if (shouldSkipRateLimit(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get user ID from security context
        String userId = authorizationService.getCurrentUserId();
        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check rate limits
        RateLimitingService.RateLimitResult minuteResult = null;
        RateLimitingService.RateLimitResult hourResult = null;
        RateLimitingService.RateLimitResult dayResult = null;

        if (authorizationService.isApiKeyAuthentication()) {
            // API key rate limits
            minuteResult = rateLimitingService.checkApiKeyRateLimit(userId, RateLimitingService.RateLimitType.PER_MINUTE);
            hourResult = rateLimitingService.checkApiKeyRateLimit(userId, RateLimitingService.RateLimitType.PER_HOUR);
            dayResult = rateLimitingService.checkApiKeyRateLimit(userId, RateLimitingService.RateLimitType.PER_DAY);
        } else {
            // User rate limits
            minuteResult = rateLimitingService.checkRateLimit(userId, RateLimitingService.RateLimitType.PER_MINUTE);
            hourResult = rateLimitingService.checkRateLimit(userId, RateLimitingService.RateLimitType.PER_HOUR);
            dayResult = rateLimitingService.checkRateLimit(userId, RateLimitingService.RateLimitType.PER_DAY);
        }

        // Check if any limit is exceeded
        if (!minuteResult.isAllowed() || !hourResult.isAllowed() || !dayResult.isAllowed()) {
            handleRateLimitExceeded(response, minuteResult, hourResult, dayResult);
            return;
        }

        // Add rate limit headers
        addRateLimitHeaders(response, minuteResult, hourResult, dayResult);

        // Increment counters
        if (authorizationService.isApiKeyAuthentication()) {
            rateLimitingService.incrementApiKeyRateLimit(userId, RateLimitingService.RateLimitType.PER_MINUTE);
            rateLimitingService.incrementApiKeyRateLimit(userId, RateLimitingService.RateLimitType.PER_HOUR);
            rateLimitingService.incrementApiKeyRateLimit(userId, RateLimitingService.RateLimitType.PER_DAY);
        } else {
            rateLimitingService.incrementRateLimit(userId, RateLimitingService.RateLimitType.PER_MINUTE);
            rateLimitingService.incrementRateLimit(userId, RateLimitingService.RateLimitType.PER_HOUR);
            rateLimitingService.incrementRateLimit(userId, RateLimitingService.RateLimitType.PER_DAY);
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipRateLimit(String requestPath) {
        return requestPath.startsWith("/health") ||
               requestPath.startsWith("/actuator") ||
               requestPath.startsWith("/api/auth") ||
               requestPath.equals("/") ||
               requestPath.startsWith("/swagger-ui") ||
               requestPath.startsWith("/v3/api-docs");
    }

    private void handleRateLimitExceeded(
            HttpServletResponse response,
            RateLimitingService.RateLimitResult minuteResult,
            RateLimitingService.RateLimitResult hourResult,
            RateLimitingService.RateLimitResult dayResult
    ) throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Determine which limit was exceeded
        RateLimitingService.RateLimitResult exceededResult = null;
        String limitType = null;

        if (!minuteResult.isAllowed()) {
            exceededResult = minuteResult;
            limitType = "minute";
        } else if (!hourResult.isAllowed()) {
            exceededResult = hourResult;
            limitType = "hour";
        } else if (!dayResult.isAllowed()) {
            exceededResult = dayResult;
            limitType = "day";
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Rate limit exceeded");
        errorResponse.put("message", "Too many requests per " + limitType);
        errorResponse.put("limit", exceededResult.getLimit());
        errorResponse.put("current", exceededResult.getCurrentCount());
        errorResponse.put("resetTimeSeconds", exceededResult.getResetTimeSeconds());

        // Add rate limit headers
        addRateLimitHeaders(response, minuteResult, hourResult, dayResult);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private void addRateLimitHeaders(
            HttpServletResponse response,
            RateLimitingService.RateLimitResult minuteResult,
            RateLimitingService.RateLimitResult hourResult,
            RateLimitingService.RateLimitResult dayResult
    ) {
        // Minute limits
        response.setHeader("X-RateLimit-Limit-Minute", String.valueOf(minuteResult.getLimit()));
        response.setHeader("X-RateLimit-Remaining-Minute", String.valueOf(minuteResult.getRemainingRequests()));
        response.setHeader("X-RateLimit-Reset-Minute", String.valueOf(minuteResult.getResetTimeSeconds()));

        // Hour limits
        response.setHeader("X-RateLimit-Limit-Hour", String.valueOf(hourResult.getLimit()));
        response.setHeader("X-RateLimit-Remaining-Hour", String.valueOf(hourResult.getRemainingRequests()));
        response.setHeader("X-RateLimit-Reset-Hour", String.valueOf(hourResult.getResetTimeSeconds()));

        // Day limits
        response.setHeader("X-RateLimit-Limit-Day", String.valueOf(dayResult.getLimit()));
        response.setHeader("X-RateLimit-Remaining-Day", String.valueOf(dayResult.getRemainingRequests()));
        response.setHeader("X-RateLimit-Reset-Day", String.valueOf(dayResult.getResetTimeSeconds()));
    }
}