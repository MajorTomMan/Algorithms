package com.majortom.algorithms.core.statistics;

/** Stable metric identifiers shared by event producers, reducers, exports and UI consumers. */
public final class MetricKeys {
  public static final String INSERTIONS = "insertions";
  public static final String REMOVALS = "removals";
  public static final String UPDATES = "updates";
  public static final String SWAPS = "swaps";
  public static final String WRITES = "writes";
  public static final String COMPARISONS = "comparisons";
  public static final String NODES_VISITED = "nodesVisited";
  public static final String EDGES_EXAMINED = "edgesExamined";
  public static final String MATCHES = "matches";
  public static final String FALLBACKS = "fallbacks";
  public static final String BACKTRACKS = "backtracks";

  private MetricKeys() {}
}
