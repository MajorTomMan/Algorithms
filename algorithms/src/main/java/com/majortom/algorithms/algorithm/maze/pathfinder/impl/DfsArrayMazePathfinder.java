package com.majortom.algorithms.algorithm.maze.pathfinder.impl;

import com.majortom.algorithms.structure.maze.MazeStructure;
import com.majortom.algorithms.algorithm.maze.MazeAlgorithm;
import com.majortom.algorithms.algorithm.maze.MazeModel;
import com.majortom.algorithms.algorithm.maze.MazeRole;
import com.majortom.algorithms.algorithm.maze.ArrayMazeSupport;
import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.structure.maze.GridPoint;
import com.majortom.algorithms.structure.maze.GridMaze;
import com.majortom.algorithms.core.runtime.Observations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Recursive depth-first pathfinder with factual visit/examine/backtrack observations. */
@Algorithm(id = "maze-pathfinder-dfs", name = "深度优先搜索", type = Boolean.class, structure = MazeStructure.class)
@MazeAlgorithm(role = MazeRole.PATHFINDER, model = MazeModel.ARRAY)
public final class DfsArrayMazePathfinder {
    @AlgorithmEntry
    public List<GridPoint> findPath(MazeStructure structure) {
        GridMaze maze = structure.grid();
        if (maze == null) {
            throw new IllegalArgumentException("maze structure has no grid");
        }
        GridPoint start = maze.entrance();
        GridPoint goal = maze.exit();
        ArrayMazeSupport.requirePathEndpoints(maze, start, goal);
        Map<GridPoint, GridPoint> previous = new HashMap<>();
        Set<GridPoint> discovered = new HashSet<>();
        discovered.add(start);
        boolean found = visit(maze, start, goal, discovered, previous);
        if (!found) return List.of();
        List<GridPoint> path = ArrayMazeSupport.reconstruct(previous, start, goal);
        ArrayMazeSupport.tracePath(path);
        Observations.pathFound(path, GridPoint::row, GridPoint::column);
        return path;
    }

    private boolean visit(
            GridMaze maze,
            GridPoint current,
            GridPoint goal,
            Set<GridPoint> discovered,
            Map<GridPoint, GridPoint> previous) {
        Observations.visited(current.row(), current.column());
        if (current.equals(goal)) {
            return true;
        }
        for (GridPoint neighbor : ArrayMazeSupport.neighbors(maze, current)) {
            Observations.examined(current.row(), current.column(), neighbor.row(), neighbor.column());
            if (!discovered.add(neighbor)) {
                continue;
            }
            previous.put(neighbor, current);
            if (visit(maze, neighbor, goal, discovered, previous)) {
                return true;
            }
        }
        Observations.backtracked(current.row(), current.column());
        return false;
    }
}
