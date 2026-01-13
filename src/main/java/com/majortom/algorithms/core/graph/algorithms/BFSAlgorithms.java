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
        graph.resetGraphState();
        Graph g = graph.getGraph();
        Node startNode = g.getNode(startNodeId);

        if (startNode == null)
            return;

        Queue<Node> queue = new LinkedList<>();

        // 🚩 1. 标记起点并入队
        // 确保 visit 方法逻辑：第一次访问返回 true，重复访问返回 false
        graph.visit(startNodeId);
        queue.add(startNode);

        while (!queue.isEmpty()) {
            Node curr = queue.poll();
            String currId = curr.getId();

            // 🚩 2. 遍历邻居
            curr.neighborNodes().forEach(neighbor -> {
                String neighborId = neighbor.getId();

                // 记录路径追踪（视觉上连线高亮）
                graph.trace(currId, neighborId);

                // 🚩 3. 关键修正：尝试访问邻居
                // 只有当该节点从未被访问过时，才将其加入队列
                if (graph.visit(neighborId)) {
                    queue.add(neighbor);
                    sync(graph, currId, neighborId);
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