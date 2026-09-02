package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.library.graph.IntGraph;

import java.util.Objects;

/** A graph-maze represented as a bidirectional spanning tree. */
public record GraphMazeGenerationOutput(int rows, int columns, IntGraph graph) {

    public GraphMazeGenerationOutput {
        Objects.requireNonNull(graph, "graph");
    }
}
