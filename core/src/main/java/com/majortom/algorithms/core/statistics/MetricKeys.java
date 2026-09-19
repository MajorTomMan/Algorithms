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
  /** Search facts are separate from existing visited/compared/matched counters. */
  public static final String SEARCH_PROBES = "searchProbes";
  public static final String SEARCH_RESULTS = "searchResults";
  public static final String CACHE_HITS = "cacheHits";
  public static final String CACHE_MISSES = "cacheMisses";
  public static final String CACHE_STORES = "cacheStores";
  public static final String CACHE_EVICTIONS = "cacheEvictions";

  public static final String FRONTIER_ADDITIONS = "frontierAdditions";
  public static final String FRONTIER_SELECTIONS = "frontierSelections";
  public static final String FRONTIER_REJECTIONS = "frontierRejections";
  public static final String FRONTIER_PRUNES = "frontierPrunes";
  public static final String RECURSIVE_CALLS = "recursiveCalls";
  public static final String RECURSIVE_RETURNS = "recursiveReturns";

  private MetricKeys() {}
}
