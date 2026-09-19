package com.majortom.algorithms.visualization.runtime.maze;

import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.structure.maze.GridPoint;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;

/** Reduces factual Maze observations and Runtime lifecycle into MazeViewState. */
public final class MazeEventReducer implements EventReducer<MazeViewState> {
  private final MazeViewState initialState;
  private final boolean generation;

  public MazeEventReducer(int rows, int columns, boolean graphBased) {
    this.initialState = MazeViewState.generation(rows, columns, graphBased);
    this.generation = true;
  }

  public MazeEventReducer(com.majortom.algorithms.core.snapshot.MazeSnapshot snapshot) {
    this.initialState = MazeViewState.source(snapshot);
    this.generation = false;
  }

  @Override
  public MazeViewState initialState() {
    return initialState;
  }

  @Override
  public Reduction<MazeViewState> reduce(MazeViewState previous, EventEnvelope envelope) {
    Object event = envelope.event();
    if (event instanceof AlgorithmEvent.Visited visited) {
      GridPoint point = point(visited.ref());
      if (point != null) {
        MazeViewState next;
        if (generation && !previous.graphBased()) {
          next = previous.open(point);
        } else {
          next = previous.visit(point);
        }
        return observation(next);
      }
    }
    if (event instanceof AlgorithmEvent.Examined examined) {
      GridPoint from = point(examined.fromRef());
      GridPoint to = point(examined.toRef());
      if (to != null) {
        MazeViewState next = previous.examine(from, to);
        if (generation && previous.graphBased() && from != null) {
          next = next.connect(from, to);
        }
        return observation(next);
      }
    }
    if (event instanceof AlgorithmEvent.PathTraced pathTraced) {
      GridPoint point = point(pathTraced.ref());
      if (point != null) {
        return Reduction.changed(previous.tracePath(point), EventImportance.STATE_CHANGE, true);
      }
    }
    if (event instanceof AlgorithmEvent.PathFound pathFound) {
      java.util.LinkedHashSet<GridPoint> path = new java.util.LinkedHashSet<>();
      for (AlgorithmEvent.Reference ref : pathFound.refs()) {
        GridPoint point = point(ref);
        if (point != null)
          path.add(point);
      }
      if (!path.isEmpty()) {
        return Reduction.changed(previous.withPath(path), EventImportance.STATE_CHANGE, true);
      }
    }
    if (event instanceof AlgorithmEvent.Backtracked backtracked) {
      GridPoint point = point(backtracked.ref());
      if (point != null) {
        return observation(previous.backtrack(point));
      }
    }
    if (event instanceof RunCompletedEvent) {
      return Reduction.changed(previous.completedBase(), EventImportance.TERMINAL, true);
    }
    return Reduction.unchanged(previous, EventImportance.TRANSIENT);
  }

  private static GridPoint point(AlgorithmEvent.Reference reference) {
    if (reference instanceof AlgorithmEvent.CoordinateRef coordinate) {
      return new GridPoint(coordinate.row(), coordinate.column());
    }
    return null;
  }

  private static Reduction<MazeViewState> observation(MazeViewState state) {
    return Reduction.changed(state, EventImportance.TRANSIENT, true);
  }
}
