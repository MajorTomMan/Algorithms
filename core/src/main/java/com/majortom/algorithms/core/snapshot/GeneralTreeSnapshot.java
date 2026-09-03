package com.majortom.algorithms.core.snapshot;

import java.util.List;
import java.util.Objects;

/** UI-neutral immutable snapshot of an ordered general/N-ary tree. */
public record GeneralTreeSnapshot<T>(Node<T> root, int size) {
    public GeneralTreeSnapshot {
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
    }

    public record Node<T>(long id, T value, List<Node<T>> children) {
        public Node {
            if (id <= 0) {
                throw new IllegalArgumentException("node id must be positive");
            }
            value = Objects.requireNonNull(value, "value");
            children = List.copyOf(Objects.requireNonNull(children, "children"));
        }
    }
}
