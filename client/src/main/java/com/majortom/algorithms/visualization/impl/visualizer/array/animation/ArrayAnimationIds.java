package com.majortom.algorithms.visualization.impl.visualizer.array.animation;

/** Logical animation ids for array cells. Layout ids stay owned by the render/layout API. */
public final class ArrayAnimationIds {
  private static final String EXIT_PREFIX = "array:exit:";

  private ArrayAnimationIds() {}

  public static String node(int index) {
    return "array:" + index;
  }

  public static String exit(int previousIndex) {
    return EXIT_PREFIX + previousIndex;
  }

  public static int exitIndex(String logicalId) {
    if (logicalId == null || !logicalId.startsWith(EXIT_PREFIX)) return -1;
    try {
      return Integer.parseInt(logicalId.substring(EXIT_PREFIX.length()));
    } catch (NumberFormatException ignored) {
      return -1;
    }
  }
}
