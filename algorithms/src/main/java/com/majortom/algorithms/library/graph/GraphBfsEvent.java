package com.majortom.algorithms.library.graph;

import com.majortom.algorithms.core.api.AlgorithmEvent;
import com.majortom.algorithms.core.api.StatisticsContribution;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Typed BFS events sufficient to replay traversal state. */
public sealed interface GraphBfsEvent extends AlgorithmEvent, StatisticsContribution
        permits GraphBfsEvent.Initialized, GraphBfsEvent.Discovered,
        GraphBfsEvent.Entered, GraphBfsEvent.EdgeExamined,
        GraphBfsEvent.Visited, GraphBfsEvent.Completed {

    @Override
    default Map<String, Long> metricDeltas() {
        if (this instanceof Discovered) {
            return Map.of("nodes.discovered", 1L);
        }
        if (this instanceof Entered) {
            return Map.of("nodes.entered", 1L);
        }
        if (this instanceof EdgeExamined) {
            return Map.of("edges.examined", 1L);
        }
        if (this instanceof Visited) {
            return Map.of("nodes.visited", 1L);
        }
        return Map.of();
    }

    record Initialized(IntGraph graph, int startNode) implements GraphBfsEvent {
        public Initialized {
            Objects.requireNonNull(graph, "graph");
        }
    }

    record Discovered(int node, Integer parent) implements GraphBfsEvent {
    }

    record Entered(int node, Integer parent) implements GraphBfsEvent {
    }

    record EdgeExamined(int from, int to) implements GraphBfsEvent {
    }

    record Visited(int node) implements GraphBfsEvent {
    }

    record Completed(List<Integer> visitOrder) implements GraphBfsEvent {
        public Completed {
            Objects.requireNonNull(visitOrder, "visitOrder");
            visitOrder = List.copyOf(visitOrder);
        }
    }
}
