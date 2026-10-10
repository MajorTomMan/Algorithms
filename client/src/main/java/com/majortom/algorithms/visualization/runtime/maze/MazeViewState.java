package com.majortom.algorithms.visualization.runtime.maze;

import com.majortom.algorithms.core.snapshot.MazeSnapshot;
import com.majortom.algorithms.structure.maze.GridPoint;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

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

  /** Finds changed cells without scanning all 99x99 grid elements for every frame. */
  public void forEachChangedCell(MazeViewState previous, IntConsumer changedIndex) {
    java.util.Objects.requireNonNull(previous, "previous");
    java.util.Objects.requireNonNull(changedIndex, "changedIndex");
    if (rows != previous.rows || columns != previous.columns)
      throw new IllegalArgumentException("maze dimensions must match");

    MazeCellBits.forEachDifference(previous.openCells, openCells, changedIndex);
    MazeVisitedBits.forEachDifference(rows, columns, previous.visited, visited, changedIndex);

    for (GridPoint point : previous.path) {
      if (!path.contains(point))
        markPathNeighborhood(point, changedIndex);
    }
    for (GridPoint point : path) {
      if (!previous.path.contains(point))
        markPathNeighborhood(point, changedIndex);
    }
    if (!java.util.Objects.equals(previous.active, active)) {
      mark(previous.active, changedIndex);
      mark(active, changedIndex);
    }
    if (!java.util.Objects.equals(previous.observed, observed)) {
      mark(previous.observed, changedIndex);
      mark(observed, changedIndex);
    }
    if (!java.util.Objects.equals(previous.backtracked, backtracked)) {
      mark(previous.backtracked, changedIndex);
      mark(backtracked, changedIndex);
    }
    if (!java.util.Objects.equals(previous.entrance, entrance)) {
      mark(previous.entrance, changedIndex);
      mark(entrance, changedIndex);
    }
    if (!java.util.Objects.equals(previous.exit, exit)) {
      mark(previous.exit, changedIndex);
      mark(exit, changedIndex);
    }
  }

  private void markPathNeighborhood(GridPoint point, IntConsumer changedIndex) {
    if (point == null) return;
    mark(point, changedIndex);
    if (point.row() > 0)
      mark(new GridPoint(point.row() - 1, point.column()), changedIndex);
    if (point.row() + 1 < rows)
      mark(new GridPoint(point.row() + 1, point.column()), changedIndex);
    if (point.column() > 0)
      mark(new GridPoint(point.row(), point.column() - 1), changedIndex);
    if (point.column() + 1 < columns)
      mark(new GridPoint(point.row(), point.column() + 1), changedIndex);
  }

  private void mark(GridPoint point, IntConsumer changedIndex) {
    if (point != null && point.row() >= 0 && point.row() < rows
        && point.column() >= 0 && point.column() < columns)
      changedIndex.accept(point.row() * columns + point.column());
  }

  private static GridPoint point(MazeSnapshot.Cell cell) {
    if (cell == null) {
      return null;
    } else {
      return new GridPoint(cell.row(), cell.column());
    }
  }
}
