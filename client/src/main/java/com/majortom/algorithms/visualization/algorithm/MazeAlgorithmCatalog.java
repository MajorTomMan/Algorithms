package com.majortom.algorithms.visualization.algorithm;

import com.majortom.algorithms.algorithm.maze.MazeAlgorithm;
import com.majortom.algorithms.algorithm.maze.MazeModel;
import com.majortom.algorithms.algorithm.maze.MazeRole;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import java.util.List;

/** Maze-only presentation classification. Invocation remains owned by AlgorithmDescriptor. */
public final class MazeAlgorithmCatalog {
  private MazeAlgorithmCatalog() {}

  public static List<String> ids(MazeRole role, MazeModel model) {
    return AlgorithmCatalog.descriptorsForWorkbenchModule("maze")
        .stream()
        .filter(descriptor -> matches(descriptor, role, model))
        .map(AlgorithmDescriptor::id)
        .distinct()
        .toList();
  }

  private static boolean matches(AlgorithmDescriptor descriptor, MazeRole role, MazeModel model) {
    MazeAlgorithm metadata = descriptor.implementation().getAnnotation(MazeAlgorithm.class);
    return metadata != null && metadata.role() == role && metadata.model() == model;
  }
}
