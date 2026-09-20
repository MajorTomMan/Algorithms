package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.event.structure.LinkedStructureEvent;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import java.util.List;
import java.util.Map;
import static com.majortom.algorithms.visualization.metrics.MetricsSupport.*;

/** Metrics for the linear structure family. */
final class LinearMetricsProvider implements StructureMetricsProvider<LinearStructureViewState> {
  @Override public Class<LinearStructureViewState> stateType() { return LinearStructureViewState.class; }

  @Override
  public boolean supports(String structureId) {
    return StructureIds.STACK.equals(structureId) || StructureIds.QUEUE.equals(structureId);
  }

  @Override
  public List<MetricItem> metrics(LinearStructureViewState state, StructureMetricsContext context) {
    boolean stack = StructureIds.STACK.equals(context.structureId());
    String sizeLabel = stack ? "label.workspace.metric.depth" : "label.workspace.metric.length";
    String insertedLabel = stack ? "label.workspace.metric.pushes" : "label.workspace.metric.enqueues";
    String removedLabel = stack ? "label.workspace.metric.pops" : "label.workspace.metric.dequeues";
    return List.of(
        MetricItem.of("size", sizeLabel, state.values().size()),
        MetricItem.of("added", insertedLabel, context.count(LinkedStructureEvent.NodeInserted.class)),
        MetricItem.of("removed", removedLabel, context.count(LinkedStructureEvent.NodeRemoved.class)),
        MetricItem.of("operations", "label.workspace.metric.structure_operations", context.operationCount()));
  }

  @Override
  public List<MetricItem> executionMetrics(LinearStructureViewState state, List<com.majortom.algorithms.core.runtime.EventEnvelope> events, Map<String, Long> peaks) {
    long peak = peaks.getOrDefault(StateMetricKeys.SIZE, 0L);
    if (peak <= 0L) return List.of();
    return List.of(MetricItem.of(
        StructureIds.STACK.equals(state.kind()) ? "peakDepth" : "peakLength",
        StructureIds.STACK.equals(state.kind()) ? "label.workspace.metric.peak_depth" : "label.workspace.metric.peak_length",
        peak));
  }

  @Override
  public Map<String, Long> samples(LinearStructureViewState state) {
    return Map.of(StateMetricKeys.SIZE, (long) state.values().size());
  }
}
