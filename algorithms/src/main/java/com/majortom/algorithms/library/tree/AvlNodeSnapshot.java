package com.majortom.algorithms.library.tree;

/** Immutable AVL tree node with a stable execution-local identity and derived height. */
public record AvlNodeSnapshot(long id, int value, int height, AvlNodeSnapshot left, AvlNodeSnapshot right) {

    public AvlNodeSnapshot {
        if (height < 1) {
            throw new IllegalArgumentException("node height must be positive");
        }
        if (id < 1) {
            throw new IllegalArgumentException("node id must be positive");
        }
    }
}
