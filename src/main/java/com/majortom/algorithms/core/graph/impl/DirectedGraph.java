package com.majortom.algorithms.core.graph.impl;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.node.Vertex;

/**
 * 有向图的具体实现
 */
public class DirectedGraph<V> extends BaseGraph<V> {

    /**
     * 实现基类的抽象加边方法
     * 在有向图中，仅建立从 from 到 to 的单向引用
     */
    @Override
    public void addEdge(V from, V to, int weight) {
        Vertex<V> u = findVertex(from);
        Vertex<V> v = findVertex(to);

        if (u != null && v != null) {
            u.addEdge(v, weight);
        } else {
            System.err.println("警告：尝试连接不存在的顶点 " + from + " -> " + to);
        }
    }

}
