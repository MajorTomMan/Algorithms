package com.majortom.algorithms.visualization.algorithm;

/**
 * 算法适配的数据结构分类。
 *
 * <p>它用于避免把某个算法错误地展示到不兼容的数据结构上。例如二维数组迷宫算法
 * 只能操作 {@code ArrayMaze}，不能在图迷宫结构下直接复用。</p>
 */
public enum AlgorithmStructure {

    /**
     * 数组排序结构。
     */
    ARRAY_SORT,

    /**
     * 二维数组迷宫结构。
     */
    ARRAY_MAZE,

    /**
     * 图结构迷宫。
     */
    GRAPH_MAZE,

    /**
     * AVL 树结构。
     */
    AVL_TREE,

    /**
     * 有向图结构。
     */
    DIRECTED_GRAPH
}
