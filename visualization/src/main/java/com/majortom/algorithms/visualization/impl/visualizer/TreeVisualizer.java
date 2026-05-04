package com.majortom.algorithms.visualization.impl.visualizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.visualization.VisualizationEvent;
import com.majortom.algorithms.visualization.base.BaseTreeVisualizer;

import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class TreeVisualizer<T extends Comparable<T>> extends BaseTreeVisualizer<T> {

    private static final double NODE_RADIUS = 26.0;
    private static final double BASE_LEVEL_HEIGHT = 115.0;
    private static final double BASE_NODE_GAP = 85.0;
    private long accentUntilMillis;
    private TreeNode<T> retainedFocus;

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
        TreeNode<T> treeFocus = resolveFocus();
        TreeNode<T> secondaryFocus = resolveSecondaryFocus();
        boolean isPathToFocus = isSameNode(childNode, treeFocus);
        boolean isFocusChild = isSameNode(childNode, secondaryFocus);
        int ancestorDistance = ancestorDistance(childNode, treeFocus);
        boolean isDirectParentEdge = ancestorDistance == 1;
        boolean isAncestorEdge = ancestorDistance > 1;

        if (isPathToFocus) {
            gc.setStroke(RAN_BLUE);
            gc.setLineWidth(4.0 / autoScale);
        } else if (isFocusChild) {
            gc.setStroke(RAN_YELLOW.deriveColor(0, 1, 0.9, 0.9));
            gc.setLineWidth(3.4 / autoScale);
        } else if (isDirectParentEdge) {
            gc.setStroke(RAN_RED.deriveColor(0, 1, 0.95, 0.95));
            gc.setLineWidth(3.2 / autoScale);
        } else if (isAncestorEdge) {
            gc.setStroke(RAN_RED.deriveColor(0, 1, 0.75, 0.75));
            gc.setLineWidth(2.8 / autoScale);
        } else {
            gc.setStroke(RAN_SLATE.deriveColor(0, 1, 0.9, 0.4));
            gc.setLineWidth(2.2 / autoScale);
        }
        gc.strokeLine(parent.x, parent.y, child.x, child.y);
    }

    private void drawRanNode(TreeNode<T> node, double x, double y) {
        TreeNode<T> focus = resolveFocus();
        TreeNode<T> secondaryFocus = resolveSecondaryFocus();
        boolean isLeaf = node.isLeaf();
        boolean isActive = isSameNode(node, focus);
        boolean isFocusChild = isSameNode(node, secondaryFocus);
        int ancestorDistance = ancestorDistance(node, focus);
        boolean isDirectParent = ancestorDistance == 1;
        boolean isAncestor = ancestorDistance > 1;

        // 颜色分配逻辑：恢复深色填充与发光
        Color strokeColor;
        Color fillColor;
        Color glowColor = null;

        if (isActive) {
            strokeColor = RAN_BLUE;
            fillColor = Color.rgb(0, 45, 90); // 深蓝甲胄感
            glowColor = RAN_BLUE;
        } else if (isFocusChild) {
            strokeColor = RAN_YELLOW;
            fillColor = Color.rgb(70, 52, 8);
            glowColor = RAN_YELLOW.deriveColor(0, 1, 1, 0.18);
        } else if (isDirectParent) {
            strokeColor = RAN_RED;
            fillColor = Color.rgb(58, 12, 12);
            glowColor = RAN_RED.deriveColor(0, 1, 1, 0.28);
        } else if (isAncestor) {
            strokeColor = RAN_RED.deriveColor(0, 1, 0.82, 0.9);
            fillColor = Color.rgb(42, 12, 12);
            glowColor = RAN_RED.deriveColor(0, 1, 0.9, 0.16);
        } else if (!isLeaf) {
            strokeColor = RAN_SLATE;
            fillColor = Color.rgb(28, 28, 32);
            glowColor = null;
        } else {
            strokeColor = RAN_SILVER.deriveColor(0, 1, 0.9, 0.65);
            fillColor = Color.rgb(22, 22, 26);
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

    @Override
    public void onControlAction(VisualizationEvent event) {
        super.onControlAction(event);
        accentUntilMillis = System.currentTimeMillis() + FEEDBACK_DURATION_MS;
    }

    @Override
    protected void resetTreeVisualizationState() {
        super.resetTreeVisualizationState();
        accentUntilMillis = 0L;
        retainedFocus = null;
    }

    private TreeNode<T> resolveFocus() {
        if (focusA instanceof TreeNode<?> activeNode) {
            @SuppressWarnings("unchecked")
            TreeNode<T> typedNode = (TreeNode<T>) activeNode;
            retainedFocus = typedNode;
        } else if (focusA != null) {
            TreeNode<T> resolvedNode = findNodeByData(treeInstance == null ? null : treeInstance.getRoot(), focusA);
            if (resolvedNode != null) {
                retainedFocus = resolvedNode;
            }
        }
        if (treeInstance == null) {
            return retainedFocus;
        }
        TreeNode<T> liveFocus = treeInstance.getCurrentHighlight();
        if (liveFocus != null) {
            retainedFocus = liveFocus;
        }
        return retainedFocus;
    }

    private TreeNode<T> resolveSecondaryFocus() {
        if (focusB instanceof TreeNode<?> secondaryNode) {
            @SuppressWarnings("unchecked")
            TreeNode<T> typedNode = (TreeNode<T>) secondaryNode;
            return typedNode;
        }
        if (focusB != null && treeInstance != null) {
            return findNodeByData(treeInstance.getRoot(), focusB);
        }
        return null;
    }

    private int ancestorDistance(TreeNode<T> node, TreeNode<T> focus) {
        if (node == null || focus == null || treeInstance == null || treeInstance.getRoot() == null) {
            return -1;
        }
        if (isSameNode(node, focus)) {
            return 0;
        }
        return distanceToFocus(node, focus);
    }

    private int distanceToFocus(TreeNode<T> current, TreeNode<T> focus) {
        if (current == null || current.getChildren() == null) {
            return -1;
        }
        for (TreeNode<T> child : current.getChildren()) {
            if (child == null) {
                continue;
            }
            if (isSameNode(child, focus)) {
                return 1;
            }
            int nestedDistance = distanceToFocus(child, focus);
            if (nestedDistance > 0) {
                return nestedDistance + 1;
            }
        }
        return -1;
    }

    private boolean isSameNode(TreeNode<T> left, TreeNode<T> right) {
        if (left == null || right == null) {
            return false;
        }
        return left == right || Objects.equals(left.data, right.data);
    }

    private TreeNode<T> findNodeByData(TreeNode<T> current, Object value) {
        if (current == null) {
            return null;
        }
        if (Objects.equals(current.data, value)) {
            return current;
        }
        if (current.getChildren() == null) {
            return null;
        }
        for (TreeNode<T> child : current.getChildren()) {
            TreeNode<T> result = findNodeByData(child, value);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
