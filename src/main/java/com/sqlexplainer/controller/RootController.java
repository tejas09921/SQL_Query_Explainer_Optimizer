package com.sqlexplainer.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "name", "SQL Query Explainer + Optimizer",
                "status", "live",
                "health", "/health",
                "history", "/api/v1/history",
                "analyze", "/api/v1/analyze"
        );
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "timestamp", Instant.now()
        );
    }
}
