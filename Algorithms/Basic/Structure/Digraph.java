package Basic.Structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import Basic.Structure.Node.Edge;
import Basic.Structure.Node.Vertex;

public class Digraph {
    /* 顶点集合 */
    private List<Vertex> vertexs = new ArrayList<>();

    /* 增加顶点 */
    public void addVertex(Vertex v) {
        vertexs.add(v);
    }

    /* 增加边 */
    public void addEdge(Vertex src, Vertex dest, int weight) {
        Edge edge = new Edge();
        edge.setDest(dest);
        edge.setWeight(weight);
        src.getEdges().add(edge);
    }

    /* 检测图中是否有环 */
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
            for (Edge edge : vertex.getEdges()) {
                Vertex dest = edge.getDest();
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
        vertexs.stream().forEach(item->{
            visited.put(item, false);
        });
        return hasPath(src, dest, visited);
    }

    private Boolean hasPath(Vertex src, Vertex dest, HashMap<Vertex, Boolean> visited) {
        visited.put(src, true);
        if(src==dest){
            return true;
        }
        for (Edge edge : src.getEdges()) {
            Vertex vertex = edge.getDest();
            if(!visited.get(vertex)){
                Boolean isFind = hasPath(vertex, dest, visited);
                if(isFind){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return 获取顶点
     */
    public List<Vertex> getVertexs() {
        return vertexs;
    }

    /**
     * @param vertexs 设置顶点
     */
    public void setVertexs(List<Vertex> vertexs) {
        this.vertexs = vertexs;
    }
}
