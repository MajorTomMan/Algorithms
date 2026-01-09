package com.majortom.algorithms.core.graph.impl;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.node.Vertex;

/**
 * 无向图的具体实现
 */
public class UndirectedGraph<V> extends BaseGraph<V> {

    @Override
    public void addEdge(V from, V to, int weight) {
        Vertex<V> u = findVertex(from);
        Vertex<V> v = findVertex(to);

        if (u != null && v != null) {
            // 无向图的本质：相互指向的有向边
            u.addEdge(v, weight);
            v.addEdge(u, weight);
        }
    }
}