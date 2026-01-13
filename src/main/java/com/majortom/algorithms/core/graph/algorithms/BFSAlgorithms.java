package com.majortom.algorithms.core.graph.algorithms;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.BaseGraphAlgorithms;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 广度优先搜索 (BFS) 算法实现
 * 适配 GraphStream 数据模型，通过节点 ID 进行拓扑遍历
 */
public class BFSAlgorithms<V> extends BaseGraphAlgorithms<V> {

    @Override
    public void run(BaseGraph<V> graph, String startNodeId) {
        // 1. 初始化：重置统计数据与图中所有元素的视觉状态
        graph.resetGraphState();

        // 获取 GraphStream 核心实例
        Graph g = graph.getGraph();
        Node startNode = g.getNode(startNodeId);

        if (startNode == null)
            return;

        // 2. 准备队列
        Queue<Node> queue = new LinkedList<>();

        // 3. 处理起始节点
        queue.add(startNode);
        graph.visit(startNodeId); // 内部触发 sync，将节点标记为 highlight

        while (!queue.isEmpty()) {
            Node curr = queue.poll();

            // 4. 遍历当前节点的邻居
            // GraphStream 会根据图的有向/无向属性自动返回正确的邻居集合
            curr.neighborNodes().forEach(neighbor -> {
                String neighborId = neighbor.getId();
                graph.trace(curr.getId(), neighborId);

                if (!graph.visit(neighborId)) {
                    graph.visit(neighborId);
                    queue.add(neighbor);
                }
            });
        }
    }

    @Override
    public void run(BaseGraph<V> structure) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'run'");
    }
}