package com.majortom.algorithms.app.leetcode.ds.graph;

import com.majortom.algorithms.core.visualization.impl.frame.GraphFrame;
import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.algorithms.BFSAlgorithms;
import com.majortom.algorithms.core.graph.impl.DirectedGraph;
import com.majortom.algorithms.core.graph.node.Vertex;

public class 有向图测试 {
    public static void main(String[] args) {
        BaseGraph<String> graph = new DirectedGraph<>();
        graph.addVertex(new Vertex<>("A"));
        graph.addVertex(new Vertex<>("B"));
        graph.addVertex(new Vertex<>("C"));
        graph.addVertex(new Vertex<>("D"));

        graph.addEdge("A", "B", 1);
        graph.addEdge("B", "C", 1);
        graph.addEdge("C", "D", 1);
        graph.addEdge("D", "A", 1);
        graph.addEdge("A", "C", 1);

        // 2. 准备算法
        BFSAlgorithms<String> bfs = new BFSAlgorithms<>();

        // 3. 启动可视化窗口
        GraphFrame.launch(graph, bfs, "A");
    }
}
