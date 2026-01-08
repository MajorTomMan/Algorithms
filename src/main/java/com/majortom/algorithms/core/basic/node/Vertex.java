package com.majortom.algorithms.core.basic.node;

import java.util.List;

public class Vertex {
    private String name; // 名字
    private List<Edge> edges; // 边

    /**
     * @param name
     * @param edges
     */
    public Vertex(String name, List<Edge> edges) {
        this.name = name;
        this.edges = edges;
    }

    /**
     * @return 获取顶点名字
     */
    public String getName() {
        return name;
    }

    /**
     * @param name 设置顶点名字
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return 获取边
     */
    public List<Edge> getEdges() {
        return edges;
    }

    /**
     * @param edges 设置边
     */
    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }
}
