package com.majortom.algorithms.library.tree;

import java.util.List;
import java.util.Objects;

/** Exact initial AVL snapshot or initial values plus ordered AVL mutations. */
public record AvlTreeInput(AvlNodeSnapshot initialRoot, List<Integer> initialValues, List<AvlCommand> commands) {

    public static final int MAX_OPERATIONS = 100_000;

    public AvlTreeInput {
        initialValues = List.copyOf(Objects.requireNonNull(initialValues, "initialValues"));
        commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
        if (initialRoot != null && !initialValues.isEmpty()) {
            throw new IllegalArgumentException("initialRoot and initialValues are mutually exclusive");
        }
        long initialCount = initialRoot == null ? initialValues.size() : count(initialRoot);
        if (initialCount + commands.size() > MAX_OPERATIONS) {
            throw new IllegalArgumentException("AVL input must contain at most " + MAX_OPERATIONS + " operations");
        }
    }

    public static AvlTreeInput fromValues(List<Integer> values, List<AvlCommand> commands) {
        return new AvlTreeInput(null, values, commands);
    }

    public static AvlTreeInput fromSnapshot(AvlNodeSnapshot root, List<AvlCommand> commands) {
        return new AvlTreeInput(Objects.requireNonNull(root, "root"), List.of(), commands);
    }

    private static long count(AvlNodeSnapshot node) {
        if (node == null) return 0L;
        return 1L + count(node.left()) + count(node.right());
    }
}
