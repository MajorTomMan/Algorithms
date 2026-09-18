package com.majortom.algorithms.visualization.metrics;

import com.majortom.algorithms.core.event.structure.ArrayStructureEvent;
import com.majortom.algorithms.core.event.structure.GraphStructureEvent;
import com.majortom.algorithms.core.event.structure.LinkedStructureEvent;
import com.majortom.algorithms.core.event.structure.StringStructureEvent;
import com.majortom.algorithms.core.event.structure.TreeStructureEvent;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Registry of semantic metrics for every currently visualized structure family. */
public final class StructureMetricsRegistry {
  private final List<StructureMetricsProvider<?>> providers;

  public StructureMetricsRegistry(List<StructureMetricsProvider<?>> providers) {
    this.providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
  }

  public static StructureMetricsRegistry defaults() {
    return new StructureMetricsRegistry(List.of(
        new ArrayProvider(),
        new LinearProvider(),
        new LinkedProvider(),
        new TreeProvider(),
        new GraphProvider(),
        new StringProvider(),
        new MazeProvider()));
  }

  public List<MetricItem> metrics(String structureId, Object state, List<com.majortom.algorithms.core.runtime.EventEnvelope> events) {
    if (state == null) return List.of();
    StructureMetricsProvider<Object> provider = provider(structureId, state);
    if (provider == null) return List.of();
    return provider.metrics(state, new StructureMetricsContext(structureId, events));
  }

  public Map<String, Long> samples(String structureId, Object state) {
    if (state == null) return Map.of();
    StructureMetricsProvider<Object> provider = provider(structureId, state);
    return provider == null ? Map.of() : provider.samples(state);
  }

  public List<MetricItem> executionMetrics(
      String structureId, Object state, List<com.majortom.algorithms.core.runtime.EventEnvelope> events,
      Map<String, Long> peaks) {
    if (state == null) return List.of();
    StructureMetricsProvider<Object> provider = provider(structureId, state);
    return provider == null ? List.of() : provider.executionMetrics(state, events, peaks);
  }

  @SuppressWarnings("unchecked")
  private StructureMetricsProvider<Object> provider(String structureId, Object state) {
    for (StructureMetricsProvider<?> candidate : providers) {
      if (candidate.stateType().isInstance(state) && candidate.supports(structureId)) {
        return (StructureMetricsProvider<Object>) candidate;
      }
    }
    return null;
  }

  private static final class ArrayProvider implements StructureMetricsProvider<ArrayViewState> {
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

  private static final class LinearProvider implements StructureMetricsProvider<LinearStructureViewState> {
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

  private static final class LinkedProvider implements StructureMetricsProvider<LinkedListViewState> {
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

  private static final class TreeProvider implements StructureMetricsProvider<TreeViewState> {
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

  private static final class GraphProvider implements StructureMetricsProvider<GraphViewState> {
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

  private static final class StringProvider implements StructureMetricsProvider<StringViewState> {
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

  private static final class MazeProvider implements StructureMetricsProvider<MazeViewState> {
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

  private static void addEventMetric(
      List<MetricItem> result, List<com.majortom.algorithms.core.runtime.EventEnvelope> events,
      Class<?> eventType, String key, String labelKey) {
    long value = countEvents(events, eventType);
    if (value > 0L) result.add(MetricItem.of(key, labelKey, value));
  }

  private static long countEvents(
      List<com.majortom.algorithms.core.runtime.EventEnvelope> events, Class<?> eventType) {
    return events.stream().filter(event -> eventType.isInstance(event.event())).count();
  }

  private static void addPeakMetric(
      List<MetricItem> result, Map<String, Long> peaks, String sampleKey, String key, String labelKey) {
    long value = peaks.getOrDefault(sampleKey, 0L);
    if (value > 0L) result.add(MetricItem.of(key, labelKey, value));
  }

  private static String formatDecimal(double value) {
    if (Math.rint(value) == value) return Long.toString((long) value);
    return String.format(java.util.Locale.ROOT, "%.2f", value);
  }
}
