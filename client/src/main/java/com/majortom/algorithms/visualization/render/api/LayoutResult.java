package com.majortom.algorithms.visualization.render.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record LayoutResult(LayoutRequestId requestId, long modelRevision,
    Map<String, ElementGeometry> elements, List<EdgeGeometry> edges, BoundsSnapshot bounds) {
  public LayoutResult {
    Objects.requireNonNull(requestId, "requestId");
    elements = Map.copyOf(Objects.requireNonNull(elements, "elements"));
    edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
    Objects.requireNonNull(bounds, "bounds");
  }
  public LayoutResult withModelRevision(long revision) {
    return new LayoutResult(requestId, revision, elements, edges, bounds);
  }
}
