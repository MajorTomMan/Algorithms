package com.majortom.algorithms.visualization.impl.visualizer.linked.animation;

import com.majortom.algorithms.visualization.impl.visualizer.linked.LinkedListVisualIds;

/** Stable logical ids shared by the linked-list planner and its FX scene adapter. */
public final class LinkedListAnimationIds {
  private LinkedListAnimationIds() {}

  public static String node(long id) {
    return LinkedListVisualIds.node(id);
  }

  public static String nextEdge(long sourceId, long targetId) {
    return LinkedListVisualIds.nextEdge(sourceId, targetId);
  }

  public static String previousEdge(long sourceId, long targetId) {
    return LinkedListVisualIds.previousEdge(sourceId, targetId);
  }
}
