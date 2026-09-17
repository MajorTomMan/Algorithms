package com.majortom.algorithms.visualization.impl.visualizer.tree;

import com.majortom.algorithms.core.metadata.StructureIds;

/** Stable tree visual identities shared by capture, layout, animation, and FX commit. */
public final class TreeVisualIds {
  private static final String PREFIX = StructureIds.TREE + ":";
  private static final String CHILD_SEGMENT = "child";
  private static final String LEFT_SEGMENT = "left";
  private static final String RIGHT_SEGMENT = "right";

  private TreeVisualIds() {}

  public static String node(long id) {
    return PREFIX + id;
  }

  public static String childEdge(int index, long sourceId, long targetId) {
    return edge(CHILD_SEGMENT, index, sourceId, targetId);
  }

  public static String leftEdge(long sourceId, long targetId) {
    return edge(LEFT_SEGMENT, 0, sourceId, targetId);
  }

  public static String rightEdge(long sourceId, long targetId) {
    return edge(RIGHT_SEGMENT, 1, sourceId, targetId);
  }

  private static String edge(String relation, int index, long sourceId, long targetId) {
    return PREFIX + relation + ":" + index + ":" + sourceId + ":" + targetId;
  }
}
