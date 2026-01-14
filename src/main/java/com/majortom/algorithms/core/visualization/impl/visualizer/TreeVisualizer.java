package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.animation.AnimationTimer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 树形结构可视化实现类
 * 视觉风格参考黑泽明《乱》中的红、蓝、黄配色方案。
 */
public class TreeVisualizer<T> extends BaseVisualizer<BaseTree<T>> {

    // 绘图常量
    private static final double NODE_RADIUS = 26.0;
    private static final double BASE_LEVEL_HEIGHT = 115.0;
    private static final double BASE_NODE_GAP = 85.0;
    private static final double LERP_FACTOR = 0.18;

    // 颜色定义（来源：黑泽明《乱》）
    private final Color RAN_RED = Color.rgb(180, 0, 0); // 红色：非叶子节点（父节点）
    private final Color RAN_BLUE = Color.rgb(0, 120, 255); // 蓝色：当前活跃/遍历节点
    private final Color RAN_GOLD = Color.rgb(220, 180, 0); // 黄色：叶子节点
    private final Color ARMOR_GRAY = Color.rgb(25, 25, 30); // 深灰色：节点背景
    private final Color BONE_WHITE = Color.rgb(240, 240, 230); // 骨白色：文字
    private final Color LINE_DARK = Color.rgb(60, 60, 70); // 铁灰色：默认线条

    private final Map<TreeNode<T>, Point> currentPosMap = new ConcurrentHashMap<>();
    private final Map<TreeNode<T>, Point> targetPosMap = new ConcurrentHashMap<>();
    private BaseTree<T> treeInstance;
    private Object focusA;

    private double autoScale = 1.0;
    private double autoOffsetX = 0;
    private double autoOffsetY = 80;

    public TreeVisualizer() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateAnimationPositions();
                renderFrame();
            }
        }.start();
    }

    @Override
    protected void draw(BaseTree<T> tree, Object a, Object b) {
        if (tree == null || tree.getRoot() == null) {
            this.treeInstance = null;
            return;
        }
        this.treeInstance = tree;
        this.focusA = a;

        Map<TreeNode<T>, Point> nextLayout = new HashMap<>();
        double rawW = calculateLayoutRecursively(tree.getRoot(), 0, 0, nextLayout);
        double rawH = getTreeHeight(nextLayout);

        double availableW = canvas.getWidth() * 0.92;
        double availableH = canvas.getHeight() * 0.85;

        autoScale = Math.min(availableW / rawW, availableH / (rawH + 150));
        autoOffsetX = (canvas.getWidth() - rawW * autoScale) / 2;
        autoOffsetY = 85 * autoScale;

        targetPosMap.clear();
        targetPosMap.putAll(nextLayout);
        nextLayout.forEach((node, p) -> currentPosMap.putIfAbsent(node, new Point(p.x, p.y)));
    }

    private void renderFrame() {
        if (treeInstance == null || treeInstance.getRoot() == null)
            return;
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.save();
        gc.translate(autoOffsetX, autoOffsetY);
        gc.scale(autoScale, autoScale);
        renderTreeRecursive(treeInstance.getRoot());
        gc.restore();
    }

    private void renderTreeRecursive(TreeNode<T> node) {
        Point p = currentPosMap.get(node);
        if (p == null)
            return;

        List<? extends TreeNode<T>> children = node.getChildren();
        if (children != null) {
            for (TreeNode<T> child : children) {
                if (child != null && currentPosMap.containsKey(child)) {
                    Point cp = currentPosMap.get(child);

                    // 连线逻辑判定
                    TreeNode<T> treeFocus = treeInstance.getCurrentHighlight();
                    boolean isPathToFocus = (child == treeFocus
                            || (treeFocus != null && Objects.equals(child.data, treeFocus.data)));
                    boolean isChildLeaf = child.isLeaf();

                    // 连线颜色与粗细设置
                    if (isPathToFocus) {
                        gc.setStroke(RAN_BLUE);
                        gc.setLineWidth(4.0 / autoScale);
                    } else if (!isChildLeaf) {
                        gc.setStroke(RAN_RED.deriveColor(0, 1, 0.8, 0.6)); // 父节点到父节点：红色
                        gc.setLineWidth(3.0 / autoScale);
                    } else {
                        gc.setStroke(RAN_GOLD.deriveColor(0, 1, 0.8, 0.5)); // 父节点到叶子节点：黄色
                        gc.setLineWidth(2.5 / autoScale);
                    }

                    gc.strokeLine(p.x, p.y, cp.x, cp.y);
                    renderTreeRecursive(child);
                }
            }
        }
        drawRanNode(node, p.x, p.y);
    }

    private void drawRanNode(TreeNode<T> node, double x, double y) {
        TreeNode<T> treeFocus = treeInstance.getCurrentHighlight();
        boolean isLeaf = node.isLeaf();
        boolean isActive = (node == treeFocus) ||
                (treeFocus != null && Objects.equals(node.data, treeFocus.data)) ||
                (focusA != null && Objects.equals(node.data, focusA));

        Color strokeColor;
        Color fillColor = ARMOR_GRAY;
        Color glowColor = null;

        // 根据状态分配颜色
        if (isActive) {
            strokeColor = RAN_BLUE;
            fillColor = Color.rgb(0, 45, 90);
            glowColor = RAN_BLUE;
        } else if (!isLeaf) {
            strokeColor = RAN_RED;
            fillColor = Color.rgb(45, 10, 10);
            glowColor = RAN_RED.deriveColor(0, 1, 1, 0.3);
        } else {
            strokeColor = RAN_GOLD;
            fillColor = ARMOR_GRAY;
            glowColor = null;
        }

        // 绘制光晕
        if (glowColor != null) {
            gc.setFill(glowColor.deriveColor(0, 1, 1, 0.2));
            gc.fillOval(x - NODE_RADIUS - 10, y - NODE_RADIUS - 10, (NODE_RADIUS + 10) * 2, (NODE_RADIUS + 10) * 2);
        }

        // 绘制节点主体
        gc.setFill(fillColor);
        gc.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
        gc.setStroke(strokeColor);
        gc.setLineWidth(isActive ? 4.5 : 2.8);
        gc.strokeOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

        // 绘制文本
        gc.setFill(BONE_WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18 / Math.sqrt(autoScale)));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(String.valueOf(node.data), x, y + 7);
    }

    private double calculateLayoutRecursively(TreeNode<T> node, double xOffset, double y,
            Map<TreeNode<T>, Point> layout) {
        if (node == null)
            return 0;
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
        double px = (layout.get(children.get(0)).x + layout.get(children.get(children.size() - 1)).x) / 2;
        layout.put(node, new Point(px, y));
        return totalW;
    }

    private double getTreeHeight(Map<TreeNode<T>, Point> layout) {
        return layout.values().stream().mapToDouble(p -> p.y).max().orElse(0);
    }

    private void updateAnimationPositions() {
        targetPosMap.forEach((node, target) -> {
            Point current = currentPosMap.getOrDefault(node, target);
            currentPosMap.put(node, new Point(
                    current.x + (target.x - current.x) * LERP_FACTOR,
                    current.y + (target.y - current.y) * LERP_FACTOR));
        });
        currentPosMap.keySet().removeIf(node -> !targetPosMap.containsKey(node));
    }

    private static class Point {
        double x, y;

        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}