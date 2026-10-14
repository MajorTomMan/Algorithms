package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.annotation.GraphEdges;
import com.majortom.algorithms.core.annotation.GraphFrom;
import com.majortom.algorithms.core.annotation.GraphNodes;
import com.majortom.algorithms.core.annotation.GraphTo;
import com.majortom.algorithms.core.annotation.ValueId;
import com.majortom.algorithms.core.annotation.ValueType;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.structure.graph.MultiGraph;
import com.majortom.algorithms.structure.graph.MultiGraphStructure;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import java.util.List;

/** Validates typed nodes and generic graph traversal registrations without JavaFX startup. */
public final class MultiGraphGenericChecks {
  @ValueType(value = MultiGraphStructure.class, immutable = true)
  record City(@ValueId String id, String name) {}
  record Road(@GraphFrom City from, @GraphTo City to) {}
  record Roads(@GraphNodes List<City> cities, @GraphEdges List<Road> roads) {}

  public static void main(String[] args) {
    City shenzhen = new City("sz", "深圳");
    City guangzhou = new City("gz", "广州");
    Roads network = new Roads(List.of(shenzhen, guangzhou),
        List.of(new Road(shenzhen, guangzhou)));
    MultiGraph<City> typed = MultiGraph.from(GraphDirection.DIRECTED, City.class, network);
    City first = typed.snapshot().vertices().getFirst().value();
    if (!first.equals(shenzhen) || typed.edgeCount() != 1)
      throw new AssertionError("MultiGraph<City> lost its vertex type");
    var names = AlgorithmCatalog.multiGraphAlgorithms(City.class);
    if (!names.contains("multi-graph-bfs") || !names.contains("multi-graph-dfs"))
      throw new AssertionError("Generic traversal algorithms must remain compatible");
    if (AlgorithmCatalog.multiGraphDescriptor(City.class, "multi-graph-bfs")
        .structureContract() != MultiGraphStructure.class)
      throw new AssertionError("MultiGraph algorithm chosen from wrong structure contract");
    if (names.contains("graph-bfs") || names.contains("graph-dfs"))
      throw new AssertionError("Single graph algorithms leaked into MultiGraph");
    boolean wrongTypeRejected = false;
    try {
      MultiGraph.from(GraphDirection.DIRECTED, String.class, network);
    } catch (ClassCastException expected) {
      wrongTypeRejected = true;
    }
    if (!wrongTypeRejected) throw new AssertionError("Incorrect generic type not validated");
    System.out.println("MultiGraphGenericChecks passed");
  }
}
