package com.sqlexplainer.util;

import com.sqlexplainer.dto.Bottleneck;
import com.sqlexplainer.dto.PlanNode;
import java.util.List;
import java.util.Locale;

public final class PromptTemplates {
    public static final String SYSTEM_PROMPT = """
            You are a senior PostgreSQL performance engineer inside the SQL Query Explainer + Optimizer tool.
            Explain what's slow and why using concrete numbers from the plan, ranked biggest contributor first.
            Propose 1-3 concrete rewritten queries/indexes, only as drop-in replacements if confident they're semantically equivalent; otherwise say so explicitly.
            Give a qualitative estimated speedup range per suggestion, labeled as an estimate, not a benchmark.
            Never invent stats/table/index names not present in the supplied plan or SQL.
            Respond with only a single valid JSON object, no markdown fences, no commentary, shaped exactly as:
            {
              "explanation": "string",
              "bottlenecks": [{"severity": "HIGH|MEDIUM|LOW", "nodeType": "", "relationName": "", "issue": "", "metric": ""}],
              "suggestions": [{"title": "", "rewrittenSql": "", "rationale": "", "estimatedSpeedup": ""}]
            }
            """;

    private PromptTemplates() {
    }

    public static String buildUserPrompt(String sql, PlanNode planSummary, List<Bottleneck> heuristicBottlenecks) {
        StringBuilder builder = new StringBuilder();
        builder.append("Original SQL:\n```sql\n").append(sql).append("\n```\n\n");
        builder.append("Condensed query plan:\n");
        appendPlanNode(builder, planSummary, 0);
        builder.append("\nHeuristic bottlenecks:\n");
        if (heuristicBottlenecks == null || heuristicBottlenecks.isEmpty()) {
            builder.append("- None detected by heuristics.\n");
        } else {
            for (Bottleneck bottleneck : heuristicBottlenecks) {
                builder.append("- ")
                        .append(bottleneck.severity()).append(": ")
                        .append(nullToEmpty(bottleneck.nodeType()));
                if (bottleneck.relationName() != null && !bottleneck.relationName().isBlank()) {
                    builder.append(" on ").append(bottleneck.relationName());
                }
                builder.append(" - ").append(nullToEmpty(bottleneck.issue()))
                        .append(" (").append(nullToEmpty(bottleneck.metric())).append(")\n");
            }
        }
        return builder.toString();
    }

    private static void appendPlanNode(StringBuilder builder, PlanNode node, int depth) {
        if (node == null) {
            builder.append("(no plan)\n");
            return;
        }
        builder.append("  ".repeat(depth))
                .append("- ").append(nullToEmpty(node.nodeType()));
        if (node.relationName() != null) {
            builder.append(" relation=").append(node.relationName());
        }
        if (node.indexName() != null) {
            builder.append(" index=").append(node.indexName());
        }
        builder.append(" rows=").append(node.actualRows())
                .append(" planRows=").append(node.planRows())
                .append(" loops=").append(node.actualLoops())
                .append(" actualTotalTimeMs=").append(String.format(Locale.ROOT, "%.3f", node.actualTotalTime()))
                .append(" percentOfTotal=").append(String.format(Locale.ROOT, "%.1f%%", node.percentOfTotalTime()))
                .append(" rowsRemovedByFilter=").append(node.rowsRemovedByFilter())
                .append("\n");
        for (PlanNode child : node.children()) {
            appendPlanNode(builder, child, depth + 1);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
