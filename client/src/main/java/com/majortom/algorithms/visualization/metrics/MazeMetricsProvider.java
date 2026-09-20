package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.majortom.algorithms.visualization.metrics.MetricsSupport.*;

/** Metrics for the maze structure family. */
final class MazeMetricsProvider implements StructureMetricsProvider<MazeViewState> {
  @Override public Class<MazeViewState> stateType() { return MazeViewState.class; }

  @Override
  public List<MetricItem> metrics(MazeViewState state, StructureMetricsContext context) {
    long open = state.openCells().stream().filter(Boolean.TRUE::equals).count();
    long total = (long) state.rows() * state.columns();
    return List.of(
        MetricItem.text("dimensions", "label.workspace.metric.dimensions", state.rows() + "×" + state.columns()),
        MetricItem.of("openCells", "label.workspace.metric.open_cells", open),
        MetricItem.of("walls", "label.workspace.metric.walls", Math.max(0L, total - open)),
        MetricItem.of("operations", "label.workspace.metric.structure_operations", context.operationCount()));
  }

  @Override
  public List<MetricItem> executionMetrics(MazeViewState state, List<com.majortom.algorithms.core.runtime.EventEnvelope> events, Map<String, Long> peaks) {
    List<MetricItem> result = new ArrayList<>();
    addPeakMetric(result, peaks, StateMetricKeys.VISITED, "peakVisited", "label.workspace.metric.peak_visited");
    addPeakMetric(result, peaks, StateMetricKeys.PATH, "peakPath", "label.workspace.metric.peak_path");
    return List.copyOf(result);
  }

  @Override
  public Map<String, Long> samples(MazeViewState state) {
    return Map.of(
        StateMetricKeys.VISITED, (long) state.visited().size(),
        StateMetricKeys.PATH, (long) state.path().size());
  }
}
