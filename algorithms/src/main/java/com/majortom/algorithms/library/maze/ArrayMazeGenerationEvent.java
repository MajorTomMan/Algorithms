package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.api.AlgorithmEvent;
import com.majortom.algorithms.core.api.StatisticsContribution;

import java.util.Map;
import java.util.Objects;

/** Typed deltas sufficient to replay array-maze generation. */
public sealed interface ArrayMazeGenerationEvent extends AlgorithmEvent, StatisticsContribution
        permits ArrayMazeGenerationEvent.Initialized,
        ArrayMazeGenerationEvent.CellOpened,
        ArrayMazeGenerationEvent.Completed {

    @Override
    default Map<String, Long> metricDeltas() {
        if (this instanceof CellOpened) {
            return Map.of("cells.opened", 1L);
        }
        return Map.of();
    }

    record Initialized(int rows, int columns, GridPoint entrance, GridPoint exit)
            implements ArrayMazeGenerationEvent {
        public Initialized {
            if (rows < 1 || columns < 1) {
                throw new IllegalArgumentException("maze dimensions must be positive");
            }
            Objects.requireNonNull(entrance, "entrance");
            Objects.requireNonNull(exit, "exit");
        }
    }

    record CellOpened(GridPoint point) implements ArrayMazeGenerationEvent {
        public CellOpened {
            Objects.requireNonNull(point, "point");
        }
    }

    record Completed(GridMaze maze) implements ArrayMazeGenerationEvent {
        public Completed {
            Objects.requireNonNull(maze, "maze");
        }
    }
}
