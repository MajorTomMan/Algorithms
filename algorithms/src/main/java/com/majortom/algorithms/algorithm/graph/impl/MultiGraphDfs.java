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
@Algorithm(id = "multi-graph-dfs", name = "多重图深度优先遍历", types = {Object.class},
    structure = MultiGraphStructure.class)
public final class MultiGraphDfs {
  @AlgorithmEntry
  public List<Object> traverse(MultiGraphStructure<Object> graph) {
    Objects.requireNonNull(graph, "graph");
    List<Object> order = new ArrayList<>();
    Set<Long> visited = new HashSet<>();
    for (Vertex<Object> origin : graph.vertices()) {
      if (!visited.contains(origin.id())) {
        ArrayDeque<Vertex<Object>> pending = new ArrayDeque<>();
        pending.addLast(origin);
        while (!pending.isEmpty()) {
          Vertex<Object> current = pending.removeLast();
          if (!visited.add(current.id())) continue;
          AlgorithmEvents.visited(GraphObservationDomains.VERTEX, current.id());
          order.add(current.value());
          List<Vertex<Object>> neighbors = new ArrayList<>();
          for (MultiEdge<Object> edge : graph.outgoingEdges(current)) {
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
