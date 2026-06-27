package com.sqlexplainer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        String apiKey,
        String model,
        String apiUrl,
        int maxTokens,
        int timeoutSeconds
) {
}
