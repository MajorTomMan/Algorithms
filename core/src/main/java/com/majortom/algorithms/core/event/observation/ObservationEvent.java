package com.majortom.algorithms.core.event.observation;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.statistics.StatisticsContribution;

import java.util.Map;
import java.util.Objects;

/**
 * Small factual contract for execution observations that do not mutate a Structure.
 * Runtime metadata remains in EventEnvelope; these payloads only describe what happened.
 */
public sealed interface ObservationEvent extends ExecutionEvent, StatisticsContribution
        permits ObservationEvent.Compared, ObservationEvent.Visited, ObservationEvent.Examined,
        ObservationEvent.Matched, ObservationEvent.Fallback, ObservationEvent.Backtracked {

    /** Marker for stable references carried by observation facts. */
    sealed interface Reference permits EntityRef, IndexRef, CoordinateRef, ValueRef {
    }

    record EntityRef(String domain, long id) implements Reference {
        public EntityRef {
            domain = requireText(domain, "domain");
            if (id <= 0L) {
                throw new IllegalArgumentException("entity id must be positive");
            }
        }
    }

    record IndexRef(String source, int index) implements Reference {
        public IndexRef {
            source = requireText(source, "source");
            if (index < 0) {
                throw new IllegalArgumentException("index must not be negative");
            }
        }
    }

    record CoordinateRef(int row, int column) implements Reference {
        public CoordinateRef {
            if (row < 0 || column < 0) {
                throw new IllegalArgumentException("coordinates must not be negative");
            }
        }
    }

    record ValueRef(Object value) implements Reference {
        public ValueRef {
            Objects.requireNonNull(value, "value");
        }
    }

    record Compared(Reference leftRef, Reference rightRef) implements ObservationEvent {
        public Compared {
            Objects.requireNonNull(leftRef, "leftRef");
            Objects.requireNonNull(rightRef, "rightRef");
        }

        @Override
        public Map<String, Long> metricDeltas() {
            return Map.of("comparisons", 1L);
        }
    }

    record Visited(Reference ref) implements ObservationEvent {
        public Visited {
            Objects.requireNonNull(ref, "ref");
        }

        @Override
        public Map<String, Long> metricDeltas() {
            return Map.of("nodesVisited", 1L);
        }
    }

    record Examined(Reference fromRef, Reference toRef) implements ObservationEvent {
        public Examined {
            Objects.requireNonNull(fromRef, "fromRef");
            Objects.requireNonNull(toRef, "toRef");
        }

        @Override
        public Map<String, Long> metricDeltas() {
            return Map.of("edgesExamined", 1L);
        }
    }

    record Matched(int index, int length) implements ObservationEvent {
        public Matched {
            if (index < 0) {
                throw new IllegalArgumentException("match index must not be negative");
            }
            if (length <= 0) {
                throw new IllegalArgumentException("match length must be positive");
            }
        }

        @Override
        public Map<String, Long> metricDeltas() {
            return Map.of("matches", 1L);
        }
    }

    record Fallback(int fromIndex, int toIndex) implements ObservationEvent {
        public Fallback {
            if (fromIndex < 0 || toIndex < 0) {
                throw new IllegalArgumentException("fallback indexes must not be negative");
            }
            if (toIndex > fromIndex) {
                throw new IllegalArgumentException("fallback target must not move forward");
            }
        }

        @Override
        public Map<String, Long> metricDeltas() {
            return Map.of("fallbacks", 1L);
        }
    }

    record Backtracked(Reference ref) implements ObservationEvent {
        public Backtracked {
            Objects.requireNonNull(ref, "ref");
        }

        @Override
        public Map<String, Long> metricDeltas() {
            return Map.of("backtracks", 1L);
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
