package com.majortom.algorithms.algorithm.graph.model;

import com.majortom.algorithms.core.annotation.GraphEdges;
import com.majortom.algorithms.core.annotation.GraphNodes;
import java.util.List;
import java.util.Objects;

/** Annotated input model; Graph.from(...) builds the actual directed multigraph. */
public record Region(
    String name,
    @GraphNodes List<City> cities,
    @GraphEdges(from = "from", to = "to",
        weight = "distance", bidirectional = "bidirectional")
    List<Road> roads) {

  public Region {
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("region name cannot be blank");
    name = name.trim();
    cities = List.copyOf(Objects.requireNonNull(cities, "cities"));
    roads = List.copyOf(Objects.requireNonNull(roads, "roads"));
  }
}
