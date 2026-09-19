package com.majortom.algorithms.algorithm.graph.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.logging.Log;
import com.majortom.algorithms.core.domain.observation.GraphObservationDomains;
import com.majortom.algorithms.core.runtime.AlgorithmEvents;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.structure.graph.Edge;
import com.majortom.algorithms.structure.graph.GraphStructure;
import com.majortom.algorithms.structure.graph.Vertex;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Algorithm(id = "graph-dfs", name = "深度优先遍历", type = Integer.class, structure = GraphStructure.class)
public final class GraphDfs {
  private static Map<Integer, Boolean> isVisited = new HashMap<>();

  @AlgorithmEntry
  public List<Integer> traverse(GraphStructure<Integer> graph) {
    isVisited.clear();
    Integer firstVertex = firstVertexValue(graph);
    Vertex<Integer> startVertex = graph.vertex(firstVertex);
    dfs(graph, startVertex);
    List<Integer> results = isVisited.entrySet().stream().map((entry) -> {
      return entry.getKey();
    }).collect(Collectors.toList());
    return results;
  }

  private static void dfs(GraphStructure<Integer> graph, Vertex<Integer> vertex) {
    if (!isVisited.containsKey(vertex.value())) {
      AlgorithmEvents.visited(GraphObservationDomains.VERTEX, vertex.id());
      isVisited.put(vertex.value(), true);
      for (Vertex<Integer> neighbor : graph.neighbors(vertex)) {
        AlgorithmEvents.examined(GraphObservationDomains.VERTEX, vertex.id(), neighbor.id());
        dfs(graph, neighbor);
      }
    } else {
      return;
    }
  }

  private static Integer firstVertexValue(GraphStructure<Integer> graph) {
    for (Vertex<Integer> vertex : graph.vertices()) {
      return vertex.value();
    }
    throw new IllegalArgumentException("graph must contain at least one vertex");
  }
}
