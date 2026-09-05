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
        GridPoint observed,
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

    /** Compatibility constructor for callers that predate the transient observed candidate. */
    public MazeViewState(
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
        this(rows, columns, openCells, path, visited, active, null, backtracked,
                entrance, exit, graphEdges, graphBased, completed);
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
                null,
                List.of(),
                graphBased,
                false);
    }

    public static MazeViewState generation(int rows, int columns, boolean graphBased) {
        MazeViewState empty = empty(rows, columns, graphBased);
        if (graphBased) {
            return empty;
        }
        return new MazeViewState(
                rows,
                columns,
                empty.openCells(),
                empty.path(),
                empty.visited(),
                null,
                null,
                null,
                new GridPoint(1, 1),
                new GridPoint(rows - 2, columns - 2),
                empty.graphEdges(),
                false,
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
        return new MazeViewState(rows, columns, openCells, path, nextVisited, point, null, null,
                entrance, exit, graphEdges, graphBased, false);
    }

    public MazeViewState open(GridPoint point) {
        int index = point.row() * columns + point.column();
        if (index < 0 || index >= openCells.size() || openCells.get(index)) {
            return visit(point);
        }
        java.util.ArrayList<Boolean> nextOpenCells = new java.util.ArrayList<>(openCells);
        nextOpenCells.set(index, true);
        LinkedHashSet<GridPoint> nextVisited = new LinkedHashSet<>(visited);
        nextVisited.add(point);
        return new MazeViewState(rows, columns, nextOpenCells, path, nextVisited, point, null, null,
                entrance, exit, graphEdges, graphBased, false);
    }

    public MazeViewState examine(GridPoint from, GridPoint to) {
        GridPoint current = from == null ? active : from;
        return new MazeViewState(rows, columns, openCells, path, visited, current, to, null,
                entrance, exit, graphEdges, graphBased, false);
    }

    public MazeViewState backtrack(GridPoint point) {
        return new MazeViewState(rows, columns, openCells, path, visited, point, null, point,
                entrance, exit, graphEdges, graphBased, false);
    }

    public MazeViewState connect(GridPoint from, GridPoint to) {
        if (from == null || to == null) return this;
        int fromIndex = from.row() * columns + from.column();
        int toIndex = to.row() * columns + to.column();
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= rows * columns || toIndex >= rows * columns) {
            return this;
        }
        java.util.ArrayList<MazeSnapshot.Edge> nextEdges = new java.util.ArrayList<>(graphEdges);
        MazeSnapshot.Edge forward = new MazeSnapshot.Edge(fromIndex, toIndex);
        MazeSnapshot.Edge reverse = new MazeSnapshot.Edge(toIndex, fromIndex);
        if (!nextEdges.contains(forward)) nextEdges.add(forward);
        if (!nextEdges.contains(reverse)) nextEdges.add(reverse);
        return new MazeViewState(rows, columns, openCells, path, visited, active, observed, backtracked,
                entrance, exit, nextEdges, graphBased, false);
    }

    public MazeViewState withPath(java.util.Collection<GridPoint> points) {
        LinkedHashSet<GridPoint> nextPath = new LinkedHashSet<>(points);
        return new MazeViewState(rows, columns, openCells, nextPath, visited, active, observed, backtracked,
                entrance, exit, graphEdges, graphBased, false);
    }

    public MazeViewState completedBase() {
        return new MazeViewState(rows, columns, openCells, path, Set.of(), null, null, null,
                entrance, exit, graphEdges, graphBased, true);
    }

    private static GridPoint point(MazeSnapshot.Cell cell) {
        return cell == null ? null : new GridPoint(cell.row(), cell.column());
    }
}
