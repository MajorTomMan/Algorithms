package basic.structure;

import java.util.ArrayList;
import java.util.List;

import basic.structure.interfaces.Queue;

public class Graph { // 无向图
    private List<List<Integer>> graph; // 邻接表形式表示图
    private boolean[] visited;
    private Integer totalVertex;
    /* 并查集结构 */
    private UnionFind unionFind;

    public Graph(Integer totalVertex, Integer[][] edges) {
        this.totalVertex = totalVertex;
        graph = new ArrayList<>();
        visited = new boolean[totalVertex];
        unionFind = new UnionFind(totalVertex);
        for (Integer i = 0; i < totalVertex; i++) {
            graph.add(new ArrayList<>());
        }
        for (Integer[] edge : edges) {
            Integer u = edge[0], v = edge[1];
            graph.get(u).add(v);
            graph.get(v).add(u);
            unionFind.union(u, v);
        }
    }

    public List<Integer> getEdges(Integer vertex) {
        return graph.get(vertex);
    }

    public void bfs(Integer vertex) {
        visited[vertex] = true;
        Queue<Integer> queue = new LinkedList<>();
        queue.add(vertex);
        while (!queue.isEmpty()) {
            Integer v = queue.poll();
            for (Integer w : graph.get(v)) {
                if (!visited[w]) {
                    queue.add(w);
                    visited[w] = true;
                }
            }
        }
        cleanVisited();
    }

    private void cleanVisited() {
        visited = new boolean[totalVertex];
    }

    public void dfs(Integer vertex, boolean[] visited) {
        visited[vertex] = true;
        for (Integer v : graph.get(vertex)) {
            if (!visited[v]) {
                dfs(v, visited);
            }
        }
    }

    /* 检测是否有环 */
    /* 通过BFS */
    public boolean hasCycle() {
        visited = new boolean[graph.size()];
        Queue<Integer> queue = new LinkedList<>();
        queue.add(graph.get(0).get(0));
        while (!queue.isEmpty()) {
            Integer v = queue.poll();
            for (Integer w : graph.get(v)) {
                if (w == v && !visited[v]) {
                    return true;
                }
                if (!visited[w]) {
                    queue.add(w);
                    visited[w] = true;
                }
            }
        }
        return false;
    }

    public boolean isConnected(Integer src, Integer dest) {
        return unionFind.find(src) == unionFind.find(dest);
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