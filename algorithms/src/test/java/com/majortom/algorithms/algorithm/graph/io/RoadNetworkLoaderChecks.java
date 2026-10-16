package com.majortom.algorithms.algorithm.graph.io;

import com.majortom.algorithms.algorithm.graph.model.City;
import com.majortom.algorithms.algorithm.graph.model.Road;
import com.majortom.algorithms.structure.graph.Edge;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.graph.Vertex;

/** Standalone end-to-end check: classpath JSON -> entities -> annotated directed multigraph. */
public final class RoadNetworkLoaderChecks {
  public static void main(String[] args) {
    String resource = "graph/regions/demo.json";
    var region = RoadNetworkLoader.loadRegion(resource);
    Graph<City> graph = RoadNetworkLoader.loadGraph(resource);
    check(region.cities().size() == 4 && region.roads().size() == 7, "resource parsed");
    check(graph.isDirected() && graph.vertexCount() == 4, "directed graph and cities");
    check(graph.edgeCount() == 12, "bidirectional edges expanded independently");

    Vertex<City> shenzhen = find(graph, "sz");
    Vertex<City> guangzhou = find(graph, "gz");
    check(graph.edges(shenzhen, guangzhou).size() == 2, "parallel roads retained");
    check(graph.edges(guangzhou, shenzhen).size() == 1, "reverse directed road");
    check(graph.edges(shenzhen, shenzhen).size() == 1, "one-way self-loop");
    check(graph.edges(shenzhen, guangzhou).stream()
        .map(edge -> ((Road) edge.value()).id())
        .toList().containsAll(java.util.List.of("g4", "s3")), "road payload retained");
    for (Edge<City> edge : graph.edges())
      check(edge.weight() == ((Road) edge.value()).distance(), "road distance weight");

    expectInvalid("graph/regions/invalid-endpoint.json", "Unknown city ID");
    expectInvalid("graph/regions/missing.json", "resource not found");
    System.out.println("RoadNetworkLoaderChecks passed");
  }

  private static Vertex<City> find(Graph<City> graph, String id) {
    for (Vertex<City> vertex : graph.vertices())
      if (vertex.value().id().equals(id)) return vertex;
    throw new AssertionError("No graph vertex for city: " + id);
  }

  private static void expectInvalid(String path, String message) {
    try {
      RoadNetworkLoader.loadGraph(path);
    } catch (IllegalArgumentException expected) {
      if (expected.getMessage().contains(message)) return;
      throw new AssertionError("Unexpected failure: " + expected.getMessage(), expected);
    }
    throw new AssertionError("Expected invalid resource to fail: " + path);
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
