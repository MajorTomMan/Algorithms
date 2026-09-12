package com.majortom.algorithms.structure.graph;

import java.util.Collection;
import java.util.Map;

public interface GraphStructure<T> {
    int vertexCount();
    int edgeCount();

    default boolean isEmpty() {
        return vertexCount() == 0;
    }

    boolean isDirected();

    /** Replaces the complete graph through the trusted bulk-load path. */
    void initialize(Map<T, ? extends Collection<T>> adjacency);

    Vertex<T> vertex(T value);
    Vertex<T> addVertex(T value);
    boolean removeVertex(Vertex<T> vertex);
    Edge<T> addEdge(Vertex<T> from, Vertex<T> to);
    boolean removeEdge(Vertex<T> from, Vertex<T> to);
    boolean containsVertex(Vertex<T> vertex);
    boolean containsEdge(Vertex<T> from, Vertex<T> to);
    Iterable<Vertex<T>> vertices();
    Iterable<Edge<T>> edges();
    Iterable<Vertex<T>> neighbors(Vertex<T> vertex);
}
