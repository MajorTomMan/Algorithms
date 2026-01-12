package com.majortom.algorithms.core.graph.impl;

import com.majortom.algorithms.core.graph.BaseGraph;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;

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

            // 为有向图定制箭头样式
            e.setAttribute("ui.style", "arrow-shape: arrow; arrow-size: 10px, 5px;");
        }
    }

    @Override
    public void run(Graph data) {
        // TODO Auto-generated method stub
    }
}