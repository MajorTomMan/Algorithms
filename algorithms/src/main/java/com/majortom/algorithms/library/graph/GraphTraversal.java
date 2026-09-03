package com.majortom.algorithms.library.graph;

import com.majortom.algorithms.library.structure.GraphStructure;

import java.util.List;

/** Domain contract for graph traversal algorithms. */
public interface GraphTraversal<T> {
    List<T> traverse(GraphStructure<T> graph, T startNode);
}
