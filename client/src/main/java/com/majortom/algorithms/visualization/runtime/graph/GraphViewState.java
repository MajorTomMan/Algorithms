package com.majortom.algorithms.visualization.runtime.graph;

import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.core.snapshot.MultiGraphSnapshot;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable graph facts and per-step algorithm observations, independent of JavaFX. */
public record GraphViewState(GraphDirection direction, List<Node> nodes, List<Edge> edges,
    Set<Long> visitedNodeIds, Observation observation, boolean completed) {
  public GraphViewState {
    direction = Objects.requireNonNull(direction, "direction");
    nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
    edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
    visitedNodeIds = Set.copyOf(Objects.requireNonNull(visitedNodeIds, "visitedNodeIds"));
    observation = Objects.requireNonNull(observation, "observation");
  }

  public static GraphViewState initial(GraphSnapshot<?> graph) {
    Objects.requireNonNull(graph, "graph");
    List<Node> nodes = graph.vertices().stream()
        .map(vertex -> new Node(vertex.id(), VisualValue.of(vertex.value())))
        .toList();
    List<Edge> edges = graph.edges().stream()
        .map(edge -> new Edge(edge.id(), edge.fromId(), edge.toId(), edge.weight()))
        .toList();
    return new GraphViewState(graph.direction(), nodes, edges, Set.of(), Observation.none(), false);
  }

  /** Convert a multigraph with compact value summaries; full details load on selection. */
  public static GraphViewState initial(MultiGraphSnapshot<?> graph) {
    Objects.requireNonNull(graph, "graph");
    List<Node> nodes = graph.vertices().stream()
        .map(vertex -> new Node(vertex.id(), VisualValue.compact(vertex.value()))).toList();
    List<Edge> edges = graph.edges().stream()
        .map(edge -> new Edge(edge.id(), edge.fromId(), edge.toId(), edge.weight(),
            edge.value() == null ? null :
                com.majortom.algorithms.visualization.runtime.value.ValueAdapters
                    .summary(edge.value()))).toList();
    return new GraphViewState(graph.direction(), nodes, edges, Set.of(), Observation.none(), false);
  }

  public Map<Long, Node> nodesById() {
    Map<Long, Node> result = new LinkedHashMap<>();
    for (Node node : nodes) {
      result.put(node.id(), node);
    }
    return Map.copyOf(result);
  }

  public Set<Long> visitedWith(long nodeId) {
    LinkedHashSet<Long> next = new LinkedHashSet<>(visitedNodeIds);
    next.add(nodeId);
    return Set.copyOf(next);
  }

  public record Node(long id, VisualValue value) {
    public Node {
      value = Objects.requireNonNull(value, "value");
    }
  }

  /** Optional label exists only for graphs with typed edge values. */
  public record Edge(long id, long fromId, long toId, double weight, String label) {
    public Edge(long id, long fromId, long toId, double weight) {
      this(id, fromId, toId, weight, null);
    }
    public Edge withWeight(double weight) {
      return new Edge(id, fromId, toId, weight, label);
    }
  }

  public record Observation(Type type, Long firstNodeId, Long secondNodeId) {
    public Observation {
      Objects.requireNonNull(type, "type");
    }
    public static Observation none() {
      return new Observation(Type.NONE, null, null);
    }
    public static Observation visited(long nodeId) {
      return new Observation(Type.VISITED, nodeId, null);
    }
    public static Observation examined(long fromId, long toId) {
      return new Observation(Type.EXAMINED, fromId, toId);
    }
  }

  public enum Type {
    NONE, VISITED, EXAMINED
  }
}
