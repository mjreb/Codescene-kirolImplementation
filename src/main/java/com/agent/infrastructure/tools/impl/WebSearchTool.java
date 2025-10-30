package com.agent.infrastructure.tools.impl;

import com.agent.domain.model.ParameterDefinition;
import com.agent.domain.model.ToolDefinition;
import com.agent.domain.model.ToolResult;
import com.agent.infrastructure.tools.BaseTool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Web search tool for retrieving information from the internet.
 * This is a simplified implementation that searches DuckDuckGo.
 */
@Component
public class WebSearchTool extends BaseTool {
    
    private static final String DUCKDUCKGO_INSTANT_ANSWER_API = "https://api.duckduckgo.com/";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    public WebSearchTool() {
        super(createDefinition());
    }
    
    private static ToolDefinition createDefinition() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("web_search");
        definition.setDescription("Searches the web for information using DuckDuckGo instant answers");
        definition.setCategory("information");
        definition.setAsync(false);
        
        Map<String, ParameterDefinition> parameters = new HashMap<>();
        
        // Query parameter
        ParameterDefinition query = new ParameterDefinition();
        query.setName("query");
        query.setType("string");
        query.setDescription("Search query to look up information");
        query.setRequired(true);
        query.setMinValue(1); // Minimum length
        query.setMaxValue(500); // Maximum length
        parameters.put("query", query);
        
        // Max results parameter (optional)
        ParameterDefinition maxResults = new ParameterDefinition();
        maxResults.setName("max_results");
        maxResults.setType("integer");
        maxResults.setDescription("Maximum number of results to return (default: 5)");
        maxResults.setRequired(false);
        maxResults.setDefaultValue(5);
        maxResults.setMinValue(1);
        maxResults.setMaxValue(10);
        parameters.put("max_results", maxResults);
        
        definition.setParameters(parameters);
        return definition;
    }
    
    @Override
    protected ToolResult executeInternal(Map<String, Object> parameters) {
        String query = getRequiredParameter(parameters, "query", String.class);
        Integer maxResults = getParameter(parameters, "max_results", Integer.class);
        
        if (maxResults == null) {
            maxResults = 5;
        }
        
        try {
            SearchResult result = performSearch(query, maxResults);
            return createSuccessResult(result);
            
        } catch (Exception e) {
            logger.error("Web search failed for query '{}': {}", query, e.getMessage());
            return createErrorResult("Web search failed: " + e.getMessage());
        }
    }
    
    private SearchResult performSearch(String query, int maxResults) throws IOException, InterruptedException {
        // Use DuckDuckGo Instant Answer API for basic information
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = DUCKDUCKGO_INSTANT_ANSWER_API + "?q=" + encodedQuery + "&format=json&no_html=1&skip_disambig=1";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "Agent-Application/1.0")
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Search API returned status code: " + response.statusCode());
        }
        
        return parseSearchResponse(response.body(), query, maxResults);
    }
    
    private SearchResult parseSearchResponse(String jsonResponse, String query, int maxResults) {
        SearchResult result = new SearchResult();
        result.setQuery(query);
        result.setMaxResults(maxResults);
        
        try {
            // Simple JSON parsing for DuckDuckGo response
            // In a real implementation, you'd use a proper JSON library like Jackson
            
            String abstractText = extractJsonField(jsonResponse, "Abstract");
            String abstractSource = extractJsonField(jsonResponse, "AbstractSource");
            String abstractUrl = extractJsonField(jsonResponse, "AbstractURL");
            String answer = extractJsonField(jsonResponse, "Answer");
            String answerType = extractJsonField(jsonResponse, "AnswerType");
            
            if (!answer.isEmpty()) {
                result.setAnswer(answer);
                result.setAnswerType(answerType);
            }
            
            if (!abstractText.isEmpty()) {
                result.setAbstractText(abstractText);
                result.setAbstractSource(abstractSource);
                result.setAbstractUrl(abstractUrl);
            }
            
            if (result.getAnswer() == null && result.getAbstractText() == null) {
                result.setMessage("No instant answer found. Try a more specific query or search manually.");
            }
            
        } catch (Exception e) {
            logger.warn("Failed to parse search response: {}", e.getMessage());
            result.setMessage("Search completed but results could not be parsed properly.");
        }
        
        return result;
    }
    
    private String extractJsonField(String json, String fieldName) {
        try {
            Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]*?)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
            }
        } catch (Exception e) {
            logger.debug("Failed to extract field '{}' from JSON", fieldName);
        }
        return "";
    }
    
    /**
     * Represents a web search result.
     */
    public static class SearchResult {
        private String query;
        private int maxResults;
        private String answer;
        private String answerType;
        private String abstractText;
        private String abstractSource;
        private String abstractUrl;
        private String message;
        
        // Getters and setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
        
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        
        public String getAnswerType() { return answerType; }
        public void setAnswerType(String answerType) { this.answerType = answerType; }
        
        public String getAbstractText() { return abstractText; }
        public void setAbstractText(String abstractText) { this.abstractText = abstractText; }
        
        public String getAbstractSource() { return abstractSource; }
        public void setAbstractSource(String abstractSource) { this.abstractSource = abstractSource; }
        
        public String getAbstractUrl() { return abstractUrl; }
        public void setAbstractUrl(String abstractUrl) { this.abstractUrl = abstractUrl; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Search Results for: ").append(query).append("\n");
            
            if (answer != null && !answer.isEmpty()) {
                sb.append("Answer: ").append(answer).append("\n");
                if (answerType != null && !answerType.isEmpty()) {
                    sb.append("Type: ").append(answerType).append("\n");
                }
            }
            
            if (abstractText != null && !abstractText.isEmpty()) {
                sb.append("Summary: ").append(abstractText).append("\n");
                if (abstractSource != null && !abstractSource.isEmpty()) {
                    sb.append("Source: ").append(abstractSource);
                    if (abstractUrl != null && !abstractUrl.isEmpty()) {
                        sb.append(" (").append(abstractUrl).append(")");
                    }
                    sb.append("\n");
                }
            }
            
            if (message != null && !message.isEmpty()) {
                sb.append("Note: ").append(message).append("\n");
            }
            
            return sb.toString().trim();
        }
    }
}