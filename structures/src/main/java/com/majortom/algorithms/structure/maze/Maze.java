package com.majortom.algorithms.structure.maze;

import java.util.Objects;

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
