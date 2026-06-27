package com.sqlexplainer.controller;

import com.sqlexplainer.dto.AnalyzeResponse;
import com.sqlexplainer.dto.HistorySummaryDto;
import com.sqlexplainer.service.QueryHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
public class QueryHistoryController {
    private final QueryHistoryService historyService;

    public QueryHistoryController(QueryHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public Page<HistorySummaryDto> history(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return historyService.summaries(pageable);
    }

    @GetMapping("/{id}")
    public AnalyzeResponse get(@PathVariable Long id) {
        return historyService.get(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        historyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
