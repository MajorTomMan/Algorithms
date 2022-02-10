package Graph.Dir;

import Basic.Structure.Bag;
import Graph.E.Edge;

public class EdgeWeightedGraph { //加权无向图的实现
    private final int V;
    private int E;
    private Bag<Edge>[] adj;
    public EdgeWeightedGraph(int V) {
        this.V = V;
        E = 0;
        adj=(Bag<Edge>[]) new Bag[V];
        for (int v = 0; v < V; v++) {
            adj[v]=new Bag<Edge>();
        }
    }
    public int V(){
        return V;
    }
    public int E(){
        return E;
    }
    public void addEdge(Edge e){
        int v=e.either(),w=e.other(v);
        adj[v].add(e);
        adj[w].add(e);
        E++;
    }
    public Iterable<Edge> adj(int v){
        return adj[v];
    }
    public Iterable<Edge> edges(){
        Bag<Edge> b=new Bag<Edge>();
        for(int v=0;v<V;v++){
            for(Edge e:adj[v]){
                if(e.other(V)>v){
                    b.add(e);
                }
            }
        }
        return b;
    }
}
