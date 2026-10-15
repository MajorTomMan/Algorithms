package com.majortom.algorithms.algorithm.graph.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.domain.observation.GraphObservationDomains;
import com.majortom.algorithms.core.runtime.AlgorithmEvents;
import com.majortom.algorithms.structure.graph.Edge;
import com.majortom.algorithms.structure.graph.GraphStructure;
import com.majortom.algorithms.structure.graph.Vertex;


/** Traverses all components, using stable vertex IDs instead of business-value equality. */
@Algorithm(id = "graph-dfs", name = "深度优先遍历", types = {Object.class},
    structure = GraphStructure.class)
public final class GraphDfs {
  @AlgorithmEntry
  public List<Object> traverse(GraphStructure<Object> graph) {
    Objects.requireNonNull(graph, "graph");
    List<Object> order = new ArrayList<>();
    Set<Long> visited = new HashSet<>();
    // Neither stack nor neighbor buffer must be rebuilt for each component/node.
    ArrayDeque<Vertex<Object>> pending = new ArrayDeque<>();
    List<Vertex<Object>> neighbors = new ArrayList<>();
    for (Vertex<Object> origin : graph.vertices()) {
      if (!visited.contains(origin.id())) {
        pending.addLast(origin);
        while (!pending.isEmpty()) {
          Vertex<Object> current = pending.removeLast();
          if (!visited.add(current.id())) continue;
          AlgorithmEvents.visited(GraphObservationDomains.VERTEX, current.id());
          order.add(current.value());
          neighbors.clear();
          for (Edge<Object> edge : graph.outgoingEdges(current)) {
            Vertex<Object> neighbor = edge.from() == current ? edge.to() : edge.from();
            AlgorithmEvents.examined(GraphObservationDomains.VERTEX, current.id(), neighbor.id());
            if (!visited.contains(neighbor.id())) neighbors.add(neighbor);
          }
          for (int index = neighbors.size() - 1; index >= 0; index--)
            pending.addLast(neighbors.get(index));
        }
      }
    }
    return List.copyOf(order);
  }
}
