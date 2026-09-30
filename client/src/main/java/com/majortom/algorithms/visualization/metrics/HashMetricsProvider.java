package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.event.structure.HashStructureEvent;
import com.majortom.algorithms.visualization.runtime.hash.HashTableViewState;
import java.util.List;
import java.util.Map;

/** Metrics for the hash-table structure family. */
final class HashMetricsProvider implements StructureMetricsProvider<HashTableViewState> {
  @Override
  public Class<HashTableViewState> stateType() {
    return HashTableViewState.class;
  }

  @Override
  public List<MetricItem> metrics(
      HashTableViewState state, StructureMetricsContext context) {
    String load = String.format(java.util.Locale.ROOT, "%.2f",
        state.capacity() == 0 ? 0.0d : (double) state.size() / state.capacity());
    return List.of(
        MetricItem.of("entries", "label.workspace.metric.entries", state.size()),
        MetricItem.of("capacity", "label.workspace.metric.capacity", state.capacity()),
        MetricItem.text("loadFactor", "label.workspace.metric.load_factor", load),
        MetricItem.of("rehashes", "label.workspace.metric.rehashes",
            context.count(HashStructureEvent.Rehashed.class)));
  }

  @Override
  public Map<String, Long> samples(HashTableViewState state) {
    return Map.of(
        StateMetricKeys.SIZE, (long) state.size(),
        "hash.capacity", (long) state.capacity());
  }
}
