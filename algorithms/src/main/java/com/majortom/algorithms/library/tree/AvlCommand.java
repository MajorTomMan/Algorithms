package com.majortom.algorithms.library.tree;

import java.util.Objects;

/** One mutation in an AVL command batch. */
public record AvlCommand(Operation operation, int value) {

    public AvlCommand {
        Objects.requireNonNull(operation, "operation");
    }

    public enum Operation {
        INSERT,
        REMOVE
    }
}
