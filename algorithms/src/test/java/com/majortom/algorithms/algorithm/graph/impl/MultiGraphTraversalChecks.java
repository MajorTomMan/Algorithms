package com.majortom.algorithms.algorithm.graph.impl;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.structure.graph.MultiGraph;
import java.util.List;
import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.structure.graph.MultiGraphStructure;

/** Runtime-free traversal checks: parallel edges and disconnected components. */
public final class MultiGraphTraversalChecks {
  public static void main(String[] args) {
    MultiGraph<Object> graph = new MultiGraph<>(GraphDirection.DIRECTED);
    var a = graph.addVertex("A");
    var b = graph.addVertex("B");
    var isolated = graph.addVertex("ISOLATED");
    graph.addEdge(a, b, "rail");
    graph.addEdge(a, b, "road");
    graph.addEdge(b, a, "return");
    List<Object> bfs = new MultiGraphBfs().traverse(graph);
    List<Object> dfs = new MultiGraphDfs().traverse(graph);
    if (bfs.size() != 3 || dfs.size() != 3
        || !bfs.contains(isolated.value()) || !dfs.contains(isolated.value())
        || bfs.stream().distinct().count() != 3 || dfs.stream().distinct().count() != 3)
      throw new AssertionError("Traversal must visit each unique vertex, including isolated vertices");

    var registered = ComponentDiscovery.discover().compatibleAlgorithms(
        MultiGraphStructure.class, AlgorithmTypeSignature.of(Object.class));
    if (registered.stream().noneMatch(item -> item.id().equals("multi-graph-bfs"))
        || registered.stream().noneMatch(item -> item.id().equals("multi-graph-dfs")))
      throw new AssertionError("MultiGraph traversal algorithms not discovered");
    System.out.println("MultiGraphTraversalChecks passed");
  }
}
