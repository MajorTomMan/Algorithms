package com.majortom.algorithms.structure.maze;

import java.util.Objects;

/** A resolved entrance and exit pair for one maze generation. */
public record MazeEndpoints(GridPoint entrance, GridPoint exit) {
  public MazeEndpoints {
    entrance = Objects.requireNonNull(entrance, "entrance");
    exit = Objects.requireNonNull(exit, "exit");
  }
}
