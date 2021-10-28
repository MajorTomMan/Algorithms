package Graph.Traversal;

import Basic.Structure.Queue;
import Basic.Structure.Stack;
import Graph.UnDig.Graph;

public class BreadthFirstPaths{//广度优先搜索实现查找图路径算法
    private boolean[] marked;
    private int[] edgeTo;
    private final int s;
    public BreadthFirstPaths(Graph G,int s){
        marked=new boolean[G.V()];
        edgeTo=new int[G.V()];
        this.s=s;
        bfs(G,s);
    }
    private void bfs(Graph G, int s) {
        Queue<Integer> queue=new Queue<>();
        marked[s]=true;
        queue.enqueue(s);
        while(!queue.isEmpty()){
            int v=queue.dequeue();
            for (int w: G.adj(v)) {
                if(!marked[w]){
                    edgeTo[w]=v;
                    marked[w]=true;
                    queue.enqueue(w);
                }
            }
        }
    }
    public boolean hasPathTo(int v){
        return marked[v];
    }
    public Iterable<Integer> pathTo(int v){
        if(!hasPathTo(v)){
            return null;
        }
        Stack<Integer> path=new Stack<>();
        for(int x=v;x!=s;x=edgeTo[x]){
            path.push(x);
        }
        path.push(s);
        return path;
    }
}
