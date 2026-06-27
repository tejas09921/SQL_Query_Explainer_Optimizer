package com.sqlexplainer.service;

import com.sqlexplainer.dto.AnalyzeRequest;
import com.sqlexplainer.dto.AnalyzeResponse;
import com.sqlexplainer.dto.Bottleneck;
import com.sqlexplainer.dto.RewriteSuggestion;
import com.sqlexplainer.service.LlmExplainerService.LlmResult;
import com.sqlexplainer.service.QueryPlanParserService.ParsedPlan;
import com.sqlexplainer.util.SqlSafetyValidator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class QueryAnalysisOrchestrator {
    private final SqlSafetyValidator sqlSafetyValidator;
    private final ExplainAnalyzeService explainAnalyzeService;
    private final QueryPlanParserService queryPlanParserService;
    private final BottleneckDetectorService bottleneckDetectorService;
    private final LlmExplainerService llmExplainerService;
    private final QueryHistoryService queryHistoryService;

    public QueryAnalysisOrchestrator(SqlSafetyValidator sqlSafetyValidator,
                                     ExplainAnalyzeService explainAnalyzeService,
                                     QueryPlanParserService queryPlanParserService,
                                     BottleneckDetectorService bottleneckDetectorService,
                                     LlmExplainerService llmExplainerService,
                                     QueryHistoryService queryHistoryService) {
        this.sqlSafetyValidator = sqlSafetyValidator;
        this.explainAnalyzeService = explainAnalyzeService;
        this.queryPlanParserService = queryPlanParserService;
        this.bottleneckDetectorService = bottleneckDetectorService;
        this.llmExplainerService = llmExplainerService;
        this.queryHistoryService = queryHistoryService;
    }

    public AnalyzeResponse analyze(AnalyzeRequest request) {
        sqlSafetyValidator.validate(request.sql(), request.allowNonSelect());
        String explainJson = explainAnalyzeService.explain(request.connection(), request.sql(), request.includeBuffers());
        ParsedPlan parsedPlan = queryPlanParserService.parse(explainJson);
        List<Bottleneck> heuristicBottlenecks = bottleneckDetectorService.detect(parsedPlan.planTree());
        LlmResult llmResult = llmExplainerService.explain(request.sql(), parsedPlan.planTree(), heuristicBottlenecks);
        List<Bottleneck> finalBottlenecks = llmResult.bottlenecks().isEmpty()
                ? heuristicBottlenecks
                : llmResult.bottlenecks();
        List<RewriteSuggestion> suggestions = llmResult.suggestions();
        return queryHistoryService.save(
                request.sql(),
                parsedPlan.planningTimeMs(),
                parsedPlan.executionTimeMs(),
                parsedPlan.planTree(),
                finalBottlenecks,
                llmResult.explanation(),
                suggestions);
    }
}
