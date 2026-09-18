package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.core.statistics.MetricKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Generic algorithm metrics driven only by cross-structure typed event contributions. */
public final class DefaultAlgorithmMetricsProvider implements AlgorithmMetricsProvider {
  @Override
  public List<MetricItem> metrics(
      String structureId,
      ExecutionStatistics statistics,
      List<EventEnvelope> events,
      Map<String, Long> peaks) {
    List<MetricItem> result = new ArrayList<>();
    add(result, statistics, MetricKeys.NODES_VISITED, "label.workspace.metric.nodes_visited");
    add(result, statistics, MetricKeys.EDGES_EXAMINED, "label.workspace.metric.edges_examined");
    add(result, statistics, MetricKeys.COMPARISONS, "label.workspace.metric.comparisons");
    add(result, statistics, MetricKeys.WRITES, "label.workspace.metric.writes");
    add(result, statistics, MetricKeys.SWAPS, "label.workspace.metric.swaps");
    add(result, statistics, MetricKeys.MATCHES, "label.workspace.metric.matches");
    add(result, statistics, MetricKeys.FALLBACKS, "label.workspace.metric.fallbacks");
    add(result, statistics, MetricKeys.BACKTRACKS, "label.workspace.metric.backtracks");
    add(result, statistics, MetricKeys.INSERTIONS, "label.workspace.metric.insertions");
    add(result, statistics, MetricKeys.REMOVALS, "label.workspace.metric.removals");
    add(result, statistics, MetricKeys.UPDATES, "label.workspace.metric.updates");
    if (result.isEmpty() && statistics.totalEventCount() > 0L) {
      result.add(MetricItem.of("domainEvents", "label.workspace.metric.domain_events", statistics.domainEventCount()));
    }
    if (statistics.totalEventCount() > 0L) {
      result.add(MetricItem.of("totalEvents", "label.workspace.metric.total_events", statistics.totalEventCount()));
    }
    return List.copyOf(result);
  }

  private static void add(List<MetricItem> result, ExecutionStatistics statistics, String key, String labelKey) {
    long value = statistics.metric(key);
    if (value > 0L) result.add(MetricItem.of(key, labelKey, value));
  }
}
