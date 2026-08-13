package com.majortom.algorithms.library.tree;

import com.majortom.algorithms.core.api.AlgorithmInput;

import java.util.List;
import java.util.Objects;

/** Initial values and ordered AVL mutations. Duplicate insertions are idempotent. */
public record AvlTreeInput(List<Integer> initialValues, List<AvlCommand> commands) implements AlgorithmInput {

    public static final int MAX_OPERATIONS = 100_000;

    public AvlTreeInput {
        Objects.requireNonNull(initialValues, "initialValues");
        Objects.requireNonNull(commands, "commands");
        long operationCount = (long) initialValues.size() + commands.size();
        if (operationCount > MAX_OPERATIONS) {
            throw new IllegalArgumentException("AVL input must contain at most " + MAX_OPERATIONS + " operations");
        }
        initialValues = List.copyOf(initialValues);
        commands = List.copyOf(commands);
    }
}
