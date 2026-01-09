package com.majortom.algorithms.core.visualization.impl.panel;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.node.Edge;
import com.majortom.algorithms.core.graph.node.Vertex;
import com.majortom.algorithms.core.visualization.BasePanel;

import java.awt.*;

public class GraphPanel<V> extends BasePanel<BaseGraph<V>> {
    private static final int NODE_RADIUS = 20;

    public GraphPanel(BaseGraph<V> data) {
        super(data);
        // 显式设置画布的推荐大小，比如 800x600
        this.setPreferredSize(new Dimension(800, 600));
        // 之前的深色背景设置
        setBackground(new Color(33, 37, 43));
    }

    @Override
    protected void calculateScale() {
        // 图算法通常使用固定坐标或力导向布局，cellSize 可作为节点缩放系数
        this.cellSize = 1;
    }

    @Override
    protected void render(Graphics2D g2d) {
        if (data == null)
            return;

        // 1. 绘制所有的边 (先画线，防止覆盖点)
        for (Vertex<V> v : data.getVertices()) {
            for (Edge<V> edge : v.getEdges()) {
                drawEdge(g2d, edge);
            }
        }

        // 2. 绘制所有的顶点
        for (Vertex<V> v : data.getVertices()) {
            drawVertex(g2d, v);
        }
    }

    private void drawEdge(Graphics2D g, Edge<V> edge) {
        Vertex<V> src = edge.getSource();
        Vertex<V> dest = edge.getDest();

        // 逻辑：如果当前 activeA 是起点，activeB 是终点，则高亮这条边
        if (activeA == src && activeB == dest) {
            g.setColor(new Color(224, 108, 117)); // 高亮红
            g.setStroke(new BasicStroke(3.0f));
        } else {
            g.setColor(new Color(90, 94, 105)); // 默认灰
            g.setStroke(new BasicStroke(1.5f));
        }

        g.drawLine(src.getX(), src.getY(), dest.getX(), dest.getY());

        // 如果是有权图，可以在中点画权重
        if (edge.getWeight() != 0) {
            g.setColor(Color.GRAY);
            g.drawString(String.valueOf(edge.getWeight()), (src.getX() + dest.getX()) / 2,
                    (src.getY() + dest.getY()) / 2);
        }
    }

    private void drawVertex(Graphics2D g, Vertex<V> v) {
        // 节点颜色状态映射
        if (v == activeA) {
            g.setColor(new Color(152, 195, 121)); // 正在访问：绿
        } else if (v.isVisited()) {
            g.setColor(new Color(97, 175, 239)); // 已访问：蓝
        } else {
            g.setColor(new Color(45, 49, 58)); // 未访问：深灰
        }

        g.fillOval(v.getX() - NODE_RADIUS, v.getY() - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

        // 画圆圈边界
        g.setColor(Color.WHITE);
        g.drawOval(v.getX() - NODE_RADIUS, v.getY() - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

        // 画文字：需要计算文字宽度以实现水平居中
        String label = v.getData().toString();
        FontMetrics fm = g.getFontMetrics();
        int textX = v.getX() - fm.stringWidth(label) / 2;
        int textY = v.getY() + fm.getAscent() / 2 - 2; // 垂直居中修正
        g.drawString(label, textX, textY);
    }
}