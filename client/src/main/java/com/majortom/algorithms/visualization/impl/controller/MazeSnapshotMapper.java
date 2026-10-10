package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.MazeSnapshot;
import com.majortom.algorithms.structure.maze.GridMaze;
import com.majortom.algorithms.structure.maze.GridPoint;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import java.util.Set;

/** Stateless conversions between grid mazes, snapshots and view states. */
final class MazeSnapshotMapper {
    private MazeSnapshotMapper() {}

    static MazeSnapshot snapshot(GridMaze maze) {
        return new MazeSnapshot(maze.rows(), maze.columns(), maze.openCells(),
                cell(maze.entrance()), cell(maze.exit()));
    }

    static MazeViewState completedViewState(MazeSnapshot snapshot, Set<GridPoint> path) {
        MazeViewState state = MazeViewState.source(snapshot);
        return new MazeViewState(state.rows(), state.columns(), state.openCells(),
                path, state.visited(), state.active(), state.observed(), state.backtracked(),
                state.entrance(), state.exit(), true);
    }

    static MazeSnapshot snapshotFromView(MazeViewState state) {
        return new MazeSnapshot(state.rows(), state.columns(), state.openCells(),
                cell(state.entrance()), cell(state.exit()));
    }

    static GridMaze gridMaze(MazeSnapshot state) {
        if (state == null || state.entrance() == null || state.exit() == null) {
            return null;
        }
        return new GridMaze(state.rows(), state.columns(), state.openCells(),
                point(state.entrance()), point(state.exit()));
    }

    static MazeViewState viewState(MazeSnapshot state) {
        return MazeViewState.source(state);
    }

    static MazeSnapshot.Cell cell(GridPoint point) {
        if (point == null) {
            return null;
        }
        return new MazeSnapshot.Cell(point.row(), point.column());
    }

    static GridPoint point(MazeSnapshot.Cell cell) {
        if (cell == null) {
            return null;
        }
        return new GridPoint(cell.row(), cell.column());
    }
}
