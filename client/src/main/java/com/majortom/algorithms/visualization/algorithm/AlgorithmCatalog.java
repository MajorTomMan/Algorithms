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


    /** Resolves display metadata from the discovered descriptor, never from I18N or an id map. */
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

    public static String name(String moduleId, Class<?> valueType, String algorithmId) {
        return REGISTRY.requireAlgorithm(moduleId, valueType, algorithmId).name();
    }

    public static List<String> forWorkbenchModule(String moduleId) {
        return forWorkbenchModule(moduleId, Integer.class);
    }

    public static List<String> forWorkbenchModule(String moduleId, Class<?> valueType) {
        return switch (moduleId) {
            case "array" -> arraySorts(valueType);
            case "maze" -> concat(arrayMazeGenerators(), graphMazeGenerators(), arrayMazePathfinders());
            case "tree" -> generalTreeAlgorithms(valueType);
            case "graph" -> basicGraphAlgorithms(valueType);
            case "string" -> stringAlgorithms();
            default -> List.of();
        };
    }

    public static List<String> arraySorts() {
        return arraySorts(Integer.class);
    }

    public static List<String> arraySorts(Class<?> valueType) {
        return ids("array", valueType, Sort.class);
    }

    public static List<String> basicGraphAlgorithms() {
        return basicGraphAlgorithms(Integer.class);
    }

    public static List<String> basicGraphAlgorithms(Class<?> valueType) {
        return ids("graph", valueType, GraphAlgorithm.class);
    }

    public static List<String> weightedGraphAlgorithms() {
        return weightedGraphAlgorithms(Integer.class);
    }

    public static List<String> weightedGraphAlgorithms(Class<?> valueType) {
        return concat(
                ids("graph", valueType, GraphAlgorithm.class),
                ids("graph", valueType, WeightedGraphAlgorithm.class));
    }

    public static List<String> graphTraversals() {
        return graphTraversals(Integer.class);
    }

    public static List<String> graphTraversals(Class<?> valueType) {
        return ids("graph", valueType, GraphTraversal.class);
    }

    public static List<String> generalTreeAlgorithms() {
        return generalTreeAlgorithms(Integer.class);
    }

    public static List<String> generalTreeAlgorithms(Class<?> valueType) {
        return concat(
                ids("tree", valueType, TreeAlgorithm.class),
                ids("tree", valueType, GeneralTreeAlgorithm.class));
    }

    public static List<String> avlTreeAlgorithms() {
        return avlTreeAlgorithms(Integer.class);
    }

    public static List<String> avlTreeAlgorithms(Class<?> valueType) {
        return concat(
                ids("tree", valueType, TreeAlgorithm.class),
                ids("tree", valueType, BinaryTreeAlgorithm.class),
                ids("tree", valueType, SearchTreeAlgorithm.class),
                ids("tree", valueType, AvlTreeAlgorithm.class));
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
