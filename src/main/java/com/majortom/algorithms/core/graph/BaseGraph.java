package com.majortom.algorithms.core.graph;

import com.majortom.algorithms.core.graph.node.Vertex;

import java.util.ArrayList;
import java.util.List;

import com.majortom.algorithms.core.base.BaseAlgorithm;

public abstract class BaseGraph<V> extends BaseAlgorithm<BaseGraph<V>> {
    protected List<Vertex<V>> vertices = new ArrayList<>();
    protected Vertex<V> activeVertex;

    // --- 图特有的原子同步动作 ---

    protected void visit(Vertex<V> v) {
        this.activeVertex = v;
        v.setVisited(true);
        actionCount++;
        sync(this, v, null); // 同步
    }

    protected void trace(Vertex<V> from, Vertex<V> to) {
        compareCount++;
        sync(this, from, to); // 同步
    }

    // 辅助方法
    public void addVertex(Vertex<V> v) {
        this.vertices.add(v);
    }

    public List<Vertex<V>> getVertices() {
        return vertices;
    }

    public Vertex<V> getActiveVertex() {
        return activeVertex;
    }
}