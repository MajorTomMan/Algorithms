package com.majortom.algorithms.visualization.render.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record LayoutRequest(LayoutRequestId requestId, RenderSessionId sessionId,
    long modelRevision, long geometryRevision, String engineId, List<LayoutElement> elements,
    List<LayoutLink> links, Map<String, String> metadata) {
  public LayoutRequest {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(sessionId, "sessionId");
    Objects.requireNonNull(engineId, "engineId");
    elements = List.copyOf(Objects.requireNonNull(elements, "elements"));
    links = List.copyOf(Objects.requireNonNull(links, "links"));
    metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
  }

  /** Compatibility constructor for layouts without explicit topology links. */
  public LayoutRequest(LayoutRequestId requestId, RenderSessionId sessionId, long modelRevision,
      long geometryRevision, String engineId, List<LayoutElement> elements,
      Map<String, String> metadata) {
    this(requestId, sessionId, modelRevision, geometryRevision, engineId, elements, List.of(),
        metadata);
  }
}
