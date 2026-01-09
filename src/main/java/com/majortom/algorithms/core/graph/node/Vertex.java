package com.majortom.algorithms.core.graph.node;

import java.util.ArrayList;
import java.util.List;

/**
 * 顶点类
 * 
 * @param <V> 顶点存放的数据类型
 */
public class Vertex<V> {
    private V data; // 顶点存放的数据内容
    private List<Edge<V>> edges; // 从该顶点出发的边

    // --- 可视化属性 ---
    private int x, y; // 顶点在画布上的物理坐标
    private boolean visited; // 算法通用状态：是否被访问

    public Vertex(V data) {
        this(data, 0, 0);
    }

    public Vertex(V data, int x, int y) {
        this.data = data;
        this.x = x;
        this.y = y;
        this.edges = new ArrayList<>();
        this.visited = false;
    }

    // 快捷添加边的方法
    public void addEdge(Vertex<V> dest, int weight) {
        this.edges.add(new Edge<>(this, dest, weight));
    }

    // Getter & Setter
    public V getData() {
        return data;
    }

    public void setData(V data) {
        this.data = data;
    }

    public List<Edge<V>> getEdges() {
        return edges;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }
}