package com.majortom.algorithms.structure.graph;

import com.majortom.algorithms.core.annotation.*;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import java.util.List;

/** No test framework dependency: run with java -ea after compiling structures. */
public final class GraphChecks {
  @ValueType(GraphStructure.class)
  record City(@ValueId String code, @ValueField(ValueDisplay.SUMMARY) String name) {}
  record Railway(City from, City to, double distance, String name) {}
  record Road(City from, City to, double distance, boolean twoWay) {}
  record Network(@GraphNodes List<City> cities,
      @GraphEdges(from="from", to="to", weight="distance") List<Railway> railways,
      @GraphEdges(from="from", to="to", weight="distance", bidirectional="twoWay") List<Road> roads) {}

  public static void main(String[] args) {
    City shenzhen = new City("sz", "深圳");
    City guangzhou = new City("gz", "广州");
    City dongguan = new City("dg", "东莞");
    // Endpoint may be a distinct instance with the same explicitly declared business identity.
    Network input = new Network(List.of(shenzhen, guangzhou, dongguan),
        List.of(new Railway(new City("sz", "深圳"), guangzhou, 102, "广深铁路")),
        List.of(new Road(shenzhen, guangzhou, 125, true)));
    Graph<City> graph = Graph.from(GraphDirection.DIRECTED, City.class, input);
    check(graph.vertexCount() == 3 && graph.edgeCount() == 3, "batch topology");
    var sourceAccessor = com.majortom.algorithms.core.metadata.AnnotatedMembers
        .named(Railway.class, "from");
    check(sourceAccessor == com.majortom.algorithms.core.metadata.AnnotatedMembers
        .named(Railway.class, "from"), "named graph edge property lookup is cached");
    var nodes = graph.snapshot().vertices();
    Vertex<City> sz = graph.vertex(nodes.get(0).id());
    Vertex<City> gz = graph.vertex(nodes.get(1).id());
    Vertex<City> dg = graph.vertex(nodes.get(2).id());
    check(graph.edges(sz, gz).size() == 2, "parallel road and railway");
    check(graph.edges(gz, sz).size() == 1, "automatic reverse road");
    check(graph.outgoingEdges(dg).isEmpty(), "isolated node");

    var copied = Graph.fromSnapshot(graph.snapshot());
    check(copied.edgeCount() == graph.edgeCount(), "snapshot restoration");
    check(copied.edges(copied.vertex(sz.id()), copied.vertex(gz.id())).size() == 2,
        "snapshot parallel edges");
    long removed = graph.edges(sz, gz).getFirst().id();
    check(graph.removeEdge(removed) && graph.edges(sz, gz).size() == 1, "delete by edge ID");
    double previous = graph.setWeight(graph.edges(sz, gz).getFirst().id(), 12);
    check(Double.isFinite(previous), "weight update");

    Graph<City> undirected = Graph.from(GraphDirection.UNDIRECTED, City.class, input);
    check(undirected.edgeCount() == 2, "no redundant reverse in undirected graph");
    var un = undirected.snapshot().vertices();
    check(undirected.edges(undirected.vertex(un.get(0).id()),
                           undirected.vertex(un.get(1).id())).size() == 2,
        "undirected parallel edges");

    Graph<City> manual = new Graph<>(GraphDirection.DIRECTED);
    var first = manual.addVertex(shenzhen);
    var equal = manual.addVertex(new City("sz", "深圳"));
    check(first.id() != equal.id(), "different vertices may hold equal business values");
    check(manual.addBiEdge(first, equal, new Object()).reverse() != null, "bidirectional API");
    check(manual.edgeCount() == 2, "bidirectional edge count");
    var rail = manual.addEdge(first, equal, "rail", 10);
    var road = manual.addEdge(first, equal, "road", 5);
    check(rail.id() != road.id() && manual.edges(first, equal).size() == 3,
        "parallel edges retain separate IDs and payloads");
    check(manual.removeEdge(rail.id()) && manual.edges(first, equal).size() == 2,
        "removing one parallel edge leaves the others");
    var restored = Graph.fromSnapshot(manual.snapshot());
    check(restored.edge(road.id()).value().equals("road"),
        "graph snapshot preserves edge payloads");
    check(manual.containsEdge(first, equal), "parallel edges still report an edge");
    check(manual.containsEdge(equal, first), "explicit reverse direction is indexed");
    var oldEdges = manual.outgoingEdges(first);
    check(oldEdges.size() == 2, "all parallel outgoing edges are included");
    check(manual.neighbors(first).iterator().next() == equal,
        "parallel edges yield a single neighbor without changing order");
    check(manual.vertex(first.id()) == first && manual.vertex(equal.id()) == equal,
        "equal-valued nodes must remain distinguishable by ID");
    check(manual.edge(road.id()) == road && manual.edges(first, equal).size() == 2,
        "parallel edges must remain distinguishable by ID");

    Graph<String> loops = new Graph<>(GraphDirection.UNDIRECTED);
    Vertex<String> loopNode = loops.addVertex("A");
    Edge<String> loop = loops.addEdge(loopNode, loopNode, "self", 3);
    check(loops.edges(loopNode, loopNode).size() == 1, "undirected self-loop is stored once");
    check(loops.outgoingEdges(loopNode).size() == 1
            && loops.incomingEdges(loopNode).size() == 1, "self-loop indexes");
    check(loops.containsEdge(loopNode, loopNode)
            && loops.neighbors(loopNode).iterator().next() == loopNode,
        "undirected self-loop must be visible only once in adjacency");
    check(loops.removeVertex(loopNode.id()) && loops.edgeCount() == 0,
        "vertex removal deletes self-loops");
    check(loops.edge(loop.id()) == null, "removed self-loop cannot be fetched");
    // Neighbor and existence queries must follow direction and update after removals.
    Graph<String> directed = new Graph<>(GraphDirection.DIRECTED);
    Vertex<String> source = directed.addVertex("S");
    Vertex<String> target = directed.addVertex("T");
    Vertex<String> isolated = directed.addVertex("X");
    Edge<String> link = directed.addEdge(source, target, 1.0);
    check(directed.containsEdge(source, target) && !directed.containsEdge(target, source)
            && !directed.containsEdge(isolated, source), "directed existence lookup");
    check(directed.removeEdge(link.id()) && !directed.containsEdge(source, target)
            && directed.neighbors(source).iterator().hasNext() == false,
        "removed edges disappear from adjacency lookup");


    boolean rejected = false;
    try {
      Graph.from(GraphDirection.DIRECTED, City.class,
          new Network(List.of(shenzhen, new City("sz", "重复")), List.of(), List.of()));
    } catch (IllegalArgumentException expected) { rejected = true; }
    check(rejected, "duplicate @ValueId rejected");

    rejected = false;
    try {
      Graph.from(GraphDirection.DIRECTED, City.class,
          new Network(List.of(shenzhen), List.of(new Railway(shenzhen, guangzhou, 1, "missing")), List.of()));
    } catch (IllegalArgumentException expected) { rejected = true; }
    check(rejected, "undeclared endpoints rejected");

    GraphSnapshot<City> initial = graph.snapshot();
    rejected = false;
    try {
      graph.initialize(new GraphSnapshot<>(GraphDirection.DIRECTED, initial.vertices(),
          List.of(new GraphSnapshot.Edge(1000000L, sz.id(), 99999999L, null, 1))));
    } catch (IllegalArgumentException expected) { rejected = true; }
    check(rejected && graph.edgeCount() == initial.edges().size(), "failed restore is atomic");
    checkRecursiveGraph();
    checkMappedParallelLoops();
    System.out.println("GraphChecks passed");
  }

