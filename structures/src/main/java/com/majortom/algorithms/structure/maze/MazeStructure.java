package com.majortom.algorithms.structure.maze;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureModule;

/** Basic mutable maze data contract. */
@Structure(id = "maze", name = "Maze", module = StructureModule.MAZE, implementation = Maze.class)
public interface MazeStructure {
    MazeDimensions dimensions();

    GridMaze grid();

    void initialize(MazeDimensions dimensions);

    void initialize(GridMaze grid);
}
