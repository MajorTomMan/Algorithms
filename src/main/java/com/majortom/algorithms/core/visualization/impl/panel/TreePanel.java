package com.majortom.algorithms.core.visualization.impl.panel;

import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.core.visualization.BasePanel;
import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动效树结构可视化画布
 * 修复：解决动画过程中高亮圆圈覆盖节点数据的问题，通过分层渲染（边->圆->字）实现。
 */
public class TreePanel<T> extends BasePanel<TreeNode<T>> {
    private final int RADIUS = 22;
    private final int ANIMATION_DURATION = 400;

    private final Map<TreeNode<T>, Point> posMap = new ConcurrentHashMap<>();
    private final Map<TreeNode<T>, Point> oldPosMap = new ConcurrentHashMap<>();

    private float progress = 1.0f;
    private Timer animationTimer;

    public TreePanel(TreeNode<T> data) {
        super(data);
        this.setPreferredSize(new Dimension(800, 600));
    }

    @Override
    public void updateData(TreeNode<T> node, Object a, Object b) {
        oldPosMap.clear();
        oldPosMap.putAll(posMap);
        this.data = node;
        this.activeA = a;
        this.activeB = b;
        calculateScale();
        startAnimation();
    }

    private void startAnimation() {
        if (animationTimer != null && animationTimer.isRunning())
            animationTimer.stop();
        progress = 0;
        animationTimer = new Timer(16, e -> {
            progress += 16.0f / ANIMATION_DURATION;
            if (progress >= 1.0f) {
                progress = 1.0f;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        animationTimer.start();
    }

    private Point getInterpolatedPoint(TreeNode<T> node) {
        Point target = posMap.get(node);
        if (target == null)
            return null;
        Point old = oldPosMap.getOrDefault(node, target);
        float eased = (float) (1 - Math.pow(1 - progress, 3));
        return new Point(
                (int) (old.x + (target.x - old.x) * eased),
                (int) (old.y + (target.y - old.y) * eased));
    }

    @Override
    protected void render(Graphics2D g) {
        if (data == null)
            return;

        // --- 第一步：递归绘制连线 ---
        drawEdges(g, data);

        // --- 第二步：递归绘制所有节点主体（圆圈） ---
        drawNodeBodies(g, data);

        // --- 第三步：最后递归绘制所有节点文字 ---
        // 关键点：这一步确保文字在所有圆圈层级之上
        drawNodeTexts(g, data);
    }

    private void drawEdges(Graphics2D g, TreeNode<T> node) {
        Point p = getInterpolatedPoint(node);
        if (p == null)
            return;

        g.setColor(UIManager.getColor("Separator.foreground"));
        g.setStroke(new BasicStroke(2f));

        for (var child : node.getAllChildren()) {
            if (child != null) {
                Point cp = getInterpolatedPoint(child);
                if (cp != null) {
                    g.drawLine(p.x, p.y, cp.x, cp.y);
                    drawEdges(g, child);
                }
            }
        }
    }

    private void drawNodeBodies(Graphics2D g, TreeNode<T> node) {
        Point p = getInterpolatedPoint(node);
        if (p == null)
            return;

        // 处理高亮逻辑
        Color accentColor = UIManager.getColor("ProgressBar.selectionBackground");
        if (accentColor == null)
            accentColor = new Color(255, 121, 198);

        if (node == activeA || node == activeB) {
            // 绘制外发光
            g.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60));
            g.fillOval(p.x - RADIUS - 6, p.y - RADIUS - 6, (RADIUS + 6) * 2, (RADIUS + 6) * 2);
            g.setColor(accentColor);
        } else {
            g.setColor(UIManager.getColor("ProgressBar.foreground"));
        }

        // 填充圆饼
        g.fillOval(p.x - RADIUS, p.y - RADIUS, RADIUS * 2, RADIUS * 2);

        // 绘制白色描边
        g.setColor(UIManager.getColor("Component.borderColor"));
        g.drawOval(p.x - RADIUS, p.y - RADIUS, RADIUS * 2, RADIUS * 2);

        for (var child : node.getAllChildren()) {
            if (child != null)
                drawNodeBodies(g, child);
        }
    }

    private void drawNodeTexts(Graphics2D g, TreeNode<T> node) {
        Point p = getInterpolatedPoint(node);
        if (p == null)
            return;

        // 绘制文字
        g.setColor(Color.WHITE);
        g.setFont(new Font("JetBrains Mono", Font.BOLD, 13));
        String txt = String.valueOf(node.data);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(txt, p.x - fm.stringWidth(txt) / 2, p.y + fm.getAscent() / 2 - 2);

        for (var child : node.getAllChildren()) {
            if (child != null)
                drawNodeTexts(g, child);
        }
    }

    @Override
    protected void calculateScale() {
        if (data == null || getWidth() <= 0)
            return;
        posMap.clear();
        int treeHeight = getTreeHeight(data);
        int dynamicLevelGap = Math.min(70, (getHeight() - 100) / Math.max(treeHeight, 1));
        doCalculate(data, padding, getWidth() - padding, padding + 40, dynamicLevelGap);
    }

    private void doCalculate(TreeNode<T> node, int left, int right, int y, int gap) {
        if (node == null)
            return;
        int x = (left + right) / 2;
        posMap.put(node, new Point(x, y));
        var children = node.getAllChildren();
        if (children.isEmpty())
            return;

        double totalWeight = children.stream()
                .mapToDouble(c -> c != null ? Math.max(c.subTreeCount, 1) : 0.5)
                .sum();

        int currentLeft = left;
        for (var child : children) {
            if (child == null)
                continue;
            int childWidth = (int) ((right - left) * (Math.max(child.subTreeCount, 1) / totalWeight));
            doCalculate(child, currentLeft, currentLeft + childWidth, y + gap, gap);
            currentLeft += childWidth;
        }
    }

    private int getTreeHeight(TreeNode<T> node) {
        if (node == null)
            return 0;
        return 1 + Math.max(getTreeHeight(node.left), getTreeHeight(node.right));
    }
}