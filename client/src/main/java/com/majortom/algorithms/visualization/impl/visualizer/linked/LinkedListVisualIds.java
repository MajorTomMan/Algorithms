package com.majortom.algorithms.visualization.impl.visualizer.linked;

import java.util.OptionalLong;

/** Stable linked-list visual identities shared by capture, layout, animation, and FX commit. */
public final class LinkedListVisualIds {
  private static final String PREFIX = "linked:";
  private static final String NEXT_SEGMENT = "next";
  private static final String PREVIOUS_SEGMENT = "previous";

  private LinkedListVisualIds() {}

  public static String node(long id) {
    return PREFIX + id;
  }

  /** Resolves a canonical linked-list node visual id back to its factual node id. */
  public static OptionalLong parseNodeId(String visualId) {
    if (visualId == null || !visualId.startsWith(PREFIX)) return OptionalLong.empty();
    String suffix = visualId.substring(PREFIX.length());
    if (suffix.isEmpty() || suffix.indexOf(':') >= 0) return OptionalLong.empty();
    try {
      return OptionalLong.of(Long.parseLong(suffix));
    } catch (NumberFormatException ignored) {
      return OptionalLong.empty();
    }
  }

  public static String nextEdge(long sourceId, long targetId) {
    return PREFIX + NEXT_SEGMENT + ":" + sourceId + ":" + targetId;
  }

  public static String previousEdge(long sourceId, long targetId) {
    return PREFIX + PREVIOUS_SEGMENT + ":" + sourceId + ":" + targetId;
  }
}
