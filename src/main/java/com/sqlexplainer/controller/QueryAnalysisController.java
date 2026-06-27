package com.sqlexplainer.controller;

import com.sqlexplainer.dto.AnalyzeRequest;
import com.sqlexplainer.dto.AnalyzeResponse;
import com.sqlexplainer.service.QueryAnalysisOrchestrator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analyze")
public class QueryAnalysisController {
    private final QueryAnalysisOrchestrator orchestrator;

    public QueryAnalysisController(QueryAnalysisOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    public AnalyzeResponse analyze(@Valid @RequestBody AnalyzeRequest request) {
        return orchestrator.analyze(request);
    }
}
