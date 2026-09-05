package com.majortom.algorithms.visualization.runtime.array;

import java.util.List;
import java.util.Objects;

/** Immutable JavaFX-neutral Array state derived from factual mutations and observations. */
public record ArrayViewState(List<Integer> values, Mutation mutation, Observation observation, boolean completed) {

    public ArrayViewState {
        values = List.copyOf(Objects.requireNonNull(values, "values"));
        mutation = Objects.requireNonNull(mutation, "mutation");
        observation = Objects.requireNonNull(observation, "observation");
    }

    /** Compatibility constructor for Structure-only presentation states. */
    public ArrayViewState(List<Integer> values, Mutation mutation, boolean completed) {
        this(values, mutation, Observation.none(), completed);
    }

    public static ArrayViewState source(List<Integer> values) {
        return new ArrayViewState(values, Mutation.none(), Observation.none(), false);
    }

    public record Mutation(Type type, int index, int otherIndex) {
        public Mutation {
            Objects.requireNonNull(type, "type");
        }

        public static Mutation none() {
            return new Mutation(Type.NONE, -1, -1);
        }

        public static Mutation inserted(int index) {
            return new Mutation(Type.INSERTED, index, -1);
        }

        public static Mutation removed(int index) {
            return new Mutation(Type.REMOVED, index, -1);
        }

        public static Mutation updated(int index) {
            return new Mutation(Type.UPDATED, index, -1);
        }

        public static Mutation swapped(int leftIndex, int rightIndex) {
            return new Mutation(Type.SWAPPED, leftIndex, rightIndex);
        }
    }

    /** Current factual comparison. It never changes the Array Structure itself. */
    public record Observation(ObservationType type, int firstIndex, int secondIndex, Integer comparedValue) {
        public Observation {
            Objects.requireNonNull(type, "type");
        }

        public static Observation none() {
            return new Observation(ObservationType.NONE, -1, -1, null);
        }

        public static Observation comparedIndexes(int firstIndex, int secondIndex) {
            return new Observation(ObservationType.COMPARED_INDEXES, firstIndex, secondIndex, null);
        }

        public static Observation comparedValue(int index, int value) {
            return new Observation(ObservationType.COMPARED_VALUE, index, -1, value);
        }
    }

    public enum Type {
        NONE,
        INSERTED,
        REMOVED,
        UPDATED,
        SWAPPED
    }

    public enum ObservationType {
        NONE,
        COMPARED_INDEXES,
        COMPARED_VALUE
    }
}
