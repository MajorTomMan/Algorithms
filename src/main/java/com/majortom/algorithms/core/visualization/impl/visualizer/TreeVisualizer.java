package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 通用树形结构可视化器 (重构版)
 * 适配：不维护 x, y 坐标的 TreeNode。
 * 逻辑：在每一帧渲染时动态计算坐标，保证 UI 与数据彻底解耦。
 */
public class TreeVisualizer<T> extends BaseVisualizer<BaseTree<T>> {

    private static final double NODE_RADIUS = 22.0;
    private static final double MIN_NODE_GAP = 55.0;
    private static final double LEVEL_HEIGHT = 90.0;

    // 🚩 核心：使用临时 Map 存储这一帧的布局坐标
    private final Map<TreeNode<T>, Double> xCoords = new HashMap<>();
    private final Map<TreeNode<T>, Double> yCoords = new HashMap<>();

    @Override
    protected void draw(BaseTree<T> tree, Object a, Object b) {
        if (tree == null || tree.getRoot() == null) {
            clear();
            return;
        }

        // 1. 清理上一帧的坐标缓存
        xCoords.clear();
        yCoords.clear();

        // 2. 动态布局计算：确定所有节点的相对坐标
        double totalTreeWidth = calculateLayout(tree.getRoot(), 0, 60);

        clear();

        // 3. 计算居中偏移量
        double horizontalOffset = (canvas.getWidth() - totalTreeWidth) / 2;
        horizontalOffset = Math.max(20, horizontalOffset);

        // 4. 执行渲染
        renderTree(tree.getRoot(), a, b, horizontalOffset);
    }

    /**
     * 【动态布局核心】
     * 现在坐标存储在内部的 Map 中，而不是 Node 对象里
     */
    private double calculateLayout(TreeNode<T> node, double xOffset, double y) {
        if (node == null)
            return 0;

        List<? extends TreeNode<T>> children = node.getChildren();
        List<? extends TreeNode<T>> validChildren = (children == null) ? List.of()
                : children.stream().filter(Objects::nonNull).toList();

        if (validChildren.isEmpty()) {
            double x = xOffset + MIN_NODE_GAP / 2;
            xCoords.put(node, x);
            yCoords.put(node, y);
            return MIN_NODE_GAP;
        }

        double currentSubtreeWidth = 0;
        for (TreeNode<T> child : validChildren) {
            currentSubtreeWidth += calculateLayout(child, xOffset + currentSubtreeWidth, y + LEVEL_HEIGHT);
        }

        // 父节点位置取决于子节点的平均中点
        double firstX = xCoords.get(validChildren.get(0));
        double lastX = xCoords.get(validChildren.get(validChildren.size() - 1));

        xCoords.put(node, (firstX + lastX) / 2);
        yCoords.put(node, y);

        return currentSubtreeWidth;
    }

    private void renderTree(TreeNode<T> node, Object a, Object b, double offset) {
        if (node == null || !xCoords.containsKey(node))
            return;

        double drawX = xCoords.get(node) + offset;
        double drawY = yCoords.get(node);

        // 1. 绘制连线
        List<? extends TreeNode<T>> children = node.getChildren();
        if (children != null) {
            gc.setStroke(Color.web("#B0BEC5", 0.4));
            gc.setLineWidth(1.5);
            for (TreeNode<T> child : children) {
                if (child != null && xCoords.containsKey(child)) {
                    drawCubicCurve(drawX, drawY, xCoords.get(child) + offset, yCoords.get(child));
                    renderTree(child, a, b, offset);
                }
            }
        }

        // 2. 绘制节点实体 (逻辑保持之前的文艺风格)
        boolean isFocus = node.equals(a) || node.equals(b);
        drawNodeEntity(node, drawX, drawY, isFocus);
    }

    // drawCubicCurve 和 drawNodeEntity 逻辑保持不变...
    private void drawCubicCurve(double x1, double y1, double x2, double y2) {
        gc.beginPath();
        gc.moveTo(x1, y1);
        gc.bezierCurveTo(x1, (y1 + y2) / 2, x2, (y1 + y2) / 2, x2, y2);
        gc.stroke();
    }

    private void drawNodeEntity(TreeNode<T> node, double x, double y, boolean highlight) {
        if (highlight) {
            gc.setFill(highlightColor.deriveColor(0, 1, 1, 0.25));
            gc.fillOval(x - NODE_RADIUS - 8, y - NODE_RADIUS - 8, (NODE_RADIUS + 8) * 2, (NODE_RADIUS + 8) * 2);
        }

        RadialGradient g = new RadialGradient(0, 0, x - 5, y - 5, NODE_RADIUS, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#3A3A45")), new Stop(1, Color.web("#121218")));

        gc.setFill(g);
        gc.setStroke(highlight ? highlightColor : Color.web("#565656"));
        gc.setLineWidth(2.5);
        gc.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
        gc.strokeOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

        drawCenteredText(x, y, String.valueOf(node.data), Color.WHITE, Font.font("Consolas", 14));
    }
}