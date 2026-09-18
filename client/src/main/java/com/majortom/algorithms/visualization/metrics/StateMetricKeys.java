package com.majortom.algorithms.visualization.metrics;

/** Internal state-sample keys used to derive execution peaks without changing domain events. */
public final class StateMetricKeys {
  public static final String SIZE = "state.size";
  public static final String VERTICES = "state.vertices";
  public static final String EDGES = "state.edges";
  public static final String VISITED = "state.visited";
  public static final String PATH = "state.path";
  public static final String HEIGHT = "state.height";

  private StateMetricKeys() {}
}
