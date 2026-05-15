package com.majortom.algorithms.visualization.algorithm;

/**
 * 可视化层使用的算法用途分类。
 *
 * <p>同一个模块可能有多组算法下拉框，例如迷宫模块同时包含“生成”和“寻路”。
 * 控制器通过这个枚举向 {@link AlgorithmRegistry} 查询对应用途的候选算法。</p>
 */
public enum AlgorithmFamily {

    /**
     * 排序算法。
     */
    SORT,

    /**
     * 迷宫生成算法。
     */
    MAZE_GENERATOR,

    /**
     * 迷宫寻路算法。
     */
    MAZE_PATHFINDER,

    /**
     * 图遍历算法。
     */
    GRAPH_TRAVERSAL,

    /**
     * 树结构操作算法。
     */
    TREE_OPERATION
}
