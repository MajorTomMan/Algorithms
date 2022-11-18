package NonLinear;

import Basic.Structure.Graph;
import Basic.Structure.Queue;

public class 无向图测试 extends Example {
    public static void main(String[] args) {
        int[][] edges = {
                { 0, 1 }, { 0, 2 }, { 1, 2 }, { 1, 3 }, { 2, 4 }, { 3, 4 }
        };
        int[][] matrix = {
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

    private static void bfs(int v, boolean[] visited, int[][] graph) {
        visited[v] = true;
        printGraph(graph, visited);
        System.out.println("D------------------------------");
        Queue<Integer> queue = new Queue<>();
        queue.enqueue(v);
        while (!queue.isEmpty()) {
            int vertex = queue.dequeue();
            for (int w = 0; w < graph[0].length; w++) {
                if (graph[vertex][w] == 1 && !visited[w]) {
                    visited[w] = true;
                    queue.enqueue(w);
                    graph[vertex][w] = 9;
                    printGraph(graph, visited);
                    System.out.println("------------------------------");
                }
            }
        }
    }

    private static void dfs(int v, boolean[] visited, int[][] graph) {
        visited[v] = true;
        for (int w = 0; w < graph[0].length; w++) {
            if (graph[v][w] == 1 && !visited[w]) {
                graph[v][w] = 9;
                printGraph(graph, visited);
                graph[v][w] = 1;
                System.out.println("------------------------------");
                dfs(w, visited, graph);
            }
        }
    }
}
