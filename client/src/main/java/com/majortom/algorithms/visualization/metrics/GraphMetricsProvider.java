package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.event.structure.GraphStructureEvent;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import static com.majortom.algorithms.visualization.metrics.MetricsSupport.*;

/** Metrics for the graph structure family. */
final class GraphMetricsProvider implements StructureMetricsProvider<GraphViewState> {
  @Override public Class<GraphViewState> stateType() { return GraphViewState.class; }

  @Override
  public List<MetricItem> metrics(GraphViewState state, StructureMetricsContext context) {
    boolean weighted = state.weighted();
    List<MetricItem> result = new ArrayList<>();
    result.add(MetricItem.of("vertices", "label.workspace.metric.vertices", state.nodes().size()));
    result.add(MetricItem.of("edges", "label.workspace.metric.edges", state.edges().size()));
    result.add(MetricItem.localizedValue("graphType", "label.workspace.metric.graph_type",
        state.directed() ? "label.workspace.metric.directed" : "label.workspace.metric.undirected"));
    if (weighted) {
      double total = state.edges().stream().map(GraphViewState.Edge::weight)
          .filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
      result.add(MetricItem.text("weight", "label.workspace.metric.total_weight", formatDecimal(total)));
    } else {
      result.add(MetricItem.localizedValue("weight", "label.workspace.metric.weight",
          "label.workspace.metric.unweighted"));
    }
    result.add(MetricItem.of("components", "label.workspace.metric.components", componentCount(state)));
    result.add(MetricItem.of("operations", "label.workspace.metric.structure_operations", context.operationCount()));
    return List.copyOf(result);
  }

  @Override
  public List<MetricItem> executionMetrics(GraphViewState state, List<com.majortom.algorithms.core.runtime.EventEnvelope> events, Map<String, Long> peaks) {
    List<MetricItem> result = new ArrayList<>();
    addEventMetric(result, events, GraphStructureEvent.VertexAdded.class, "verticesAdded", "label.workspace.metric.vertices_added");
    addEventMetric(result, events, GraphStructureEvent.VertexRemoved.class, "verticesRemoved", "label.workspace.metric.vertices_removed");
    addEventMetric(result, events, GraphStructureEvent.EdgeAdded.class, "edgesAdded", "label.workspace.metric.edges_added");
    addEventMetric(result, events, GraphStructureEvent.EdgeRemoved.class, "edgesRemoved", "label.workspace.metric.edges_removed");
    addEventMetric(result, events, GraphStructureEvent.EdgeWeightChanged.class, "weightChanges", "label.workspace.metric.weight_changes");
    addPeakMetric(result, peaks, StateMetricKeys.VISITED, "peakVisited", "label.workspace.metric.peak_visited");
    return List.copyOf(result);
  }

  @Override
  public Map<String, Long> samples(GraphViewState state) {
    return Map.of(
        StateMetricKeys.VERTICES, (long) state.nodes().size(),
        StateMetricKeys.EDGES, (long) state.edges().size(),
        StateMetricKeys.VISITED, (long) state.visitedNodeIds().size());
  }

  private static long componentCount(GraphViewState state) {
    Map<Long, Set<Long>> adjacency = new LinkedHashMap<>();
    for (GraphViewState.Node node : state.nodes()) adjacency.put(node.id(), new HashSet<>());
    for (GraphViewState.Edge edge : state.edges()) {
      adjacency.computeIfAbsent(edge.fromId(), ignored -> new HashSet<>()).add(edge.toId());
      adjacency.computeIfAbsent(edge.toId(), ignored -> new HashSet<>()).add(edge.fromId());
    }
    Set<Long> seen = new HashSet<>();
    long components = 0L;
    for (Long start : adjacency.keySet()) {
      if (!seen.add(start)) continue;
      components++;
      Deque<Long> queue = new ArrayDeque<>();
      queue.add(start);
      while (!queue.isEmpty()) {
        Long current = queue.removeFirst();
        for (Long next : adjacency.getOrDefault(current, Set.of())) {
          if (seen.add(next)) queue.addLast(next);
        }
      }
    }
    return components;
  }
}
