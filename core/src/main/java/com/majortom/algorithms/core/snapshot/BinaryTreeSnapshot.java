package com.majortom.algorithms.core.snapshot;

import java.util.Objects;

/** UI-neutral immutable snapshot of a generic binary tree. */
public record BinaryTreeSnapshot<T>(Node<T> root, int size) {
    public BinaryTreeSnapshot {
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
    }

    public record Node<T>(long id, T value, Node<T> left, Node<T> right) {
        public Node {
            if (id <= 0) {
                throw new IllegalArgumentException("node id must be positive");
            }
            value = Objects.requireNonNull(value, "value");
        }
    }
}
