package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.snapshot.GraphSnapshot;

import java.util.Objects;

public record GraphMazeGenerationOutput(int rows, int columns, GraphSnapshot<Integer> graph) {
    public GraphMazeGenerationOutput {
        Objects.requireNonNull(graph, "graph");
    }
}
