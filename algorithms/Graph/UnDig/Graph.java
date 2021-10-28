package Graph.UnDig;

import Basic.Structure.Bag;
import Func.Input_Output.StdIn;

public class Graph{ //无向图定义和基础算法
    private final int V;
    private int E;
    private Bag<Integer>[] adj;
    public Graph(int V){
        this.V=V;
        this.E=0;
        adj=(Bag<Integer>[]) new Bag[V];
        for(int v=0;v<V;v++){
            adj[v]=new Bag<Integer>();
        }
    }
    public Graph(StdIn in){
        this(StdIn.readInt());
        int E=StdIn.readInt();
        for(int i=0;i<E;i++){
            int v=StdIn.readInt();
            int w=StdIn.readInt();
            addEdge(v, w);
        }
    }
    public int V(){ //顶点数
        return V;
    }
    public int E(){ //边数
        return E;
    } 
    public void addEdge(int v,int w){ //向图中添加一条边v-w
        adj[v].add(w);
        adj[w].add(v);
        E++;
    } 
    public Iterable<Integer> adj(int v){ //和v相邻的所有顶点
        return adj[v];
    }
    public static int degree(Graph G,int v){
        int degree=0;
        for(int w:G.adj(v)){
            degree++;
        }
        return degree;
    }
    public static double avgDegree(Graph G){
        return 2.0*G.E()/G.V();
    }
    public static int numberOfSelfLoops(Graph G){
        int count=0;
        for(int v=0;v<G.V();v++){
            for(int w:G.adj(v)){
                if(v==w){
                    count++;
                }
            }
        }
        return count/2;
    }
    public String toString(){
        String s=V+" vertices, "+E+"edges\n";
        for(int v=0;v<V;v++){
            s+=v+": ";
            for(int w:this.adj(v)){
                s+=w+" ";
            }
            s+="\n";
        }
        return s;
    }
}
