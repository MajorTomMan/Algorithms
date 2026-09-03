package com.majortom.algorithms.app.leetcode.ds.graph;

import com.majortom.algorithms.library.basic.graph.Graph;
import com.majortom.algorithms.library.basic.graph.Vertex;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class 并查集测试 {
    public static void main(String[] args) {
        Graph<Integer> graph = createGraph(6, new Integer[][] {
                {0, 4},
                {1, 1}, {1, 5},
                {2, 2}, {2, 3}, {2, 5},
                {3, 2}, {3, 3},
                {4, 0}, {4, 4},
                {5, 1}, {5, 2}, {5, 5}
        });
        System.out.println(hasCycle(graph));
        System.out.println(isConnected(graph, 1, 5));
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

    private static boolean isConnected(Graph<Integer> graph, int from, int to) {
        Vertex<Integer> start = graph.vertex(from);
        Vertex<Integer> target = graph.vertex(to);
        if (start == null || target == null) {
            return false;
        }
        ArrayDeque<Vertex<Integer>> queue = new ArrayDeque<>();
        Set<Vertex<Integer>> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            Vertex<Integer> vertex = queue.remove();
            if (vertex == target) {
                return true;
            }
            for (Vertex<Integer> neighbor : graph.neighbors(vertex)) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

    private static boolean hasCycle(Graph<Integer> graph) {
        Set<Vertex<Integer>> visited = new HashSet<>();
        for (Vertex<Integer> vertex : graph.vertices()) {
            if (!visited.contains(vertex) && hasCycle(graph, vertex, null, visited)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCycle(Graph<Integer> graph, Vertex<Integer> vertex, Vertex<Integer> parent, Set<Vertex<Integer>> visited) {
        visited.add(vertex);
        for (Vertex<Integer> neighbor : graph.neighbors(vertex)) {
            if (neighbor == vertex) {
                return true;
            }
            if (!visited.contains(neighbor)) {
                if (hasCycle(graph, neighbor, vertex, visited)) {
                    return true;
                }
            } else if (neighbor != parent) {
                return true;
            }
        }
        return false;
    }
}
