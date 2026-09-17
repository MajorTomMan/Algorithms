package com.majortom.algorithms.visualization.impl.visualizer.array.animation;

import com.majortom.algorithms.visualization.impl.visualizer.array.ArrayVisualIds;

/** Logical animation ids for array cells. Layout ids stay owned by the render/layout API. */
public final class ArrayAnimationIds {
  private ArrayAnimationIds() {}

  public static String node(int index) {
    return ArrayVisualIds.node(index);
  }

  public static String exit(int previousIndex) {
    return ArrayVisualIds.exit(previousIndex);
  }

  public static int exitIndex(String logicalId) {
    return ArrayVisualIds.exitIndex(logicalId);
  }
}
