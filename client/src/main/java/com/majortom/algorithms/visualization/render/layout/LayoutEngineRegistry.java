package com.majortom.algorithms.visualization.render.layout;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class LayoutEngineRegistry {
  private final Map<String, LayoutEngine> engines = new LinkedHashMap<>();
  public LayoutEngineRegistry register(LayoutEngine engine) {
    engines.put(Objects.requireNonNull(engine, "engine").id(), engine);
    return this;
  }
  public LayoutEngine require(String id) {
    LayoutEngine engine = engines.get(id);
    if (engine == null)
      throw new IllegalArgumentException("Unknown layout engine: " + id);
    return engine;
  }
}
