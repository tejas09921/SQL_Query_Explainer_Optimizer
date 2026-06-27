package com.sqlexplainer.dto;

public record RewriteSuggestion(
        String title,
        String rewrittenSql,
        String rationale,
        String estimatedSpeedup
) {
}
