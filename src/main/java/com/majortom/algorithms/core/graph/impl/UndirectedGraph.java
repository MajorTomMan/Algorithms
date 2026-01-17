package com.majortom.algorithms.core.graph.impl;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.graph.BaseGraph;
import org.graphstream.graph.Edge;

/**
 * 无向图数据实现
 * 职责：维护无向边的对称性连接
 */
public class UndirectedGraph<V> extends BaseGraph<V> {

    public UndirectedGraph(String id) {
        super(id);
        // 配置全局渲染属性
        this.graph.setAttribute("ui.quality");
        this.graph.setAttribute("ui.antialias");
    }

    @Override
    public void addEdge(String fromId, String toId, int weight) {
        // 排序 ID 确保无向边的唯一性
        String edgeId = fromId.compareTo(toId) < 0 ? fromId + "_" + toId : toId + "_" + fromId;
        if (graph.getEdge(edgeId) == null) {
            Edge e = graph.addEdge(edgeId, fromId, toId, false);
            e.setAttribute("weight", weight);
            e.setAttribute("ui.label", String.valueOf(weight));
        }
    }

    @Override
    public V getData() {
        return null;
    }

    @Override
    public void resetStatistics() {
        // TODO Auto-generated method stub
        super.resetStatistics();
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        super.clear();
    }

    @Override
    protected BaseGraph<V> createEmptyInstance(String id) {
        return new UndirectedGraph<>(id);
    }

}