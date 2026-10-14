package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.annotation.GraphEdges;
import com.majortom.algorithms.core.annotation.GraphNodes;
import com.majortom.algorithms.core.annotation.ValueId;
import com.majortom.algorithms.core.annotation.ValueType;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.graph.GraphStructure;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import java.util.List;

/** Validates typed nodes and generic graph traversal registrations without JavaFX startup. */
public final class GraphGenericChecks {
  @ValueType(value = GraphStructure.class, immutable = true)
  record City(@ValueId String id, String name) {}
  record Road(City from, City to) {}
  record Roads(@GraphNodes List<City> cities, @GraphEdges(from="from", to="to") List<Road> roads) {}

  public static void main(String[] args) {
    City shenzhen = new City("sz", "深圳");
    City guangzhou = new City("gz", "广州");
    Roads network = new Roads(List.of(shenzhen, guangzhou),
        List.of(new Road(shenzhen, guangzhou)));
    Graph<City> typed = Graph.from(GraphDirection.DIRECTED, City.class, network);
    City first = typed.snapshot().vertices().getFirst().value();
    if (!first.equals(shenzhen) || typed.edgeCount() != 1)
      throw new AssertionError("Graph<City> lost its vertex type");
    var names = AlgorithmCatalog.graphAlgorithms(City.class);
    if (!names.contains("graph-bfs") || !names.contains("graph-dfs"))
      throw new AssertionError("Generic traversal algorithms must remain compatible");
    if (AlgorithmCatalog.graphDescriptor(City.class, "graph-bfs")
        .structureContract() != GraphStructure.class)
      throw new AssertionError("Graph algorithm chosen from wrong structure contract");
    boolean wrongTypeRejected = false;
    try {
      Graph.from(GraphDirection.DIRECTED, String.class, network);
    } catch (IllegalArgumentException expected) {
      wrongTypeRejected = true;
    }
    if (!wrongTypeRejected) throw new AssertionError("Incorrect generic type not validated");
    System.out.println("GraphGenericChecks passed");
  }
}
