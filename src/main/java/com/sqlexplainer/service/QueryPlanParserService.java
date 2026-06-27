package com.sqlexplainer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlexplainer.dto.PlanNode;
import com.sqlexplainer.exception.TargetDbException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class QueryPlanParserService {
    private final ObjectMapper objectMapper;

    public QueryPlanParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedPlan parse(String explainJson) {
        try {
            JsonNode root = objectMapper.readTree(explainJson);
            JsonNode wrapper = root.isArray() ? root.get(0) : root;
            double planningTime = doubleValue(wrapper, "Planning Time");
            double executionTime = doubleValue(wrapper, "Execution Time");
            PlanNode planTree = parseNode(wrapper.get("Plan"), executionTime);
            return new ParsedPlan(planningTime, executionTime, planTree);
        } catch (Exception ex) {
            throw new TargetDbException("Unable to parse Postgres EXPLAIN JSON output.", ex);
        }
    }

    private PlanNode parseNode(JsonNode node, double totalExecutionTime) {
        List<PlanNode> children = new ArrayList<>();
        JsonNode plans = node.get("Plans");
        if (plans != null && plans.isArray()) {
            for (JsonNode child : plans) {
                children.add(parseNode(child, totalExecutionTime));
            }
        }
        double actualTotal = doubleValue(node, "Actual Total Time");
        double percent = totalExecutionTime <= 0.0 ? 0.0 : actualTotal / totalExecutionTime * 100.0;
        return new PlanNode(
                textValue(node, "Node Type"),
                textValue(node, "Relation Name"),
                textValue(node, "Alias"),
                textValue(node, "Index Name"),
                textValue(node, "Join Type"),
                doubleValue(node, "Startup Cost"),
                doubleValue(node, "Total Cost"),
                longValue(node, "Plan Rows"),
                intValue(node, "Plan Width"),
                doubleValue(node, "Actual Startup Time"),
                actualTotal,
                longValue(node, "Actual Rows"),
                longValue(node, "Actual Loops"),
                longValue(node, "Rows Removed by Filter"),
                textValue(node, "Filter"),
                longValue(node, "Shared Hit Blocks"),
                longValue(node, "Shared Read Blocks"),
                percent,
                List.copyOf(children)
        );
    }

    private String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private double doubleValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? 0.0 : value.asDouble();
    }

    private long longValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? 0L : value.asLong();
    }

    private int intValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? 0 : value.asInt();
    }

    public record ParsedPlan(double planningTimeMs, double executionTimeMs, PlanNode planTree) {
    }
}
