package com.sqlexplainer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlexplainer.config.LlmProperties;
import com.sqlexplainer.dto.Bottleneck;
import com.sqlexplainer.dto.PlanNode;
import com.sqlexplainer.dto.RewriteSuggestion;
import com.sqlexplainer.util.PromptTemplates;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LlmExplainerService {
    private final RestClient restClient;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    public LlmExplainerService(RestClient restClient, LlmProperties properties, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public LlmResult explain(String sql, PlanNode planTree, List<Bottleneck> heuristicBottlenecks) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            return new LlmResult(
                    "LLM explanation unavailable because ANTHROPIC_API_KEY is not configured. Heuristic bottlenecks are still included.",
                    List.of(),
                    List.of());
        }
        String userPrompt = PromptTemplates.buildUserPrompt(sql, planTree, heuristicBottlenecks);
        Map<String, Object> body = Map.of(
                "model", properties.model(),
                "max_tokens", properties.maxTokens(),
                "system", PromptTemplates.SYSTEM_PROMPT,
                "messages", List.of(Map.of("role", "user", "content", userPrompt))
        );
        try {
            JsonNode response = restClient.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", properties.apiKey())
                    .header("anthropic-version", "2023-06-01")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String rawText = extractText(response);
            return parseModelJson(rawText);
        } catch (Exception ex) {
            return new LlmResult(
                    "LLM explanation failed: " + ex.getMessage() + ". Heuristic bottlenecks are still included.",
                    List.of(),
                    List.of());
        }
    }

    private String extractText(JsonNode response) {
        if (response == null || !response.has("content")) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode block : response.get("content")) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        return text.toString();
    }

    private LlmResult parseModelJson(String rawText) {
        String stripped = stripJsonFences(rawText);
        try {
            JsonNode root = objectMapper.readTree(stripped);
            String explanation = root.path("explanation").asText("");
            List<Bottleneck> bottlenecks = new ArrayList<>();
            for (JsonNode node : root.path("bottlenecks")) {
                bottlenecks.add(objectMapper.treeToValue(node, Bottleneck.class));
            }
            List<RewriteSuggestion> suggestions = new ArrayList<>();
            for (JsonNode node : root.path("suggestions")) {
                suggestions.add(objectMapper.treeToValue(node, RewriteSuggestion.class));
            }
            return new LlmResult(explanation, List.copyOf(bottlenecks), List.copyOf(suggestions));
        } catch (Exception ex) {
            return new LlmResult(rawText, List.of(), List.of());
        }
    }

    private String stripJsonFences(String rawText) {
        String stripped = rawText == null ? "" : rawText.trim();
        if (stripped.startsWith("```")) {
            stripped = stripped.replaceFirst("^```(?:json)?\\s*", "");
            stripped = stripped.replaceFirst("\\s*```$", "");
        }
        return stripped.trim();
    }

    public record LlmResult(
            String explanation,
            List<Bottleneck> bottlenecks,
            List<RewriteSuggestion> suggestions
    ) {
    }
}
