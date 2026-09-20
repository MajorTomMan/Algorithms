package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.core.snapshot.MazeSnapshot;
import com.majortom.algorithms.structure.maze.GridMaze;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import java.util.List;

/** Stateless conversions between maze domain objects, snapshots and view states. */
final class MazeSnapshotMapper {
    private MazeSnapshotMapper() {}

    static MazeSnapshot snapshot(GridMaze maze) {
        return new MazeSnapshot(
                maze.rows(),
                maze.columns(),
                maze.openCells(),
                cell(maze.entrance()),
                cell(maze.exit()),
                List.of(),
                false);
    }


    static MazeSnapshot snapshot(int rows, int columns, GraphSnapshot<Integer> graph) {
        java.util.Map<Long, Integer> valuesById = graph.vertices().stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.majortom.algorithms.core.snapshot.GraphSnapshot.Vertex::id,
                        com.majortom.algorithms.core.snapshot.GraphSnapshot.Vertex::value));
        List<MazeSnapshot.Edge> edges = graph.edges().stream()
                .map(edge -> new MazeSnapshot.Edge(valuesById.get(edge.fromId()), valuesById.get(edge.toId())))
                .toList();
        return new MazeSnapshot(
                rows,
                columns,
                java.util.Collections.nCopies(rows * columns, true),
                null,
                null,
                edges,
                true);
    }


    static MazeViewState completedViewState(MazeSnapshot snapshot, java.util.Set<com.majortom.algorithms.structure.maze.GridPoint> path) {
        MazeViewState state = MazeViewState.source(snapshot);
        return new MazeViewState(
                state.rows(),
                state.columns(),
                state.openCells(),
                path,
                state.visited(),
                state.active(),
                state.observed(),
                state.backtracked(),
                state.entrance(),
                state.exit(),
                state.graphEdges(),
                state.graphBased(),
                true);
    }


    static MazeSnapshot snapshotFromView(MazeViewState state) {
        return new MazeSnapshot(state.rows(), state.columns(), state.openCells(),
                cell(state.entrance()), cell(state.exit()), state.graphEdges().stream()
                .map(edge -> new MazeSnapshot.Edge(edge.from(), edge.to())).toList(), state.graphBased());
    }


    static GridMaze gridMaze(MazeSnapshot state) {
        if (state == null || state.graphBased() || state.entrance() == null || state.exit() == null) {
            return null;
        }
        return new GridMaze(state.rows(), state.columns(), state.openCells(),
                point(state.entrance()), point(state.exit()));
    }


    static MazeViewState viewState(MazeSnapshot state) {
        return MazeViewState.source(state);
    }


    static MazeSnapshot.Cell cell(com.majortom.algorithms.structure.maze.GridPoint point) {
        if (point == null) {
            return null;
        } else {
            return new MazeSnapshot.Cell(point.row(), point.column());
        }
    }


    static com.majortom.algorithms.structure.maze.GridPoint point(MazeSnapshot.Cell cell) {
        if (cell == null) {
            return null;
        } else {
            return new com.majortom.algorithms.structure.maze.GridPoint(cell.row(), cell.column());
        }
    }

}
