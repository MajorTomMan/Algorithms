package com.majortom.algorithms.visualization.impl.visualizer.string.animation;

/** Logical animation ids for String character cells. */
public final class StringAnimationIds {
  private static final String EXIT_PREFIX = "string:exit:";

  private StringAnimationIds() {}

  public static String node(int index) {
    return "string:" + index;
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
