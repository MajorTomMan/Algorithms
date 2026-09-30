package com.majortom.algorithms.visualization.render.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable decoration geometry produced beside structural layout. */
public record DecorationLayoutResult(
    Map<String, DecorationGeometry> elements,
    Map<String, EdgeDecorationGeometry> edges) {

  public DecorationLayoutResult {
    elements = Map.copyOf(Objects.requireNonNull(elements, "elements"));
    edges = Map.copyOf(Objects.requireNonNull(edges, "edges"));
  }

  public static DecorationLayoutResult empty() {
    return new DecorationLayoutResult(Map.of(), Map.of());
  }

  public static DecorationLayoutResult ofElements(List<DecorationGeometry> elements) {
    Objects.requireNonNull(elements, "elements");
    Map<String, DecorationGeometry> result = new LinkedHashMap<>();
    for (DecorationGeometry element : elements) {
      result.put(element.id(), element);
    }
    return new DecorationLayoutResult(result, Map.of());
  }
}
