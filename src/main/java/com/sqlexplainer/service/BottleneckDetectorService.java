package com.sqlexplainer.service;

import com.sqlexplainer.dto.Bottleneck;
import com.sqlexplainer.dto.PlanNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class BottleneckDetectorService {

    public List<Bottleneck> detect(PlanNode root) {
        List<Bottleneck> bottlenecks = new ArrayList<>();
        Set<String> flaggedNodes = new HashSet<>();
        walk(root, bottlenecks, flaggedNodes);
        bottlenecks.sort(Comparator.comparingInt(b -> severityRank(b.severity())));
        return bottlenecks;
    }

    private void walk(PlanNode node, List<Bottleneck> bottlenecks, Set<String> flaggedNodes) {
        if (node == null) {
            return;
        }
        String key = key(node);
        if ("Seq Scan".equalsIgnoreCase(node.nodeType()) && node.actualRows() > 10_000) {
            add(bottlenecks, flaggedNodes, key, "HIGH", node, "Sequential scan over a large relation",
                    "actualRows=" + node.actualRows());
        }
        long expensiveFilterThreshold = Math.max(1000L, node.actualRows() * 3L);
        if (node.rowsRemovedByFilter() > expensiveFilterThreshold) {
            add(bottlenecks, flaggedNodes, key, "MEDIUM", node, "Filter discards many rows after reading them",
                    "rowsRemovedByFilter=" + node.rowsRemovedByFilter() + ", actualRows=" + node.actualRows());
        }
        if ("Nested Loop".equalsIgnoreCase(node.nodeType()) && node.actualLoops() > 1_000) {
            add(bottlenecks, flaggedNodes, key, "HIGH", node, "Nested loop has high multiplicative cost",
                    "actualLoops=" + node.actualLoops());
        }
        if (node.planRows() > 0) {
            double ratio = (double) node.actualRows() / (double) node.planRows();
            if (ratio > 10.0 || ratio < 0.1) {
                add(bottlenecks, flaggedNodes, key, "MEDIUM", node, "Row estimate differs greatly from actual rows",
                        "actualRows=" + node.actualRows() + ", planRows=" + node.planRows() + ", ratio=" + String.format(Locale.ROOT, "%.2f", ratio));
            }
        }
        if (!flaggedNodes.contains(key)) {
            if (node.percentOfTotalTime() >= 30.0) {
                add(bottlenecks, flaggedNodes, key, "HIGH", node, "Node consumes a large share of total execution time",
                        "percentOfTotalTime=" + String.format(Locale.ROOT, "%.1f%%", node.percentOfTotalTime()));
            } else if (node.percentOfTotalTime() >= 10.0) {
                add(bottlenecks, flaggedNodes, key, "LOW", node, "Node consumes a noticeable share of total execution time",
                        "percentOfTotalTime=" + String.format(Locale.ROOT, "%.1f%%", node.percentOfTotalTime()));
            }
        }
        for (PlanNode child : node.children()) {
            walk(child, bottlenecks, flaggedNodes);
        }
    }

    private void add(List<Bottleneck> bottlenecks, Set<String> flaggedNodes, String key, String severity,
                     PlanNode node, String issue, String metric) {
        bottlenecks.add(new Bottleneck(severity, node.nodeType(), node.relationName(), issue, metric));
        flaggedNodes.add(key);
    }

    private String key(PlanNode node) {
        return node.nodeType() + "|" + node.relationName() + "|" + node.alias() + "|" + node.actualTotalTime();
    }

    private int severityRank(String severity) {
        return switch (severity) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            default -> 2;
        };
    }
}
