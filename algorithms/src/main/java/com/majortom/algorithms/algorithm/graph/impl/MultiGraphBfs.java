package com.majortom.algorithms.algorithm.graph.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.domain.observation.GraphObservationDomains;
import com.majortom.algorithms.core.runtime.AlgorithmEvents;
import com.majortom.algorithms.structure.graph.MultiEdge;
import com.majortom.algorithms.structure.graph.MultiGraphStructure;
import com.majortom.algorithms.structure.graph.Vertex;
import java.util.*;

/** Traverses all components, using stable vertex IDs instead of business-value equality. */
@Algorithm(id = "multi-graph-bfs", name = "多重图广度优先遍历", types = {Object.class},
    structure = MultiGraphStructure.class)
public final class MultiGraphBfs {
  @AlgorithmEntry
  public List<Object> traverse(MultiGraphStructure<Object> graph) {
    Objects.requireNonNull(graph, "graph");
    List<Object> order = new ArrayList<>();
    Set<Long> visited = new HashSet<>();
    for (Vertex<Object> origin : graph.vertices()) {
      if (!visited.contains(origin.id())) {
        ArrayDeque<Vertex<Object>> pending = new ArrayDeque<>();
        pending.addLast(origin);
        visited.add(origin.id());
        while (!pending.isEmpty()) {
          Vertex<Object> current = pending.removeFirst();
          AlgorithmEvents.visited(GraphObservationDomains.VERTEX, current.id());
          order.add(current.value());
          for (MultiEdge<Object> edge : graph.outgoingEdges(current)) {
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
