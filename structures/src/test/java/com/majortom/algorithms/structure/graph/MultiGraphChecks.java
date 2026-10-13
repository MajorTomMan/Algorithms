package com.majortom.algorithms.structure.graph;

import com.majortom.algorithms.core.annotation.*;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.snapshot.MultiGraphSnapshot;
import java.util.List;

/** No test framework dependency: run with java -ea after compiling structures. */
public final class MultiGraphChecks {
  @ValueType(MultiGraphStructure.class)
  record City(@ValueId String code, @ValueField(ValueDisplay.SUMMARY) String name) {}
  record Railway(@GraphFrom City from, @GraphTo City to,
                 @GraphWeight double distance, String name) {}
  record Road(@GraphFrom City from, @GraphTo City to,
              @GraphWeight double distance, @GraphBidirectional boolean twoWay) {}
  record Network(@GraphNodes List<City> cities, @GraphEdges List<Railway> railways,
                 @GraphEdges List<Road> roads) {}

  public static void main(String[] args) {
    City shenzhen = new City("sz", "深圳");
    City guangzhou = new City("gz", "广州");
    City dongguan = new City("dg", "东莞");
    // Endpoint may be a distinct instance with the same explicitly declared business identity.
    Network input = new Network(List.of(shenzhen, guangzhou, dongguan),
        List.of(new Railway(new City("sz", "深圳"), guangzhou, 102, "广深铁路")),
        List.of(new Road(shenzhen, guangzhou, 125, true)));
    MultiGraph<City> graph = MultiGraph.from(GraphDirection.DIRECTED, City.class, input);
    check(graph.vertexCount() == 3 && graph.edgeCount() == 3, "batch topology");
    var nodes = graph.snapshot().vertices();
    Vertex<City> sz = graph.vertex(nodes.get(0).id());
    Vertex<City> gz = graph.vertex(nodes.get(1).id());
    Vertex<City> dg = graph.vertex(nodes.get(2).id());
    check(graph.edges(sz, gz).size() == 2, "parallel road and railway");
    check(graph.edges(gz, sz).size() == 1, "automatic reverse road");
    check(graph.outgoingEdges(dg).isEmpty(), "isolated node");

    var copied = MultiGraph.fromSnapshot(graph.snapshot());
    check(copied.edgeCount() == graph.edgeCount(), "snapshot restoration");
    check(copied.edges(copied.vertex(sz.id()), copied.vertex(gz.id())).size() == 2,
        "snapshot parallel edges");
    long removed = graph.edges(sz, gz).getFirst().id();
    check(graph.removeEdge(removed) && graph.edges(sz, gz).size() == 1, "delete by edge ID");
    double previous = graph.setWeight(graph.edges(sz, gz).getFirst().id(), 12);
    check(Double.isFinite(previous), "weight update");

    MultiGraph<City> undirected = MultiGraph.from(GraphDirection.UNDIRECTED, City.class, input);
    check(undirected.edgeCount() == 2, "no redundant reverse in undirected graph");
    var un = undirected.snapshot().vertices();
    check(undirected.edges(undirected.vertex(un.get(0).id()),
                           undirected.vertex(un.get(1).id())).size() == 2,
        "undirected parallel edges");

    MultiGraph<City> manual = new MultiGraph<>(GraphDirection.DIRECTED);
    var first = manual.addVertex(shenzhen);
    var equal = manual.addVertex(new City("sz", "深圳"));
    check(first.id() != equal.id(), "different vertices may hold equal business values");
    check(manual.addBiEdge(first, equal, new Object()).reverse() != null, "bidirectional API");
    check(manual.edgeCount() == 2, "bidirectional edge count");

    boolean rejected = false;
    try {
      MultiGraph.from(GraphDirection.DIRECTED, City.class,
          new Network(List.of(shenzhen, new City("sz", "重复")), List.of(), List.of()));
    } catch (IllegalArgumentException expected) { rejected = true; }
    check(rejected, "duplicate @ValueId rejected");

    rejected = false;
    try {
      MultiGraph.from(GraphDirection.DIRECTED, City.class,
          new Network(List.of(shenzhen), List.of(new Railway(shenzhen, guangzhou, 1, "missing")), List.of()));
    } catch (IllegalArgumentException expected) { rejected = true; }
    check(rejected, "undeclared endpoints rejected");

    MultiGraphSnapshot<City> initial = graph.snapshot();
    rejected = false;
    try {
      graph.initialize(new MultiGraphSnapshot<>(GraphDirection.DIRECTED, initial.vertices(),
          List.of(new MultiGraphSnapshot.Edge(1000000L, sz.id(), 99999999L, null, 1))));
    } catch (IllegalArgumentException expected) { rejected = true; }
    check(rejected && graph.edgeCount() == initial.edges().size(), "failed restore is atomic");
    System.out.println("MultiGraphChecks passed");
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
