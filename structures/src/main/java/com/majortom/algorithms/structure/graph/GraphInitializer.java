package com.majortom.algorithms.structure.graph;

import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.structure.initialization.StructureInitializer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Validates a data model and applies it through Graph's atomic bulk initialization. */
public final class GraphInitializer<T> implements StructureInitializer<Graph<T>, GraphData<T>> {

  /** Constructs a new graph without requiring a pre-existing structure instance. */
  public Graph<T> create(GraphData<T> data) {
    Objects.requireNonNull(data, "data");
    Graph<T> graph = new Graph<>(data.direction());
    initialize(graph, data);
    return graph;
  }

  @Override
  public void initialize(Graph<T> graph, GraphData<T> data) {
    Objects.requireNonNull(graph, "graph");
    Objects.requireNonNull(data, "data");
    if (graph.direction() != data.direction()) {
      throw new IllegalArgumentException("graph direction does not match initialization data");
    }

    Map<T, Map<T, Double>> adjacency = new LinkedHashMap<>();
    for (T vertex : data.vertices()) {
      Objects.requireNonNull(vertex, "vertex");
      if (adjacency.putIfAbsent(vertex, new LinkedHashMap<>()) != null) {
        throw new IllegalArgumentException("duplicate graph vertex: " + vertex);
      }
    }

    for (GraphLink<T> edge : data.edges()) {
      Map<T, Double> outgoing = adjacency.get(edge.from());
      Map<T, Double> incoming = adjacency.get(edge.to());
      if (outgoing == null || incoming == null) {
        throw new IllegalArgumentException("edge references an undeclared vertex");
      }
      if (outgoing.containsKey(edge.to())) {
        throw new IllegalArgumentException("duplicate graph edge");
      }
      if (data.direction() == GraphDirection.UNDIRECTED && incoming.containsKey(edge.from())) {
        throw new IllegalArgumentException("duplicate undirected graph edge");
      }
      outgoing.put(edge.to(), edge.weight());
    }

    // Graph.initialize builds and validates replacement state before updating the live graph.
    graph.initialize(adjacency);
  }

  /** Exports only domain values, direction and weights, not ephemeral graph IDs. */
  public GraphData<T> export(GraphStructure<T> graph) {
    Objects.requireNonNull(graph, "graph");
    List<T> vertices = new ArrayList<>();
    for (Vertex<T> vertex : graph.vertices()) {
      vertices.add(vertex.value());
    }
    List<GraphLink<T>> edges = new ArrayList<>();
    for (Edge<T> edge : graph.edges()) {
      edges.add(new GraphLink<>(edge.from().value(), edge.to().value(), graph.weight(edge)));
    }
    return new GraphData<>(graph.direction(), vertices, edges);
  }
}
