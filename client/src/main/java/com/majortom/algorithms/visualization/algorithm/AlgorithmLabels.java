package com.majortom.algorithms.visualization.algorithm;

import com.majortom.algorithms.visualization.international.I18N;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/** Maps stable algorithm IDs to localized labels, with a readable fallback for auto-discovered algorithms. */
public final class AlgorithmLabels {

    private static final Map<String, String> LABEL_KEYS = Map.ofEntries(
            Map.entry("insertion-sort", "algorithm.sort.insertion"),
            Map.entry("selection-sort", "algorithm.sort.selection"),
            Map.entry("quick-sort", "algorithm.sort.quick"),
            Map.entry("heap-sort", "algorithm.sort.heap"),
            Map.entry("maze-generator-bfs", "algorithm.maze.generate.bfs"),
            Map.entry("maze-generator-dfs", "algorithm.maze.generate.dfs"),
            Map.entry("maze-generator-union-find", "algorithm.maze.generate.uf"),
            Map.entry("graph-generator-bfs", "algorithm.graph.generate.bfs"),
            Map.entry("maze-pathfinder-astar", "algorithm.maze.solve.astar"),
            Map.entry("maze-pathfinder-dfs", "algorithm.maze.solve.dfs"),
            Map.entry("graph-bfs", "algorithm.graph.bfs"),
            Map.entry("kruskal", "algorithm.graph.kruskal"),
            Map.entry("kruskal-minimum-spanning", "algorithm.graph.kruskal"),
            Map.entry("kmp", "algorithm.string.kmp"),
            Map.entry("longest-unique-substring", "algorithm.string.longest_unique_substring"));

    private AlgorithmLabels() {
    }

    public static String text(String algorithmId) {
        String key = LABEL_KEYS.get(algorithmId);
        if (key != null) {
            return I18N.text(key);
        }
        return Arrays.stream(algorithmId.split("-"))
                .filter(part -> !part.isBlank())
                .map(AlgorithmLabels::capitalize)
                .collect(Collectors.joining(" "));
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
