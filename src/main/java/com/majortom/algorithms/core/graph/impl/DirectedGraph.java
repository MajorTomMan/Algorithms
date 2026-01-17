package com.majortom.algorithms.core.graph.impl;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.graph.BaseGraph;
import org.graphstream.graph.Edge;

/**
 * 有向图数据实现
 * 职责：维护具有方向性的拓扑连接
 */
public class DirectedGraph<V> extends BaseGraph<V> {

    public DirectedGraph(String id) {
        super(id);
    }

    @Override
    public void addEdge(String fromId, String toId, int weight) {
        String edgeId = String.format("%s->%s", fromId, toId);
        if (graph.getEdge(edgeId) == null) {
            // 第三个参数 true 表示该边为有向边
            Edge e = graph.addEdge(edgeId, fromId, toId, true);
            e.setAttribute("weight", weight);
            e.setAttribute("ui.label", String.valueOf(weight));
            // 💡 提示：具体的箭头颜色建议统一放在 GraphVisualizer 的 StyleSheet 里
            // 这样这里只负责逻辑逻辑，不负责 UI 细节
        }
    }

    /**
     * 实现 BaseStructure 要求的抽象方法
     */
    @Override
    public V getData() {
        return null; // 图作为整体容器，通常不返回单一数据项
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
        // TODO Auto-generated method stub
        return new DirectedGraph<>(id);
    }
}