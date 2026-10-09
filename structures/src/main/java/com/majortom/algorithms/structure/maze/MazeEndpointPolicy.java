package com.majortom.algorithms.structure.maze;

import java.util.Objects;
import java.util.Random;

/**
 * Code-level policy for choosing the entrance and exit of an array maze.
 *
 * <p>Custom endpoints must be inner logical cells: both coordinates are odd and neither coordinate
 * is on the outer border. This keeps every generated maze connected while allowing the generator
 * to start at the requested entrance.
 */
public record MazeEndpointPolicy(
    MazeEndpointMode mode, GridPoint customEntrance, GridPoint customExit) {

  public MazeEndpointPolicy {
    mode = Objects.requireNonNull(mode, "mode");
    if (mode == MazeEndpointMode.CUSTOM) {
      customEntrance = Objects.requireNonNull(customEntrance, "customEntrance");
      customExit = Objects.requireNonNull(customExit, "customExit");
    } else if (customEntrance != null || customExit != null) {
      throw new IllegalArgumentException("custom endpoints require CUSTOM mode");
    }
  }

  public static MazeEndpointPolicy random() {
    return new MazeEndpointPolicy(MazeEndpointMode.RANDOM, null, null);
  }

  public static MazeEndpointPolicy fixed() {
    return new MazeEndpointPolicy(MazeEndpointMode.FIXED, null, null);
  }

  public static MazeEndpointPolicy custom(GridPoint entrance, GridPoint exit) {
    return new MazeEndpointPolicy(MazeEndpointMode.CUSTOM, entrance, exit);
  }

  /** Resolve this policy using a new source of randomness when RANDOM is selected. */
  public MazeEndpoints resolve(MazeDimensions dimensions) {
    return resolve(dimensions, new Random());
  }

  /** Resolve this policy with the supplied random source, primarily for deterministic callers. */
  public MazeEndpoints resolve(MazeDimensions dimensions, Random random) {
    requireArrayDimensions(dimensions);
    Objects.requireNonNull(random, "random");
    return switch (mode) {
      case FIXED -> fixedEndpoints(dimensions);
      case RANDOM -> randomEndpoints(dimensions, random);
      case CUSTOM -> customEndpoints(dimensions);
    };
  }

  private MazeEndpoints customEndpoints(MazeDimensions dimensions) {
    requireLogicalCell(dimensions, customEntrance, "customEntrance");
    requireLogicalCell(dimensions, customExit, "customExit");
    return new MazeEndpoints(customEntrance, customExit);
  }

  private static MazeEndpoints fixedEndpoints(MazeDimensions dimensions) {
    return new MazeEndpoints(
        new GridPoint(1, 1), new GridPoint(dimensions.rows() - 2, dimensions.columns() - 2));
  }

  private static MazeEndpoints randomEndpoints(MazeDimensions dimensions, Random random) {
    int logicalColumns = (dimensions.columns() - 1) / 2;
    int logicalCellCount = ((dimensions.rows() - 1) / 2) * logicalColumns;
    int entranceIndex = random.nextInt(logicalCellCount);
    int exitIndex = logicalCellCount == 1 ? entranceIndex : random.nextInt(logicalCellCount - 1);
    if (logicalCellCount > 1 && exitIndex >= entranceIndex) {
      exitIndex++;
    }
    return new MazeEndpoints(
        logicalCell(logicalColumns, entranceIndex), logicalCell(logicalColumns, exitIndex));
  }

  private static GridPoint logicalCell(int logicalColumns, int index) {
    return new GridPoint(1 + (index / logicalColumns) * 2, 1 + (index % logicalColumns) * 2);
  }

  private static void requireLogicalCell(
      MazeDimensions dimensions, GridPoint point, String name) {
    if (point.row() <= 0
        || point.row() >= dimensions.rows() - 1
        || point.column() <= 0
        || point.column() >= dimensions.columns() - 1
        || point.row() % 2 == 0
        || point.column() % 2 == 0) {
      throw new IllegalArgumentException(name + " must be an inner logical maze cell");
    }
  }

  private static void requireArrayDimensions(MazeDimensions dimensions) {
    Objects.requireNonNull(dimensions, "dimensions");
    if (dimensions.rows() < 3
        || dimensions.columns() < 3
        || dimensions.rows() % 2 == 0
        || dimensions.columns() % 2 == 0) {
      throw new IllegalArgumentException("maze dimensions must be odd and at least 3");
    }
  }
}
