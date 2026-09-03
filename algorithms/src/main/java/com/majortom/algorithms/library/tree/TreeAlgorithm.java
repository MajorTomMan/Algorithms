package com.majortom.algorithms.library.tree;

/** Domain contract for tree algorithms with explicit input and result types. */
public interface TreeAlgorithm<T, I, O> {
    O execute(I input);
}
