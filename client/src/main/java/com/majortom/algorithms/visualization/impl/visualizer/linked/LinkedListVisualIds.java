package com.majortom.algorithms.visualization.impl.visualizer.linked;


/** Stable linked-list visual identities shared by capture, layout, animation, and FX commit. */
public final class LinkedListVisualIds {
  private static final String PREFIX = "linked:";
  private static final String NEXT_SEGMENT = "next";
  private static final String PREVIOUS_SEGMENT = "previous";

  private LinkedListVisualIds() {}

  public static String node(long id) {
    return PREFIX + id;
  }

  public static String nextEdge(long sourceId, long targetId) {
    return PREFIX + NEXT_SEGMENT + ":" + sourceId + ":" + targetId;
  }

  public static String previousEdge(long sourceId, long targetId) {
    return PREFIX + PREVIOUS_SEGMENT + ":" + sourceId + ":" + targetId;
  }
}
