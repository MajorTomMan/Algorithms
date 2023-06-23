package nonlinear;


import basic.structure.Graph;
import basic.structure.Queue;

public class 多余连接 {
    public static void main(String[] args) {
        int[][] edges={
            {0,1},{1,2},{2,3},{3,1}
        };
        Graph graph=new Graph(4,edges);
        System.out.println(bfs(1,graph.getVisited(),graph));
    }
    private static boolean bfs(int v,boolean[] visited,Graph graph){
        visited[v]=true;
        Queue<Integer> queue = new Queue<>();
        queue.enqueue(v);
        while(!queue.isEmpty()){
            int vertex=queue.dequeue();
            for(int w:graph.getGraph().get(vertex)){
                if(!visited[w]){
                    queue.enqueue(w);
                    visited[w]=true;
                }
            }
        }
        return false;
    }
}
