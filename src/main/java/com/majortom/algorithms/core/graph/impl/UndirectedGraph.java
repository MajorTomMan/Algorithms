package com.majortom.algorithms.core.graph.impl;

import com.majortom.algorithms.core.graph.BaseGraph;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;

/**
 * 无向图数据实现
 * 职责：维护无向边的对称性连接
 */
public class UndirectedGraph<V> extends BaseGraph<V> {

    public UndirectedGraph(String id) {
        super(id);
        // 显式配置图属性，告知渲染引擎处理为无向模式
        this.graph.setAttribute("ui.quality");
        this.graph.setAttribute("ui.antialias");
    }

    @Override
    public void addEdge(String fromId, String toId, int weight) {
        // 预防重复添加相同 ID 的边
        String edgeId = String.format("%s_%s", fromId, toId);
        if (graph.getEdge(edgeId) == null && graph.getEdge(toId + "_" + fromId) == null) {
            // 第三个参数 false 表示该边为无向边
            Edge e = graph.addEdge(edgeId, fromId, toId, false);
            e.setAttribute("weight", weight);
            e.setAttribute("ui.label", String.valueOf(weight));
        }
    }

    @Override
    public void run(Graph data) {
        // TODO Auto-generated method stub
    }
}