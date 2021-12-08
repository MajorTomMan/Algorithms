package Graph.UnDig;

import Basic.Structure.Linkedlist;

public class GraphST{
    private final String[] E;
    private final Linkedlist<String[]> V;
    private int i;
    public GraphST() {
        E=new String[10];
        V=new Linkedlist<>();
        
    }
    public void addEdge(String v,String edge){
        E[i]=edge;
        V.Insert(E);
    }
}
