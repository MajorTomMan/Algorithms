package com.majortom.algorithms.visualization.impl.visualizer.tree;

import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic project-owned tidy layout for general and binary tree facts. */
public final class TreeLayout {
    private static final double HORIZONTAL_GAP = 70.0d;
    private static final double LEVEL_GAP = 105.0d;
    private static final double MIN_SUBTREE_WIDTH = 92.0d;
    private static final double COMPONENT_GAP = 120.0d;

    public TreeLayoutResult layout(TreeViewState state) {
        if (state.nodes().isEmpty()) {
            return new TreeLayoutResult(Map.of());
        }

        Set<Long> visited = new HashSet<>();
        Map<Long, Point2D> positions = new LinkedHashMap<>();
        double nextComponentX = 0.0d;
        double nextComponentY = 0.0d;
        double rootComponentHeight = 0.0d;

        if (state.rootId() != null && state.nodes().containsKey(state.rootId())) {
            Subtree root = layoutSubtree(state.rootId(), state, visited, new HashSet<>());
            copyShifted(root.positions(), positions, nextComponentX, nextComponentY);
            nextComponentX += root.width() + COMPONENT_GAP;
            rootComponentHeight = root.height();
        }

        List<Long> remaining = state.nodes().keySet().stream()
                .filter(id -> !visited.contains(id))
                .sorted(Comparator.naturalOrder())
                .toList();
        if (!remaining.isEmpty()) {
            nextComponentX = 0.0d;
            nextComponentY = rootComponentHeight == 0.0d ? 0.0d : rootComponentHeight + COMPONENT_GAP;
        }
        for (Long nodeId : remaining) {
            if (visited.contains(nodeId)) {
                continue;
            }
            Subtree component = layoutSubtree(nodeId, state, visited, new HashSet<>());
            copyShifted(component.positions(), positions, nextComponentX, nextComponentY);
            nextComponentX += component.width() + COMPONENT_GAP;
        }

        normalize(positions);
        return new TreeLayoutResult(positions);
    }

    private Subtree layoutSubtree(long nodeId, TreeViewState state, Set<Long> visited, Set<Long> path) {
        if (!visited.add(nodeId)) {
            return Subtree.empty();
        }
        TreeViewState.Node node = state.nodes().get(nodeId);
        if (node == null) {
            return Subtree.empty();
        }
        path.add(nodeId);

        List<Subtree> children = new ArrayList<>();
        for (Long childId : state.childrenOf(node)) {
            Subtree child = childSubtree(childId, state, visited, path);
            if (!child.isEmpty()) {
                children.add(child);
            }
        }
        path.remove(nodeId);

        Map<Long, Point2D> positions = new LinkedHashMap<>();
        if (children.isEmpty()) {
            double rootX = MIN_SUBTREE_WIDTH / 2.0d;
            positions.put(nodeId, new Point2D(rootX, 0.0d));
            return new Subtree(MIN_SUBTREE_WIDTH, LEVEL_GAP, positions, rootX);
        }

        double x = 0.0d;
        double maxChildHeight = 0.0d;
        double firstRootX = 0.0d;
        double lastRootX = 0.0d;
        for (int index = 0; index < children.size(); index++) {
            Subtree child = children.get(index);
            copyShifted(child.positions(), positions, x, LEVEL_GAP);
            double childRootX = x + child.rootX();
            if (index == 0) {
                firstRootX = childRootX;
            }
            lastRootX = childRootX;
            maxChildHeight = Math.max(maxChildHeight, child.height());
            x += child.width();
            if (index + 1 < children.size()) {
                x += HORIZONTAL_GAP;
            }
        }

        double width = Math.max(MIN_SUBTREE_WIDTH, x);
        double rootX = (firstRootX + lastRootX) / 2.0d;
        positions.put(nodeId, new Point2D(rootX, 0.0d));
        return new Subtree(width, maxChildHeight + LEVEL_GAP, positions, rootX);
    }

    private Subtree childSubtree(Long childId, TreeViewState state, Set<Long> visited, Set<Long> path) {
        if (childId == null || !state.nodes().containsKey(childId) || path.contains(childId) || visited.contains(childId)) {
            return Subtree.empty();
        }
        return layoutSubtree(childId, state, visited, path);
    }

    private static void copyShifted(Map<Long, Point2D> source, Map<Long, Point2D> target, double x, double y) {
        for (Map.Entry<Long, Point2D> entry : source.entrySet()) {
            Point2D point = entry.getValue();
            target.put(entry.getKey(), new Point2D(point.getX() + x, point.getY() + y));
        }
    }

    private static void normalize(Map<Long, Point2D> positions) {
        if (positions.isEmpty()) {
            return;
        }
        double minX = positions.values().stream().mapToDouble(Point2D::getX).min().orElse(0.0d);
        double minY = positions.values().stream().mapToDouble(Point2D::getY).min().orElse(0.0d);
        double offsetX = 70.0d - minX;
        double offsetY = 70.0d - minY;
        List<Map.Entry<Long, Point2D>> entries = new ArrayList<>(positions.entrySet());
        for (Map.Entry<Long, Point2D> entry : entries) {
            Point2D point = entry.getValue();
            positions.put(entry.getKey(), point.add(offsetX, offsetY));
        }
    }

    public record TreeLayoutResult(Map<Long, Point2D> positions) {
        public TreeLayoutResult {
            positions = Map.copyOf(positions);
        }
    }

    private record Subtree(double width, double height, Map<Long, Point2D> positions, double rootX) {
        private static Subtree empty() {
            return new Subtree(0.0d, 0.0d, Map.of(), 0.0d);
        }

        private boolean isEmpty() {
            return positions.isEmpty();
        }
    }
}
