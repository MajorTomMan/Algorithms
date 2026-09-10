package com.majortom.algorithms.visualization.algorithm;

import com.majortom.algorithms.core.registry.ModuleLoader;
import com.majortom.algorithms.core.registry.ModuleRegistry;
import com.majortom.algorithms.algorithm.graph.GraphAlgorithm;
import com.majortom.algorithms.algorithm.graph.GraphTraversal;
import com.majortom.algorithms.algorithm.graph.WeightedGraphAlgorithm;
import com.majortom.algorithms.algorithm.maze.ArrayMazeGenerator;
import com.majortom.algorithms.algorithm.maze.ArrayMazePathfinder;
import com.majortom.algorithms.algorithm.maze.GraphMazeGenerator;
import com.majortom.algorithms.algorithm.array.sort.Sort;
import com.majortom.algorithms.algorithm.string.StringAlgorithm;
import com.majortom.algorithms.algorithm.string.StringSearch;
import com.majortom.algorithms.algorithm.tree.AvlTreeAlgorithm;
import com.majortom.algorithms.algorithm.tree.BinaryTreeAlgorithm;
import com.majortom.algorithms.algorithm.tree.GeneralTreeAlgorithm;
import com.majortom.algorithms.algorithm.tree.SearchTreeAlgorithm;
import com.majortom.algorithms.algorithm.tree.TreeAlgorithm;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Registry-backed algorithm availability used by Workbench navigation and selectors. */
public final class AlgorithmCatalog {

    private static final ModuleRegistry REGISTRY = ModuleLoader.load();

    private AlgorithmCatalog() {
    }

    public static List<String> forWorkbenchModule(String moduleId) {
        return switch (moduleId) {
            case "array" -> arraySorts();
            case "maze" -> concat(
                    arrayMazeGenerators(),
                    graphMazeGenerators(),
                    arrayMazePathfinders());
            case "tree" -> generalTreeAlgorithms();
            case "graph" -> basicGraphAlgorithms();
            case "string" -> stringAlgorithms();
            default -> List.of();
        };
    }

    public static List<String> arraySorts() {
        return ids("array", "Integer", Sort.class);
    }

    public static List<String> basicGraphAlgorithms() {
        return ids("graph", "Integer", GraphAlgorithm.class);
    }

    public static List<String> weightedGraphAlgorithms() {
        return concat(
                ids("graph", "Integer", GraphAlgorithm.class),
                ids("graph", "Integer", WeightedGraphAlgorithm.class));
    }

    public static List<String> graphTraversals() {
        return ids("graph", "Integer", GraphTraversal.class);
    }

    public static List<String> generalTreeAlgorithms() {
        return concat(
                ids("tree", "Integer", TreeAlgorithm.class),
                ids("tree", "Integer", GeneralTreeAlgorithm.class));
    }

    public static List<String> avlTreeAlgorithms() {
        return concat(
                ids("tree", "Integer", TreeAlgorithm.class),
                ids("tree", "Integer", BinaryTreeAlgorithm.class),
                ids("tree", "Integer", SearchTreeAlgorithm.class),
                ids("tree", "Integer", AvlTreeAlgorithm.class));
    }

    public static List<String> treeAlgorithms() {
        return generalTreeAlgorithms();
    }

    public static List<String> stringAlgorithms() {
        return ids("string", "String", StringAlgorithm.class);
    }

    public static List<String> stringSearches() {
        return ids("string", "String", StringSearch.class);
    }

    public static List<String> arrayMazeGenerators() {
        return ids("maze", "Boolean", ArrayMazeGenerator.class);
    }

    public static List<String> graphMazeGenerators() {
        return ids("graph", "Integer", GraphMazeGenerator.class);
    }

    public static List<String> arrayMazePathfinders() {
        return ids("maze", "Boolean", ArrayMazePathfinder.class);
    }

    private static List<String> ids(String family, String valueType, Class<?> contract) {
        String prefix = "algorithm." + family + "." + valueType + ".";
        return REGISTRY.keys(prefix).stream()
                .filter(key -> contract.isAssignableFrom(REGISTRY.require(key)))
                .map(key -> key.substring(prefix.length()))
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
