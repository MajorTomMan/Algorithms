package com.majortom.algorithms.core.maze.algorithms.graph.generate;

import com.majortom.algorithms.core.maze.BaseGraphMazeAlgorithms;
import com.majortom.algorithms.core.maze.impl.GraphMaze;

public class DFSGraphMazeGenerator extends BaseGraphMazeAlgorithms<GraphMaze> {

    @Override
    public void run(GraphMaze maze) {
        maze.initial();
    }
}
