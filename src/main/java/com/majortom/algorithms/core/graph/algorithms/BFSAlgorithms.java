package com.majortom.algorithms.core.graph.algorithms;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.BaseGraphAlgorithms;
import com.majortom.algorithms.core.graph.node.Vertex;

import com.majortom.algorithms.core.graph.node.Edge;
import java.util.LinkedList;
import java.util.Queue;

public class BFSAlgorithms<V> extends BaseGraphAlgorithms<V> {

    @Override
    public void run(BaseGraph<V> graph, Vertex<V> start) {
        // 1. 初始化：重置统计数据和所有节点的访问状态
        graph.resetGraphNodes();

        // 2. 准备队列
        Queue<Vertex<V>> queue = new LinkedList<>();

        // 3. 处理起点
        queue.add(start);
        graph.visit(start); // 这个方法会自动 sync 刷新 UI

        while (!queue.isEmpty()) {
            Vertex<V> curr = queue.poll();

            // 遍历当前节点的所有邻居
            for (Edge<V> edge : curr.getEdges()) {
                Vertex<V> neighbor = edge.getDest();

                // 即使节点访问过，我们也“探测”一下这条边
                // trace 会增加比较计数并触发 UI 上的连线高亮
                graph.trace(curr, neighbor);

                if (!neighbor.isVisited()) {
                    // 标记并加入队列
                    graph.visit(neighbor);
                    queue.add(neighbor);
                }
            }
        }
    }
}