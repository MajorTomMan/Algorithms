package com.majortom.algorithms.visualization.runtime.maze;

import com.majortom.algorithms.core.snapshot.MazeSnapshot;
import com.majortom.algorithms.structure.maze.GridPoint;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Immutable maze facts plus factual pathfinding observations. */
public record MazeViewState(int rows, int columns, List<Boolean> openCells, Set<GridPoint> path,
    Set<GridPoint> visited, GridPoint active, GridPoint observed, GridPoint backtracked,
    GridPoint entrance, GridPoint exit, boolean completed) {
  public MazeViewState {
    openCells = MazeCellBits.copyOf(openCells);
    path = Set.copyOf(path);
    visited = MazeVisitedBits.copyOf(rows, columns, visited);
  }

  public static MazeViewState empty(int rows, int columns) {
    return new MazeViewState(rows, columns, Collections.nCopies(rows * columns, false),
        Set.of(), Set.of(), null, null, null, null, null, false);
  }

  public static MazeViewState generation(int rows, int columns, GridPoint entrance, GridPoint exit) {
    MazeViewState empty = empty(rows, columns);
    java.util.Objects.requireNonNull(entrance, "entrance");
    java.util.Objects.requireNonNull(exit, "exit");
    return new MazeViewState(rows, columns, empty.openCells(), empty.path(), empty.visited(),
        null, null, null, entrance, exit, false);
  }

  public static MazeViewState source(MazeSnapshot snapshot) {
    return new MazeViewState(snapshot.rows(), snapshot.columns(), snapshot.openCells(), Set.of(),
        Set.of(), null, null, null, point(snapshot.entrance()), point(snapshot.exit()), false);
  }

  public MazeViewState visit(GridPoint point) {
    Set<GridPoint> nextVisited = MazeVisitedBits.withAdded(rows, columns, visited, point);
    return new MazeViewState(rows, columns, openCells, path, nextVisited, point, null, null,
        entrance, exit, false);
  }

  public MazeViewState open(GridPoint point) {
    int index = point.row() * columns + point.column();
    if (index < 0 || index >= openCells.size() || openCells.get(index)) {
      return visit(point);
    }
    List<Boolean> nextOpenCells = ((MazeCellBits) openCells).withOpened(index);
    Set<GridPoint> nextVisited = MazeVisitedBits.withAdded(rows, columns, visited, point);
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
    java.util.LinkedHashSet<GridPoint> nextPath = new LinkedHashSet<>(path);
    nextPath.add(point);
    return new MazeViewState(rows, columns, openCells, nextPath, visited, point, null, null,
        entrance, exit, false);
  }

  public MazeViewState withPath(java.util.Collection<GridPoint> points) {
    java.util.LinkedHashSet<GridPoint> nextPath = new LinkedHashSet<>(points);
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
