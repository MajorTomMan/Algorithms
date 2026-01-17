package com.majortom.algorithms.core.visualization.impl.visualizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.core.visualization.base.BaseTreeVisualizer;

import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class TreeVisualizer<T extends Comparable<T>> extends BaseTreeVisualizer<T> {

    private static final double NODE_RADIUS = 26.0;
    private static final double BASE_LEVEL_HEIGHT = 115.0;
    private static final double BASE_NODE_GAP = 85.0;

    @Override
    protected void updateLayout(BaseTree<T> tree) {
        if (tree == null || tree.getRoot() == null)
            return;

        Map<TreeNode<T>, Point> nextLayout = new HashMap<>();
        // 递归计算布局
        double rawW = calculateLayoutRecursively(tree.getRoot(), 0, 0, nextLayout);
        double rawH = nextLayout.values().stream().mapToDouble(p -> p.y).max().orElse(0);

        // 自动缩放适配
        double availableW = canvas.getWidth() * 0.92;
        double availableH = canvas.getHeight() * 0.85;
        autoScale = Math.min(availableW / rawW, availableH / (rawH + 150));
        autoOffsetX = (canvas.getWidth() - rawW * autoScale) / 2;
        autoOffsetY = 85 * autoScale;

        targetPosMap.clear();
        targetPosMap.putAll(nextLayout);
        nextLayout.forEach((node, p) -> currentPosMap.putIfAbsent(node, new Point(p.x, p.y)));
    }

    private double calculateLayoutRecursively(TreeNode<T> node, double xOffset, double y,
            Map<TreeNode<T>, Point> layout) {
        if (node == null)
            return 0;

        // 修复 1: 必须过滤空子节点，防止后面 layout.get() 报 NPE
        List<? extends TreeNode<T>> children = (node.getChildren() == null) ? List.of()
                : node.getChildren().stream().filter(Objects::nonNull).toList();

        if (children.isEmpty()) {
            layout.put(node, new Point(xOffset + BASE_NODE_GAP / 2, y));
            return BASE_NODE_GAP;
        }

        double totalW = 0;
        for (TreeNode<T> child : children) {
            totalW += calculateLayoutRecursively(child, xOffset + totalW, y + BASE_LEVEL_HEIGHT, layout);
        }

        // 修复 2: 防御性获取子节点坐标
        Point first = layout.get(children.get(0));
        Point last = layout.get(children.get(children.size() - 1));
        double px = (first != null && last != null) ? (first.x + last.x) / 2 : xOffset + totalW / 2;

        layout.put(node, new Point(px, y));
        return totalW;
    }

    @Override
    protected void renderTreeRecursive(TreeNode<T> node) {
        Point p = currentPosMap.get(node);
        if (p == null)
            return;

        List<? extends TreeNode<T>> children = node.getChildren();
        if (children != null) {
            for (TreeNode<T> child : children) {
                if (child != null && currentPosMap.containsKey(child)) {
                    renderEdge(p, currentPosMap.get(child), child);
                    renderTreeRecursive(child);
                }
            }
        }
        drawRanNode(node, p.x, p.y);
    }

    private void renderEdge(Point parent, Point child, TreeNode<T> childNode) {
        TreeNode<T> treeFocus = treeInstance.getCurrentHighlight();
        boolean isPathToFocus = (childNode == treeFocus
                || (treeFocus != null && Objects.equals(childNode.data, treeFocus.data)));

        if (isPathToFocus) {
            gc.setStroke(RAN_BLUE);
            gc.setLineWidth(4.0 / autoScale);
        } else {
            // 父节点到父节点用红色，叶子节点用黄色，增加对比度
            Color edgeColor = childNode.isLeaf() ? RAN_YELLOW.deriveColor(0, 1, 0.8, 0.5)
                    : RAN_RED.deriveColor(0, 1, 0.8, 0.6);
            gc.setStroke(edgeColor);
            gc.setLineWidth(2.5 / autoScale);
        }
        gc.strokeLine(parent.x, parent.y, child.x, child.y);
    }

    private void drawRanNode(TreeNode<T> node, double x, double y) {
        TreeNode<T> focus = treeInstance.getCurrentHighlight();
        boolean isLeaf = node.isLeaf();
        boolean isActive = (node == focus) || (focus != null && Objects.equals(node.data, focus.data))
                || (Objects.equals(node.data, focusA));

        // 颜色分配逻辑：恢复深色填充与发光
        Color strokeColor;
        Color fillColor;
        Color glowColor = null;

        if (isActive) {
            strokeColor = RAN_BLUE;
            fillColor = Color.rgb(0, 45, 90); // 深蓝甲胄感
            glowColor = RAN_BLUE;
        } else if (!isLeaf) {
            strokeColor = RAN_RED;
            fillColor = Color.rgb(45, 10, 10); // 深红城墙感
            glowColor = RAN_RED.deriveColor(0, 1, 1, 0.25);
        } else {
            strokeColor = RAN_YELLOW;
            fillColor = Color.rgb(25, 25, 30); // 极夜黑
        }

        // 1. 绘制光晕 (Glow Effect)
        if (glowColor != null) {
            gc.setFill(glowColor.deriveColor(0, 1, 1, 0.15));
            gc.fillOval(x - NODE_RADIUS - 8, y - NODE_RADIUS - 8, (NODE_RADIUS + 8) * 2, (NODE_RADIUS + 8) * 2);
        }

        // 2. 绘制主体
        gc.setFill(fillColor);
        gc.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
        gc.setStroke(strokeColor);
        gc.setLineWidth(isActive ? 4.5 : 2.8);
        gc.strokeOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

        // 3. 绘制文字 (恢复居中)
        gc.setFill(RAN_WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18 / Math.sqrt(autoScale)));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(String.valueOf(node.data), x, y);
    }
}