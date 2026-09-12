package com.majortom.algorithms.structure.graph;

import java.util.Map;

/** Graph contract that associates a finite numeric weight with every edge. */
public interface WeightedGraphStructure<T> extends GraphStructure<T> {
    double weight(Edge<T> edge);

    /**
     * Trusted weighted bulk-load path. A distinct method name avoids Map erasure collision with
     * GraphStructure.initialize while keeping the natural JDK Map input shape.
     */
    void initializeWeighted(Map<T, ? extends Map<T, Double>> adjacency);

    Edge<T> addEdge(Vertex<T> from, Vertex<T> to, double weight);

    double setWeight(Edge<T> edge, double weight);

    @Override
    default Edge<T> addEdge(Vertex<T> from, Vertex<T> to) {
        return addEdge(from, to, 1.0d);
    }
}
