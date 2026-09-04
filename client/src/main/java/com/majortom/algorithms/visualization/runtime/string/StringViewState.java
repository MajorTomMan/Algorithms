package com.majortom.algorithms.visualization.runtime.string;

import java.util.Objects;

/** Immutable JavaFX-neutral String state and current factual execution observation. */
public record StringViewState(String value, Mutation mutation, Observation observation, boolean completed) {

    public StringViewState {
        value = value == null ? "" : value;
        mutation = Objects.requireNonNull(mutation, "mutation");
        observation = Objects.requireNonNull(observation, "observation");
    }

    public static StringViewState source(String value) {
        return new StringViewState(value, Mutation.none(), Observation.none(), false);
    }

    public record Mutation(Type type, int index, int length) {
        public Mutation {
            Objects.requireNonNull(type, "type");
        }

        public static Mutation none() {
            return new Mutation(Type.NONE, -1, 0);
        }

        public static Mutation inserted(int index, int length) {
            return new Mutation(Type.INSERTED, index, length);
        }

        public static Mutation removed(int index, int length) {
            return new Mutation(Type.REMOVED, index, length);
        }

        public static Mutation updated(int index) {
            return new Mutation(Type.UPDATED, index, 1);
        }

        public static Mutation replaced(int index, int length) {
            return new Mutation(Type.REPLACED, index, length);
        }
    }

    public record Observation(ObservationType type, int firstIndex, int secondIndex, int length) {
        public Observation {
            Objects.requireNonNull(type, "type");
        }

        public static Observation none() {
            return new Observation(ObservationType.NONE, -1, -1, 0);
        }

        public static Observation compared(int targetIndex, int patternIndex) {
            return new Observation(ObservationType.COMPARED, targetIndex, patternIndex, 1);
        }

        public static Observation matched(int index, int length) {
            return new Observation(ObservationType.MATCHED, index, -1, length);
        }

        public static Observation fallback(int fromIndex, int toIndex) {
            return new Observation(ObservationType.FALLBACK, fromIndex, toIndex, 0);
        }
    }

    public enum Type {
        NONE,
        INSERTED,
        REMOVED,
        UPDATED,
        REPLACED
    }

    public enum ObservationType {
        NONE,
        COMPARED,
        MATCHED,
        FALLBACK
    }
}
