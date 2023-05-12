/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-27 13:44:49
 * @FilePath: /alg/App/NonLinear/拓扑排序.java
 */
package NonLinear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import basic.structure.Digraph;
import basic.structure.Queue;
import basic.structure.node.Edge;
import basic.structure.node.Vertex;

public class 拓扑排序 {
    private static Digraph digraph = new Digraph();

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
        digraph.addEdge(d, e, 0);
        digraph.addEdge(d, f, 0);
        digraph.addEdge(g, h, 0);
        TopicalOrder(First);
        printTopicalOrder(First, 0);
    }

    /* 以BFS打底作为拓扑排序框架 */
    private static void TopicalOrder(Vertex v) {
        if (digraph.hasCycle()) {
            return;
        }
        /* 先构建一张入度表来获取各节点之间的入度 */
        HashMap<String, Integer> inNumberList = digraph.getInNumberList();
        Queue<Vertex> queue = new Queue<>();
        queue.enqueue(v);
        while (!queue.isEmpty()) {
            /* 弹出节点并将其相连的节点入度减一 */
            Vertex vertex = queue.dequeue();
            for (int i = 0; i < vertex.getEdges().size(); i++) {
                Vertex dest = vertex.getEdges().get(i).getDest();
                if (inNumberList.get(dest.getName()) > 0) {
                    Integer inNumber = inNumberList.get(dest.getName());
                    inNumberList.put(dest.getName(), inNumber - 1);
                }
            }
            /* 将入度为0的节点加入队列 */
            for (Edge edge : vertex.getEdges()) {
                Vertex dest = edge.getDest();
                if (inNumberList.get(dest.getName()) == 0) {
                    queue.enqueue(dest);
                }
            }
            /* 循环操作直到队列为空 */
        }
    }
    /* 打印拓扑排序 */
    private static void printTopicalOrder(Vertex vertex, int level) {
        for (int i = 0; i < level; i++) {
            System.out.print(" ");
        }
        System.out.println(vertex.getName());
        level++;
        for (Edge edge : vertex.getEdges()) {
            Vertex dest = edge.getDest();
            printTopicalOrder(dest, level);
        }
    }
}
