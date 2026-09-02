package com.majortom.algorithms.app.leetcode.ds.graph;

import java.util.LinkedList;
import java.util.Queue;

import com.majortom.algorithms.library.structure.MutableGraph;

public class 多余连接 {
    public static void main(String[] args) {
        Integer[][] edges = {
                { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 1 }
        };
        MutableGraph<Integer> graph = new MutableGraph<>();
        for (int vertex = 0; vertex < 4; vertex++) {
            graph.addVertex(vertex);
        }
        for (Integer[] edge : edges) {
            graph.addEdge(edge[0], edge[1]);
            graph.addEdge(edge[1], edge[0]);
        }
        System.out.println(bfs(1, new boolean[graph.size()], graph));
    }

    private static boolean bfs(int v, boolean[] visited, MutableGraph<Integer> graph) {
        visited[v] = true;
        Queue<Integer> queue = new LinkedList<>();
        queue.add(v);
        while (!queue.isEmpty()) {
            int vertex = queue.poll();
            for (int w : graph.neighbors(vertex)) {
                if (!visited[w]) {
                    queue.add(w);
                    visited[w] = true;
                }
            }
        }
        return false;
    }
}
