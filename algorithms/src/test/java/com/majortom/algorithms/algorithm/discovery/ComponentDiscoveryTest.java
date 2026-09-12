package com.majortom.algorithms.algorithm.discovery;

import com.majortom.algorithms.algorithm.array.sort.Sort;
import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.core.registry.RegistrationException;
import com.majortom.algorithms.core.registry.StructureResolver;
import com.majortom.algorithms.structure.array.Array;
import com.majortom.algorithms.structure.maze.GridMaze;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentDiscoveryTest {
    private final ComponentRegistry registry = ComponentDiscovery.discover(getClass().getClassLoader());

    @Test
    void discoversIntegerAndStringVariantsUnderTheSameStableId() {
        var integer = registry.requireAlgorithm("array", Integer.class, "insertion-sort");
        var string = registry.requireAlgorithm("array", String.class, "insertion-sort");

        assertEquals("insertion-sort", integer.id());
        assertEquals(integer.id(), string.id());
        assertEquals(Integer.class, integer.valueType());
        assertEquals(String.class, string.valueType());
        assertEquals("array", string.moduleId());
        assertTrue(string.hasStructureContract());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sameStableIdExecutesTheCorrectValueTypeVariant() {
        Sort<Integer> integerSort = (Sort<Integer>) registry.createAlgorithm(
                "array", Integer.class, "insertion-sort", Sort.class);
        Sort<String> stringSort = (Sort<String>) registry.createAlgorithm(
                "array", String.class, "insertion-sort", Sort.class);
        Array<Integer> integers = new Array<>(List.of(3, 1, 2));
        Array<String> strings = new Array<>(List.of("c", "a", "b"));

        integerSort.sort(integers);
        stringSort.sort(strings);

        assertEquals(List.of(1, 2, 3), values(integers));
        assertEquals(List.of("a", "b", "c"), values(strings));
        assertEquals(List.of("insertion-sort"), registry.algorithms("array", String.class).stream()
                .map(descriptor -> descriptor.id())
                .toList());
    }

    @Test
    void integerAndStringAlgorithmsResolveTheSameGenericArrayImplementation() {
        StructureResolver resolver = new StructureResolver(registry);
        var integerStructure = resolver.resolve(
                registry.requireAlgorithm("array", Integer.class, "insertion-sort"));
        var stringStructure = resolver.resolve(
                registry.requireAlgorithm("array", String.class, "insertion-sort"));
        assertEquals(Array.class, integerStructure.implementation());
        assertEquals(integerStructure.implementation(), stringStructure.implementation());
    }

    @Test
    void mazeAlgorithmsAreDiscoverableWithoutInventingAMazeStructureContract() {
        var maze = registry.requireAlgorithm("maze", Boolean.class, "maze-generator-bfs");
        assertFalse(maze.hasStructureContract());
        assertTrue(registry.structures().stream()
                .noneMatch(descriptor -> descriptor.implementation().equals(GridMaze.class)));
        assertThrows(RegistrationException.class, () -> new StructureResolver(registry).resolve(maze));
    }
    private static <T> List<T> values(Array<T> array) {
        return IntStream.range(0, array.size()).mapToObj(array::get).toList();
    }

}
