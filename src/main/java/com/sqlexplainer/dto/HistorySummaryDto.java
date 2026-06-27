package com.sqlexplainer.dto;

import java.time.Instant;

public record HistorySummaryDto(
        Long id,
        String sql,
        double planningTimeMs,
        double executionTimeMs,
        Instant createdAt
) {
}
