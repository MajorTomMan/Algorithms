package com.majortom.algorithms.core.visualization.impl.panel;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.node.Edge;
import com.majortom.algorithms.core.graph.node.Vertex;
import com.majortom.algorithms.core.visualization.BasePanel;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * 图算法可视化画布（Graph Visualization Panel）
 * 修复：解决遍历时高亮圆圈覆盖节点数据的图层问题。
 * 兼容：适配 Vertex 类的 getX() 和 getY() 方法。
 */
public class GraphPanel<V> extends BasePanel<BaseGraph<V>> {
    private final int RADIUS = 20; // 节点半径

    public GraphPanel(BaseGraph<V> data) {
        super(data);
        this.setPreferredSize(new Dimension(800, 600));
    }

    @Override
    protected void render(Graphics2D g) {
        if (data == null)
            return;

        Collection<Vertex<V>> vertices = data.getVertices();

        // --- 第一步：绘制所有的边 ---
        drawEdges(g, vertices);

        // --- 第二步：绘制节点主体和高亮装饰 ---
        // 先画所有的圆，确保它们都在边（Edge）的上方
        for (Vertex<V> v : vertices) {
            drawVertexBody(g, v);
        }

        // --- 第三步：最后统一绘制所有节点的文字 ---
        // 关键修复：文字绘制放在最后，确保处于最顶层
        for (Vertex<V> v : vertices) {
            drawVertexText(g, v);
        }
    }

    private void drawEdges(Graphics2D g, Collection<Vertex<V>> vertices) {
        g.setStroke(new BasicStroke(1.5f));
        g.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));

        Color edgeColor = UIManager.getColor("Separator.foreground");
        if (edgeColor == null)
            edgeColor = new Color(171, 178, 191, 100);

        for (Vertex<V> v : vertices) {
            int x1 = v.getX();
            int y1 = v.getY();

            for (Edge<V> edge : v.getEdges()) {
                Vertex<V> dest = edge.getDest();
                int x2 = dest.getX();
                int y2 = dest.getY();

                // 绘制边线
                g.setColor(edgeColor);
                g.drawLine(x1, y1, x2, y2);

                // 如果有权重，绘制权重文字
                if (edge.getWeight() != 0) {
                    g.setColor(UIManager.getColor("Label.disabledForeground"));
                    g.drawString(String.valueOf(edge.getWeight()), (x1 + x2) / 2, (y1 + y2) / 2);
                }
            }
        }
    }

    private void drawVertexBody(Graphics2D g, Vertex<V> v) {
        int x = v.getX();
        int y = v.getY();

        // 1. 绘制高亮外发光（Focus 效果）
        if (v == activeA || v == activeB) {
            Color accentColor = UIManager.getColor("ProgressBar.selectionBackground");
            if (accentColor == null)
                accentColor = new Color(224, 108, 117);

            g.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 80));
            g.fillOval(x - RADIUS - 6, y - RADIUS - 6, (RADIUS + 6) * 2, (RADIUS + 6) * 2);

            // 高亮节点主体颜色
            g.setColor(accentColor);
        } else if (v.isVisited()) {
            // 已访问节点颜色
            g.setColor(UIManager.getColor("ProgressBar.foreground"));
        } else {
            // 默认节点颜色
            g.setColor(UIManager.getColor("Component.borderColor"));
        }

        // 2. 填充圆饼
        g.fillOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);

        // 3. 绘制节点边框（白色细边让节点更立体）
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        g.drawOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);
    }

    private void drawVertexText(Graphics2D g, Vertex<V> v) {
        int x = v.getX();
        int y = v.getY();

        // 统一使用白色文字
        g.setColor(Color.WHITE);
        g.setFont(new Font("JetBrains Mono", Font.BOLD, 14));

        String txt = String.valueOf(v.getData());
        FontMetrics fm = g.getFontMetrics();
        int tx = x - fm.stringWidth(txt) / 2;
        int ty = y + fm.getAscent() / 2 - 2;

        g.drawString(txt, tx, ty);
    }

    @Override
    protected void calculateScale() {
        // 布局通常在 Frame 层通过 GraphUtils.autoLayout 完成
    }
}