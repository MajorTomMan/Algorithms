package com.majortom.algorithms.visualization.algorithm;

import com.majortom.algorithms.core.registry.ModuleLoader;
import com.majortom.algorithms.core.registry.ModuleRegistry;
import com.majortom.algorithms.library.graph.GraphTraversal;
import com.majortom.algorithms.library.maze.ArrayMazeGenerator;
import com.majortom.algorithms.library.maze.ArrayMazePathfinder;
import com.majortom.algorithms.library.maze.GraphMazeGenerator;
import com.majortom.algorithms.library.sort.Sort;
import com.majortom.algorithms.library.string.StringSearch;
import com.majortom.algorithms.library.tree.TreeAlgorithm;

import java.util.ArrayList;
import java.util.List;

/** Registry-backed algorithm availability used by Workbench navigation and selectors. */
public final class AlgorithmCatalog {

    private static final ModuleRegistry REGISTRY = ModuleLoader.load();

    private AlgorithmCatalog() {
    }

    public static List<String> forWorkbenchModule(String moduleId) {
        return switch (moduleId) {
            case "array" -> ids("array", "Integer", Sort.class);
            case "maze" -> concat(
                    ids("maze", "Boolean", ArrayMazeGenerator.class),
                    ids("graph", "Integer", GraphMazeGenerator.class),
                    ids("maze", "Boolean", ArrayMazePathfinder.class));
            case "tree" -> ids("tree", "Integer", TreeAlgorithm.class);
            case "graph" -> ids("graph", "Integer", GraphTraversal.class);
            case "string" -> ids("string", "String", StringSearch.class);
            default -> List.of();
        };
    }

    public static List<String> arraySorts() {
        return ids("array", "Integer", Sort.class);
    }

    public static List<String> graphTraversals() {
        return ids("graph", "Integer", GraphTraversal.class);
    }

    public static List<String> treeAlgorithms() {
        return ids("tree", "Integer", TreeAlgorithm.class);
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
        List<String> ids = new ArrayList<>();
        for (List<String> group : groups) {
            ids.addAll(group);
        }
        return List.copyOf(ids);
    }
}
