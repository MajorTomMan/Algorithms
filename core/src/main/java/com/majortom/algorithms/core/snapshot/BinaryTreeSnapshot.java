package com.majortom.algorithms.core.snapshot;

public record BinaryTreeSnapshot<T>(Node<T> root, int size) {

    public BinaryTreeSnapshot {
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
    }

    public record Node<T>(long id, T value, int height, Node<T> left, Node<T> right) {

        public Node {
            if (id <= 0) {
                throw new IllegalArgumentException("node id must be positive");
            }
            if (height <= 0) {
                throw new IllegalArgumentException("node height must be positive");
            }
        }
    }
}
