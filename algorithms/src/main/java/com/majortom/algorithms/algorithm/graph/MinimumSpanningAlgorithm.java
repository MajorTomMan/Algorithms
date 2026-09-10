package com.majortom.algorithms.algorithm.graph;

import com.majortom.algorithms.structure.graph.WeightedGraphStructure;

/** Algorithms that construct a minimum spanning tree or forest from a weighted graph. */
public interface MinimumSpanningAlgorithm<T> extends WeightedGraphAlgorithm<T> {
    void build(WeightedGraphStructure<T> source, WeightedGraphStructure<T> result);
}
