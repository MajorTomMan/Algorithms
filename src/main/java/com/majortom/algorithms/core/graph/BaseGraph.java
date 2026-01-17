package com.majortom.algorithms.core.graph;

import com.majortom.algorithms.core.base.BaseStructure;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

/**
 * 图算法数据基类
 * 职责：作为 GraphStream Graph 的包装器，提供符合业务逻辑的算法同步接口
 */
public abstract class BaseGraph<V> extends BaseStructure<V> {

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
     * 标记节点访问状态
     * 
     * @return 如果是首次访问返回 true，否则返回 false
     */
    public boolean visit(String nodeId) {
        Node n = graph.getNode(nodeId);
        if (n != null && !n.hasAttribute("visited")) {
            n.setAttribute("visited", true);
            n.setAttribute("ui.class", "highlight");
            incrementAction(); // 使用基类方法
            return true;
        }
        return false;
    }

    public boolean isVisited(String nodeId) {
        Node n = graph.getNode(nodeId);
        return n != null && n.hasAttribute("visited");
    }

    /**
     * 追踪/比较两条边或节点
     */
    public void trace(String fromId, String toId) {
        compareCount++;
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
        }
    }

    @Override
    public void resetStatistics() {
        // TODO Auto-generated method stub
        super.resetStatistics();

        resetGraphState();
    }
    // --- 状态重置 ---

    protected void resetGraphState() {
        graph.nodes().forEach(n -> {
            n.removeAttribute("visited");
            n.removeAttribute("ui.class");
        });
        graph.edges().forEach(e -> e.removeAttribute("ui.class"));
    }

    public Graph getGraph() {
        return graph;
    }

    @Override
    public void clear() {
        // 调用 GraphStream 的底层清空方法，移除所有点、边及其属性
        if (this.graph != null) {
            this.graph.clear();
            // 重新设置必要的全局渲染属性
            this.graph.setAttribute("ui.quality");
            this.graph.setAttribute("ui.antialias");
        }
    }

    @Override
    public BaseStructure<V> copy() {
        // 1. 调用子类实现的钩子，确保“血统”正确
        BaseGraph<V> copyGraph = (BaseGraph<V>) createEmptyInstance(this.graph.getId() + "_copy");

        // 2. 通用逻辑：拷贝节点及坐标（父类处理）
        this.graph.nodes().forEach(oldNode -> {
            Node newNode = copyGraph.getGraph().addNode(oldNode.getId());
            oldNode.attributeKeys().forEach(k -> newNode.setAttribute(k, oldNode.getAttribute(k)));
        });

        // 3. 差异逻辑：调用子类的 addEdge（子类处理）
        this.graph.edges().forEach(oldEdge -> {
            String from = oldEdge.getSourceNode().getId();
            String to = oldEdge.getTargetNode().getId();
            int weight = oldEdge.hasAttribute("weight") ? (int) oldEdge.getAttribute("weight") : 1;

            // 这里会根据子类是 Undirected 还是 Directed 执行不同的逻辑
            copyGraph.addEdge(from, to, weight);

            // 补偿边属性（如高亮色）
            Edge newEdge = copyGraph.getGraph().getEdge(oldEdge.getId());
            if (newEdge != null) {
                oldEdge.attributeKeys().forEach(k -> newEdge.setAttribute(k, oldEdge.getAttribute(k)));
            }
        });

        copyGraph.actionCount = this.actionCount;
        copyGraph.compareCount = this.compareCount;
        return copyGraph;
    }

    /**
     * 抽象钩子：由子类返回自身的具体实例（如 return new DirectedGraph(id)）
     */
    protected abstract BaseGraph<V> createEmptyInstance(String id);
}