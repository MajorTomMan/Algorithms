package Graph;

import Basic.Structure.Queue;
import Graph.Dir.EdgeWeightedGraph;
import Graph.E.Edge;
import Sort.Structure.PQ.MinPQ;

public class KruskalMST { //加权无向图寻找最小生成树算法中的Kruskal算法
   private Queue<Edge> mst;
   public KruskalMST(EdgeWeightedGraph G){
       mst=new Queue<>();
       MinPQ<Edge> pq=new MinPQ<>();
       for(Edge e:G.edges()){
           pq.insert(e);
       }
       UF uf=new UF(G.V());
       while(!pq.isEmpty()&&mst.Size()<G.V()-1){
           Edge e=pq.delMin(); //从pq得到权重最小的边和它的顶点
           int v=e.either(),w=e.other(v);
           if(uf.connected(v, w)){ //忽略失效的边
               continue;
           }
           uf.union(v,w);
           mst.enqueue(e);
       }
   }
   public Iterable<Edge> edges(){
       return mst;
   }
   public double weight(){
       return 0.0;
   }
}
