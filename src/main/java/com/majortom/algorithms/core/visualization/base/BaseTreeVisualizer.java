package com.majortom.algorithms.core.visualization.base;

import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.core.visualization.BaseVisualizer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 树结构视觉呈现基类 - 负责布局计算、坐标转换与动画同步
 */
public abstract class BaseTreeVisualizer<T extends Comparable<T>> extends BaseVisualizer<BaseTree<T>> {

    protected static final double NODE_RADIUS = 26.0;
    protected static final double LEVEL_HEIGHT = 115.0;
    protected static final double NODE_GAP = 85.0;

    // --- 动画位置映射：存放在基类，确保所有树算法共享平滑移动逻辑 ---
    protected final Map<TreeNode<T>, Point> currentPosMap = new ConcurrentHashMap<>();
    protected final Map<TreeNode<T>, Point> targetPosMap = new ConcurrentHashMap<>();

    protected double autoScale = 1.0;
    protected double autoOffsetX = 0;
    protected double autoOffsetY = 80;

    @Override
    protected void draw(BaseTree<T> tree, Object a, Object b) {
        if (tree == null || tree.getRoot() == null)
            return;

        // 1. 使用局部 Map 进行布局计算（双缓冲思想，防止计算过程中的 NPE）
        Map<TreeNode<T>, Point> nextLayout = new HashMap<>();
        double rawW = calculateLayoutRecursively(tree.getRoot(), 0, 0, nextLayout);

        // 2. 计算缩放与居中
        calculateTransform(nextLayout, rawW);

        // 3. 线程安全地更新目标位置
        targetPosMap.putAll(nextLayout);
        targetPosMap.keySet().retainAll(nextLayout.keySet());

        // 4. 初始化新节点的起始坐标
        nextLayout.forEach((node, p) -> currentPosMap.putIfAbsent(node, new Point(p.x, p.y)));
    }

    private double calculateLayoutRecursively(TreeNode<T> node, double xOffset, double y,
            Map<TreeNode<T>, Point> layout) {
        if (node == null)
            return 0;
        var children = node.getChildren();
        if (children == null || children.isEmpty()) {
            layout.put(node, new Point(xOffset + NODE_GAP / 2, y));
            return NODE_GAP;
        }
        double totalW = 0;
        for (var child : children) {
            totalW += calculateLayoutRecursively(child, xOffset + totalW, y + LEVEL_HEIGHT, layout);
        }
        // 防御性：确保子节点坐标已存入，避免 NPE
        if (layout.containsKey(children.get(0)) && layout.containsKey(children.get(children.size() - 1))) {
            double px = (layout.get(children.get(0)).x + layout.get(children.get(children.size() - 1)).x) / 2;
            layout.put(node, new Point(px, y));
        }
        return totalW;
    }

    private void calculateTransform(Map<TreeNode<T>, Point> layout, double rawW) {
        double rawH = layout.values().stream().mapToDouble(p -> p.y).max().orElse(0);
        double availableW = canvas.getWidth() * 0.92;
        double availableH = canvas.getHeight() * 0.85;

        autoScale = Math.min(availableW / rawW, availableH / (rawH + 150));
        autoOffsetX = (canvas.getWidth() - rawW * autoScale) / 2;
        autoOffsetY = 85 * autoScale;
    }

    /**
     * 定义坐标点内部类
     */
    protected static class Point {
        public double x, y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}