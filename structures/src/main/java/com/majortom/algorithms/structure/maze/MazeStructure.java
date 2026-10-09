package com.majortom.algorithms.structure.maze;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.StructureModule;

/** Basic mutable maze data contract. */
@Structure(id = StructureIds.MAZE, name = "Maze", module = StructureModule.MAZE, implementation = Maze.class)
public interface MazeStructure {
  MazeDimensions dimensions();
  GridMaze grid();
  void initialize(MazeDimensions dimensions);
  void initialize(GridMaze grid);

  /** Resolves the legacy fixed endpoint pair for the current generation input. */
  default MazeEndpoints generationEndpoints() {
    MazeDimensions dimensions = dimensions();
    return new MazeEndpoints(
        new GridPoint(1, 1), new GridPoint(dimensions.rows() - 2, dimensions.columns() - 2));
  }
}
