package com.majortom.algorithms.library.tree;

import com.majortom.algorithms.core.api.AlgorithmOutput;

import java.util.List;
import java.util.Objects;

/** Final tree snapshot and its in-order values. */
public record AvlTreeOutput(AvlNodeSnapshot root, List<Integer> values) implements AlgorithmOutput {

    public AvlTreeOutput {
        Objects.requireNonNull(values, "values");
        values = List.copyOf(values);
    }
}
