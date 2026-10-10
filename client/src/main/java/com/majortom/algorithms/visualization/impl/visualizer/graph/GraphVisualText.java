package com.majortom.algorithms.visualization.impl.visualizer.graph;

import java.util.Locale;

/** Stable graph presentation text shared by detached measurement and FX rendering. */
public final class GraphVisualText {
  private GraphVisualText() {}

  public static String weight(double weight) {
    if (Math.rint(weight) == weight) {
      return Long.toString((long) weight);
    }
    return String.format(Locale.ROOT, "%.2f", weight);
  }
}
