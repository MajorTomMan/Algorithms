package com.majortom.algorithms.core.graph;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

/**
 * 图算法数据基类
 * 职责：作为 GraphStream Graph 的包装器，提供符合业务逻辑的算法同步接口
 */
public abstract class BaseGraph<V> extends BaseAlgorithms<Graph> {

    protected Graph graph;

    public BaseGraph(String id) {
        // 初始化 GraphStream 核心对象
        this.graph = new SingleGraph(id);
    }

    /**
     * 向图中添加节点
     * 
     * @param id   节点唯一标识
     * @param data 节点携带的业务数据
     */
    public void addVertex(String id, V data) {
        Node node = graph.addNode(id);
        node.setAttribute("data", data);
        node.setAttribute("ui.label", id); // 默认显示 ID 作为标签
    }

    /**
     * 建立连接：由子类实现有向/无向逻辑
     */
    public abstract void addEdge(String fromId, String toId, int weight);

    // --- 可视化同步接口 ---

    /**
     * 标记节点访问状态并同步
     */
    public void visit(String nodeId) {
        Node n = graph.getNode(nodeId);
        if (n != null) {
            n.setAttribute("visited", true);
            n.setAttribute("ui.class", "highlight");
            actionCount++;
            sync(graph, nodeId, null);
        }
    }

    /**
     * 追踪/比较两条边或节点
     */
    public void trace(String fromId, String toId) {
        compareCount++;
        sync(graph, fromId, toId);
    }

    /**
     * 高亮特定边
     */
    public void confirmEdge(String fromId, String toId) {
        Edge e = graph.getEdge(fromId + "_" + toId);
        if (e == null)
            e = graph.getEdge(toId + "_" + fromId); // 兼顾无向图

        if (e != null) {
            e.setAttribute("ui.class", "active-edge");
            actionCount++;
            sync(graph, fromId, toId);
        }
    }

    // --- 状态重置 ---

    public void resetGraphState() {
        graph.nodes().forEach(n -> {
            n.removeAttribute("visited");
            n.removeAttribute("ui.class");
        });
        graph.edges().forEach(e -> e.removeAttribute("ui.class"));
        resetStatistics();
    }

    public Graph getGraph() {
        return graph;
    }
}