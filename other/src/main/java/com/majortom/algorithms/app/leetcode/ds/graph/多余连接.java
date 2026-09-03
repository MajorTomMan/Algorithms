package com.majortom.algorithms.app.leetcode.ds.graph;

import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.basic.graph.Graph;
import com.majortom.algorithms.library.basic.graph.Vertex;
import com.majortom.algorithms.library.structure.QueueStructure;

public class 多余连接 {
    public static void main(String[] args) {
        Integer[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 1}};
        Graph<Integer> graph = createGraph(4, edges);
        System.out.println(bfs(1, new boolean[4], graph));
    }

    private static Graph<Integer> createGraph(int vertexCount, Integer[][] edges) {
        Graph<Integer> graph = new Graph<>(false);
        for (int i = 0; i < vertexCount; i++) {
            graph.addVertex(i);
        }
        for (Integer[] edge : edges) {
            graph.addEdge(graph.vertex(edge[0]), graph.vertex(edge[1]));
        }
        return graph;
    }

    private static boolean bfs(int start, boolean[] visited, Graph<Integer> graph) {
        Vertex<Integer> startVertex = graph.vertex(start);
        if (startVertex == null) {
            return false;
        }
        visited[start] = true;
        QueueStructure<Vertex<Integer>> queue = new LinkedList<>();
        queue.enqueue(startVertex);
        while (!queue.isEmpty()) {
            Vertex<Integer> vertex = queue.dequeue();
            for (Vertex<Integer> neighbor : graph.neighbors(vertex)) {
                int value = neighbor.value();
                if (!visited[value]) {
                    visited[value] = true;
                    queue.enqueue(neighbor);
                }
            }
        }
        return true;
    }
}
