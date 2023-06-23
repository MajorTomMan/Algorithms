package nonlinear;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import basic.structure.Digraph;
import basic.structure.Queue;
import basic.structure.node.Edge;
import basic.structure.node.Vertex;

public class 有向图测试 {
    public static void main(String[] args) {
        Digraph digraph=new Digraph();
        Vertex a = new Vertex("a",new ArrayList<>());
        Vertex b = new Vertex("b",new ArrayList<>());
        Vertex c = new Vertex("c",new ArrayList<>());
        Vertex d = new Vertex("d",new ArrayList<>());
        digraph.addVertex(a);
        digraph.addVertex(b);
        digraph.addVertex(c);
        digraph.addVertex(d);
        digraph.addEdge(a, b, 3);
        digraph.addEdge(a, c, 4);
        digraph.addEdge(b, c, 4);
        digraph.addEdge(b, d, 2);
        digraph.addEdge(c, d, 5);
        digraph.addEdge(c, a, 5);
        System.out.println(digraph.hasCycle());
        BFS(a);
    }
    /* 广度优先搜索遍历顶点集合 */
    private static void BFS(Vertex v){
        Queue<Vertex> queue=new Queue<>();
        Set<Vertex> visited=new HashSet<>();
        visited.add(v);
        queue.enqueue(v);
        System.out.println(v.getName()+" is Visited");
        while(!queue.isEmpty()){
            Vertex vertex = queue.dequeue();
            for (Edge edge : vertex.getEdges()) {
                Vertex dest = edge.getDest();
                if(!visited.contains(dest)){
                    visited.add(dest);
                    queue.enqueue(dest);
                    System.out.println(dest.getName()+" is Visited");
                }
            }
        }
    }
}
