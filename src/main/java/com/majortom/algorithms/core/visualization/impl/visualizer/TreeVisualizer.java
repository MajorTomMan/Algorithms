package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.core.visualization.BaseVisualizer;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * 树形结构可视化器
 * 职责：计算树结构的递归布局，并执行节点与连线的 Canvas 渲染。
 */
public class TreeVisualizer<T> extends BaseVisualizer<BaseTree<T>> {

    private static final double NODE_RADIUS = 20.0;
    private static final double MIN_NODE_GAP = 50.0;
    private static final double LEVEL_HEIGHT = 80.0;

    /**
     * 实现基类绘图钩子
     * 由 BaseVisualizer.render 调用，确保在 FX 线程执行
     */
    @Override
    protected void draw(BaseTree<T> tree, Object a, Object b) {
        if (tree == null || tree.getRoot() == null) {
            clear();
            return;
        }

        // 1. 递归计算节点布局坐标
        calculateLayout(tree.getRoot(), 0, 50);

        // 2. 执行渲染过程
        clear();
        drawEdges(tree.getRoot());
        drawNodes(tree.getRoot(), a, b);
    }

    /**
     * 递归布局算法
     * 职责：确定每个节点的 x, y 坐标，返回该子树占用的总宽度。
     */
    private double calculateLayout(TreeNode<T> node, double xOffset, double y) {
        if (node == null) return 0;

        List<? extends TreeNode<T>> children = node.getChildren();

        // 叶子节点处理
        if (children == null || children.isEmpty()) {
            node.x = xOffset + MIN_NODE_GAP / 2;
            node.y = y;
            return MIN_NODE_GAP;
        }

        // 递归计算子节点布局
        double totalWidth = 0;
        for (TreeNode<T> child : children) {
            totalWidth += calculateLayout(child, xOffset + totalWidth, y + LEVEL_HEIGHT);
        }

        // 父节点居中布局
        double firstChildX = children.get(0).x;
        double lastChildX = children.get(children.size() - 1).x;
        node.x = (firstChildX + lastChildX) / 2;
        node.y = y;

        return totalWidth;
    }

    /**
     * 绘制节点间的连接线
     */
    private void drawEdges(TreeNode<T> node) {
        if (node == null) return;
        
        gc.setLineWidth(1.5);
        gc.setStroke(Color.web("#B0BEC5", 0.6)); 

        for (TreeNode<T> child : node.getChildren()) {
            if (child != null) {
                gc.strokeLine(node.x, node.y, child.x, child.y);
                drawEdges(child);
            }
        }
    }

    /**
     * 递归绘制节点实体及其数据
     */
    private void drawNodes(TreeNode<T> node, Object a, Object b) {
        if (node == null) return;

        // 焦点判定：a 为主焦点（如插入点），b 为副焦点（如旋转支点）
        boolean isPrimary = node.equals(a) || node.isHighlighted;
        boolean isSecondary = node.equals(b);

        // 绘制高亮光晕
        if (isPrimary || isSecondary) {
            drawGlow(node.x, node.y, isPrimary ? highlightColor : Color.web("#26A69A"));
        }

        // 绘制节点球体感渐变
        RadialGradient gradient = new RadialGradient(
                0, 0, node.x - 5, node.y - 5, NODE_RADIUS, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ECEFF1")),
                new Stop(1, baseColor));
        
        gc.setFill(gradient);
        gc.fillOval(node.x - NODE_RADIUS, node.y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

        // 绘制数据文本
        drawCenteredText(
                node.x,
                node.y,
                String.valueOf(node.data),
                Color.web("#37474F"),
                Font.font("System", FontWeight.BOLD, 14));

        for (TreeNode<T> child : node.getChildren()) {
            drawNodes(child, a, b);
        }
    }

    /**
     * 辅助方法：绘制节点外围的发光效果
     */
    private void drawGlow(double x, double y, Color color) {
        gc.save();
        gc.setGlobalAlpha(0.3);
        gc.setFill(color);
        gc.fillOval(x - NODE_RADIUS - 5, y - NODE_RADIUS - 5, (NODE_RADIUS + 5) * 2, (NODE_RADIUS + 5) * 2);
        gc.restore();
    }
}