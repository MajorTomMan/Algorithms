package com.majortom.algorithms.visualization.impl.visualizer.linked.animation;

/** Stable logical ids shared by the linked-list planner and its FX scene adapter. */
public final class LinkedListAnimationIds {
  private LinkedListAnimationIds() {}

  public static String node(long id) {
    return Long.toString(id);
  }

  public static String nextEdge(long sourceId, long targetId) {
    return "linked:next:" + sourceId + ":" + targetId;
  }

  public static String previousEdge(long sourceId, long targetId) {
    return "linked:previous:" + sourceId + ":" + targetId;
  }
}
