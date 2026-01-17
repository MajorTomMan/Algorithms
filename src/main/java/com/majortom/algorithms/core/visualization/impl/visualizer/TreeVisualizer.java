package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.core.visualization.base.BaseTreeVisualizer;
import javafx.animation.AnimationTimer;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Objects;

/**
 * 树形结构可视化具体实现
 * 风格：黑泽明《乱》美学方案。
 */
public class TreeVisualizer<T extends Comparable<T>> extends BaseTreeVisualizer<T> {

    private static final double LERP_FACTOR = 0.18;

    private BaseTree<T> treeSnapshot;
    private Object focusA;

    public TreeVisualizer() {
        // 开启高频动画计时器，独立驱动 LERP 动画与渲染
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateAnimationPositions();
                renderFrame();
            }
        }.start();
    }

    /**
     * 重写父类 draw 方法
     * 职责：1. 调用父类逻辑更新 targetPosMap。2. 捕获当前树状态快照。
     */
    @Override
    protected void draw(BaseTree<T> tree, Object a, Object b) {
        // 1. 关键：先执行父类的坐标计算逻辑，更新 targetPosMap
        super.draw(tree, a, b);

        // 2. 捕获引用，供 AnimationTimer 线程渲染使用
        this.treeSnapshot = tree;
        this.focusA = a;
    }

    /**
     * 执行实际的像素绘制
     */
    private void renderFrame() {
        if (treeSnapshot == null || treeSnapshot.getRoot() == null)
            return;

        // 清空画布
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.save();
        // 使用父类计算出的变换参数
        gc.translate(autoOffsetX, autoOffsetY);
        gc.scale(autoScale, autoScale);

        renderTreeRecursive(treeSnapshot.getRoot());

        gc.restore();
    }

    private void renderTreeRecursive(TreeNode<T> node) {
        if (node == null)
            return;

        // 直接从父类的 currentPosMap 获取平滑插值后的坐标
        Point p = currentPosMap.get(node);
        if (p == null)
            return;

        List<? extends TreeNode<T>> children = node.getChildren();
        if (children != null) {
            for (TreeNode<T> child : children) {
                if (child != null) {
                    Point cp = currentPosMap.get(child);
                    if (cp != null) {
                        drawRanEdge(node, child, p, cp);
                        renderTreeRecursive(child);
                    }
                }
            }
        }
        drawRanNode(node, p.x, p.y);
    }

    private void drawRanEdge(TreeNode<T> parent, TreeNode<T> child, Point p, Point cp) {
        TreeNode<T> treeFocus = treeSnapshot.getCurrentHighlight();
        boolean isPathToFocus = (child == treeFocus
                || (treeFocus != null && Objects.equals(child.data, treeFocus.data)));

        if (isPathToFocus) {
            gc.setStroke(RAN_BLUE);
            gc.setLineWidth(3.5 / autoScale);
        } else if (!child.isLeaf()) {
            gc.setStroke(RAN_RED.deriveColor(0, 1, 0.8, 0.5));
            gc.setLineWidth(2.5 / autoScale);
        } else {
            gc.setStroke(IRON_GRAY.deriveColor(0, 1, 1, 0.4));
            gc.setLineWidth(1.5 / autoScale);
        }
        gc.strokeLine(p.x, p.y, cp.x, cp.y);
    }

    private void drawRanNode(TreeNode<T> node, double x, double y) {
        TreeNode<T> treeFocus = treeSnapshot.getCurrentHighlight();
        boolean isActive = (node == treeFocus) || (focusA != null && Objects.equals(node.data, focusA));
        boolean isLeaf = node.isLeaf();

        Color strokeColor = isLeaf ? RAN_GOLD : RAN_RED;
        Color fillColor = ARMOR_BLACK;

        if (isActive) {
            strokeColor = RAN_BLUE;
            fillColor = Color.rgb(0, 30, 60);
            drawGlow(x, y, NODE_RADIUS, RAN_BLUE);
        }

        gc.setFill(fillColor);
        gc.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

        gc.setStroke(strokeColor);
        gc.setLineWidth(isActive ? 4.0 : 2.5);
        gc.strokeOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

        drawText(String.valueOf(node.data), x, y + 6, BONE_WHITE, 16 / Math.sqrt(autoScale), true);
    }

    /**
     * 更新插值动画位置，确保节点平滑移动
     */
    private void updateAnimationPositions() {
        targetPosMap.forEach((node, target) -> {
            Point current = currentPosMap.getOrDefault(node, target);
            currentPosMap.put(node, new Point(
                    current.x + (target.x - current.x) * LERP_FACTOR,
                    current.y + (target.y - current.y) * LERP_FACTOR));
        });
        currentPosMap.keySet().removeIf(node -> !targetPosMap.containsKey(node));
    }
}