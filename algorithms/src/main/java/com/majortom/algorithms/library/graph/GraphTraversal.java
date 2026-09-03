package com.majortom.algorithms.library.graph;

import com.majortom.algorithms.library.structure.GraphStructure;

/** Domain contract for graph traversal algorithms. */
public interface GraphTraversal<T, R> {
    R traverse(GraphStructure<T> graph, T startNode);
}
