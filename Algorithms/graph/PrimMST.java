
package graph;

import graph.dir.EdgeWeightedGraph;
import graph.e.Edge;
import sort.structure.pq.IndexMinPQ;

public class PrimMST{ //加权无向图最小生成树算法中的即时Prim算法
    private Edge[] edgeTo; //距离树最近的边
    private double[] distTo; //distTo[w]=edgeTo[w].weight()
    private boolean[] marked; //如果v在树中则为true
    private IndexMinPQ<Double> pq; //有效的横切边
    public PrimMST(EdgeWeightedGraph G){
        edgeTo=new Edge[G.V()];
        distTo=new double[G.V()];
        marked=new boolean[G.V()];
        for(int v=0;v<G.V();v++){
            distTo[v]=Double.POSITIVE_INFINITY;
        }
        pq=new IndexMinPQ<>(G.V());
        distTo[0]=0.0;
        pq.insert(0,0.0); //用顶点0和权重0初始化pq
        while(!pq.isEmpty()){
            visit(G,pq.delMin());
        }
    }
    private void visit(EdgeWeightedGraph G,int v){
        //将顶点v添加到树中,更新数据
        marked[v]=true;
        for(Edge e:G.adj(v)){
            int w=e.other(v);
            if(marked[w]){
                continue;
            }
            if(e.weight()<distTo[w]){
                //连接w和树的最佳边edge变为e
                edgeTo[w]=e;
                distTo[w]=e.weight();
                if(pq.contains(w)){
                    pq.changeKey(w,distTo[w]);
                }
                else{
                    pq.insert(w,distTo[w]);
                }
            }
        }
    }
}