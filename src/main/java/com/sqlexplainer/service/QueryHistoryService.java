package com.sqlexplainer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlexplainer.dto.AnalyzeResponse;
import com.sqlexplainer.dto.Bottleneck;
import com.sqlexplainer.dto.HistorySummaryDto;
import com.sqlexplainer.dto.PlanNode;
import com.sqlexplainer.dto.RewriteSuggestion;
import com.sqlexplainer.exception.ApiException;
import com.sqlexplainer.model.QueryHistory;
import com.sqlexplainer.repository.QueryHistoryRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class QueryHistoryService {
    private final QueryHistoryRepository repository;
    private final ObjectMapper objectMapper;

    public QueryHistoryService(QueryHistoryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public AnalyzeResponse save(String sql, double planningTimeMs, double executionTimeMs, PlanNode planTree,
                                List<Bottleneck> bottlenecks, String explanation,
                                List<RewriteSuggestion> suggestions) {
        try {
            QueryHistory history = new QueryHistory();
            history.setSql(sql);
            history.setPlanningTimeMs(planningTimeMs);
            history.setExecutionTimeMs(executionTimeMs);
            history.setPlanJson(objectMapper.writeValueAsString(planTree));
            history.setBottlenecksJson(objectMapper.writeValueAsString(bottlenecks));
            history.setSuggestionsJson(objectMapper.writeValueAsString(suggestions));
            history.setExplanation(explanation == null ? "" : explanation);
            history.setCreatedAt(Instant.now());
            QueryHistory saved = repository.save(history);
            return toAnalyzeResponse(saved);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save query analysis history.", ex);
        }
    }

    public Page<HistorySummaryDto> summaries(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable).map(this::toSummary);
    }

    public AnalyzeResponse get(Long id) {
        QueryHistory history = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "History item not found: " + id));
        return toAnalyzeResponse(history);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "History item not found: " + id);
        }
        repository.deleteById(id);
    }

    private HistorySummaryDto toSummary(QueryHistory history) {
        return new HistorySummaryDto(
                history.getId(),
                history.getSql(),
                history.getPlanningTimeMs(),
                history.getExecutionTimeMs(),
                history.getCreatedAt());
    }

    private AnalyzeResponse toAnalyzeResponse(QueryHistory history) {
        try {
            PlanNode planTree = objectMapper.readValue(history.getPlanJson(), PlanNode.class);
            List<Bottleneck> bottlenecks = objectMapper.readValue(
                    history.getBottlenecksJson(), new TypeReference<>() {
                    });
            List<RewriteSuggestion> suggestions = objectMapper.readValue(
                    history.getSuggestionsJson(), new TypeReference<>() {
                    });
            return new AnalyzeResponse(
                    history.getId(),
                    history.getSql(),
                    history.getPlanningTimeMs(),
                    history.getExecutionTimeMs(),
                    planTree,
                    bottlenecks,
                    history.getExplanation(),
                    suggestions,
                    history.getCreatedAt());
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read query analysis history.", ex);
        }
    }
}
