package basic.structure.node;

public class Edge {
    private int weight; // 边权重
    private Vertex dest; // 相邻顶点

    /**
     * @return 获得权重
     */
    public int getWeight() {
        return weight;
    }

    /**
     * @param weight 设置权重
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * @return 获得相邻顶点
     */
    public Vertex getDest() {
        return dest;
    }

    /**
     * @param dest 设置相邻顶点
     */
    public void setDest(Vertex dest) {
        this.dest = dest;
    }
}