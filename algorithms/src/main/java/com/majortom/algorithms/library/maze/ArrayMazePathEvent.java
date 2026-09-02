package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.statistics.StatisticsContribution;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Search events that can replay visited cells and the final route. */
public sealed interface ArrayMazePathEvent extends ExecutionEvent, StatisticsContribution
        permits ArrayMazePathEvent.Initialized, ArrayMazePathEvent.Discovered,
        ArrayMazePathEvent.Entered, ArrayMazePathEvent.DeadEndReached,
        ArrayMazePathEvent.Backtracked, ArrayMazePathEvent.PathConfirmed,
        ArrayMazePathEvent.Completed {

    @Override
    default Map<String, Long> metricDeltas() {
        if (this instanceof Discovered) {
            return Map.of("cells.discovered", 1L);
        }
        if (this instanceof Entered) {
            return Map.of("cells.visited", 1L);
        }
        if (this instanceof DeadEndReached) {
            return Map.of("dead.ends", 1L);
        }
        if (this instanceof Backtracked) {
            return Map.of("backtracks", 1L);
        }
        if (this instanceof PathConfirmed) {
            return Map.of("path.cells", 1L);
        }
        return Map.of();
    }

    record Initialized(GridMaze maze, GridPoint start, GridPoint goal) implements ArrayMazePathEvent {
        public Initialized {
            Objects.requireNonNull(maze, "maze");
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(goal, "goal");
        }
    }

    record Discovered(GridPoint point, GridPoint parent) implements ArrayMazePathEvent {
        public Discovered {
            Objects.requireNonNull(point, "point");
        }
    }

    record Entered(GridPoint point, GridPoint parent) implements ArrayMazePathEvent {
        public Entered {
            Objects.requireNonNull(point, "point");
        }
    }

    record DeadEndReached(GridPoint point) implements ArrayMazePathEvent {
        public DeadEndReached {
            Objects.requireNonNull(point, "point");
        }
    }

    record Backtracked(GridPoint from, GridPoint to) implements ArrayMazePathEvent {
        public Backtracked {
            Objects.requireNonNull(from, "from");
        }
    }

    record PathConfirmed(GridPoint point, int index, int total) implements ArrayMazePathEvent {
        public PathConfirmed {
            Objects.requireNonNull(point, "point");
            if (index < 0 || total < 1 || index >= total) {
                throw new IllegalArgumentException("confirmed path position is outside its path");
            }
        }
    }

    record Completed(List<GridPoint> path) implements ArrayMazePathEvent {
        public Completed {
            Objects.requireNonNull(path, "path");
            path = List.copyOf(path);
        }
    }
}
