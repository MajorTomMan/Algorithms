package graph;

import basic.structure.Stack;
import graph.dir.DirectedEdge;
import graph.dir.EdgeWeightedDigraph;
import sort.structure.pq.IndexMinPQ;

public class DijkstraSP { //图的最短路径算法
    private DirectedEdge[] edgeTo; //距离树最近的边
    private double[] distTo; //distTo[w]=edgeTo[w].weight()
    private IndexMinPQ<Double> pq; //有效的横切边
    public DijkstraSP(EdgeWeightedDigraph G,int s){
        edgeTo=new DirectedEdge[G.V()];
        distTo=new double[G.V()];
        pq=new IndexMinPQ<>(G.V());
        for(int v=0;v<G.V();v++){
            distTo[v]=Double.POSITIVE_INFINITY;
        }
        distTo[s]=0.0;
        pq.insert(s,0.0); //用顶点0和权重0初始化pq
        while(!pq.isEmpty()){
            relax(G,pq.delMin());
        }
    }
    private void relax(EdgeWeightedDigraph G,int v){
        //将顶点v添加到树中,更新数据
        for(DirectedEdge e:G.adj(v)){
            int w=e.to();
            if(distTo[w]>distTo[v]+e.weight()){
                distTo[w]=distTo[v]+e.weight();
                edgeTo[w]=e;
                if(pq.contains(w)){
                    pq.changeKey(w,distTo[w]);
                }
                else{
                    pq.insert(w,distTo[w]);
                }
            }
        }
    }
    public double distTo(int v){
        return distTo[v];
    }
    public boolean hasPathTo(int v){
        return distTo[v]<Double.POSITIVE_INFINITY;
    }
    public Iterable<DirectedEdge> pathTo(int v){ //查询方法
        if(!hasPathTo(v)){
            return null;
        }
        Stack<DirectedEdge> path=new Stack<>();
        for (DirectedEdge e=edgeTo[v];e!=null;e=edgeTo[e.from()]) {
            path.push(e);
        }
        return path;
    }
}
