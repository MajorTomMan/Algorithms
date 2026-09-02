package com.majortom.algorithms.library.tree;


import java.util.List;
import java.util.Objects;

/** Final tree snapshot and its in-order values. */
public record AvlTreeOutput(AvlNodeSnapshot root, List<Integer> values) {

    public AvlTreeOutput {
        Objects.requireNonNull(values, "values");
        values = List.copyOf(values);
    }
}
