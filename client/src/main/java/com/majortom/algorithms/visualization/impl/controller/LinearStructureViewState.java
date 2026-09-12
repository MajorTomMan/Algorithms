package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.visualization.runtime.VisualValue;

import java.util.List;
import java.util.Objects;

/** JavaFX-neutral logical Stack/Queue state plus client-only mutation presentation context. */
public record LinearStructureViewState(String kind, List<VisualValue> values, Mutation mutation) {
    public LinearStructureViewState {
        kind = Objects.requireNonNull(kind, "kind");
        values = List.copyOf(Objects.requireNonNull(values, "values"));
        mutation = mutation == null ? Mutation.none() : mutation;
    }

    public LinearStructureViewState(String kind, List<?> values) {
        this(kind, values.stream().map(VisualValue::of).toList(), Mutation.none());
    }

    public static LinearStructureViewState of(String kind, List<?> values, Mutation mutation) {
        return new LinearStructureViewState(kind, values.stream().map(VisualValue::of).toList(), mutation);
    }

    public record Mutation(Type type, VisualValue value) {
        public Mutation {
            type = Objects.requireNonNull(type, "type");
        }

        public static Mutation none() {
            return new Mutation(Type.NONE, null);
        }

        public static Mutation of(Type type, Object value) {
            return new Mutation(type, value == null ? null : VisualValue.of(value));
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
