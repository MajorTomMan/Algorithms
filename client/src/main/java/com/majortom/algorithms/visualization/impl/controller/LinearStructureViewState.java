package com.majortom.algorithms.visualization.impl.controller;

import java.util.List;
import java.util.Objects;

/** JavaFX-neutral logical Stack/Queue state plus client-only mutation presentation context. */
public record LinearStructureViewState(String kind, List<Integer> values, Mutation mutation) {
    public LinearStructureViewState {
        kind = Objects.requireNonNull(kind, "kind");
        values = List.copyOf(Objects.requireNonNull(values, "values"));
        mutation = mutation == null ? Mutation.none() : mutation;
    }

    public LinearStructureViewState(String kind, List<Integer> values) {
        this(kind, values, Mutation.none());
    }

    public record Mutation(Type type, Integer value) {
        public Mutation {
            type = Objects.requireNonNull(type, "type");
        }

        public static Mutation none() {
            return new Mutation(Type.NONE, null);
        }

        public static Mutation of(Type type, Integer value) {
            return new Mutation(type, value);
        }
    }

    public enum Type {
        NONE,
        PUSH,
        POP,
        ENQUEUE,
        DEQUEUE
    }
}