  record Region(String code, @GraphNodes List<Region> children) {}
  record Connection(Region source, Region destination, double km) {}
  record Atlas(@GraphNodes List<Region> provinces,
      @GraphEdges(from="source", to="destination", weight="km") List<Connection> roads) {}

  static final class CyclicNode {
    @GraphNodes List<CyclicNode> children = List.of();
    @GraphEdges(from="from", to="to") List<CyclicEdge> roads = List.of();
  }
  record CyclicEdge(CyclicNode from, CyclicNode to) {}

  private static void checkRecursiveGraph() {
    Region district = new Region("440305", List.of());
    Region city = new Region("440300", List.of(district));
    Region province = new Region("440000", List.of(city));
    Region second = new Region("440100", List.of());
    Graph<Region> graph = Graph.from(GraphDirection.DIRECTED, Region.class,
        new Atlas(List.of(province, second), List.of(new Connection(city, second, 125))));
    check(graph.vertexCount() == 4 && graph.edgeCount() == 1,
        "recursive @GraphNodes visits descendants without creating containment edges");
    Graph<Region> rooted = Graph.from(GraphDirection.DIRECTED, Region.class, province);
    check(rooted.vertexCount() == 3 && rooted.edgeCount() == 0,
        "typed recursive root is included");

    // No business @ValueType or @ValueId annotations are required when endpoints share references.
    boolean rejected = false;
    try {
      Graph.from(GraphDirection.DIRECTED, Region.class,
          new Atlas(List.of(province), List.of(new Connection(second, district, 1))));
    } catch (IllegalArgumentException expected) { rejected = true; }
    check(rejected, "undeclared recursive endpoint rejected");

    record Wrapper(@GraphNodes List<Region> nested) {}
    record Nested(@GraphNodes List<Wrapper> wrappers) {}
    var nested = Graph.from(GraphDirection.DIRECTED, Region.class,
        new Nested(List.of(new Wrapper(List.of(province)))));
    check(nested.vertexCount() == 3, "nested non-node wrappers discover children");

    CyclicNode a = new CyclicNode();
    CyclicNode b = new CyclicNode();
    a.children = List.of(b);
    b.children = List.of(a);
    b.roads = List.of(new CyclicEdge(b, a));
    var cyclic = Graph.from(GraphDirection.DIRECTED, CyclicNode.class, a);
    check(cyclic.vertexCount() == 2 && cyclic.edgeCount() == 1,
        "cyclic references stop while nested node edges are discovered");
  }

  record LoopRoad(City from, City to, double distance) {}
  record LoopNetwork(@GraphNodes List<City> cities,
      @GraphEdges(from = "from", to = "to", weight = "distance")
      List<LoopRoad> roads) {}

  private static void checkMappedParallelLoops() {
    City city = new City("loop", "Loop City");
    Graph<City> graph = Graph.from(GraphDirection.DIRECTED, City.class,
        new LoopNetwork(List.of(city),
            List.of(new LoopRoad(city, city, 3), new LoopRoad(city, city, 7))));
    Vertex<City> vertex = graph.vertex(graph.snapshot().vertices().getFirst().id());
    check(graph.vertexCount() == 1 && graph.edgeCount() == 2, "mapped parallel self-loops");
    check(graph.edges(vertex, vertex).size() == 2, "both mapped loops remain distinct");
    check(graph.outgoingEdges(vertex).size() == 2
        && graph.incomingEdges(vertex).size() == 2, "mapped loop indices");
    check(graph.edges(vertex, vertex).getFirst().weight() == 3
        && graph.edges(vertex, vertex).getLast().weight() == 7, "mapped edge weights");
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}