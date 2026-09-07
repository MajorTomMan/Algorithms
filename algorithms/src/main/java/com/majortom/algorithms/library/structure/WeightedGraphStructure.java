package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.library.basic.graph.Edge;
import com.majortom.algorithms.library.basic.graph.Vertex;

/** Graph contract that associates a finite numeric weight with every edge. */
public interface WeightedGraphStructure<T> extends GraphStructure<T> {
    double weight(Edge<T> edge);

    Edge<T> addEdge(Vertex<T> from, Vertex<T> to, double weight);

    double setWeight(Edge<T> edge, double weight);

    @Override
    default Edge<T> addEdge(Vertex<T> from, Vertex<T> to) {
        return addEdge(from, to, 1.0d);
    }
}
