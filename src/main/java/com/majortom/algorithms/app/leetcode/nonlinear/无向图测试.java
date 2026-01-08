
package com.majortom.algorithms.app.leetcode.nonlinear;

import com.majortom.algorithms.core.basic.Graph;
import com.majortom.algorithms.core.basic.LinkedList;
import com.majortom.algorithms.core.interfaces.Queue;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 无向图测试{
    public static void main(String[] args) {
        Integer[][] edges = {
                { 0, 1 }, { 0, 2 }, { 1, 2 }, { 1, 3 }, { 2, 4 }, { 3, 4 }
        };
        Integer[][] matrix = {
                { 0, 1, 0, 1 },
                { 1, 0, 1, 0 },
                { 0, 1, 0, 1 },
                { 1, 0, 1, 0 }
                /*
                 *   0 1 2 3
                 * 0 0 1 0 1    无向图表示: 0-1-2-3-0 (有环)
                 * 1 1 0 1 0               
                 * 2 0 1 0 1
                 * 3 1 0 1 0
                 */
        };
        boolean[] visited = new boolean[matrix.length];
        Graph graph = new Graph(5, edges);
        graph.bfs(2);
        graph.dfs(2, graph.getVisited());
        // printGraph(matrix,visited);
        System.out.println("-------------------------------");
        //bfs(1, visited.clone(), matrix.clone());
        dfs(1, visited.clone(), matrix.clone());
    }

    private static void bfs(int v, boolean[] visited, Integer[][] graph) {
        visited[v] = true;
        AlgorithmsUtils.printGraph(graph, visited);
        System.out.println("D------------------------------");
        Queue<Integer> queue = new LinkedList<>();
        queue.add(v);
        while (!queue.isEmpty()) {
            int vertex = queue.poll();
            for (int w = 0; w < graph[0].length; w++) {
                if (graph[vertex][w] == 1 && !visited[w]) {
                    visited[w] = true;
                    queue.add(w);
                    graph[vertex][w] = 9;
                    AlgorithmsUtils.printGraph(graph, visited);
                    System.out.println("------------------------------");
                }
            }
        }
    }

    private static void dfs(int v, boolean[] visited, Integer[][] graph) {
        visited[v] = true;
        for (int w = 0; w < graph[0].length; w++) {
            if (graph[v][w] == 1 && !visited[w]) {
                graph[v][w] = 9;
                AlgorithmsUtils.printGraph(graph, visited);
                graph[v][w] = 1;
                System.out.println("------------------------------");
                dfs(w, visited, graph);
            }
        }
    }
}
