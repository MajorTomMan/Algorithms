package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.event.structure.ArrayStructureEvent;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
import java.util.List;
import java.util.Map;
import static com.majortom.algorithms.visualization.metrics.MetricsSupport.*;

/** Metrics for the array structure family. */
final class ArrayMetricsProvider implements StructureMetricsProvider<ArrayViewState> {
  @Override public Class<ArrayViewState> stateType() { return ArrayViewState.class; }

  @Override
  public List<MetricItem> metrics(ArrayViewState state, StructureMetricsContext context) {
    return List.of(
        MetricItem.of("elements", "label.workspace.metric.elements", state.values().size()),
        MetricItem.of("operations", "label.workspace.metric.structure_operations", context.operationCount()),
        MetricItem.of("writes", "label.workspace.metric.writes",
            context.count(ArrayStructureEvent.Inserted.class)
                + context.count(ArrayStructureEvent.Updated.class)
                + context.count(ArrayStructureEvent.Swapped.class) * 2L),
        MetricItem.of("mutations", "label.workspace.metric.mutations", context.eventCount()));
  }

  @Override
  public List<MetricItem> executionMetrics(ArrayViewState state, List<com.majortom.algorithms.core.runtime.EventEnvelope> events, Map<String, Long> peaks) {
    long peak = peaks.getOrDefault(StateMetricKeys.SIZE, 0L);
    return peak > 0L ? List.of(MetricItem.of("peakSize", "label.workspace.metric.peak_size", peak)) : List.of();
  }

  @Override
  public Map<String, Long> samples(ArrayViewState state) {
    return Map.of(StateMetricKeys.SIZE, (long) state.values().size());
  }
}
