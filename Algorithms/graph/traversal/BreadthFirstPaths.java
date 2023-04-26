package graph.traversal;

import basic.structure.Queue;
import basic.structure.Stack;
import graph.undig.Graph;

public class BreadthFirstPaths{//广度优先搜索实现查找图路径算法
    private boolean[] marked; //是否调用过dfs了
    private int[] edgeTo; // 从起点到一个顶点的已知路径上的最后一个路径
    private final int s; //起点
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
