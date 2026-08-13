package com.majortom.algorithms.visualization.runtime.graph;

import com.majortom.algorithms.library.graph.IntEdge;
import com.majortom.algorithms.library.graph.IntGraph;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable graph state with traversal parent and examined-edge context. */
public record GraphViewState(
        IntGraph graph,
        Set<Integer> discovered,
        Set<Integer> entered,
        List<Integer> visited,
        Map<Integer, Integer> parents,
        Integer focus,
        IntEdge examinedEdge,
        Phase phase,
        boolean completed) {

    public GraphViewState {
        discovered = Set.copyOf(discovered);
        entered = Set.copyOf(entered);
        visited = List.copyOf(visited);
        parents = Map.copyOf(parents);
    }

    public static GraphViewState initial(IntGraph graph) {
        return new GraphViewState(graph, Set.of(), Set.of(), List.of(), Map.of(), null, null,
                Phase.IDLE, false);
    }

    public enum Phase {
        IDLE,
        INITIALIZED,
        DISCOVERING,
        ENTERING,
        EXAMINING_EDGE,
        VISITING,
        COMPLETED
    }
}
