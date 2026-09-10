package com.majortom.algorithms.algorithm.graph;

import com.majortom.algorithms.structure.graph.GraphStructure;

import java.util.List;

/** Domain contract for graph traversal algorithms. */
public interface GraphTraversal<T> extends GraphAlgorithm<T> {
    List<T> traverse(GraphStructure<T> graph, T startNode);
}
