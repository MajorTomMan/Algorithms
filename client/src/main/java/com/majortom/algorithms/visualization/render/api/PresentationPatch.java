package com.majortom.algorithms.visualization.render.api;

import java.util.Map;
import java.util.Objects;

public record PresentationPatch(Map<String, String> elementStates) {
  public PresentationPatch {
    elementStates = Map.copyOf(Objects.requireNonNull(elementStates, "elementStates"));
  }
  public static PresentationPatch empty() {
    return new PresentationPatch(Map.of());
  }
}
