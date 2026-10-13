package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.annotation.*;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.structure.graph.MultiGraph;
import com.majortom.algorithms.structure.graph.MultiGraphStructure;
import java.util.List;

/** Headless test: JSON -> annotated Java batch object -> multigraph. */
public final class MultiGraphJsonLoaderChecks {
  @ValueType(MultiGraphStructure.class)
  public record City(@ValueId String code,
      @ValueField(ValueDisplay.SUMMARY) String name) {}
  public record Railway(@GraphFrom City from, @GraphTo City to,
      @GraphWeight double kilometers, String line) {}
  public record Road(@GraphFrom City from, @GraphTo City to,
      @GraphBidirectional boolean twoWay, String name) {}
  public record Network(@GraphNodes List<City> cities,
      @GraphEdges List<Railway> railways, @GraphEdges List<Road> roads) {}
  public record NoNodes(List<City> cities) {}

  public static void main(String[] args) {
    String data = """
      {
        "cities": [
          {"code":"sz","name":"深圳"},
          {"code":"gz","name":"广州"},
          {"code":"dg","name":"东莞"}
        ],
        "railways": [
          {
            "from":{"code":"sz","name":"深圳"},
            "to":{"code":"gz","name":"广州"},
            "kilometers":102,
            "line":"广深铁路"
          }
        ],
        "roads": [
          {
            "from":{"code":"sz","name":"深圳"},
            "to":{"code":"gz","name":"广州"},
            "twoWay":true,
            "name":"广深高速"
          }
        ]
      }
      """;
    Object batch = MultiGraphJsonLoader.read(Network.class.getName(), data,
        MultiGraphJsonLoaderChecks.class.getClassLoader());
    if (!(batch instanceof Network network) || network.cities().size() != 3)
      throw new AssertionError("Record JSON binding");

    MultiGraph<City> graph = MultiGraph.from(GraphDirection.DIRECTED, City.class, batch);
    if (graph.vertexCount() != 3 || graph.edgeCount() != 3)
      throw new AssertionError("JSON graph topology");
    var vertexList = graph.snapshot().vertices();
    var sz = graph.vertex(vertexList.getFirst().id());
    var gz = graph.vertex(vertexList.get(1).id());
    var dg = graph.vertex(vertexList.get(2).id());
    if (graph.edges(sz, gz).size() != 2 || graph.edges(gz, sz).size() != 1
        || !graph.outgoingEdges(dg).isEmpty())
      throw new AssertionError("Parallel, reversed or isolated JSON node incorrect");

    expectFailure(() -> MultiGraphJsonLoader.read(NoNodes.class.getName(), "{}"));
    expectFailure(() -> MultiGraphJsonLoader.read(Network.class.getName(), "{not-json}"));
    expectFailure(() -> MultiGraphJsonLoader.read("not.existing.Network", data));
    expectFailure(() -> MultiGraphJsonLoader.read(Network.class.getName(), "   "));
    System.out.println("MultiGraphJsonLoaderChecks passed");
  }

  private static void expectFailure(Runnable operation) {
    try {
      operation.run();
      throw new AssertionError("Expected malformed JSON/type to fail");
    } catch (IllegalArgumentException expected) {
      // Fail closed: the caller's previously displayed graph remains unchanged.
    }
  }
}
