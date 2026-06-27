package com.sqlexplainer.dto;

import java.time.Instant;
import java.util.List;

public record AnalyzeResponse(
        Long id,
        String sql,
        double planningTimeMs,
        double executionTimeMs,
        PlanNode planTree,
        List<Bottleneck> bottlenecks,
        String explanation,
        List<RewriteSuggestion> suggestions,
        Instant createdAt
) {
}
