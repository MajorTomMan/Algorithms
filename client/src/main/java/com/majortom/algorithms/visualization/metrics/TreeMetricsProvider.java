package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.event.structure.TreeStructureEvent;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.majortom.algorithms.visualization.metrics.MetricsSupport.*;

/** Metrics for the tree structure family. */
final class TreeMetricsProvider implements StructureMetricsProvider<TreeViewState> {
  @Override public Class<TreeViewState> stateType() { return TreeViewState.class; }

  @Override
  public List<MetricItem> metrics(TreeViewState state, StructureMetricsContext context) {
    long height = height(state, state.rootId(), new HashSet<>());
    long leaves = state.nodes().values().stream().filter(node -> state.childrenOf(node).isEmpty()).count();
    List<MetricItem> result = new ArrayList<>();
    result.add(MetricItem.of("nodes", "label.workspace.metric.nodes", state.nodes().size()));
    result.add(MetricItem.of("height", "label.workspace.metric.height", height));
    result.add(MetricItem.of("leaves", "label.workspace.metric.leaves", leaves));
    if (StructureIds.AVL_TREE.equals(context.structureId()) && state.kind() == TreeViewState.Kind.BINARY) {
      result.add(MetricItem.of("maxBalance", "label.workspace.metric.max_balance", maxBalance(state)));
    } else {
      long relationChanges = context.count(TreeStructureEvent.LeftChanged.class)
          + context.count(TreeStructureEvent.RightChanged.class)
          + context.count(TreeStructureEvent.ChildInserted.class)
          + context.count(TreeStructureEvent.ChildRemoved.class)
          + context.count(TreeStructureEvent.RootChanged.class);
      result.add(MetricItem.of("relationChanges", "label.workspace.metric.relation_changes", relationChanges));
    }
    return List.copyOf(result);
  }

  @Override
  public List<MetricItem> executionMetrics(TreeViewState state, List<com.majortom.algorithms.core.runtime.EventEnvelope> events, Map<String, Long> peaks) {
    List<MetricItem> result = new ArrayList<>();
    addEventMetric(result, events, TreeStructureEvent.NodeInserted.class, "nodeInsertions", "label.workspace.metric.insertions");
    addEventMetric(result, events, TreeStructureEvent.NodeRemoved.class, "nodeRemovals", "label.workspace.metric.removals");
    long relations = countEvents(events, TreeStructureEvent.LeftChanged.class)
        + countEvents(events, TreeStructureEvent.RightChanged.class)
        + countEvents(events, TreeStructureEvent.ChildInserted.class)
        + countEvents(events, TreeStructureEvent.ChildRemoved.class)
        + countEvents(events, TreeStructureEvent.RootChanged.class);
    if (relations > 0L) result.add(MetricItem.of("relationChanges", "label.workspace.metric.relation_changes", relations));
    addPeakMetric(result, peaks, StateMetricKeys.VISITED, "peakVisited", "label.workspace.metric.peak_visited");
    addPeakMetric(result, peaks, StateMetricKeys.HEIGHT, "peakHeight", "label.workspace.metric.peak_height");
    return List.copyOf(result);
  }

  @Override
  public Map<String, Long> samples(TreeViewState state) {
    return Map.of(
        StateMetricKeys.SIZE, (long) state.nodes().size(),
        StateMetricKeys.HEIGHT, height(state, state.rootId(), new HashSet<>()),
        StateMetricKeys.VISITED, (long) state.visitedNodeIds().size());
  }

  private static long height(TreeViewState state, Long id, Set<Long> seen) {
    if (id == null || !seen.add(id)) return 0L;
    TreeViewState.Node node = state.nodes().get(id);
    if (node == null) return 0L;
    long max = 0L;
    for (Long child : state.childrenOf(node)) {
      max = Math.max(max, height(state, child, seen));
    }
    return 1L + max;
  }

  private static long maxBalance(TreeViewState state) {
    long max = 0L;
    for (TreeViewState.Node node : state.nodes().values()) {
      long left = height(state, node.leftId(), new HashSet<>());
      long right = height(state, node.rightId(), new HashSet<>());
      max = Math.max(max, Math.abs(left - right));
    }
    return max;
  }
}
