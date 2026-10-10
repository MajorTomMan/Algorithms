package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.event.structure.TreeStructureEvent;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.majortom.algorithms.visualization.metrics.MetricsSupport.*;

/** Metrics for the tree structure family. */
final class TreeMetricsProvider implements StructureMetricsProvider<TreeViewState> {
  private Map<Long, TreeViewState.Node> cachedNodes;
  private Long cachedRootId;
  private TreeViewState.Kind cachedKind;
  private TreeShape cachedShape;

  @Override public Class<TreeViewState> stateType() { return TreeViewState.class; }

  @Override
  public List<MetricItem> metrics(TreeViewState state, StructureMetricsContext context) {
    TreeShape shape = shape(state);
    long height = shape.height();
    long leaves = shape.leaves();
    List<MetricItem> result = new ArrayList<>();
    result.add(MetricItem.of("nodes", "label.workspace.metric.nodes", state.nodes().size()));
    result.add(MetricItem.of("height", "label.workspace.metric.height", height));
    result.add(MetricItem.of("leaves", "label.workspace.metric.leaves", leaves));
    if (StructureIds.AVL_TREE.equals(context.structureId()) && state.kind() == TreeViewState.Kind.BINARY) {
      result.add(MetricItem.of("maxBalance", "label.workspace.metric.max_balance", shape.maxBalance()));
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
        StateMetricKeys.HEIGHT, shape(state).height(),
        StateMetricKeys.VISITED, (long) state.visitedNodeIds().size());
  }

  /**
   * Observations change highlight sets, not tree topology. A shared immutable
   * node map identifies the same structure across thousands of animation frames.
   */
  private TreeShape shape(TreeViewState state) {
    if (cachedShape != null && cachedNodes == state.nodes()
        && cachedKind == state.kind()
        && java.util.Objects.equals(cachedRootId, state.rootId())) {
      return cachedShape;
    }
    HeightScan scan = new HeightScan(state);
    long height = scan.height(state.rootId());
    for (Long nodeId : state.nodes().keySet())
      scan.height(nodeId);
    long leaves = 0L;
    for (TreeViewState.Node node : state.nodes().values()) {
      if (state.childrenOf(node).isEmpty())
        leaves++;
    }
    TreeShape result = new TreeShape(height, leaves, scan.maxBalance);
    cachedNodes = state.nodes();
    cachedRootId = state.rootId();
    cachedKind = state.kind();
    cachedShape = result;
    return result;
  }

  private record TreeShape(long height, long leaves, long maxBalance) {}

  private static final class HeightScan {
    private final TreeViewState state;
    private final Map<Long, Long> heights = new HashMap<>();
    private final Set<Long> visiting = new HashSet<>();
    private long maxBalance;

    private HeightScan(TreeViewState state) {
      this.state = state;
    }

    private long height(Long nodeId) {
      if (nodeId == null) return 0L;
      Long saved = heights.get(nodeId);
      if (saved != null) return saved;
      if (!visiting.add(nodeId)) return 0L;
      TreeViewState.Node node = state.nodes().get(nodeId);
      if (node == null) {
        visiting.remove(nodeId);
        return 0L;
      }
      long value;
      if (state.kind() == TreeViewState.Kind.BINARY) {
        long left = height(node.leftId());
        long right = height(node.rightId());
        value = 1L + Math.max(left, right);
        maxBalance = Math.max(maxBalance, Math.abs(left - right));
      } else {
        long maximum = 0L;
        for (Long childId : node.childIds())
          maximum = Math.max(maximum, height(childId));
        value = 1L + maximum;
      }
      visiting.remove(nodeId);
      heights.put(nodeId, value);
      return value;
    }
  }
}
