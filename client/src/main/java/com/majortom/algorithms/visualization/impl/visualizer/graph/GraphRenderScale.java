package com.majortom.algorithms.visualization.impl.visualizer.graph;

/**
 * Shared adaptive graph rendering policy. Topology and underlying snapshots are never pruned:
 * only layout complexity and on-screen labels change as the graph grows.
 */
public final class GraphRenderScale {
  private static final int FAST_LAYOUT_NODES = 180;
  private static final int FAST_LAYOUT_EDGES = 450;
  private static final int NODE_LABEL_NODES = 80;
  private static final int EDGE_LABEL_EDGES = 160;
  private static final int ANIMATED_NODES = 160;
  private static final int ANIMATED_EDGES = 360;

  private GraphRenderScale() {}

  public static boolean fastLayout(int nodes, int edges) {
    return nodes > FAST_LAYOUT_NODES || edges > FAST_LAYOUT_EDGES;
  }
  public static boolean nodeIds(int nodes, int edges) {
    return nodes <= NODE_LABEL_NODES && edges <= FAST_LAYOUT_EDGES;
  }
  public static boolean allEdgeLabels(int nodes, int edges) {
    return nodes <= NODE_LABEL_NODES && edges <= EDGE_LABEL_EDGES;
  }
  public static boolean fullNodeText(int nodes, int edges) {
    return nodes <= NODE_LABEL_NODES && edges <= FAST_LAYOUT_EDGES;
  }
  public static boolean animate(int nodes, int edges) {
    return nodes <= ANIMATED_NODES && edges <= ANIMATED_EDGES;
  }
}
