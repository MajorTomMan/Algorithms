package com.majortom.algorithms.algorithm.graph.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.domain.observation.GraphObservationDomains;
import com.majortom.algorithms.core.runtime.Observations;
import com.majortom.algorithms.core.event.observation.ObservationEvent;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.structure.graph.Edge;
import com.majortom.algorithms.structure.graph.GraphStructure;
import com.majortom.algorithms.structure.graph.Vertex;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Algorithm(
    id = "graph-bfs", name = "广度优先遍历", type = Integer.class, structure = GraphStructure.class)
public final class GraphBfs {

  @AlgorithmEntry
  public List<Integer> traverse(GraphStructure<Integer> graph) {
    Objects.requireNonNull(graph, "graph");
    Integer start = firstVertexValue(graph);
    Vertex<Integer> startVertex = vertex(graph, start);
    if (startVertex == null) {
      throw new IllegalArgumentException("startNode must exist in graph");
    }
    ArrayDeque<Vertex<Integer>> queue = new ArrayDeque<>();
    Set<Vertex<Integer>> discovered = new HashSet<>();
    List<Integer> order = new ArrayList<>();
    queue.add(startVertex);
    discovered.add(startVertex);
    Observations.candidateAdded("graph-bfs", "vertex-" + startVertex.id(),
        new ObservationEvent.EntityRef(
            GraphObservationDomains.VERTEX, startVertex.id()));
    while (!queue.isEmpty()) {
      Vertex<Integer> node = queue.removeFirst();
      Observations.candidateSelected("graph-bfs", "vertex-" + node.id());
      Observations.visited(GraphObservationDomains.VERTEX, node.id());
      order.add(node.value());
      for (Vertex<Integer> neighbor : graph.neighbors(node)) {
        Observations.examined(GraphObservationDomains.VERTEX, node.id(), neighbor.id());
        if (discovered.add(neighbor)) {
          queue.addLast(neighbor);
          Observations.candidateAdded("graph-bfs", "vertex-" + neighbor.id(),
              new ObservationEvent.EntityRef(
                  GraphObservationDomains.VERTEX, neighbor.id()));
        }
      }
    }
    return List.copyOf(order);
  }

  public static GraphSnapshot<Integer> snapshot(GraphStructure<Integer> graph) {
    Objects.requireNonNull(graph, "graph");
    List<GraphSnapshot.Vertex<Integer>> vertices = new ArrayList<>();
    for (Vertex<Integer> vertex : graph.vertices()) {
      vertices.add(new GraphSnapshot.Vertex<>(vertex.id(), vertex.value()));
    }
    List<GraphSnapshot.Edge> edges = new ArrayList<>();
    for (Edge<Integer> edge : graph.edges()) {
      edges.add(new GraphSnapshot.Edge(edge.id(), edge.from().id(), edge.to().id()));
    }
    return new GraphSnapshot<>(graph.isDirected(), vertices, edges);
  }

  private static Integer firstVertexValue(GraphStructure<Integer> graph) {
    for (Vertex<Integer> vertex : graph.vertices()) {
      return vertex.value();
    }
    throw new IllegalArgumentException("graph must contain at least one vertex");
  }

  private static Vertex<Integer> vertex(GraphStructure<Integer> graph, int value) {
    for (Vertex<Integer> vertex : graph.vertices()) {
      if (vertex.value() == value) {
        return vertex;
      }
    }
    return null;
  }
}
