package com.majortom.algorithms.structure.maze;

import java.util.Objects;
import com.majortom.algorithms.core.snapshot.MazeSnapshot;

/** Mutable maze data holder. */
public final class Maze implements MazeStructure {
  private MazeDimensions dimensions = new MazeDimensions(51, 51);
  private GridMaze grid;
  private MazeEndpointPolicy endpointPolicy = MazeEndpointPolicy.fixed();
  private MazeEndpoints resolvedEndpoints;

  public Maze() {}

  public Maze(MazeDimensions dimensions) {
    initialize(emptySnapshot(dimensions));
  }

  public Maze(MazeDimensions dimensions, MazeEndpointPolicy endpointPolicy) {
    this.endpointPolicy = Objects.requireNonNull(endpointPolicy, "endpointPolicy");
    initialize(emptySnapshot(dimensions));
  }

  public Maze(GridMaze grid) {
    initialize(gridSnapshot(grid));
  }

  /** A graph-based maze uses another model and cannot be restored as GridMaze. */
  public static Maze fromSnapshot(MazeSnapshot snapshot) {
    Maze maze = new Maze();
    maze.initialize(snapshot);
    return maze;
  }

  @Override
  public void initialize(MazeSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    if (snapshot.graphBased()) {
      throw new IllegalArgumentException("graph-based maze cannot be restored as GridMaze");
    }
    if (!snapshot.graphEdges().isEmpty()) {
      throw new IllegalArgumentException("grid maze snapshot cannot contain graph edges");
    }
    MazeDimensions nextDimensions = new MazeDimensions(snapshot.rows(), snapshot.columns());
    GridMaze nextGrid = null;
    if (snapshot.entrance() == null && snapshot.exit() == null) {
      for (Boolean open : snapshot.openCells()) {
        if (open) {
          throw new IllegalArgumentException("open maze cells require entrance and exit");
        }
      }
    } else {
      if (snapshot.entrance() == null || snapshot.exit() == null) {
        throw new IllegalArgumentException("maze must have both entrance and exit");
      }
      GridPoint entrance = new GridPoint(snapshot.entrance().row(), snapshot.entrance().column());
      GridPoint exit = new GridPoint(snapshot.exit().row(), snapshot.exit().column());
      nextGrid = new GridMaze(
          snapshot.rows(), snapshot.columns(), snapshot.openCells(), entrance, exit);
    }
    dimensions = nextDimensions;
    grid = nextGrid;
    resolvedEndpoints = null;
  }

  private static MazeSnapshot emptySnapshot(MazeDimensions dimensions) {
    Objects.requireNonNull(dimensions, "dimensions");
    return new MazeSnapshot(
        dimensions.rows(), dimensions.columns(),
        java.util.Collections.nCopies(dimensions.cellCount(), false),
        null, null, java.util.List.of(), false);
  }

  private static MazeSnapshot gridSnapshot(GridMaze grid) {
    Objects.requireNonNull(grid, "grid");
    return new MazeSnapshot(grid.rows(), grid.columns(), grid.openCells(),
        new MazeSnapshot.Cell(grid.entrance().row(), grid.entrance().column()),
        new MazeSnapshot.Cell(grid.exit().row(), grid.exit().column()),
        java.util.List.of(), false);
  }

  public MazeSnapshot snapshot() {
    if (grid != null) {
      return gridSnapshot(grid);
    }
    return emptySnapshot(dimensions);
  }

  @Override
  public MazeDimensions dimensions() {
    return dimensions;
  }

  @Override
  public GridMaze grid() {
    return grid;
  }

  public MazeEndpointPolicy endpointPolicy() {
    return endpointPolicy;
  }

  @Override
  public synchronized MazeEndpoints generationEndpoints() {
    if (resolvedEndpoints == null) {
      resolvedEndpoints = endpointPolicy.resolve(dimensions);
    }
    return resolvedEndpoints;
  }

}
