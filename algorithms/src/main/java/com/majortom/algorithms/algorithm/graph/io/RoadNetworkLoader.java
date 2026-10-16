package com.majortom.algorithms.algorithm.graph.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.majortom.algorithms.algorithm.graph.model.City;
import com.majortom.algorithms.algorithm.graph.model.Region;
import com.majortom.algorithms.algorithm.graph.model.Road;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.structure.graph.Graph;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Loads declarative road-network JSON from the Maven classpath.
 *
 * <p>Road endpoints in JSON are city business IDs, not duplicated city objects.
 * The loader resolves them to canonical City instances before invoking Graph.from(...).
 */
public final class RoadNetworkLoader {
  private static final ObjectMapper JSON = new ObjectMapper();

  private RoadNetworkLoader() {}

  /** Example: graph/regions/demo.json (relative to src/main/resources). */
  public static Region loadRegion(String resourcePath) {
    Objects.requireNonNull(resourcePath, "resourcePath");
    String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
    if (path.isBlank())
      throw new IllegalArgumentException("resource path cannot be blank");
    try (InputStream input = RoadNetworkLoader.class.getClassLoader()
        .getResourceAsStream(path)) {
      if (input == null)
        throw new IllegalArgumentException("Road-network resource not found: " + path);
      RegionFile source = JSON.readValue(input, RegionFile.class);
      return toRegion(source);
    } catch (IOException exception) {
      throw new IllegalArgumentException(
          "Cannot read road-network resource: " + path, exception);
    }
  }

  public static Graph<City> loadGraph(String resourcePath) {
    return Graph.from(GraphDirection.DIRECTED, City.class, loadRegion(resourcePath));
  }

  private static Region toRegion(RegionFile source) {
    if (source == null)
      throw new IllegalArgumentException("Road-network JSON must contain an object");
    if (source.cities() == null || source.cities().isEmpty())
      throw new IllegalArgumentException("Region must declare at least one city");
    if (source.roads() == null)
      throw new IllegalArgumentException("Region must declare a roads array");

    Map<String, City> byId = new LinkedHashMap<>();
    for (CityFile raw : source.cities()) {
      if (raw == null)
        throw new IllegalArgumentException("Null city entry");
      City city = new City(raw.id(), raw.name());
      if (byId.putIfAbsent(city.id(), city) != null)
        throw new IllegalArgumentException("Duplicate city ID: " + city.id());
    }

    List<Road> roads = new ArrayList<>();
    Set<String> roadIds = new LinkedHashSet<>();
    for (RoadFile raw : source.roads()) {
      if (raw == null)
        throw new IllegalArgumentException("Null road entry");
      String id = requireText(raw.id(), "road id");
      if (!roadIds.add(id))
        throw new IllegalArgumentException("Duplicate road ID: " + id);
      City from = resolveCity(byId, raw.from(), id, "from");
      City to = resolveCity(byId, raw.to(), id, "to");
      if (raw.distance() == null)
        throw new IllegalArgumentException("Missing distance on road: " + id);
      roads.add(new Road(id, from, to, raw.distance(),
          Boolean.TRUE.equals(raw.bidirectional())));
    }
    return new Region(source.name(), List.copyOf(byId.values()), roads);
  }

  private static City resolveCity(Map<String, City> cities, String id, String roadId,
                                  String endpoint) {
    City city = cities.get(requireText(id, endpoint));
    if (city == null)
      throw new IllegalArgumentException(
          "Unknown city ID on road " + roadId + " (" + endpoint + "): " + id);
    return city;
  }

  private static String requireText(String text, String field) {
    if (text == null || text.isBlank())
      throw new IllegalArgumentException(field + " cannot be blank");
    return text.trim();
  }

  /** File-facing DTOs: JSON uses compact IDs rather than nested City objects. */
  public record RegionFile(String name, List<CityFile> cities, List<RoadFile> roads) {}
  public record CityFile(String id, String name) {}
  public record RoadFile(String id, String from, String to, Double distance,
                         Boolean bidirectional) {}
}
