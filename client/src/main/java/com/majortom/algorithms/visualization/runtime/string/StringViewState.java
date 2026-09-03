package com.majortom.algorithms.visualization.runtime.string;

import java.util.Objects;

/** Immutable JavaFX-neutral String state derived only from factual string mutations. */
public record StringViewState(String value, Mutation mutation, boolean completed) {

    public StringViewState {
        value = value == null ? "" : value;
        mutation = Objects.requireNonNull(mutation, "mutation");
    }

    public static StringViewState source(String value) {
        return new StringViewState(value, Mutation.none(), false);
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

    public enum Type {
        NONE,
        INSERTED,
        REMOVED,
        UPDATED,
        REPLACED
    }
}
