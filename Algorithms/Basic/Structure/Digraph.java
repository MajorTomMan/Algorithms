package Basic.Structure;

import java.util.ArrayList;
import java.util.List;

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
