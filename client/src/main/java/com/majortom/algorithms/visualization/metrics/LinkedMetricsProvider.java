package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.event.structure.LinkedStructureEvent;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.majortom.algorithms.visualization.metrics.MetricsSupport.*;

/** Metrics for the linked structure family. */
final class LinkedMetricsProvider implements StructureMetricsProvider<LinkedListViewState> {
  @Override public Class<LinkedListViewState> stateType() { return LinkedListViewState.class; }

  @Override
  public List<MetricItem> metrics(LinkedListViewState state, StructureMetricsContext context) {
    long links = state.nodes().values().stream().filter(node -> node.nextId() != null).count();
    long rewires = context.count(LinkedStructureEvent.NextChanged.class)
        + context.count(LinkedStructureEvent.PreviousChanged.class);
    return List.of(
        MetricItem.of("nodes", "label.workspace.metric.nodes", state.nodes().size()),
        MetricItem.of("links", "label.workspace.metric.links", links),
        MetricItem.of("rewires", "label.workspace.metric.rewires", rewires),
        MetricItem.of("operations", "label.workspace.metric.structure_operations", context.operationCount()));
  }

  @Override
  public List<MetricItem> executionMetrics(LinkedListViewState state, List<com.majortom.algorithms.core.runtime.EventEnvelope> events, Map<String, Long> peaks) {
    List<MetricItem> result = new ArrayList<>();
    addEventMetric(result, events, LinkedStructureEvent.NodeInserted.class, "nodeInsertions", "label.workspace.metric.insertions");
    addEventMetric(result, events, LinkedStructureEvent.NodeRemoved.class, "nodeRemovals", "label.workspace.metric.removals");
    long rewires = countEvents(events, LinkedStructureEvent.NextChanged.class)
        + countEvents(events, LinkedStructureEvent.PreviousChanged.class);
    if (rewires > 0L) result.add(MetricItem.of("rewires", "label.workspace.metric.rewires", rewires));
    long peak = peaks.getOrDefault(StateMetricKeys.SIZE, 0L);
    if (peak > 0L) result.add(MetricItem.of("peakSize", "label.workspace.metric.peak_size", peak));
    return List.copyOf(result);
  }

  @Override
  public Map<String, Long> samples(LinkedListViewState state) {
    return Map.of(StateMetricKeys.SIZE, (long) state.nodes().size());
  }
}
