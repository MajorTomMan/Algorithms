package NonLinear;

import Basic.Structure.Graph;
import Basic.Structure.Queue;

public class 无向图测试 extends Example {
    public static void main(String[] args) {
        int[][] edges = {
            {0, 1},{1,2},{2,3}
        };
        int[][] matrix={
            {0,1,0,1},
            {1,0,1,0},
            {0,1,0,1},
            {1,0,1,0}
        };
        boolean[] visited=new boolean[matrix.length];
        Graph graph = new Graph(4, edges);
        graph.bfs(2);
        graph.dfs(2,graph.getVisited());
        printGraph(matrix,visited);
        System.out.println("-------------------------------");
        bfs(1, visited, matrix);
    }
    private static void bfs(int v,boolean[] visited,int[][] graph){
        visited[v]=true;
        printGraph(graph, visited);
        Queue<Integer> queue=new Queue<>();
        queue.enqueue(v);
        while(!queue.isEmpty()){
            int vertex=queue.dequeue();
            for(int w=0;w<graph[0].length;w++){
                if(graph[vertex][w]==1&&!visited[w]){
                    visited[w]=true;
                    queue.enqueue(w);
                }
                graph[vertex][w]=9;
                printGraph(graph,visited);
                System.out.println("------------------------------");
            }
        }
    }
}
