package com.majortom.algorithms.visualization.impl.visualizer.graph;

/** Stable graph decoration identities shared by capture, layout and FX commit. */
public final class GraphDecorationIds {
  private static final String NODE_ID_PREFIX = "graph:decoration:node-id:";
  private static final String EDGE_LABEL_PREFIX = "graph:decoration:edge-label:";

  private GraphDecorationIds() {}

  public static String nodeId(String nodeId) {
    return NODE_ID_PREFIX + nodeId;
  }

  public static String edgeLabel(String edgeId) {
    return EDGE_LABEL_PREFIX + edgeId;
  }
}
