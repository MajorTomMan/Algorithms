package com.majortom.algorithms.library.graph;

import com.majortom.algorithms.library.structure.WeightedGraphStructure;

/** Algorithms that construct a minimum spanning tree or forest from a weighted graph. */
public interface MinimumSpanningAlgorithm<T> extends WeightedGraphAlgorithm<T> {
    void build(WeightedGraphStructure<T> source, WeightedGraphStructure<T> result);
}
