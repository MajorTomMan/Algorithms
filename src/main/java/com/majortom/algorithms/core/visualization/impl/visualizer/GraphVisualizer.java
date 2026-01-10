package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.node.Edge;
import com.majortom.algorithms.core.graph.node.Vertex;
import com.majortom.algorithms.core.visualization.BaseVisualizer;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.util.Collection;

/**
 * 图算法可视化画布 (JavaFX Canvas 实现)
 * 职责：
 * 1. 绘制边（Edges）和权重。
 * 2. 绘制节点（Vertex）主体、高亮发光效果。
 * 3. 绘制节点文本，确保处于最顶层。
 */
public class GraphVisualizer<V> extends BaseVisualizer<BaseGraph<V>> {

    private static final double RADIUS = 20.0;

    // 定义配色（推荐后续通过配置文件或 CSS 变量动态读取）
    private final Color ACCENT_COLOR = Color.web("#E06C75"); // 红色高亮
    private final Color VISITED_COLOR = Color.web("#98C379"); // 绿色访问
    private final Color DEFAULT_NODE_COLOR = Color.web("#5C6370"); // 灰色默认
    private final Color EDGE_COLOR = Color.rgb(171, 178, 191, 0.4); // 半透明边线

    public GraphVisualizer(BaseGraph<V> data) {
        super(data);
    }

    @Override
    protected void onMeasure(double width, double height) {
        // 布局通常由 GraphUtils.autoLayout 完成，此处仅保留扩展位
    }

    @Override
    protected void draw(GraphicsContext gc, double width, double height) {
        BaseGraph<V> graph = data.get();
        if (graph == null)
            return;

        Collection<Vertex<V>> vertices = graph.getVertices();

        // --- 第一步：绘制所有的边 ---
        drawEdges(gc, vertices);

        // --- 第二步：绘制节点主体 ---
        for (Vertex<V> v : vertices) {
            drawVertexBody(gc, v);
        }

        // --- 第三步：最后统一绘制文字（确保在顶层） ---
        drawVertexText(gc, vertices);
    }

    private void drawEdges(GraphicsContext gc, Collection<Vertex<V>> vertices) {
        gc.setLineWidth(1.5);
        gc.setStroke(EDGE_COLOR);
        gc.setFont(Font.font("JetBrains Mono", 12));
        gc.setFill(Color.web("#ABB2BF", 0.6)); // 权重文字颜色

        for (Vertex<V> v : vertices) {
            double x1 = v.getX();
            double y1 = v.getY();

            for (Edge<V> edge : v.getEdges()) {
                Vertex<V> dest = edge.getDest();
                double x2 = dest.getX();
                double y2 = dest.getY();

                // 绘制边线
                gc.strokeLine(x1, y1, x2, y2);

                // 如果有权重，绘制权重文字
                if (edge.getWeight() != 0) {
                    double midX = (x1 + x2) / 2;
                    double midY = (y1 + y2) / 2;
                    gc.fillText(String.valueOf(edge.getWeight()), midX, midY);
                }
            }
        }
    }

    private void drawVertexBody(GraphicsContext gc, Vertex<V> v) {
        double x = v.getX();
        double y = v.getY();

        // 1. 绘制高亮外发光 (Focus 效果)
        if (v == activeA.get() || v == activeB.get()) {
            gc.setFill(ACCENT_COLOR.deriveColor(0, 1, 1, 0.3)); // 30% 不透明度
            gc.fillOval(x - RADIUS - 6, y - RADIUS - 6, (RADIUS + 6) * 2, (RADIUS + 6) * 2);
            gc.setFill(ACCENT_COLOR);
        } else if (v.isVisited()) {
            gc.setFill(VISITED_COLOR);
        } else {
            gc.setFill(DEFAULT_NODE_COLOR);
        }

        // 2. 填充节点圆饼
        gc.fillOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);

        // 3. 绘制节点边框
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2.0);
        gc.strokeOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);
    }

    private void drawVertexText(GraphicsContext gc, Collection<Vertex<V>> vertices) {
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        for (Vertex<V> v : vertices) {
            String txt = String.valueOf(v.getData());
            gc.fillText(txt, v.getX(), v.getY());
        }
    }
}