package com.majortom.algorithms.visualization.runtime.maze;

import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;

/** Runtime-only reducer for Maze algorithms while observation events remain intentionally deferred. */
public final class MazeEventReducer implements EventReducer<MazeViewState> {
    private final int rows;
    private final int columns;
    private final boolean graphBased;

    public MazeEventReducer(int rows, int columns, boolean graphBased) {
        this.rows = rows;
        this.columns = columns;
        this.graphBased = graphBased;
    }

    @Override
    public MazeViewState initialState() {
        return MazeViewState.empty(rows, columns, graphBased);
    }

    @Override
    public Reduction<MazeViewState> reduce(MazeViewState previous, EventEnvelope envelope) {
        if (envelope.event() instanceof RunCompletedEvent) {
            MazeViewState completed = new MazeViewState(
                    previous.rows(),
                    previous.columns(),
                    previous.openCells(),
                    previous.path(),
                    previous.entrance(),
                    previous.exit(),
                    previous.graphEdges(),
                    previous.graphBased(),
                    true);
            return Reduction.changed(completed, EventImportance.TERMINAL, true);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }
}
