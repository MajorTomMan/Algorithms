package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.statistics.StatisticsContribution;
import com.majortom.algorithms.library.graph.IntEdge;
import com.majortom.algorithms.library.graph.IntGraph;

import java.util.Map;
import java.util.Objects;

/** Typed graph-maze construction events. */
public sealed interface GraphMazeGenerationEvent extends ExecutionEvent, StatisticsContribution
        permits GraphMazeGenerationEvent.Initialized,
        GraphMazeGenerationEvent.EdgeAdded,
        GraphMazeGenerationEvent.Completed {

    @Override
    default Map<String, Long> metricDeltas() {
        if (this instanceof EdgeAdded) {
            return Map.of("edges.added", 1L);
        }
        return Map.of();
    }

    record Initialized(int rows, int columns) implements GraphMazeGenerationEvent {
    }

    record EdgeAdded(IntEdge edge) implements GraphMazeGenerationEvent {
        public EdgeAdded {
            Objects.requireNonNull(edge, "edge");
        }
    }

    record Completed(IntGraph graph) implements GraphMazeGenerationEvent {
        public Completed {
            Objects.requireNonNull(graph, "graph");
        }
    }
}
