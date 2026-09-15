package com.majortom.algorithms.algorithm.discovery;

import com.majortom.algorithms.algorithm.maze.MazeAlgorithm;
import com.majortom.algorithms.algorithm.maze.MazeModel;
import com.majortom.algorithms.algorithm.maze.MazeRole;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.core.registry.StructureResolver;
import com.majortom.algorithms.structure.array.Array;
import com.majortom.algorithms.structure.array.ArrayStructure;
import com.majortom.algorithms.structure.maze.GridMaze;
import com.majortom.algorithms.structure.maze.Maze;
import com.majortom.algorithms.structure.maze.MazeStructure;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentDiscoveryTest {
    private final ComponentRegistry registry = ComponentDiscovery.discover(getClass().getClassLoader());

    @Test
    void discoversIntegerAndStringVariantsUnderTheSameStableId() {
        var integer = registry.requireAlgorithm(StructureModule.ARRAY, Integer.class, "insertion-sort");
        var string = registry.requireAlgorithm(StructureModule.ARRAY, String.class, "insertion-sort");

        assertEquals("insertion-sort", integer.id());
        assertEquals("Insertion Sort", integer.name());
        assertEquals(integer.name(), string.name());
        assertEquals(integer.id(), string.id());
        assertEquals(Integer.class, integer.valueType());
        assertEquals(String.class, string.valueType());
        assertEquals(StructureModule.ARRAY, string.module());
        assertEquals(ArrayStructure.class, string.structureContract());
        assertEquals("sort", string.entryPoint().getName());
    }

    @Test
    void sameStableIdExecutesTheCorrectValueTypeVariant() {
        var integerSort = registry.requireAlgorithm(StructureModule.ARRAY, Integer.class, "insertion-sort");
        var stringSort = registry.requireAlgorithm(StructureModule.ARRAY, String.class, "insertion-sort");
        Array<Integer> integers = new Array<>(List.of(3, 1, 2));
        Array<String> strings = new Array<>(List.of("c", "a", "b"));

        integerSort.invoke(integers);
        stringSort.invoke(strings);

        assertEquals(List.of(1, 2, 3), values(integers));
        assertEquals(List.of("a", "b", "c"), values(strings));
        assertEquals(List.of("insertion-sort"), registry.algorithms(StructureModule.ARRAY, String.class).stream()
                .map(descriptor -> descriptor.id())
                .toList());
    }

    @Test
    void integerAndStringAlgorithmsResolveTheSameGenericArrayImplementation() {
        StructureResolver resolver = new StructureResolver(registry);
        var integerStructure = resolver.resolve(
                registry.requireAlgorithm(StructureModule.ARRAY, Integer.class, "insertion-sort"));
        var stringStructure = resolver.resolve(
                registry.requireAlgorithm(StructureModule.ARRAY, String.class, "insertion-sort"));
        assertEquals(Array.class, integerStructure.implementation());
        assertEquals(integerStructure.implementation(), stringStructure.implementation());
    }

    @Test
    void mazeAlgorithmsDeriveModuleFromMetadataOnlyMazeContract() {
        var maze = registry.requireAlgorithm(StructureModule.MAZE, Boolean.class, "maze-generator-bfs");
        assertEquals(StructureModule.MAZE, maze.module());
        assertEquals(MazeStructure.class, maze.structureContract());
        assertTrue(registry.structures().stream()
                .noneMatch(descriptor -> descriptor.implementation().equals(GridMaze.class)));
        assertInstanceOf(Maze.class, new StructureResolver(registry).create(MazeStructure.class));
    }

    @Test
    void mazeGeneratorAndPathfinderUseMazeDomainMetadataWithoutChangingInvocationContract() {
        var mazeAlgorithms = registry.algorithms().stream()
                .filter(descriptor -> descriptor.module() == StructureModule.MAZE)
                .toList();
        assertTrue(mazeAlgorithms.stream()
                .allMatch(descriptor -> descriptor.implementation().getAnnotation(MazeAlgorithm.class) != null));

        var generator = registry.requireAlgorithm(StructureModule.MAZE, Boolean.class, "maze-generator-bfs");
        var pathfinder = registry.requireAlgorithm(StructureModule.MAZE, Boolean.class, "maze-pathfinder-astar");

        MazeAlgorithm generatorMetadata = generator.implementation().getAnnotation(MazeAlgorithm.class);
        MazeAlgorithm pathfinderMetadata = pathfinder.implementation().getAnnotation(MazeAlgorithm.class);
        assertEquals(MazeRole.GENERATOR, generatorMetadata.role());
        assertEquals(MazeModel.ARRAY, generatorMetadata.model());
        assertEquals(MazeRole.PATHFINDER, pathfinderMetadata.role());
        assertEquals(MazeModel.ARRAY, pathfinderMetadata.model());
        assertEquals(1, generator.entryPoint().getParameterCount());
        assertEquals(1, pathfinder.entryPoint().getParameterCount());
        assertEquals(MazeStructure.class, generator.entryPoint().getParameterTypes()[0]);
        assertEquals(MazeStructure.class, pathfinder.entryPoint().getParameterTypes()[0]);
    }

    private static <T> List<T> values(Array<T> array) {
        return IntStream.range(0, array.size()).mapToObj(array::get).toList();
    }
}
