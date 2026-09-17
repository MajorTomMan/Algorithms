package com.majortom.algorithms.visualization.impl.visualizer.string.animation;

import com.majortom.algorithms.visualization.impl.visualizer.string.StringVisualIds;

/** Logical animation ids for String character cells. */
public final class StringAnimationIds {
  private StringAnimationIds() {}

  public static String node(int index) {
    return StringVisualIds.node(index);
  }

  public static String exit(int previousIndex) {
    return StringVisualIds.exit(previousIndex);
  }

  public static int exitIndex(String logicalId) {
    return StringVisualIds.exitIndex(logicalId);
  }
}
