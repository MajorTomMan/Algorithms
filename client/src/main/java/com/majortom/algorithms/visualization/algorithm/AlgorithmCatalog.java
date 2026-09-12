package com.majortom.algorithms.visualization.algorithm;

import com.majortom.algorithms.algorithm.array.sort.Sort;
import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.algorithm.graph.GraphAlgorithm;
import com.majortom.algorithms.algorithm.graph.GraphTraversal;
import com.majortom.algorithms.algorithm.graph.WeightedGraphAlgorithm;
import com.majortom.algorithms.algorithm.maze.ArrayMazeGenerator;
import com.majortom.algorithms.algorithm.maze.ArrayMazePathfinder;
import com.majortom.algorithms.algorithm.maze.GraphMazeGenerator;
import com.majortom.algorithms.algorithm.string.StringAlgorithm;
import com.majortom.algorithms.algorithm.string.StringSearch;
import com.majortom.algorithms.algorithm.tree.AvlTreeAlgorithm;
import com.majortom.algorithms.algorithm.tree.BinaryTreeAlgorithm;
import com.majortom.algorithms.algorithm.tree.GeneralTreeAlgorithm;
import com.majortom.algorithms.algorithm.tree.SearchTreeAlgorithm;
import com.majortom.algorithms.algorithm.tree.TreeAlgorithm;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.registry.ComponentRegistry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AlgorithmCatalog {

    private static final ComponentRegistry REGISTRY = ComponentDiscovery.discover();

    private AlgorithmCatalog() {
    }

    public static List<String> forWorkbenchModule(String moduleId) {
        return switch (moduleId) {
            case "array" -> arraySorts();
            case "maze" -> concat(arrayMazeGenerators(), graphMazeGenerators(), arrayMazePathfinders());
            case "tree" -> generalTreeAlgorithms();
            case "graph" -> basicGraphAlgorithms();
            case "string" -> stringAlgorithms();
            default -> List.of();
        };
    }

    public static List<String> arraySorts() {
        return ids("array", Integer.class, Sort.class);
    }

    public static List<String> basicGraphAlgorithms() {
        return ids("graph", Integer.class, GraphAlgorithm.class);
    }

    public static List<String> weightedGraphAlgorithms() {
        return concat(
                ids("graph", Integer.class, GraphAlgorithm.class),
                ids("graph", Integer.class, WeightedGraphAlgorithm.class));
    }

    public static List<String> graphTraversals() {
        return ids("graph", Integer.class, GraphTraversal.class);
    }

    public static List<String> generalTreeAlgorithms() {
        return concat(
                ids("tree", Integer.class, TreeAlgorithm.class),
                ids("tree", Integer.class, GeneralTreeAlgorithm.class));
    }

    public static List<String> avlTreeAlgorithms() {
        return concat(
                ids("tree", Integer.class, TreeAlgorithm.class),
                ids("tree", Integer.class, BinaryTreeAlgorithm.class),
                ids("tree", Integer.class, SearchTreeAlgorithm.class),
                ids("tree", Integer.class, AvlTreeAlgorithm.class));
    }

    public static List<String> treeAlgorithms() {
        return generalTreeAlgorithms();
    }

    public static List<String> stringAlgorithms() {
        return ids("string", java.lang.String.class, StringAlgorithm.class);
    }

    public static List<String> stringSearches() {
        return ids("string", java.lang.String.class, StringSearch.class);
    }

    public static List<String> arrayMazeGenerators() {
        return ids("maze", Boolean.class, ArrayMazeGenerator.class);
    }

    public static List<String> graphMazeGenerators() {
        return ids("maze", Integer.class, GraphMazeGenerator.class);
    }

    public static List<String> arrayMazePathfinders() {
        return ids("maze", Boolean.class, ArrayMazePathfinder.class);
    }

    private static List<String> ids(String moduleId, Class<?> valueType, Class<?> contract) {
        return REGISTRY.algorithms(moduleId, valueType).stream()
                .filter(descriptor -> contract.isAssignableFrom(descriptor.implementation()))
                .map(AlgorithmDescriptor::id)
                .toList();
    }

    @SafeVarargs
    private static List<String> concat(List<String>... groups) {
        Set<String> ids = new LinkedHashSet<>();
        for (List<String> group : groups) {
            ids.addAll(group);
        }
        return List.copyOf(ids);
    }
}
