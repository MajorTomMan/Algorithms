package com.majortom.algorithms.algorithm.graph.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.domain.observation.GraphObservationDomains;
import com.majortom.algorithms.core.runtime.AlgorithmEvents;
import com.majortom.algorithms.structure.graph.Edge;
import com.majortom.algorithms.structure.graph.GraphStructure;
import com.majortom.algorithms.structure.graph.Vertex;
import java.util.*;

/** Traverses all components, using stable vertex IDs instead of business-value equality. */
@Algorithm(id = "graph-bfs", name = "广度优先遍历", types = {Object.class},
    structure = GraphStructure.class)
public final class GraphBfs {
  @AlgorithmEntry
  public List<Object> traverse(GraphStructure<Object> graph) {
    Objects.requireNonNull(graph, "graph");
    List<Object> order = new ArrayList<>();
    Set<Long> visited = new HashSet<>();
    // Reuse one queue across disconnected components, including isolated vertices.
    ArrayDeque<Vertex<Object>> pending = new ArrayDeque<>();
    for (Vertex<Object> origin : graph.vertices()) {
      if (visited.add(origin.id())) {
        pending.addLast(origin);
        while (!pending.isEmpty()) {
          Vertex<Object> current = pending.removeFirst();
          AlgorithmEvents.visited(GraphObservationDomains.VERTEX, current.id());
          order.add(current.value());
          for (Edge<Object> edge : graph.outgoingEdges(current)) {
            Vertex<Object> neighbor = edge.from() == current ? edge.to() : edge.from();
            AlgorithmEvents.examined(GraphObservationDomains.VERTEX, current.id(), neighbor.id());
            if (visited.add(neighbor.id())) pending.addLast(neighbor);
          }
        }
      }
    }
    return List.copyOf(order);
  }
}
