package com.majortom.algorithms.algorithm.graph.impl;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.structure.graph.Graph;
import java.util.List;
import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.structure.graph.GraphStructure;

/** Runtime-free traversal checks: parallel edges and disconnected components. */
public final class GraphTraversalChecks {
  public static void main(String[] args) {
    Graph<Object> graph = new Graph<>(GraphDirection.DIRECTED);
    var a = graph.addVertex("A");
    var b = graph.addVertex("B");
    var isolated = graph.addVertex("ISOLATED");
    graph.addEdge(a, b, "rail");
    graph.addEdge(a, b, "road");
    graph.addEdge(b, a, "return");
    List<Object> bfs = new GraphBfs().traverse(graph);
    List<Object> dfs = new GraphDfs().traverse(graph);
    if (bfs.size() != 3 || dfs.size() != 3
        || !bfs.contains(isolated.value()) || !dfs.contains(isolated.value())
        || bfs.stream().distinct().count() != 3 || dfs.stream().distinct().count() != 3)
      throw new AssertionError("Traversal must visit each unique vertex, including isolated vertices");

    var registered = ComponentDiscovery.discover().compatibleAlgorithms(
        GraphStructure.class, AlgorithmTypeSignature.of(Object.class));
    if (registered.stream().noneMatch(item -> item.id().equals("graph-bfs"))
        || registered.stream().noneMatch(item -> item.id().equals("graph-dfs")))
      throw new AssertionError("Graph traversal algorithms not discovered");
    System.out.println("GraphTraversalChecks passed");
  }
}
