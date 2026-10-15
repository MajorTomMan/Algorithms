package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.event.structure.LinkedStructureEvent;
import com.majortom.algorithms.core.event.structure.TreeStructureEvent;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.core.runtime.ExecutionSummary;
import com.majortom.algorithms.core.statistics.MetricKeys;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Verifies a single metric identity across typed and structure-event counters. */
public final class MetricsDeduplicationChecks {
  private MetricsDeduplicationChecks() {}

  public static void main(String[] args) {
    checkFamily(StructureIds.LINKED_LIST, LinkedListViewState.fromValues(List.of(7)),
        List.of(envelope(0, new LinkedStructureEvent.NodeInserted(1L, 7)),
            envelope(1, new LinkedStructureEvent.NodeRemoved(1L, 7))));
    checkFamily(StructureIds.TREE, TreeViewState.empty(TreeViewState.Kind.GENERAL),
        List.of(envelope(0, new TreeStructureEvent.NodeInserted(1L, 7)),
            envelope(1, new TreeStructureEvent.NodeRemoved(1L, 7))));
    System.out.println("MetricsDeduplicationChecks passed");
  }

  private static void checkFamily(String id, Object state, List<EventEnvelope> events) {
    RuntimeOverviewService overview = new RuntimeOverviewService();
    ExecutionStatistics withContributions = statistics(events.size(),
        Map.of(MetricKeys.INSERTIONS, 8L, MetricKeys.REMOVALS, 4L));
    var typed = overview.build(id, state, List.of(), withContributions, events,
        ExecutionSummary.from(withContributions), Optional.empty(), Map.of());
    checkUnique(typed.algorithmMetrics());
    checkMetric(typed.algorithmMetrics(), MetricKeys.INSERTIONS, "8");
    checkMetric(typed.algorithmMetrics(), MetricKeys.REMOVALS, "4");

    ExecutionStatistics noContributions = statistics(events.size(), Map.of());
    var fallback = overview.build(id, state, List.of(), noContributions, events,
        ExecutionSummary.from(noContributions), Optional.empty(), Map.of());
    checkUnique(fallback.algorithmMetrics());
    checkMetric(fallback.algorithmMetrics(), MetricKeys.INSERTIONS, "1");
    checkMetric(fallback.algorithmMetrics(), MetricKeys.REMOVALS, "1");
  }

  private static EventEnvelope envelope(long sequence,
      com.majortom.algorithms.core.event.ExecutionEvent event) {
    return new EventEnvelope("run", "operation", sequence, Instant.EPOCH, "source", event);
  }

  private static ExecutionStatistics statistics(long events, Map<String, Long> metrics) {
    return new ExecutionStatistics(events, events, 0L, Optional.empty(), Optional.empty(),
        Duration.ZERO, metrics);
  }

  private static void checkMetric(List<MetricItem> metrics, String key, String value) {
    var matching = metrics.stream().filter(metric -> metric.key().equals(key)).toList();
    if (matching.size() != 1 || !matching.getFirst().value().equals(value))
      throw new AssertionError("Expected one " + key + "=" + value + ": " + metrics);
  }

  private static void checkUnique(List<MetricItem> metrics) {
    Set<String> seen = new HashSet<>();
    for (var metric : metrics) {
      if (!seen.add(metric.key())) throw new AssertionError("Duplicate metric key: " + metric.key());
    }
  }
}
