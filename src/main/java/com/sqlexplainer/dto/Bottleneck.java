package com.sqlexplainer.dto;

public record Bottleneck(
        String severity,
        String nodeType,
        String relationName,
        String issue,
        String metric
) {
}
