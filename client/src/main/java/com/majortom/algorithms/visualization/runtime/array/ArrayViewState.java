package com.majortom.algorithms.visualization.runtime.array;

import java.util.List;
import java.util.Objects;

/** Immutable JavaFX-neutral Array state derived only from factual structure mutations. */
public record ArrayViewState(List<Integer> values, Mutation mutation, boolean completed) {

    public ArrayViewState {
        values = List.copyOf(Objects.requireNonNull(values, "values"));
        mutation = Objects.requireNonNull(mutation, "mutation");
    }

    public static ArrayViewState source(List<Integer> values) {
        return new ArrayViewState(values, Mutation.none(), false);
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

    public enum Type {
        NONE,
        INSERTED,
        REMOVED,
        UPDATED,
        SWAPPED
    }
}
