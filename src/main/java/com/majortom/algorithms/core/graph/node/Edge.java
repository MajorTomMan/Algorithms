package com.majortom.algorithms.core.graph.node;

/**
 * 边类
 * 
 * @param <V> 顶点数据类型
 */
public class Edge<V> {
    private Vertex<V> source; // 起点
    private Vertex<V> dest; // 终点
    private int weight; // 权重

    // --- 可视化属性 ---
    private boolean isHighlighted; // 是否为算法当前选中的路径边

    public Edge(Vertex<V> source, Vertex<V> dest, int weight) {
        this.source = source;
        this.dest = dest;
        this.weight = weight;
        this.isHighlighted = false;
    }

    // Getter & Setter
    public Vertex<V> getSource() {
        return source;
    }

    public Vertex<V> getDest() {
        return dest;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setHighlighted(boolean highlighted) {
        isHighlighted = highlighted;
    }
}