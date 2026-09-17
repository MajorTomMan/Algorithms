package com.majortom.algorithms.visualization.impl.visualizer.array;

import com.majortom.algorithms.core.metadata.StructureIds;

/** Stable array visual identities shared by capture, animation, and FX commit. */
public final class ArrayVisualIds {
  private static final String NODE_PREFIX = StructureIds.ARRAY + ":";
  private static final String EXIT_PREFIX = StructureIds.ARRAY + ":exit:";

  private ArrayVisualIds() {}

  public static String node(int index) {
    return NODE_PREFIX + index;
  }

  public static String exit(int previousIndex) {
    return EXIT_PREFIX + previousIndex;
  }

  public static int nodeIndex(String logicalId) {
    return parseIndex(logicalId, NODE_PREFIX);
  }

  public static int exitIndex(String logicalId) {
    return parseIndex(logicalId, EXIT_PREFIX);
  }

  private static int parseIndex(String logicalId, String prefix) {
    if (logicalId == null || !logicalId.startsWith(prefix)) return -1;
    try {
      return Integer.parseInt(logicalId.substring(prefix.length()));
    } catch (NumberFormatException ignored) {
      return -1;
    }
  }
}
