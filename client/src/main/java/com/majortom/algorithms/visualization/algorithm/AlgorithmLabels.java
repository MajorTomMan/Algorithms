package com.majortom.algorithms.visualization.algorithm;

import java.util.Map;

/**
 * 将核心算法的稳定 ID 映射为客户端国际化资源 key。
 */
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
            Map.entry("tree-avl", "algorithm.tree.avl"),
            Map.entry("graph-bfs", "algorithm.graph.bfs"));

    private AlgorithmLabels() {
    }

    /**
     * 返回算法 ID 对应的国际化资源 key。
     *
     * @param algorithmId 稳定算法 ID
     * @return 国际化资源 key
     * @throws IllegalArgumentException ID 未配置客户端文案时抛出
     */
    public static String key(String algorithmId) {
        String key = LABEL_KEYS.get(algorithmId);
        if (key == null) {
            throw new IllegalArgumentException("Missing client label for algorithm: " + algorithmId);
        }
        return key;
    }
}
