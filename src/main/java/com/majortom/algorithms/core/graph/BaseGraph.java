package com.majortom.algorithms.core.graph;

import com.majortom.algorithms.core.graph.node.Edge;
import com.majortom.algorithms.core.graph.node.Vertex;
import com.majortom.algorithms.core.base.BaseAlgorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * 图算法数据基类
 * 职责：维护图的拓扑结构，提供可视化同步接口
 */
public abstract class BaseGraph<V> extends BaseAlgorithm<BaseGraph<V>> {
    protected List<Vertex<V>> vertices = new ArrayList<>();
    protected Vertex<V> activeVertex;

    // --- 核心构建接口：由子类实现有向或无向的逻辑 ---

    /**
     * 在图中建立连接
     * 
     * @param from   起点数据
     * @param to     终点数据
     * @param weight 权重
     */
    public abstract void addEdge(V from, V to, int weight);

    // --- 可视化同步钩子 ---

    public void visit(Vertex<V> v) {
        this.activeVertex = v;
        v.setVisited(true);
        actionCount++;
        sync(this, v, null);
    }

    public void trace(Vertex<V> from, Vertex<V> to) {
        compareCount++;
        sync(this, from, to);
    }

    public void confirmEdge(Vertex<V> from, Vertex<V> to) {
        for (Edge<V> e : from.getEdges()) {
            if (e.getDest().equals(to)) {
                e.setHighlighted(true);
                break;
            }
        }
        actionCount++;
        sync(this, from, to);
    }

    // --- 状态重置 ---

    public void resetGraphNodes() {
        for (Vertex<V> v : vertices) {
            v.setVisited(false);
            for (Edge<V> e : v.getEdges()) {
                e.setHighlighted(false);
            }
        }
        this.activeVertex = null;
        resetStatistics();
    }

    public void clearEdgesHighlight() {
        for (Vertex<V> v : vertices) {
            for (Edge<V> e : v.getEdges()) {
                e.setHighlighted(false);
            }
        }
    }

    // --- 常用辅助工具 ---

    public void addVertex(Vertex<V> v) {
        this.vertices.add(v);
    }

    public Vertex<V> findVertex(V data) {
        for (Vertex<V> v : vertices) {
            if (v.getData().equals(data))
                return v;
        }
        return null;
    }

    public List<Vertex<V>> getVertices() {
        return vertices;
    }

    public Vertex<V> getActiveVertex() {
        return activeVertex;
    }

    public int getVertexCount() {
        return vertices.size();
    }
}