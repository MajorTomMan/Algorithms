package basic.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import basic.structure.node.Edge;
import basic.structure.node.Vertex;

public class Digraph {
    /* 顶点集合 */
    private List<Vertex> vertexs = new ArrayList<>();
    private static HashMap<String, Integer> inNumberList = new HashMap<>();

    /* 增加顶点 */
    public void addVertex(Vertex v) {
        if (!vertexs.contains(v)) {
            vertexs.add(v);
        }
    }

    /* 增加边 */
    public void addEdge(Vertex src, Vertex dest, int weight) {
        Edge edge = new Edge();
        edge.setDest(dest);
        edge.setWeight(weight);
        src.getEdges().add(edge);
    }

    /* 广度优先搜索检测图中是否有环 */
    public Boolean hasCycle() {
        if (vertexs.isEmpty()) {
            return false;
        }
        Queue<Vertex> queue = new Queue<>();
        Set<Vertex> visited = new HashSet<>();
        Vertex v = vertexs.get(0);
        visited.add(v);
        queue.enqueue(v);
        while (!queue.isEmpty()) {
            Vertex vertex = queue.dequeue();
            /* 遍历所有邻居节点 */
            for (Edge edge : vertex.getEdges()) {
                Vertex dest = edge.getDest();
                /* 取出邻居节点中相连的节点并检查是否有相连 */
                for (Edge e : dest.getEdges()) {
                    if (e.getDest() == vertex) {
                        return true;
                    }
                    if (!visited.contains(dest)) {
                        visited.add(dest);
                        queue.enqueue(dest);
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检测图中是否有从Src到Dest有路径相连
     * 
     * @param Src  起点
     * @param Dest 终点
     * 
     * 
     */
    public Boolean hasPath(Vertex src, Vertex dest) {
        if (vertexs.isEmpty()) {
            return false;
        }
        HashMap<Vertex, Boolean> visited = new HashMap<>();
        vertexs.stream().forEach(item -> {
            visited.put(item, false);
        });
        return hasPath(src, dest, visited);
    }

    /* 深度优先算法实现查找两点之间是否连通 */
    private Boolean hasPath(Vertex src, Vertex dest, HashMap<Vertex, Boolean> visited) {
        visited.put(src, true);
        if (src == dest) {
            return true;
        }
        /* 查找该点相连的邻居节点 */
        for (Edge edge : src.getEdges()) {
            Vertex vertex = edge.getDest();
            /* 如果尚未访问到就继续递归寻找路径,找到了直接返回true */
            if (!visited.get(vertex)) {
                Boolean isFind = hasPath(vertex, dest, visited);
                if (isFind) {
                    return true;
                }
            }
        }
        return false;
    }

    /* 广度优先搜索遍历顶点集合并找出其对应的节点入度 */
    private void BFS(Vertex v) {
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

    /**
     * @return 获取顶点
     */
    public List<Vertex> getVertexs() {
        return vertexs;
    }

    /**
     * @param Vextex 起点
     * @return 入度表
     */

    public HashMap<String, Integer> getInNumberList() {
        BFS(vertexs.get(0));
        return inNumberList;
    }

    /**
     * @param vertexs 设置顶点
     */
    public void setVertexs(List<Vertex> vertexs) {
        this.vertexs = vertexs;
    }
}
