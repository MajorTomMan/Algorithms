package com.majortom.algorithms.app.visualization.impl.panel;

import com.majortom.algorithms.app.visualization.BasePanel;
import com.majortom.algorithms.core.tree.node.TreeNode;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TreePanel<T> extends BasePanel<TreeNode<T>> {
    private final int RADIUS = 22; // 节点半径
    private final int LEVEL_GAP = 70; // 层级间距
    private final Map<TreeNode<T>, Point> posMap = new HashMap<>();

    public TreePanel(TreeNode<T> data) {
        super(data);
    }

    /**
     * 外部直接投喂一个 TreeNode 节点
     */
    public void display(TreeNode<T> node) {
        this.data = node;
        this.calculateScale();
        this.repaint();
    }

    @Override
    protected void calculateScale() {
        if (data == null)
            return;
        posMap.clear();
        // 递归计算所有节点的 x, y 坐标并存入 map
        doCalculate(data, padding, getWidth() - padding, padding + 40);
    }

    private void doCalculate(TreeNode<T> node, int left, int right, int y) {
        if (node == null)
            return;

        int x = (left + right) / 2;
        posMap.put(node, new Point(x, y));

        var children = node.getAllChildren();
        if (children.isEmpty())
            return;

        // 空间分配权重：如果子树节点多，分配的宽度就大
        double totalWeight = children.stream()
                .mapToDouble(c -> c != null ? Math.max(c.subTreeCount, 1) : 0.5)
                .sum();

        int currentLeft = left;
        for (var child : children) {
            if (child == null)
                continue;
            int childWidth = (int) ((right - left) * (Math.max(child.subTreeCount, 1) / totalWeight));
            doCalculate(child, currentLeft, currentLeft + childWidth, y + LEVEL_GAP);
            currentLeft += childWidth;
        }
    }

    @Override
    protected void render(Graphics2D g) {
        if (data == null)
            return;
        drawEdges(g, data);
        drawNodes(g, data);
    }

    private void drawEdges(Graphics2D g, TreeNode<T> node) {
        Point p = posMap.get(node);
        for (var child : node.getAllChildren()) {
            if (child != null && posMap.containsKey(child)) {
                Point cp = posMap.get(child);
                g.setColor(new Color(171, 178, 191));
                g.setStroke(new BasicStroke(2f));
                g.drawLine(p.x, p.y, cp.x, cp.y);
                drawEdges(g, child);
            }
        }
    }

    private void drawNodes(Graphics2D g, TreeNode<T> node) {
        Point p = posMap.get(node);
        // 画节点背景
        g.setColor(new Color(97, 175, 239));
        g.fillOval(p.x - RADIUS, p.y - RADIUS, RADIUS * 2, RADIUS * 2);
        // 画描边
        g.setColor(Color.WHITE);
        g.drawOval(p.x - RADIUS, p.y - RADIUS, RADIUS * 2, RADIUS * 2);
        // 画文字
        g.setFont(new Font("JetBrains Mono", Font.BOLD, 13));
        String txt = String.valueOf(node.data);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(txt, p.x - fm.stringWidth(txt) / 2, p.y + fm.getAscent() / 2 - 2);

        for (var child : node.getAllChildren()) {
            if (child != null)
                drawNodes(g, child);
        }
    }
}