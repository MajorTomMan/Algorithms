package com.majortom.algorithms.visualization.runtime.maze;

import com.majortom.algorithms.core.snapshot.MazeSnapshot;
import com.majortom.algorithms.library.maze.GridPoint;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Immutable maze facts. Exploration/visit/backtrack intent is not stored in ViewState. */
public record MazeViewState(
        int rows,
        int columns,
        List<Boolean> openCells,
        Set<GridPoint> path,
        GridPoint entrance,
        GridPoint exit,
        List<MazeSnapshot.Edge> graphEdges,
        boolean graphBased,
        boolean completed) {

    public MazeViewState {
        openCells = List.copyOf(openCells);
        path = Set.copyOf(path);
        graphEdges = List.copyOf(graphEdges);
    }

    public static MazeViewState empty(int rows, int columns, boolean graphBased) {
        return new MazeViewState(
                rows,
                columns,
                Collections.nCopies(rows * columns, graphBased),
                Set.of(),
                null,
                null,
                List.of(),
                graphBased,
                false);
    }

    public static MazeViewState source(MazeSnapshot snapshot) {
        return new MazeViewState(
                snapshot.rows(),
                snapshot.columns(),
                snapshot.openCells(),
                Set.of(),
                point(snapshot.entrance()),
                point(snapshot.exit()),
                snapshot.graphEdges(),
                snapshot.graphBased(),
                false);
    }

    private static GridPoint point(MazeSnapshot.Cell cell) {
        return cell == null ? null : new GridPoint(cell.row(), cell.column());
    }
}
