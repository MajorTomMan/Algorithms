package com.majortom.algorithms.algorithm.discovery;

import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.core.snapshot.WeightedGraphSnapshot;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.graph.WeightedGraph;
import com.majortom.algorithms.structure.maze.GridMaze;
import com.majortom.algorithms.structure.maze.Maze;
import com.majortom.algorithms.structure.maze.MazeDimensions;
import com.majortom.algorithms.structure.maze.MazeStructure;
import com.majortom.algorithms.structure.string.StringStructure;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleStructureAlgorithmContractTest {
    private final ComponentRegistry registry = ComponentDiscovery.discover(getClass().getClassLoader());

    @Test
    void everyAlgorithmEntryAcceptsExactlyItsDeclaredStructureContract() {
        for (var descriptor : registry.algorithms()) {
            assertEquals(1, descriptor.entryPoint().getParameterCount(), descriptor.id());
            Class<?> parameter = descriptor.entryPoint().getParameterTypes()[0];
            assertTrue(parameter.isAssignableFrom(descriptor.structureContract()), descriptor.id());
        }
    }

    @Test
    void structuresExposeNoAlgorithmSpecificConfiguration() {
        Set<String> forbidden = Set.of(
                "traversalStart", "setTraversalStart",
                "searchPattern", "setSearchPattern",
                "generationSeed", "setGenerationSeed",
                "pathStart", "pathGoal", "setPathEndpoints");
        for (Class<?> structure : List.of(
                com.majortom.algorithms.structure.graph.GraphStructure.class,
                StringStructure.class,
                MazeStructure.class)) {
            for (Method method : structure.getMethods()) {
                assertTrue(!forbidden.contains(method.getName()), structure.getName() + "#" + method.getName());
            }
        }
    }

    @Test
    void graphBfsChoosesItsOwnStart() {
        Graph<Integer> graph = new Graph<>(false);
        var one = graph.addVertex(1);
        var two = graph.addVertex(2);
        graph.addEdge(one, two);
        var descriptor = registry.requireAlgorithm(StructureModule.GRAPH, Integer.class, "graph-bfs");
        assertEquals(List.of(1, 2), descriptor.invoke(graph));
    }

    @Test
    void kmpOwnsItsPatternPolicy() {
        com.majortom.algorithms.structure.string.String text =
                new com.majortom.algorithms.structure.string.String("ABABDABACDABABCABAB");
        var descriptor = registry.requireAlgorithm(StructureModule.STRING, java.lang.String.class, "kmp");
        assertEquals(List.of(10), descriptor.invoke(text));
    }

    @Test
    void mazeGeneratorOwnsItsRandomnessWhileDimensionsRemainStructureData() {
        Maze maze = new Maze(new MazeDimensions(11, 11));
        var descriptor = registry.requireAlgorithm(StructureModule.MAZE, Boolean.class, "maze-generator-bfs");
        GridMaze generated = assertInstanceOf(GridMaze.class, descriptor.invoke(maze));
        assertEquals(11, generated.rows());
        assertEquals(11, generated.columns());
    }

    @Test
    void kruskalReturnsResultInsteadOfRequiringSecondStructureInput() {
        WeightedGraph<Integer> graph = new WeightedGraph<>(false);
        var one = graph.addVertex(1);
        var two = graph.addVertex(2);
        var three = graph.addVertex(3);
        graph.addEdge(one, two, 4.0d);
        graph.addEdge(one, three, 1.0d);
        graph.addEdge(two, three, 2.0d);

        var descriptor = registry.requireAlgorithm(StructureModule.GRAPH, Integer.class, "kruskal-minimum-spanning");
        WeightedGraphSnapshot<?> result = assertInstanceOf(WeightedGraphSnapshot.class, descriptor.invoke(graph));
        assertEquals(3, result.vertices().size());
        assertEquals(2, result.edges().size());
    }
}
