package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.api.AlgorithmOutput;

import java.util.List;
import java.util.Objects;

/** Result path, empty when the goal is unreachable. */
public record ArrayMazePathOutput(List<GridPoint> path, int visitedCount) implements AlgorithmOutput {

    public ArrayMazePathOutput {
        Objects.requireNonNull(path, "path");
        path = List.copyOf(path);
        if (visitedCount < 0) {
            throw new IllegalArgumentException("visitedCount must not be negative");
        }
    }
}
