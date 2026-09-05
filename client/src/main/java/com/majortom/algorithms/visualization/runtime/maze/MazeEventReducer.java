package com.majortom.algorithms.visualization.runtime.maze;

import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.event.observation.ObservationEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.library.maze.GridPoint;
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
        if (event instanceof ObservationEvent.Visited visited) {
            GridPoint point = point(visited.ref());
            if (point != null) {
                MazeViewState next = generation && !previous.graphBased()
                        ? previous.open(point)
                        : previous.visit(point);
                return observation(next);
            }
        }
        if (event instanceof ObservationEvent.Examined examined) {
            GridPoint point = point(examined.toRef());
            if (point != null) {
                return observation(previous.examine(point));
            }
        }
        if (event instanceof ObservationEvent.Backtracked backtracked) {
            GridPoint point = point(backtracked.ref());
            if (point != null) {
                return observation(previous.backtrack(point));
            }
        }
        if (event instanceof RunCompletedEvent) {
            MazeViewState completed = new MazeViewState(
                    previous.rows(),
                    previous.columns(),
                    previous.openCells(),
                    previous.path(),
                    previous.visited(),
                    previous.active(),
                    previous.backtracked(),
                    previous.entrance(),
                    previous.exit(),
                    previous.graphEdges(),
                    previous.graphBased(),
                    true);
            return Reduction.changed(completed, EventImportance.TERMINAL, true);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static GridPoint point(ObservationEvent.Reference reference) {
        if (reference instanceof ObservationEvent.CoordinateRef coordinate) {
            return new GridPoint(coordinate.row(), coordinate.column());
        }
        return null;
    }

    private static Reduction<MazeViewState> observation(MazeViewState state) {
        return Reduction.changed(state, EventImportance.TRANSIENT, true);
    }
}
