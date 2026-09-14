package com.majortom.algorithms.visualization.algorithm;

import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.structure.graph.GraphStructure;
import com.majortom.algorithms.structure.graph.WeightedGraphStructure;
import com.majortom.algorithms.structure.maze.GridMaze;
import com.majortom.algorithms.structure.maze.GridPoint;
import com.majortom.algorithms.structure.maze.MazeDimensions;
import com.majortom.algorithms.structure.string.StringStructure;
import com.majortom.algorithms.structure.tree.AvlTreeStructure;
import com.majortom.algorithms.structure.tree.GeneralTreeStructure;

import java.util.List;

public final class AlgorithmCatalog {

    private static final ComponentRegistry REGISTRY = ComponentDiscovery.discover();

    private AlgorithmCatalog() {
    }

    public static String name(String algorithmId) {
        List<AlgorithmDescriptor> matches = REGISTRY.algorithms().stream()
                .filter(descriptor -> descriptor.id().equals(algorithmId))
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No Algorithm registered for id: " + algorithmId);
        }
        List<String> names = matches.stream().map(AlgorithmDescriptor::name).distinct().toList();
        if (names.size() != 1) {
            throw new IllegalArgumentException("Algorithm id has multiple display names across registrations: "
                    + algorithmId + " -> " + names);
        }
        return names.getFirst();
    }

    public static AlgorithmDescriptor descriptor(String moduleId, Class<?> valueType, String algorithmId) {
        return REGISTRY.requireAlgorithm(StructureModule.fromId(moduleId), valueType, algorithmId);
    }

    public static AlgorithmDescriptor compatibleDescriptor(
            Class<?> activeStructure, Class<?> valueType, String algorithmId) {
        List<AlgorithmDescriptor> matches = REGISTRY.compatibleAlgorithms(activeStructure, valueType).stream()
                .filter(descriptor -> descriptor.id().equals(algorithmId))
                .toList();
        if (matches.size() != 1) {
            throw new IllegalArgumentException("Expected exactly one compatible Algorithm for structure="
                    + activeStructure.getName() + ", type=" + valueType.getName() + ", id=" + algorithmId
                    + ", found=" + matches.size());
        }
        return matches.getFirst();
    }

    public static String name(String moduleId, Class<?> valueType, String algorithmId) {
        return descriptor(moduleId, valueType, algorithmId).name();
    }

    public static List<String> forWorkbenchModule(String moduleId) {
        StructureModule module = StructureModule.fromId(moduleId);
        return REGISTRY.algorithms().stream()
                .filter(descriptor -> descriptor.module() == module)
                .map(AlgorithmDescriptor::id)
                .distinct()
                .toList();
    }

    public static List<String> forWorkbenchModule(String moduleId, Class<?> valueType) {
        return ids(StructureModule.fromId(moduleId), valueType);
    }

    public static List<String> stackAlgorithms(Class<?> valueType) { return ids(StructureModule.STACK, valueType); }
    public static List<String> queueAlgorithms(Class<?> valueType) { return ids(StructureModule.QUEUE, valueType); }
    public static List<String> linkedListAlgorithms(Class<?> valueType) { return ids(StructureModule.LINKED_LIST, valueType); }
    public static List<String> arraySorts(Class<?> valueType) { return ids(StructureModule.ARRAY, valueType); }

    public static List<String> basicGraphAlgorithms(Class<?> valueType) {
        return compatible(GraphStructure.class, valueType);
    }

    public static List<String> weightedGraphAlgorithms(Class<?> valueType) {
        return compatible(WeightedGraphStructure.class, valueType);
    }

    public static List<String> graphTraversals(Class<?> valueType) {
        return REGISTRY.algorithms(StructureModule.GRAPH, valueType).stream()
                .filter(descriptor -> descriptor.entryPoint().getParameterCount() == 2)
                .filter(descriptor -> GraphStructure.class.isAssignableFrom(descriptor.entryPoint().getParameterTypes()[0]))
                .filter(descriptor -> !WeightedGraphStructure.class.equals(descriptor.structureContract()))
                .map(AlgorithmDescriptor::id)
                .toList();
    }

    public static List<String> generalTreeAlgorithms(Class<?> valueType) {
        return compatible(GeneralTreeStructure.class, valueType);
    }

    public static List<String> avlTreeAlgorithms(Class<?> valueType) {
        return compatible(AvlTreeStructure.class, valueType);
    }

    public static List<String> treeAlgorithms() {
        return ids(StructureModule.TREE, Integer.class);
    }

    public static List<String> stringAlgorithms() {
        return ids(StructureModule.STRING, java.lang.String.class);
    }

    public static List<String> stringSearches() {
        return REGISTRY.algorithms(StructureModule.STRING, java.lang.String.class).stream()
                .filter(descriptor -> signature(descriptor, List.class, StringStructure.class, java.lang.String.class))
                .map(AlgorithmDescriptor::id)
                .toList();
    }

    public static List<String> arrayMazeGenerators() {
        return REGISTRY.algorithms(StructureModule.MAZE, Boolean.class).stream()
                .filter(descriptor -> signature(descriptor, GridMaze.class, MazeDimensions.class, long.class))
                .map(AlgorithmDescriptor::id)
                .toList();
    }

    public static List<String> graphMazeGenerators() {
        return REGISTRY.algorithms(StructureModule.MAZE, Integer.class).stream()
                .filter(descriptor -> signature(descriptor, GraphSnapshot.class, MazeDimensions.class, long.class))
                .map(AlgorithmDescriptor::id)
                .toList();
    }

    public static List<String> arrayMazePathfinders() {
        return REGISTRY.algorithms(StructureModule.MAZE, Boolean.class).stream()
                .filter(descriptor -> signature(descriptor, List.class, GridMaze.class, GridPoint.class, GridPoint.class))
                .map(AlgorithmDescriptor::id)
                .toList();
    }

    private static List<String> ids(StructureModule module, Class<?> valueType) {
        return REGISTRY.algorithms(module, valueType).stream().map(AlgorithmDescriptor::id).distinct().toList();
    }

    private static List<String> compatible(Class<?> activeStructure, Class<?> valueType) {
        return REGISTRY.compatibleAlgorithms(activeStructure, valueType).stream()
                .map(AlgorithmDescriptor::id)
                .distinct()
                .toList();
    }

    private static boolean signature(AlgorithmDescriptor descriptor, Class<?> returnType, Class<?>... parameters) {
        if (!returnType.isAssignableFrom(descriptor.entryPoint().getReturnType())) {
            return false;
        }
        Class<?>[] actual = descriptor.entryPoint().getParameterTypes();
        if (actual.length != parameters.length) {
            return false;
        }
        for (int index = 0; index < actual.length; index++) {
            if (!actual[index].equals(parameters[index])) {
                return false;
            }
        }
        return true;
    }
}
