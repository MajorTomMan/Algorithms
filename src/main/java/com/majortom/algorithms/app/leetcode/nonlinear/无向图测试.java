
package com.majortom.algorithms.app.leetcode.nonlinear;

import com.majortom.algorithms.core.graph.algorithms.BFSAlgorithms;
import com.majortom.algorithms.core.graph.impl.UndirectedGraph;
import com.majortom.algorithms.core.graph.node.Vertex;
import com.majortom.algorithms.core.visualization.impl.frame.GraphFrame;

public class 无向图测试 {
    public static void main(String[] args) {
        // 1. 初始化无向图结构
        UndirectedGraph<String> graph = new UndirectedGraph<>();
        graph.addVertex(new Vertex<>("A"));
        graph.addVertex(new Vertex<>("B"));
        graph.addVertex(new Vertex<>("C"));
        graph.addVertex(new Vertex<>("D"));
        graph.addVertex(new Vertex<>("E"));

        graph.addEdge("A", "B", 1);
        graph.addEdge("B", "C", 1);
        graph.addEdge("C", "D", 1);
        graph.addEdge("D", "E", 1);
        graph.addEdge("E", "A", 1); // 形成一个环
        graph.addEdge("A", "C", 1); // 内部跨越边

        BFSAlgorithms<String> bfs = new BFSAlgorithms<>();

        GraphFrame.launch(graph, bfs, "A");
    }
}
