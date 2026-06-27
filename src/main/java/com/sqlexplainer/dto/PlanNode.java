package com.sqlexplainer.dto;

import java.util.List;

public record PlanNode(
        String nodeType,
        String relationName,
        String alias,
        String indexName,
        String joinType,
        double startupCost,
        double totalCost,
        long planRows,
        int planWidth,
        double actualStartupTime,
        double actualTotalTime,
        long actualRows,
        long actualLoops,
        long rowsRemovedByFilter,
        String filter,
        long sharedHitBlocks,
        long sharedReadBlocks,
        double percentOfTotalTime,
        List<PlanNode> children
) {
}
