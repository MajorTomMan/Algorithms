package com.majortom.algorithms.visualization.impl.visualizer.graph;

import com.majortom.algorithms.core.metadata.StructureIds;

/** Stable graph visual identities shared by capture, layout, animation, and FX commit. */
public final class GraphVisualIds {
  private static final String NODE_PREFIX = StructureIds.GRAPH + ":node:";
  private static final String EDGE_PREFIX = StructureIds.GRAPH + ":edge:";
  private static final String EXIT_SUFFIX = ":exit";

  private GraphVisualIds() {}

  public static String node(long id) {
    return NODE_PREFIX + id;
  }

  public static String edge(long id) {
    return EDGE_PREFIX + id;
  }

  public static String rewiredExitEdge(long id) {
    return edge(id) + EXIT_SUFFIX;
  }
}
