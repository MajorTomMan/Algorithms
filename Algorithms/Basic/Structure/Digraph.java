package Basic.Structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import Basic.Structure.Node.Edge;
import Basic.Structure.Node.Vertex;

public class Digraph {
    /* 顶点集合 */
    private List<Vertex> vertexs=new ArrayList<>();

    /* 增加顶点 */
    public void addVertex(Vertex v){
        vertexs.add(v);
    }
    /* 增加边 */
    public void addEdge(Vertex src,Vertex dest,int weight){
        Edge edge=new Edge();
        edge.setDest(dest);
        edge.setWeight(weight);
        src.getEdges().add(edge);
    }
    public Boolean hasCycle(){
        Queue<Vertex> queue=new Queue<>();
        Set<Vertex> visited=new HashSet<>();
        Vertex v = vertexs.get(0);
        visited.add(v);
        queue.enqueue(v);
        while(!queue.isEmpty()){
            Vertex vertex = queue.dequeue();
            for (Edge edge : vertex.getEdges()) {
                Vertex dest = edge.getDest();
                for (Edge e : dest.getEdges()) {
                    if(e.getDest()==vertex){
                        return true;
                    }
                    if(!visited.contains(dest)){
                        visited.add(dest);
                        queue.enqueue(dest);
                    }
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
