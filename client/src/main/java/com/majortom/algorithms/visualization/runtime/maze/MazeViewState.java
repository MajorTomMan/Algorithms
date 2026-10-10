package com.majortom.algorithms.visualization.runtime.maze;

import com.majortom.algorithms.core.snapshot.MazeSnapshot;
import com.majortom.algorithms.structure.maze.GridPoint;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable maze facts plus factual pathfinding observations. */
public record MazeViewState(int rows, int columns, List<Boolean> openCells, Set<GridPoint> path,
    Set<GridPoint> visited, GridPoint active, GridPoint observed, GridPoint backtracked,
    GridPoint entrance, GridPoint exit, boolean completed) {
  public MazeViewState {
    openCells = List.copyOf(openCells);
    path = Set.copyOf(path);
    visited = Set.copyOf(visited);
  }

  public static MazeViewState empty(int rows, int columns) {
    return new MazeViewState(rows, columns, Collections.nCopies(rows * columns, false),
        Set.of(), Set.of(), null, null, null, null, null, false);
  }

  public static MazeViewState generation(int rows, int columns, GridPoint entrance, GridPoint exit) {
    MazeViewState empty = empty(rows, columns);
    Objects.requireNonNull(entrance, "entrance");
    Objects.requireNonNull(exit, "exit");
    return new MazeViewState(rows, columns, empty.openCells(), empty.path(), empty.visited(),
        null, null, null, entrance, exit, false);
  }

  public static MazeViewState source(MazeSnapshot snapshot) {
    return new MazeViewState(snapshot.rows(), snapshot.columns(), snapshot.openCells(), Set.of(),
        Set.of(), null, null, null, point(snapshot.entrance()), point(snapshot.exit()), false);
  }

  public MazeViewState visit(GridPoint point) {
    LinkedHashSet<GridPoint> nextVisited = new LinkedHashSet<>(visited);
    nextVisited.add(point);
    return new MazeViewState(rows, columns, openCells, path, nextVisited, point, null, null,
        entrance, exit, false);
  }

  public MazeViewState open(GridPoint point) {
    int index = point.row() * columns + point.column();
    if (index < 0 || index >= openCells.size() || openCells.get(index)) {
      return visit(point);
    }
    java.util.ArrayList<Boolean> nextOpenCells = new java.util.ArrayList<>(openCells);
    nextOpenCells.set(index, true);
    LinkedHashSet<GridPoint> nextVisited = new LinkedHashSet<>(visited);
    nextVisited.add(point);
    return new MazeViewState(rows, columns, nextOpenCells, path, nextVisited, point, null, null,
        entrance, exit, false);
  }

  public MazeViewState examine(GridPoint from, GridPoint to) {
    GridPoint current;
    if (from == null) {
      current = active;
    } else {
      current = from;
    }
    return new MazeViewState(rows, columns, openCells, path, visited, current, to, null, entrance,
        exit, false);
  }

  public MazeViewState backtrack(GridPoint point) {
    return new MazeViewState(rows, columns, openCells, path, visited, point, null, point, entrance,
        exit, false);
  }

  public MazeViewState tracePath(GridPoint point) {
    LinkedHashSet<GridPoint> nextPath = new LinkedHashSet<>(path);
    nextPath.add(point);
    return new MazeViewState(rows, columns, openCells, nextPath, visited, point, null, null,
        entrance, exit, false);
  }

  public MazeViewState withPath(java.util.Collection<GridPoint> points) {
    LinkedHashSet<GridPoint> nextPath = new LinkedHashSet<>(points);
    return new MazeViewState(rows, columns, openCells, nextPath, visited, active, observed,
        backtracked, entrance, exit, false);
  }

  public MazeViewState completedBase() {
    return new MazeViewState(rows, columns, openCells, path, Set.of(), null, null, null, entrance,
        exit, true);
  }

  private static GridPoint point(MazeSnapshot.Cell cell) {
    if (cell == null) {
      return null;
    } else {
      return new GridPoint(cell.row(), cell.column());
    }
  }
}
