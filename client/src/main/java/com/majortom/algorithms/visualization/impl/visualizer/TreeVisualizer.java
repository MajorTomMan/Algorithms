package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.library.tree.AvlNodeSnapshot;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.VisualizationActionType;
import com.majortom.algorithms.visualization.VisualizationEvent;
import com.majortom.algorithms.visualization.runtime.tree.AvlTreeViewState;
import javafx.animation.AnimationTimer;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Stable, animated renderer for immutable AVL snapshots. */
public final class TreeVisualizer extends BaseVisualizer<AvlTreeViewState> {

    private static final double LERP_FACTOR = 0.18d;
    private static final double POSITION_EPSILON = 0.35d;
    private static final double BASE_RADIUS = 26.0d;

    private final Map<Long, Point> currentPositions = new HashMap<>();
    private final Map<Long, Point> targetPositions = new LinkedHashMap<>();
    private final Map<Long, Long> parentById = new HashMap<>();
    private final AnimationTimer layoutTimer;
    private AvlTreeViewState state;
    private boolean timerRunning;
    private boolean animationRequested = true;
    private double nodeRadius = BASE_RADIUS;

    public TreeVisualizer() {
        layoutTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                boolean moving = interpolatePositions();
                renderFrame();
                if (!moving) {
                    stopLayoutTimer();
                }
            }
        };
    }

    @Override
    protected void draw(AvlTreeViewState nextState, Object unusedA, Object unusedB) {
        state = nextState;
        if (nextState.root() == null) {
            currentPositions.clear();
            targetPositions.clear();
            parentById.clear();
            stopLayoutTimer();
            clear();
            drawTransientFeedbackOverlay();
            return;
        }
        updateLayout(nextState.root());
        renderFrame();
        updateTimerState();
    }

    private void updateLayout(AvlNodeSnapshot root) {
        targetPositions.clear();
        parentById.clear();
        Map<Long, LayoutNode> rawLayout = new LinkedHashMap<>();
        int[] rank = {0};
        collectInOrder(root, 0, null, rank, rawLayout);

        double width = Math.max(1.0d, canvas.getWidth());
        double height = Math.max(1.0d, canvas.getHeight());
        int nodeCount = Math.max(1, rawLayout.size());
        int maximumDepth = rawLayout.values().stream().mapToInt(LayoutNode::depth).max().orElse(0);
        double horizontalPadding = Math.min(70.0d, width * 0.08d);
        double topPadding = Math.min(85.0d, Math.max(42.0d, height * 0.1d));
        double bottomPadding = 45.0d;
        double usableWidth = Math.max(1.0d, width - horizontalPadding * 2.0d);
        double usableHeight = Math.max(1.0d, height - topPadding - bottomPadding);
        double xSpacing = usableWidth / Math.max(1, nodeCount - 1);
        double ySpacing = usableHeight / Math.max(1, maximumDepth);
        nodeRadius = Math.max(11.0d, Math.min(BASE_RADIUS, Math.min(xSpacing * 0.32d, ySpacing * 0.28d)));

        for (Map.Entry<Long, LayoutNode> entry : rawLayout.entrySet()) {
            LayoutNode raw = entry.getValue();
            double x = width / 2.0d;
            if (nodeCount > 1) {
                x = horizontalPadding + raw.rank() * xSpacing;
            }
            double y = topPadding + raw.depth() * ySpacing;
            targetPositions.put(entry.getKey(), new Point(x, y));
        }

        currentPositions.keySet().retainAll(targetPositions.keySet());
        for (Map.Entry<Long, Point> entry : targetPositions.entrySet()) {
            long nodeId = entry.getKey();
            if (currentPositions.containsKey(nodeId)) {
                continue;
            }
            Long parent = parentById.get(nodeId);
            Point origin = null;
            if (parent != null) {
                origin = currentPositions.get(parent);
            }
            if (origin == null) {
                origin = entry.getValue();
            }
            currentPositions.put(nodeId, new Point(origin.x(), origin.y()));
        }
    }

    private void collectInOrder(
            AvlNodeSnapshot node,
            int depth,
            Long parent,
            int[] rank,
            Map<Long, LayoutNode> layout) {
        if (node == null) {
            return;
        }
        collectInOrder(node.left(), depth + 1, node.id(), rank, layout);
        layout.put(node.id(), new LayoutNode(rank[0], depth));
        parentById.put(node.id(), parent);
        rank[0]++;
        collectInOrder(node.right(), depth + 1, node.id(), rank, layout);
    }

    private boolean interpolatePositions() {
        boolean moving = false;
        for (Map.Entry<Long, Point> entry : targetPositions.entrySet()) {
            Point target = entry.getValue();
            Point current = currentPositions.getOrDefault(entry.getKey(), target);
            double dx = target.x() - current.x();
            double dy = target.y() - current.y();
            if (Math.abs(dx) <= POSITION_EPSILON && Math.abs(dy) <= POSITION_EPSILON) {
                currentPositions.put(entry.getKey(), target);
                continue;
            }
            moving = true;
            currentPositions.put(entry.getKey(), new Point(
                    current.x() + dx * LERP_FACTOR,
                    current.y() + dy * LERP_FACTOR));
        }
        return moving;
    }

    private void renderFrame() {
        clear();
        if (state == null || state.root() == null) {
            return;
        }
        Set<Long> focusPath = state.ancestorIds();
        drawEdges(state.root(), focusPath);
        drawNodes(state.root(), focusPath);
        drawTransientFeedbackOverlay();
    }

    private void drawEdges(AvlNodeSnapshot node, Set<Long> focusPath) {
        if (node == null) {
            return;
        }
        Point parent = currentPositions.get(node.id());
        if (parent == null) {
            return;
        }
        drawEdgeToChild(node, node.left(), parent, focusPath);
        drawEdgeToChild(node, node.right(), parent, focusPath);
        drawEdges(node.left(), focusPath);
        drawEdges(node.right(), focusPath);
    }

    private void drawEdgeToChild(
            AvlNodeSnapshot parentNode, AvlNodeSnapshot childNode, Point parent, Set<Long> focusPath) {
        if (childNode == null) {
            return;
        }
        Point child = currentPositions.get(childNode.id());
        if (child == null) {
            return;
        }
        boolean pathEdge = focusPath.contains(parentNode.id()) && focusPath.contains(childNode.id());
        gc.setStroke(RAN_SLATE.deriveColor(0.0d, 1.0d, 0.9d, 0.48d));
        gc.setLineWidth(2.2d);
        if (pathEdge || childNode.id() == value(state.focusId())) {
            gc.setStroke(RAN_RED.deriveColor(0.0d, 1.0d, 1.0d, 0.9d));
            gc.setLineWidth(3.4d);
        }
        if (childNode.id() == value(state.childId())) {
            gc.setStroke(RAN_YELLOW);
        }
        gc.strokeLine(parent.x(), parent.y(), child.x(), child.y());
    }

    private void drawNodes(AvlNodeSnapshot node, Set<Long> focusPath) {
        if (node == null) {
            return;
        }
        drawNodes(node.left(), focusPath);
        drawNodes(node.right(), focusPath);
        Point point = currentPositions.get(node.id());
        if (point == null) {
            return;
        }
        boolean active = node.id() == value(state.focusId());
        boolean directParent = node.id() == value(state.parentId());
        boolean selectedChild = node.id() == value(state.childId());
        boolean ancestor = !active && !directParent && focusPath.contains(node.id());
        boolean leaf = node.left() == null && node.right() == null;

        Color stroke = RAN_SLATE;
        Color fill = Color.rgb(28, 28, 32);
        if (leaf) {
            stroke = RAN_SILVER.deriveColor(0.0d, 1.0d, 0.9d, 0.72d);
            fill = Color.rgb(22, 22, 26);
        }
        if (ancestor) {
            stroke = RAN_RED.deriveColor(0.0d, 1.0d, 0.88d, 0.95d);
            fill = Color.rgb(48, 12, 12);
            drawGlow(point, RAN_RED, 0.14d);
        }
        if (directParent) {
            stroke = RAN_RED;
            fill = Color.rgb(86, 12, 12);
            drawGlow(point, RAN_RED, 0.22d);
        }
        if (selectedChild) {
            stroke = RAN_YELLOW;
            fill = Color.rgb(82, 65, 0);
            drawGlow(point, RAN_YELLOW, 0.18d);
        }
        if (active) {
            stroke = RAN_BLUE;
            fill = Color.rgb(0, 45, 90);
            drawGlow(point, RAN_BLUE, 0.23d);
        }

        gc.setFill(fill);
        gc.fillOval(point.x() - nodeRadius, point.y() - nodeRadius, nodeRadius * 2.0d, nodeRadius * 2.0d);
        gc.setStroke(stroke);
        gc.setLineWidth(active ? 4.2d : 2.6d);
        gc.strokeOval(point.x() - nodeRadius, point.y() - nodeRadius, nodeRadius * 2.0d, nodeRadius * 2.0d);
        gc.setFill(RAN_WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, Math.max(10.0d, nodeRadius * 0.68d)));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(String.valueOf(node.value()), point.x(), point.y());
    }

    private void drawGlow(Point point, Color color, double opacity) {
        double radius = nodeRadius + 9.0d;
        gc.setFill(color.deriveColor(0.0d, 1.0d, 1.0d, opacity));
        gc.fillOval(point.x() - radius, point.y() - radius, radius * 2.0d, radius * 2.0d);
    }

    private long value(Long nodeId) {
        if (nodeId == null) {
            return Long.MIN_VALUE;
        }
        return nodeId;
    }

    @Override
    public void onControlAction(VisualizationEvent event) {
        super.onControlAction(event);
        VisualizationActionType action = event.actionType();
        if (action == VisualizationActionType.EXECUTION_PAUSE) {
            animationRequested = false;
        } else if (action == VisualizationActionType.EXECUTION_RESUME
                || action == VisualizationActionType.EXECUTION_START
                || action == VisualizationActionType.TREE_INSERT
                || action == VisualizationActionType.TREE_DELETE
                || action == VisualizationActionType.TREE_RANDOM
                || action == VisualizationActionType.EXECUTION_RESET) {
            animationRequested = true;
        }
        updateTimerState();
    }

    @Override
    public void onVisualizationReset() {
        resetLocalState();
        animationRequested = true;
        super.onVisualizationReset();
    }

    @Override
    public void onModuleAttached(String moduleId) {
        super.onModuleAttached(moduleId);
        updateTimerState();
    }

    @Override
    public void onModuleDetached(String moduleId) {
        resetLocalState();
        super.onModuleDetached(moduleId);
        clear();
    }

    @Override
    protected void onResizeStateChanged(boolean resizing) {
        if (resizing) {
            stopLayoutTimer();
            return;
        }
        if (state != null && state.root() != null) {
            updateLayout(state.root());
            renderFrame();
        }
        updateTimerState();
    }

    @Override
    public void dispose() {
        resetLocalState();
        super.dispose();
    }

    private void updateTimerState() {
        if (!animationRequested || !isModuleAttached() || isResizeInProgress() || isDisposed()) {
            stopLayoutTimer();
            return;
        }
        if (positionsDiffer()) {
            startLayoutTimer();
        } else {
            stopLayoutTimer();
        }
    }

    private boolean positionsDiffer() {
        for (Map.Entry<Long, Point> entry : targetPositions.entrySet()) {
            Point current = currentPositions.get(entry.getKey());
            if (current == null) {
                return true;
            }
            Point target = entry.getValue();
            if (Math.abs(target.x() - current.x()) > POSITION_EPSILON
                    || Math.abs(target.y() - current.y()) > POSITION_EPSILON) {
                return true;
            }
        }
        return false;
    }

    private void startLayoutTimer() {
        if (timerRunning) {
            return;
        }
        timerRunning = true;
        layoutTimer.start();
    }

    private void stopLayoutTimer() {
        if (!timerRunning) {
            return;
        }
        layoutTimer.stop();
        timerRunning = false;
    }

    private void resetLocalState() {
        stopLayoutTimer();
        state = null;
        currentPositions.clear();
        targetPositions.clear();
        parentById.clear();
        nodeRadius = BASE_RADIUS;
    }

    private record Point(double x, double y) {
    }

    private record LayoutNode(int rank, int depth) {
    }
}
