package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.tree.node.BinaryTreeNode;
import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动效树结构可视化画布 (JavaFX Canvas 版)
 */
public class TreeVisualizer<T> extends BaseVisualizer<TreeNode<T>> {

    private static final double RADIUS = 22.0;
    private static final long ANIMATION_DURATION_NS = 400_000_000L; // 400ms 转换为纳秒

    private final Map<TreeNode<T>, Point2D> posMap = new ConcurrentHashMap<>();
    private final Map<TreeNode<T>, Point2D> oldPosMap = new ConcurrentHashMap<>();

    private double progress = 1.0;
    private long startTime = -1;
    private AnimationTimer timer;

    public TreeVisualizer(TreeNode<T> data) {
        super(data);
    }

    @Override
    public void updateState(TreeNode<T> node, Object a, Object b) {
        // 记录旧位置用于插值动画
        oldPosMap.clear();
        oldPosMap.putAll(posMap);

        // 更新数据源
        this.data.set(node);
        this.activeA.set(a);
        this.activeB.set(b);

        // 触发布局计算
        onMeasure(getWidth(), getHeight());

        // 启动 JavaFX 动画计时器
        startAnimation();
    }

    private void startAnimation() {
        if (timer != null)
            timer.stop();

        progress = 0;
        startTime = -1;

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (startTime < 0)
                    startTime = now;

                double t = (now - startTime) / (double) ANIMATION_DURATION_NS;
                if (t >= 1.0) {
                    progress = 1.0;
                    drawInternal(); // 最后一帧
                    this.stop();
                } else {
                    progress = t;
                    drawInternal();
                }
            }
        };
        timer.start();
    }

    private void drawInternal() {
        // 调用 BaseVisualizer 里的重绘逻辑
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        draw(gc, canvas.getWidth(), canvas.getHeight());
    }

    private Point2D getInterpolatedPoint(TreeNode<T> node) {
        Point2D target = posMap.get(node);
        if (target == null)
            return null;

        Point2D old = oldPosMap.getOrDefault(node, target);
        // 三次缓动函数 (Ease Out Cubic)
        double eased = 1 - Math.pow(1 - progress, 3);

        return new Point2D(
                old.getX() + (target.getX() - old.getX()) * eased,
                old.getY() + (target.getY() - old.getY()) * eased);
    }

    @Override
    protected void onMeasure(double width, double height) {
        TreeNode<T> root = data.get();
        if (root == null || width <= 0)
            return;

        posMap.clear();
        int treeHeight = getTreeHeight(root);
        double dynamicLevelGap = Math.min(70.0, (height - 100.0) / Math.max(treeHeight, 1));

        doCalculate(root, padding, width - padding, padding + 40, dynamicLevelGap);
    }

    private void doCalculate(TreeNode<T> node, double left, double right, double y, double gap) {
        if (node == null)
            return;

        double x = (left + right) / 2.0;
        posMap.put(node, new Point2D(x, y));

        var children = node.getChildren();
        if (children.isEmpty())
            return;

        double totalWeight = children.stream()
                .mapToDouble(c -> c != null ? Math.max(c.subTreeCount, 1) : 0.5)
                .sum();

        double currentLeft = left;
        for (var child : children) {
            if (child == null)
                continue;
            double childWidth = (right - left) * (Math.max(child.subTreeCount, 1) / totalWeight);
            doCalculate(child, currentLeft, currentLeft + childWidth, y + gap, gap);
            currentLeft += childWidth;
        }
    }

    @Override
    protected void draw(GraphicsContext gc, double width, double height) {
        TreeNode<T> root = data.get();
        if (root == null)
            return;

        // 1. 绘制连线
        gc.setStroke(Color.web("#ABB2BF", 0.5)); // 使用半透明的主题分隔符颜色
        gc.setLineWidth(2.0);
        drawEdges(gc, root);

        // 2. 绘制节点主体
        drawNodeBodies(gc, root);

        // 3. 绘制文字
        drawNodeTexts(gc, root);
    }

    private void drawEdges(GraphicsContext gc, TreeNode<T> node) {
        Point2D p = getInterpolatedPoint(node);
        if (p == null)
            return;

        for (var child : node.getChildren()) {
            if (child != null) {
                Point2D cp = getInterpolatedPoint(child);
                if (cp != null) {
                    gc.strokeLine(p.getX(), p.getY(), cp.getX(), cp.getY());
                    drawEdges(gc, child);
                }
            }
        }
    }

    private void drawNodeBodies(GraphicsContext gc, TreeNode<T> node) {
        Point2D p = getInterpolatedPoint(node);
        if (p == null)
            return;

        if (node == activeA.get() || node == activeB.get()) {
            // 绘制发光外圈
            gc.setFill(Color.web("#FF79C6", 0.2));
            gc.fillOval(p.getX() - RADIUS - 6, p.getY() - RADIUS - 6, (RADIUS + 6) * 2, (RADIUS + 6) * 2);
            gc.setFill(Color.web("#FF79C6"));
        } else {
            gc.setFill(Color.web("#61AFEF"));
        }

        // 节点填充
        gc.fillOval(p.getX() - RADIUS, p.getY() - RADIUS, RADIUS * 2, RADIUS * 2);

        // 描边
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeOval(p.getX() - RADIUS, p.getY() - RADIUS, RADIUS * 2, RADIUS * 2);

        for (var child : node.getChildren()) {
            if (child != null)
                drawNodeBodies(gc, child);
        }
    }

    private void drawNodeTexts(GraphicsContext gc, TreeNode<T> node) {
        Point2D p = getInterpolatedPoint(node);
        if (p == null)
            return;

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("JetBrains Mono", FontWeight.BOLD, 13));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        gc.fillText(String.valueOf(node.data), p.getX(), p.getY());

        for (var child : node.getChildren()) {
            if (child != null)
                drawNodeTexts(gc, child);
        }
    }

    private int getTreeHeight(TreeNode<T> node) {
        if (node == null)
            return 0;

        int maxHeight = 0;
        for (TreeNode<T> child : node.getChildren()) {
            if (child != null) {
                maxHeight = Math.max(maxHeight, getTreeHeight(child));
            }
        }
        return 1 + maxHeight;
    }
}