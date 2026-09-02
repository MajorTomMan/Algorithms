package com.majortom.algorithms.library.structure.event;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.statistics.StatisticsContribution;

import java.util.Map;

public sealed interface TreeStructureEvent extends ExecutionEvent, StatisticsContribution
        permits TreeStructureEvent.Inserted, TreeStructureEvent.Removed,
        TreeStructureEvent.RotatedLeft, TreeStructureEvent.RotatedRight {

    record Inserted(long nodeId, Object value) implements TreeStructureEvent {
        @Override
        public Map<String, Long> metricDeltas() {
            return Map.of("nodes.inserted", 1L);
        }
    }

    record Removed(long nodeId, Object value) implements TreeStructureEvent {
        @Override
        public Map<String, Long> metricDeltas() {
            return Map.of("nodes.removed", 1L);
        }
    }

    record RotatedLeft(long rootId, long replacementId) implements TreeStructureEvent {
        @Override
        public Map<String, Long> metricDeltas() {
            return Map.of("rotations", 1L);
        }
    }

    record RotatedRight(long rootId, long replacementId) implements TreeStructureEvent {
        @Override
        public Map<String, Long> metricDeltas() {
            return Map.of("rotations", 1L);
        }
    }
}
