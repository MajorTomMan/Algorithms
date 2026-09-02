package com.majortom.algorithms.core.snapshot;

import java.util.List;
import java.util.Objects;

/** UI-neutral snapshot of the editable maze/grid structure. */
public record MazeSnapshot(
        int rows,
        int columns,
        List<Boolean> openCells,
        Cell entrance,
        Cell exit,
        List<Edge> graphEdges,
        boolean graphBased) {

    public MazeSnapshot {
        if (rows <= 0 || columns <= 0) throw new IllegalArgumentException("maze dimensions must be positive");
        openCells = List.copyOf(Objects.requireNonNull(openCells, "openCells"));
        graphEdges = List.copyOf(Objects.requireNonNull(graphEdges, "graphEdges"));
        if (openCells.size() != Math.multiplyExact(rows, columns)) {
            throw new IllegalArgumentException("openCells size must match maze dimensions");
        }
    }

    public record Cell(int row, int column) {}

    public record Edge(int from, int to) {}
}
