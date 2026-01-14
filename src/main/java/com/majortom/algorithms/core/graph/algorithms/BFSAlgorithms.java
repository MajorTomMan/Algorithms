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

        graph.visit(startNodeId);
        queue.add(startNode);

        while (!queue.isEmpty()) {
            Node curr = queue.poll();
            String currId = curr.getId();

            curr.neighborNodes().forEach(neighbor -> {
                String neighborId = neighbor.getId();
                graph.trace(currId, neighborId);

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