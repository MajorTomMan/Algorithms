package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.event.structure.StringStructureEvent;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.majortom.algorithms.visualization.metrics.MetricsSupport.*;

/** Metrics for the string structure family. */
final class StringMetricsProvider implements StructureMetricsProvider<StringViewState> {
  @Override public Class<StringViewState> stateType() { return StringViewState.class; }

  @Override
  public List<MetricItem> metrics(StringViewState state, StructureMetricsContext context) {
    long edits = context.count(StringStructureEvent.Replaced.class)
        + context.count(StringStructureEvent.Inserted.class)
        + context.count(StringStructureEvent.Removed.class)
        + context.count(StringStructureEvent.Updated.class);
    return List.of(
        MetricItem.of("characters", "label.workspace.metric.characters", state.value().length()),
        MetricItem.of("edits", "label.workspace.metric.edits", edits),
        MetricItem.of("operations", "label.workspace.metric.structure_operations", context.operationCount()),
        MetricItem.of("events", "label.workspace.metric.structure_events", context.eventCount()));
  }

  @Override
  public List<MetricItem> executionMetrics(StringViewState state, List<com.majortom.algorithms.core.runtime.EventEnvelope> events, Map<String, Long> peaks) {
    long edits = countEvents(events, StringStructureEvent.Replaced.class)
        + countEvents(events, StringStructureEvent.Inserted.class)
        + countEvents(events, StringStructureEvent.Removed.class)
        + countEvents(events, StringStructureEvent.Updated.class);
    List<MetricItem> result = new ArrayList<>();
    if (edits > 0L) result.add(MetricItem.of("edits", "label.workspace.metric.edits", edits));
    long peak = peaks.getOrDefault(StateMetricKeys.SIZE, 0L);
    if (peak > 0L) result.add(MetricItem.of("peakSize", "label.workspace.metric.peak_size", peak));
    return List.copyOf(result);
  }

  @Override
  public Map<String, Long> samples(StringViewState state) {
    return Map.of(StateMetricKeys.SIZE, (long) state.value().length());
  }
}
