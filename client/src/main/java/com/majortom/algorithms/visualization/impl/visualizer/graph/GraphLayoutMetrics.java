package com.majortom.algorithms.visualization.impl.visualizer.graph;

/** Shared graph geometry policy used by capture, layout and FX commit. */
public final class GraphLayoutMetrics {
  public static final double MIN_NODE_RADIUS = 28.0d;
  public static final double NODE_LABEL_PADDING = 24.0d;
  public static final double NODE_ID_GAP = 6.0d;
  public static final double DECORATION_HORIZONTAL_PADDING = 8.0d;
  public static final double DECORATION_VERTICAL_PADDING = 4.0d;
  public static final double EDGE_LABEL_OFFSET = 20.0d;
  public static final double EDGE_LABEL_OFFSET_STEP = 12.0d;
  public static final double EDGE_LABEL_COLLISION_PADDING = 4.0d;

  private GraphLayoutMetrics() {}
}
