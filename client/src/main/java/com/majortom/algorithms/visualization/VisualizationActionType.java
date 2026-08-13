package com.majortom.algorithms.visualization;

/**
 * 可视化联动动作类型。
 * 职责：统一描述控制器触发给可视化层的动作语义。
 */
public enum VisualizationActionType {
    EXECUTION_START,
    EXECUTION_PAUSE,
    EXECUTION_RESUME,
    EXECUTION_RESET,
    EXECUTION_REPLAY,
    EXECUTION_EXPORT,
    EXECUTION_COMPARE,

    MODULE_SORT,
    MODULE_MAZE,
    MODULE_TREE,
    MODULE_GRAPH,
    LANGUAGE_TOGGLE,

    SORT_GENERATE,
    SORT_RUN,

    MAZE_BUILD,
    MAZE_SOLVE,

    TREE_INSERT,
    TREE_DELETE,
    TREE_RANDOM,

    GRAPH_RUN,
    GRAPH_ADD_NODE,
    GRAPH_DELETE_NODE,
    GRAPH_LINK
}
