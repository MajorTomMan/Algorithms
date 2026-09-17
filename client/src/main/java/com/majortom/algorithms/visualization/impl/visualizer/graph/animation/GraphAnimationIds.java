package com.majortom.algorithms.visualization.impl.visualizer.graph.animation;

import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphVisualIds;

/** Stable logical ids shared by the graph planner and FX scene adapter. */
public final class GraphAnimationIds {
    private GraphAnimationIds() {}

    public static String node(long id) {
        return GraphVisualIds.node(id);
    }

    public static String edge(long id) {
        return GraphVisualIds.edge(id);
    }

    /** Transient identity for the old visual when a factual edge id is rewired in-place. */
    public static String rewiredExitEdge(long id) {
        return GraphVisualIds.rewiredExitEdge(id);
    }
}
