
package com.majortom.algorithms.core.visualization.impl.panel;

import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.core.visualization.BasePanel;
import java.awt.*;
import java.util.Map;
import javax.swing.Timer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动效树结构可视化画布（Animated Tree Visualization Panel）
 * * 核心能力：
 * 1. 自动布局：基于子树规模（subTreeCount）递归计算每个节点的屏幕坐标。
 * 2. 平滑过渡：当树结构变化（如旋转、重新平衡）时，通过计时器实现节点移动动画。
 * 3. 焦点渲染：支持双焦点（ActiveA/B）高亮，通常用于展示正在比较或旋转的节点。
 */
public class TreePanel<T> extends BasePanel<TreeNode<T>> {
    private final int RADIUS = 22; // 节点圆圈半径
    private final Color highlightColor = new Color(255, 121, 198); // 主焦点颜色（粉色）
    private final Color secondaryColor = new Color(152, 195, 121); // 辅焦点颜色（绿色）

    /** 坐标映射表：存储当前时刻每个节点应在的目标坐标 */
    private final Map<TreeNode<T>, Point> posMap = new ConcurrentHashMap<>();
    /** 旧坐标映射表：存储动画开始前节点的位置，用于计算插值 */
    private final Map<TreeNode<T>, Point> oldPosMap = new ConcurrentHashMap<>();

    private float progress = 1.0f; // 动画进度（0.0 到 1.0）
    private Timer animationTimer; // Swing 计时器，驱动动画帧率
    private final int ANIMATION_DURATION = 400; // 动画总持续时间（毫秒）

    public TreePanel(TreeNode<T> data) {
        super(data);
        this.setPreferredSize(new Dimension(800, 600));
    }

    /**
     * 更新数据并启动过渡动画
     * 覆盖父类方法，在数据更新的同时捕获位置变化。
     */
    @Override
    public void updateData(TreeNode<T> node, Object a, Object b) {
        // 1. 在更新前，记录当前坐标作为下一阶段动画的“起点”
        oldPosMap.clear();
        oldPosMap.putAll(posMap);

        // 2. 更新同步过来的数据模型和焦点状态
        this.data = node;
        this.activeA = a;
        this.activeB = b;

        // 3. 递归计算新结构下的目标位置
        calculateScale();

        // 4. 重置进度并启动动画线程
        startAnimation();
    }

    /**
     * 启动动画驱动引擎
     */
    private void startAnimation() {
        if (animationTimer != null && animationTimer.isRunning())
            animationTimer.stop();

        progress = 0;
        // 每 16ms 触发一次（约 60 帧/秒）
        animationTimer = new Timer(16, e -> {
            progress += 16.0f / ANIMATION_DURATION;
            if (progress >= 1.0f) {
                progress = 1.0f;
                ((Timer) e.getSource()).stop();
            }
            repaint(); // 触发重绘
        });
        animationTimer.start();
    }

    /**
     * 计算节点的平滑插值坐标
     * 使用三次方缓动函数（Ease-Out），使节点移动更具“物理感”。
     */
    private Point getInterpolatedPoint(TreeNode<T> node) {
        Point target = posMap.get(node);
        if (target == null)
            return null;

        Point old = oldPosMap.getOrDefault(node, target);
        // 三次方缓动：f(t) = 1 - (1 - t)^3
        float eased = (float) (1 - Math.pow(1 - progress, 3));

        int x = (int) (old.x + (target.x - old.x) * eased);
        int y = (int) (old.y + (target.y - old.y) * eased);
        return new Point(x, y);
    }

    @Override
    protected void render(Graphics2D g) {
        if (data == null)
            return;
        // 先画连线，后画节点，确保线在圆圈下方
        drawEdges(g, data);
        drawNodes(g, data);
    }

    /** 递归绘制节点间的连线 */
    private void drawEdges(Graphics2D g, TreeNode<T> node) {
        Point p = getInterpolatedPoint(node);
        if (p == null)
            return;

        for (var child : node.getAllChildren()) {
            if (child != null) {
                Point cp = getInterpolatedPoint(child);
                if (cp != null) {
                    g.setColor(new Color(171, 178, 191, 120)); // 半透明线条
                    g.setStroke(new BasicStroke(2f));
                    g.drawLine(p.x, p.y, cp.x, cp.y);
                    drawEdges(g, child);
                }
            }
        }
    }

    /** 递归绘制节点及焦点装饰 */
    private void drawNodes(Graphics2D g, TreeNode<T> node) {
        Point p = getInterpolatedPoint(node);
        if (p == null)
            return;

        // 焦点判断逻辑
        if (node == activeA) {
            drawGlow(g, p, highlightColor); // 渲染外发光
            g.setColor(highlightColor);
        } else if (node == activeB) {
            drawGlow(g, p, secondaryColor);
            g.setColor(secondaryColor);
        } else {
            g.setColor(new Color(97, 175, 239)); // 默认主题色
        }

        // 绘制节点主体
        g.fillOval(p.x - RADIUS, p.y - RADIUS, RADIUS * 2, RADIUS * 2);
        g.setColor(Color.WHITE);
        g.drawOval(p.x - RADIUS, p.y - RADIUS, RADIUS * 2, RADIUS * 2);

        // 绘制节点文本
        g.setFont(new Font("JetBrains Mono", Font.BOLD, 13));
        String txt = String.valueOf(node.data);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(txt, p.x - fm.stringWidth(txt) / 2, p.y + fm.getAscent() / 2 - 2);

        // 递归处理子节点
        for (var child : node.getAllChildren()) {
            if (child != null)
                drawNodes(g, child);
        }
    }

    /** 绘制外发光效果（增加视觉深度） */
    private void drawGlow(Graphics2D g, Point p, Color color) {
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
        g.fillOval(p.x - RADIUS - 6, p.y - RADIUS - 6, (RADIUS + 6) * 2, (RADIUS + 6) * 2);
    }

    /**
     * 计算缩放比例与坐标映射
     * 采用“子树权重布局”：根据子树包含的节点总数分配横向宽度。
     */
    @Override
    protected void calculateScale() {
        if (data == null || getWidth() <= 0)
            return;
        posMap.clear();
        int treeHeight = getTreeHeight(data);
        // 根据树的高度动态调整层间距
        int dynamicLevelGap = Math.min(70, (getHeight() - 100) / Math.max(treeHeight, 1));
        doCalculate(data, padding, getWidth() - padding, padding + 40, dynamicLevelGap);
    }

    /**
     * 递归布局核心算法
     * 
     * @param node  当前节点
     * @param left  当前可用区域左边界
     * @param right 当前可用区域右边界
     * @param y     当前层深度
     * @param gap   层间距
     */
    private void doCalculate(TreeNode<T> node, int left, int right, int y, int gap) {
        if (node == null)
            return;

        // 节点水平居中显示在分配给它的子树区域内
        int x = (left + right) / 2;
        posMap.put(node, new Point(x, y));

        var children = node.getAllChildren();
        if (children.isEmpty())
            return;

        // 计算所有子树的总权重（用于按比例分配宽度）
        double totalWeight = children.stream()
                .mapToDouble(c -> c != null ? Math.max(c.subTreeCount, 1) : 0.5)
                .sum();

        int currentLeft = left;
        for (var child : children) {
            if (child == null)
                continue;
            // 按照子树节点数在父区域内切分宽度份额
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