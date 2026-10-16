package com.majortom.algorithms.algorithm.graph.impl;

import com.majortom.algorithms.algorithm.graph.model.City;
import com.majortom.algorithms.algorithm.graph.model.Region;
import com.majortom.algorithms.algorithm.graph.model.Road;
import com.majortom.algorithms.algorithm.io.AlgorithmResourceLoader;
import com.majortom.algorithms.core.io.ResourceLoader;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.structure.graph.Edge;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.graph.Vertex;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Algorithm-side example: the generic resource loader only deserializes JSON;
 * this caller interprets city IDs, creates business entities and builds the graph.
 * No road-specific loader is involved.
 */
public final class RoadGraphConstructionChecks {
  public record RegionInput(String name, List<CityInput> cities, List<RoadInput> roads) {}
  public record CityInput(String id, String name) {}
  public record RoadInput(String id, String from, String to, Double distance,
                          Boolean bidirectional) {}

  public static void main(String[] args) {
    ResourceLoader resources = new AlgorithmResourceLoader();

    // This is ordinary JSON -> caller-specified type, without graph semantics.
    RegionInput input = resources.load("graph/regions/demo.json", RegionInput.class);
    check(input.cities().size() == 4 && input.roads().size() == 7, "JSON decoded");

    // Road-network interpretation belongs to the algorithm, not the loader.
    Graph<City> graph = buildRoadGraph(input);
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

    // The generic loader accepts valid JSON even when roads refer to missing cities.
    RegionInput invalid = resources.load(
        "graph/regions/invalid-endpoint.json", RegionInput.class);
    check(invalid.roads().size() == 1, "loading does not validate road topology");
    expectInvalid(() -> buildRoadGraph(invalid), "Unknown city ID");
    expectInvalid(
        () -> resources.load("graph/regions/missing.json", RegionInput.class),
        "resource not found");

    System.out.println("RoadGraphConstructionChecks passed");
  }

  /**
   * Domain-specific graph construction; in a real algorithm this belongs to
   * its input preparation, not to ResourceLoader.
   */
  private static Graph<City> buildRoadGraph(RegionInput input) {
    if (input == null || input.cities() == null || input.roads() == null)
      throw new IllegalArgumentException("Road network requires cities and roads");

    Map<String, City> citiesById = new LinkedHashMap<>();
    for (CityInput data : input.cities()) {
      City city = new City(data.id(), data.name());
      if (citiesById.putIfAbsent(city.id(), city) != null)
        throw new IllegalArgumentException("Duplicate city ID: " + city.id());
    }

    List<Road> roads = new ArrayList<>();
    for (RoadInput data : input.roads()) {
      City from = citiesById.get(data.from());
      City to = citiesById.get(data.to());
      if (from == null || to == null)
        throw new IllegalArgumentException("Unknown city ID on road: " + data.id());
      if (data.distance() == null)
        throw new IllegalArgumentException("Missing road distance: " + data.id());
      roads.add(new Road(data.id(), from, to, data.distance(),
          Boolean.TRUE.equals(data.bidirectional())));
    }

    Region region = new Region(input.name(), List.copyOf(citiesById.values()), roads);
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
