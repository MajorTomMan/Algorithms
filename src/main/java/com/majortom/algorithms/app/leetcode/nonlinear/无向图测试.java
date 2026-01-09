
package com.majortom.algorithms.app.leetcode.nonlinear;

import com.majortom.algorithms.core.graph.algorithms.BFSAlgorithms;
import com.majortom.algorithms.core.graph.impl.UndirectedGraph;
import com.majortom.algorithms.core.visualization.impl.frame.GraphFrame;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 无向图测试 {
    public static void main(String[] args) {
        // 1. 初始化无向图结构
        UndirectedGraph<String> graph = new UndirectedGraph<>();
        AlgorithmsUtils.buildRandomGraph(graph, 10, 15, true);

        BFSAlgorithms<String> bfs = new BFSAlgorithms<>();

        GraphFrame.launch(graph, bfs, "A");
    }
}
