package NonLinear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import Basic.Structure.Digraph;
import Basic.Structure.Queue;
import Basic.Structure.Node.Edge;
import Basic.Structure.Node.Vertex;

public class 拓扑排序 {
    private static Digraph digraph = new Digraph();
    private static HashMap<String, Integer> inNumberList = new HashMap<>();

    public static void main(String[] args) {
        Vertex First = new Vertex("Must be Learn", new ArrayList<>());
        Vertex a = new Vertex("Java", new ArrayList<>());
        Vertex b = new Vertex("JavaSE", new ArrayList<>());
        Vertex c = new Vertex("JavaEE", new ArrayList<>());
        Vertex d = new Vertex("Spring", new ArrayList<>());
        Vertex e = new Vertex("SpringMVC", new ArrayList<>());
        Vertex f = new Vertex("SpringBoot", new ArrayList<>());
        Vertex g = new Vertex("Sql", new ArrayList<>());
        Vertex h = new Vertex("MySql", new ArrayList<>());
        digraph.addVertex(First);
        digraph.addVertex(a);
        digraph.addVertex(b);
        digraph.addVertex(c);
        digraph.addVertex(d);
        digraph.addVertex(e);
        digraph.addVertex(f);
        digraph.addVertex(g);
        digraph.addVertex(h);
        digraph.addEdge(First, a, 0);
        digraph.addEdge(First, g, 0);
        digraph.addEdge(a, b, 0);
        digraph.addEdge(a, c, 0);
        digraph.addEdge(b, d, 0);
        digraph.addEdge(b, d, 0);
        digraph.addEdge(d, e, 0);
        digraph.addEdge(d, f, 0);
        digraph.addEdge(g, h, 0);
        digraph.addEdge(g, d, 0);
        System.out.println(TopicalOrder(First));
    }

    private static String TopicalOrder(Vertex v) {
        if (digraph.hasCycle()) {
            return "Graph Has Cycle So it can't to be Ordered!";
        }
        BFS(v);
        String result = new String();
        Queue<Vertex> queue = new Queue<>();
        queue.enqueue(v);
        result += v.getName() + "->";
        while (!queue.isEmpty()) {
            Vertex vertex = queue.dequeue();
            for (int i = 0; i < vertex.getEdges().size(); i++) {
                Vertex dest = vertex.getEdges().get(i).getDest();
                if(inNumberList.get(dest.getName())>0){
                    Integer inNumber = inNumberList.get(dest.getName());
                    inNumberList.put(dest.getName(),inNumber-1);
                }
            }
            for (Edge edge : vertex.getEdges()) {
                Vertex dest = edge.getDest();
                if(inNumberList.get(dest.getName())==0){
                    queue.enqueue(dest);
                    result+=dest.getName()+"->";
                }
            }

        }
        return result;
    }

    /* 广度优先搜索遍历顶点集合 */
    private static void BFS(Vertex v) {
        Queue<Vertex> queue = new Queue<>();
        Set<Vertex> visited = new HashSet<>();
        visited.add(v);
        queue.enqueue(v);
        inNumberList.put(v.getName(), 0);
        while (!queue.isEmpty()) {
            Vertex vertex = queue.dequeue();
            for (Edge edge : vertex.getEdges()) {
                Vertex dest = edge.getDest();
                if (inNumberList.containsKey(dest.getName())) {
                    Integer inNumber = inNumberList.get(dest.getName());
                    inNumberList.put(dest.getName(), inNumber + 1);
                } else {
                    inNumberList.put(dest.getName(), 1);
                }
                if (!visited.contains(dest)) {
                    visited.add(dest);
                    queue.enqueue(dest);
                }
            }
        }
    }
}
