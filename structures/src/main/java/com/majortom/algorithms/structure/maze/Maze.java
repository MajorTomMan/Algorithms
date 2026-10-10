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
    initialize(dimensions);
  }

  public Maze(MazeDimensions dimensions, MazeEndpointPolicy endpointPolicy) {
    this.endpointPolicy = Objects.requireNonNull(endpointPolicy, "endpointPolicy");
    initialize(dimensions);
  }

  public Maze(GridMaze grid) {
    initialize(grid);
  }

  /** A graph-based maze uses a different model and cannot become GridMaze. */
  public static Maze fromSnapshot(MazeSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    if (snapshot.graphBased()) {
      throw new IllegalArgumentException("graph-based maze cannot be restored as GridMaze");
    }
    if (snapshot.entrance() == null && snapshot.exit() == null) {
      for (Boolean open : snapshot.openCells()) {
        if (open) {
          throw new IllegalArgumentException("open maze cells require entrance and exit");
        }
      }
      return new Maze(new MazeDimensions(snapshot.rows(), snapshot.columns()));
    }
    if (snapshot.entrance() == null || snapshot.exit() == null) {
      throw new IllegalArgumentException("maze must have both entrance and exit");
    }
    GridPoint entrance = new GridPoint(snapshot.entrance().row(), snapshot.entrance().column());
    GridPoint exit = new GridPoint(snapshot.exit().row(), snapshot.exit().column());
    GridMaze grid = new GridMaze(
        snapshot.rows(), snapshot.columns(), snapshot.openCells(), entrance, exit);
    return new Maze(grid);
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

  @Override
  public void initialize(MazeDimensions dimensions) {
    this.dimensions = Objects.requireNonNull(dimensions, "dimensions");
    this.grid = null;
    this.resolvedEndpoints = null;
  }

  @Override
  public void initialize(GridMaze grid) {
    this.grid = Objects.requireNonNull(grid, "grid");
    this.dimensions = new MazeDimensions(grid.rows(), grid.columns());
    this.resolvedEndpoints = null;
  }
}
