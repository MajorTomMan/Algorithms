package com.majortom.algorithms.app.leetcode.nonlinear;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.majortom.algorithms.core.basic.Digraph;
import com.majortom.algorithms.core.basic.node.Edge;
import com.majortom.algorithms.core.basic.node.Vertex;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 最短路径算法测试{
    /* 有向带权图 */
    private static Digraph graph=new Digraph();
    /* 距离 */
    private static Map<Vertex,Integer> distance=new HashMap<>();
    /* 前驱节点集合 */
    private static Map<Vertex,Vertex> previous=new HashMap<>();
    /* 未访问的节点集合,排序规则是每个顶点到源点的距离从小到大*/
    private static PriorityQueue<Vertex> unvisited=new PriorityQueue<>(
        (v1,v2)-> distance.get(v1)-distance.get(v2)
    );
    /* 最短路径列表 */
    private static List<Vertex> shortestPath = new ArrayList<>();
    
    public static void main(String[] args) {
        Vertex v1 = new Vertex("上海", new ArrayList<>());
        Vertex v2 = new Vertex("北京", new ArrayList<>());
        Vertex v3 = new Vertex("天津", new ArrayList<>());
        Vertex v4 = new Vertex("济南", new ArrayList<>());
        Vertex v5 = new Vertex("南京", new ArrayList<>());
        Vertex v6 = new Vertex("石家庄", new ArrayList<>());
        Vertex v7 = new Vertex("郑州", new ArrayList<>());
        graph.addVertex(v1);
        graph.addVertex(v2);
        graph.addVertex(v3);
        graph.addVertex(v4);
        graph.addVertex(v5);
        graph.addVertex(v6);
        graph.addVertex(v7);
        graph.addEdge(v1, v2, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v1, v3, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v1, v4, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v2, v1, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v2, v4, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v2, v5, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v3, v1, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v3, v6, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v3, v7, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v4, v2, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v4, v5, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v4, v6, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v5, v3, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v5, v1, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v5, v2, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v6, v1, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v6, v3, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v6, v7, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v7, v5, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v7, v4, AlgorithmsUtils.randomNum(7,1));
        graph.addEdge(v7, v3, AlgorithmsUtils.randomNum(7,1));
        Dijkstra(v6,v5);
    }
    /*  最短路径算法之迪杰斯特拉算法的实现 */
    private static void Dijkstra(Vertex src,Vertex dest) {
        /* 初始化距离为0 */
        distance.put(src, 0);
        /* 将所有未访问的节点距离初始化为无穷大 */
        for (Vertex v: getAllVertex() ) {
            if(!v.equals(src)){
                distance.put(v, Integer.MAX_VALUE);
            }
        }
        /* 将所有节点加入到未访问的集合 */
        unvisited.addAll(getAllVertex());
        while(!unvisited.isEmpty()){
            /* 抛出节点 */
            Vertex current=unvisited.poll();
            /* 如果这个终点已经被访问过了,结束循环 */
            if(current==dest){
                break;
            }
            /* 访问现在节点的每个邻居节点 */
            for (Edge edge : current.getEdges()) {
                Vertex neighbor=edge.getDest();
                int weight = edge.getWeight();
                
                /* 如果现在节点的距离加上邻居节点的距离小于邻居节点的距离 
                 * 即检查从源点到邻居点的总距离是否小于算法目前找到的源点到邻居点的距离
                 * 如果这个条件成立，就意味着通过经过当前点,算法找到了从源点到邻居点的一条更短的路径。
                 * 所以就要更新源点到邻居点的距离.
                 * 这样下一次访问邻居点时，就可以使用更新过后的更短路径来访问。
                */
                if(distance.get(current)+weight<distance.get(neighbor)){
                    /* 将找到的更短的路径更新为到邻居节点的路径 */
                    distance.put(neighbor, distance.get(current) + weight);
                    /* 将邻居节点和现在节点都加入到前驱节点集合 */
                    previous.put(neighbor, current);
                    /* 将邻居节点从未访问顶点集合中删除 */
                    unvisited.remove(neighbor);
                    /* 再将邻居节点加入未访问顶点集合 */
                    unvisited.add(neighbor);
                }
            }
        }
         // 重建最短路径
         /* 将前驱节点的各个节点都添加到最短路径列表中 */
         for (Vertex v = dest; v != src; v = previous.get(v)) {
             shortestPath.add(v);
         }
         shortestPath.add(src);
         /* 因为前驱节点是倒转过来的,所以要倒转回来 */
         Collections.reverse(shortestPath);
         /* 打印结果 */
         System.out.println("从" + src.getName() + "到" + dest.getName() + "的最短路径:");
         for (int i=0; i<shortestPath.size(); i++) {
            Vertex v=shortestPath.get(i);
            if(i==shortestPath.size()-1){
                System.out.printf(v.getName());
            }
            else{
                System.out.printf(v.getName()+"->");
            }
         }
         System.out.println();
         System.out.println("\n最短距离是: " + distance.get(dest));
 
    }
    private static List<Vertex> getAllVertex(){
        return graph.getVertexs();
    }

}
