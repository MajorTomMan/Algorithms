package com.majortom.algorithms.visualization.algorithm;

import com.majortom.algorithms.algorithm.maze.MazeModel;
import com.majortom.algorithms.algorithm.maze.MazeRole;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MazeAlgorithmCatalogTest {

    @Test
    void classifiesMazeAlgorithmsOnlyByMazeDomainMetadata() {
        assertEquals(Set.of("maze-generator-bfs", "maze-generator-dfs", "maze-generator-union-find"),
                Set.copyOf(MazeAlgorithmCatalog.ids(MazeRole.GENERATOR, MazeModel.ARRAY)));
        assertEquals(Set.of("graph-generator-bfs"),
                Set.copyOf(MazeAlgorithmCatalog.ids(MazeRole.GENERATOR, MazeModel.GRAPH)));
        assertEquals(Set.of("maze-pathfinder-astar", "maze-pathfinder-dfs"),
                Set.copyOf(MazeAlgorithmCatalog.ids(MazeRole.PATHFINDER, MazeModel.ARRAY)));
        assertEquals(Set.of(),
                Set.copyOf(MazeAlgorithmCatalog.ids(MazeRole.PATHFINDER, MazeModel.GRAPH)));
    }
}
