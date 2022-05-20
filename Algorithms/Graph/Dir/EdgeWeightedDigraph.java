package Graph.Dir;

import java.util.NoSuchElementException;

import Basic.Structure.Bag;
import Basic.Structure.Stack;
import Func.Input_Output.In;

public class EdgeWeightedDigraph{ //加权有向图实现
    private final int V;
    private int E;
    private Bag<DirectedEdge>[] adj;
    private int[] indegree;
    public EdgeWeightedDigraph(int V) {
        this.V = V;
        E = 0;
        adj=(Bag<DirectedEdge>[]) new Bag[V];
        for (int v = 0; v < V; v++) {
            adj[v]=new Bag<DirectedEdge>();
        }
    }
    public EdgeWeightedDigraph(In in) {
        if (in == null) throw new IllegalArgumentException("argument is null");
        try {
            this.V = in.readInt();
            if (V < 0) throw new IllegalArgumentException("number of vertices in a Digraph must be non-negative");
            indegree = new int[V];
            adj = (Bag<DirectedEdge>[]) new Bag[V];
            for (int v = 0; v < V; v++) {
                adj[v] = new Bag<DirectedEdge>();
            }

            int E = in.readInt();
            if (E < 0) throw new IllegalArgumentException("Number of edges must be non-negative");
            for (int i = 0; i < E; i++) {
                int v = in.readInt();
                int w = in.readInt();
                validateVertex(v);
                validateVertex(w);
                double weight = in.readDouble();
                addEdge(new DirectedEdge(v, w, weight));
            }
        }   
        catch (NoSuchElementException e) {
            throw new IllegalArgumentException("invalid input format in EdgeWeightedDigraph constructor", e);
        }
    }
    private void validateVertex(int v) {
        if (v < 0 || v >= V)
            throw new IllegalArgumentException("vertex " + v + " is not between 0 and " + (V-1));
    }
    public int V(){
        return V;
    }
    public int E(){
        return E;
    }
    public void addEdge(DirectedEdge e){
        adj[e.from()].add(e);
        E++;
    }
    public Iterable<DirectedEdge> adj(int v){
        return adj[v];
    }
    public Iterable<DirectedEdge> edges(){
        Bag<DirectedEdge> bag=new Bag<DirectedEdge>();
        for(int v=0;v<V;v++){
            for(DirectedEdge e:adj[v]){
                bag.add(e);
            }
        }
        return bag;
    }
    public EdgeWeightedDigraph(EdgeWeightedDigraph G) {
        this(G.V());
        this.E = G.E();
        for (int v = 0; v < G.V(); v++)
            this.indegree[v] = G.indegree(v);
        for (int v = 0; v < G.V(); v++) {
            // reverse so that adjacency list is in same order as original
            Stack<DirectedEdge> reverse = new Stack<DirectedEdge>();
            for (DirectedEdge e : G.adj[v]) {
                reverse.push(e);
            }
            for (DirectedEdge e : reverse) {
                adj[v].add(e);
            }
        }
    }
    public int outdegree(int v) {
        validateVertex(v);
        return adj[v].Size();
    }
    public int indegree(int v) {
        validateVertex(v);
        return indegree[v];
    }

}
