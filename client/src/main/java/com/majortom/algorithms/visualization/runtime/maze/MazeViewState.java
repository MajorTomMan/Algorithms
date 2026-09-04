package com.majortom.algorithms.visualization.runtime.maze;

import com.majortom.algorithms.core.snapshot.MazeSnapshot;
import com.majortom.algorithms.library.maze.GridPoint;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Immutable maze facts plus factual pathfinding observations. */
public record MazeViewState(
        int rows,
        int columns,
        List<Boolean> openCells,
        Set<GridPoint> path,
        Set<GridPoint> visited,
        GridPoint active,
        GridPoint backtracked,
        GridPoint entrance,
        GridPoint exit,
        List<MazeSnapshot.Edge> graphEdges,
        boolean graphBased,
        boolean completed) {

    public MazeViewState {
        openCells = List.copyOf(openCells);
        path = Set.copyOf(path);
        visited = Set.copyOf(visited);
        graphEdges = List.copyOf(graphEdges);
    }

    public static MazeViewState empty(int rows, int columns, boolean graphBased) {
        return new MazeViewState(
                rows,
                columns,
                Collections.nCopies(rows * columns, graphBased),
                Set.of(),
                Set.of(),
                null,
                null,
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
                Set.of(),
                null,
                null,
                point(snapshot.entrance()),
                point(snapshot.exit()),
                snapshot.graphEdges(),
                snapshot.graphBased(),
                false);
    }

    public MazeViewState visit(GridPoint point) {
        LinkedHashSet<GridPoint> nextVisited = new LinkedHashSet<>(visited);
        nextVisited.add(point);
        return new MazeViewState(rows, columns, openCells, path, nextVisited, point, null,
                entrance, exit, graphEdges, graphBased, false);
    }

    public MazeViewState examine(GridPoint point) {
        return new MazeViewState(rows, columns, openCells, path, visited, point, null,
                entrance, exit, graphEdges, graphBased, false);
    }

    public MazeViewState backtrack(GridPoint point) {
        return new MazeViewState(rows, columns, openCells, path, visited, point, point,
                entrance, exit, graphEdges, graphBased, false);
    }

    private static GridPoint point(MazeSnapshot.Cell cell) {
        return cell == null ? null : new GridPoint(cell.row(), cell.column());
    }
}
