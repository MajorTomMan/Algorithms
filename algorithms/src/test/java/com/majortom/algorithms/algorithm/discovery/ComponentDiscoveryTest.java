package com.majortom.algorithms.algorithm.discovery;

import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.core.registry.RegistrationException;
import com.majortom.algorithms.core.registry.StructureResolver;
import com.majortom.algorithms.structure.array.Array;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentDiscoveryTest {
    private final ComponentRegistry registry = ComponentDiscovery.discover(getClass().getClassLoader());

    @Test
    void discoversStringArrayAlgorithm() {
        var descriptor = registry.requireAlgorithm("string-insertion-sort");
        assertEquals(String.class, descriptor.valueType());
        assertEquals("array", descriptor.moduleId());
        assertTrue(descriptor.hasStructureContract());
    }

    @Test
    void integerAndStringAlgorithmsResolveTheSameGenericArrayImplementation() {
        StructureResolver resolver = new StructureResolver(registry);
        var integerStructure = resolver.resolve(registry.requireAlgorithm("insertion-sort"));
        var stringStructure = resolver.resolve(registry.requireAlgorithm("string-insertion-sort"));
        assertEquals(Array.class, integerStructure.implementation());
        assertEquals(integerStructure.implementation(), stringStructure.implementation());
    }

    @Test
    void mazeAlgorithmsAreDiscoverableWithoutInventingAMazeStructureContract() {
        var maze = registry.requireAlgorithm("maze-generator-bfs");
        assertFalse(maze.hasStructureContract());
        assertThrows(RegistrationException.class, () -> new StructureResolver(registry).resolve(maze));
    }
}
