package com.majortom.algorithms.algorithm.graph.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.majortom.algorithms.algorithm.graph.model.City;
import com.majortom.algorithms.algorithm.graph.model.Region;
import com.majortom.algorithms.algorithm.graph.model.Road;
import com.majortom.algorithms.algorithm.io.AlgorithmResourceLoader;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.structure.graph.Edge;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.graph.Vertex;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Algorithm-side example: read the region descriptor, cities and roads separately.
 * The generic loader only parses JSON; this caller resolves city IDs into Road objects
 * and constructs a directed multigraph.
 */
public final class RoadGraphConstructionChecks {
  /** File manifest, NOT the domain model Region. */
  public record RegionResources(String name, String cities, String roads) {}

  /** Raw roads refer to city IDs; Road itself holds City objects. */
  public record RoadInput(String id, String from, String to, Double distance,
                          Boolean bidirectional) {}

  public static void main(String[] args) {
    // 1. Each JSON file maps to a caller-selected type.
    RegionResources regionFile = AlgorithmResourceLoader.load(
        "graph/regions/demo/region.json", RegionResources.class);
    List<City> cities = AlgorithmResourceLoader.load(
        regionFile.cities(), new TypeReference<List<City>>() {});
    List<RoadInput> roads = AlgorithmResourceLoader.load(
        regionFile.roads(), new TypeReference<List<RoadInput>>() {});
    check(cities.size() == 4 && roads.size() == 7, "split JSON resources decoded");
    check("深圳".equals(cities.getFirst().name()), "cities map directly to City");

    // 2. Only the algorithm knows how ID references become Road and Graph objects.
    Graph<City> graph = buildRoadGraph(regionFile, cities, roads);
    check(graph.isDirected() && graph.vertexCount() == 4, "directed graph");
    check(graph.edgeCount() == 12, "bidirectional roads become two directed edges");

    Vertex<City> shenzhen = find(graph, "sz");
    Vertex<City> guangzhou = find(graph, "gz");
    check(graph.edges(shenzhen, guangzhou).size() == 2, "parallel roads");
    check(graph.edges(guangzhou, shenzhen).size() == 1, "reverse road");
    check(graph.edges(shenzhen, shenzhen).size() == 1, "self-loop");
    check(graph.edges(shenzhen, guangzhou).stream()
        .map(edge -> ((Road) edge.value()).id())
        .toList().containsAll(List.of("g4", "s3")), "road payload");
    for (Edge<City> edge : graph.edges())
      check(edge.weight() == ((Road) edge.value()).distance(), "road distance weight");

    // Valid JSON can still contain broken business references: validate during construction.
    List<RoadInput> invalid = AlgorithmResourceLoader.load(
        "graph/regions/invalid-roads.json", new TypeReference<List<RoadInput>>() {});
    check(invalid.size() == 1, "generic loader does not validate road topology");
    expectInvalid(() -> buildRoadGraph(regionFile, cities, invalid), "Unknown city ID");
    expectInvalid(() -> AlgorithmResourceLoader.load(
        "graph/regions/missing.json", RegionResources.class), "resource not found");

    System.out.println("RoadGraphConstructionChecks passed");
  }

  private static Graph<City> buildRoadGraph(
      RegionResources regionFile, List<City> cities, List<RoadInput> roadInputs) {
    if (regionFile == null || cities == null || roadInputs == null)
      throw new IllegalArgumentException("Road network requires a region, cities and roads");

    Map<String, City> citiesById = new LinkedHashMap<>();
    for (City city : cities) {
      if (citiesById.putIfAbsent(city.id(), city) != null)
        throw new IllegalArgumentException("Duplicate city ID: " + city.id());
    }

    List<Road> roads = new ArrayList<>();
    for (RoadInput data : roadInputs) {
      City from = citiesById.get(data.from());
      City to = citiesById.get(data.to());
      if (from == null || to == null)
        throw new IllegalArgumentException("Unknown city ID on road: " + data.id());
      if (data.distance() == null)
        throw new IllegalArgumentException("Missing road distance: " + data.id());
      roads.add(new Road(data.id(), from, to, data.distance(),
          Boolean.TRUE.equals(data.bidirectional())));
    }

    Region region = new Region(regionFile.name(), List.copyOf(citiesById.values()), roads);
    return Graph.from(GraphDirection.DIRECTED, City.class, region);
  }

  private static Vertex<City> find(Graph<City> graph, String cityId) {
    for (Vertex<City> vertex : graph.vertices())
      if (vertex.value().id().equals(cityId)) return vertex;
    throw new AssertionError("Unknown city: " + cityId);
  }

  private static void expectInvalid(Runnable action, String expected) {
    try {
      action.run();
    } catch (IllegalArgumentException exception) {
      if (exception.getMessage().contains(expected)) return;
      throw new AssertionError("Unexpected error: " + exception.getMessage(), exception);
    }
    throw new AssertionError("Expected invalid input: " + expected);
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
