package Basic.Structure;

import java.util.ArrayList;
import java.util.List;

public class Graph { // 无向图
    private List<List<Integer>> graph; //邻接表形式表示图
    private boolean[] visited;
    private int totalVertex;
    public Graph(int totalVertex, int[][] edges) {
        this.totalVertex=totalVertex;
        graph=new ArrayList<>();
        visited = new boolean[totalVertex];
        for (int i = 0; i < totalVertex; i++) {
            graph.add(new ArrayList<>());
        }
        for (int[] edge : edges) {
            int u = edge[0], v = edge[1];
            graph.get(u).add(v);
            graph.get(v).add(u);
        }
    }

    public List<Integer> getEdges(int vertex) {
        return graph.get(vertex);
    }

    public void bfs(int vertex) {
        visited[vertex] = true;
        Queue<Integer> queue = new Queue<>();
        queue.enqueue(vertex);
        while (!queue.isEmpty()) {
            int v = queue.dequeue();
            for (int w : graph.get(v)) {
                if (!visited[w]) {
                    queue.enqueue(w);
                    visited[w] = true;
                }
            }
        }
        cleanVisited();
    }
    private void cleanVisited(){
        visited=new boolean[totalVertex];
    }
    public void dfs(int vertex, boolean[] visited) {
        visited[vertex] = true;
        for (int v : graph.get(vertex)) {
            if (!visited[v]) {
                dfs(v,visited);
            }
        }
    }

    /**
     * @return the graph
     */
    public List<List<Integer>> getGraph() {
        return graph;
    }

    /**
     * @return the visited
     */
    public boolean[] getVisited() {
        return visited;
    }
}
