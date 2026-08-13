package com.majortom.algorithms.visualization.runtime.maze;

import com.majortom.algorithms.library.graph.IntEdge;
import com.majortom.algorithms.library.maze.GridPoint;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable maze state that keeps exploration, failure and final-route semantics separate. */
public record MazeViewState(
        int rows,
        int columns,
        List<Boolean> openCells,
        Set<GridPoint> discovered,
        Set<GridPoint> visited,
        Set<GridPoint> deadEnds,
        Set<GridPoint> backtracked,
        Set<GridPoint> path,
        Map<GridPoint, GridPoint> parents,
        GridPoint entrance,
        GridPoint exit,
        GridPoint focus,
        List<IntEdge> graphEdges,
        boolean graphBased,
        Phase phase,
        boolean completed) {

    public MazeViewState {
        openCells = List.copyOf(openCells);
        discovered = Set.copyOf(discovered);
        visited = Set.copyOf(visited);
        deadEnds = Set.copyOf(deadEnds);
        backtracked = Set.copyOf(backtracked);
        path = Set.copyOf(path);
        parents = Map.copyOf(parents);
        graphEdges = List.copyOf(graphEdges);
    }

    public static MazeViewState empty(int rows, int columns, boolean graphBased) {
        return new MazeViewState(rows, columns, Collections.nCopies(rows * columns, false),
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Map.of(), null, null, null,
                List.of(), graphBased, Phase.IDLE, false);
    }

    public enum Phase {
        IDLE,
        GENERATING,
        SEARCHING,
        DISCOVERING,
        ENTERING,
        DEAD_END,
        BACKTRACKING,
        CONFIRMING_PATH,
        COMPLETED
    }
}
