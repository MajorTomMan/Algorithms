package com.majortom.algorithms.library.structure;

import java.util.Map;
import java.util.Set;

public interface GraphStructure<T> {
    int size();
    boolean containsVertex(T vertex);
    void addVertex(T vertex);
    boolean removeVertex(T vertex);
    void addEdge(T from, T to);
    boolean removeEdge(T from, T to);
    Set<T> neighbors(T vertex);
    Map<T, ? extends Set<T>> raw();
}
