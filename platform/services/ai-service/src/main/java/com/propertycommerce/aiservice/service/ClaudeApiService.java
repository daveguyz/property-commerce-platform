package com.propertycommerce.aiservice.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class ClaudeApiService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api.key}") private String apiKey;
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-6";

    public String chat(String systemPrompt, List<Map<String, String>> messages, int maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", MODEL);
        body.put("max_tokens", maxTokens);
        if (systemPrompt != null) body.put("system", systemPrompt);
        body.put("messages", messages);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    API_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
            return content != null && !content.isEmpty() ? (String) content.get(0).get("text") : "";
        } catch (Exception e) {
            log.error("Claude API error: {}", e.getMessage());
            throw new RuntimeException("AI service temporarily unavailable", e);
        }
    }

    public String singleTurn(String prompt, int maxTokens) {
        return chat(null, List.of(Map.of("role", "user", "content", prompt)), maxTokens);
    }
}
