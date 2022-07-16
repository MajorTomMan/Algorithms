package NonLinear;

import Basic.Structure.Graph;

public class 无向图测试 extends Example {
    public static void main(String[] args) {
        int[][] edges = {
            {0, 1},{1,2},{2,3}
        };
        Graph graph = new Graph(4, edges);
        graph.bfs(2);
        graph.dfs(2,graph.getVisited());
    }
}
