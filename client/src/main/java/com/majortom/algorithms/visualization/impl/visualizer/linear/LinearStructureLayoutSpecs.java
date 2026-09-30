package com.majortom.algorithms.visualization.impl.visualizer.linear;

import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.render.api.LinearLayoutDirection;

/** Built-in layout specifications for logical stack and queue visualizations. */
public final class LinearStructureLayoutSpecs {
  public static final LinearStructureLayoutSpec STACK =
      new LinearStructureLayoutSpec(
          StructureIds.STACK, LinearLayoutDirection.DOWN, 108.0d, 48.0d, 30.0d, 30.0d);

  public static final LinearStructureLayoutSpec QUEUE =
      new LinearStructureLayoutSpec(
          StructureIds.QUEUE, LinearLayoutDirection.RIGHT, 90.0d, 50.0d, 28.0d, 30.0d);

  private LinearStructureLayoutSpecs() {}
}
