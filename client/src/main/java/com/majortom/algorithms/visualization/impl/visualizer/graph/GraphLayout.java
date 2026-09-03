package com.majortom.algorithms.visualization.impl.visualizer.graph;

import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic circle initialization followed by project-owned force refinement. */
public final class GraphLayout {
    private static final int ITERATIONS = 90;
    private static final double PADDING = 90.0d;
    private static final double MIN_RADIUS = 150.0d;
    private static final double MAX_STEP = 20.0d;
    private static final double EPSILON = 0.0001d;

    public GraphLayoutResult layout(GraphViewState state) {
        List<GraphViewState.Node> nodes = state.nodes().stream()
                .sorted(Comparator.comparingLong(GraphViewState.Node::id))
                .toList();
        if (nodes.isEmpty()) {
            return new GraphLayoutResult(Map.of());
        }
        if (nodes.size() == 1) {
            return new GraphLayoutResult(Map.of(nodes.get(0).id(), new Point2D(PADDING, PADDING)));
        }

        Map<Long, Vec> positions = initialCircle(nodes);
        Map<Long, Vec> displacement = new LinkedHashMap<>();
        double radius = Math.max(MIN_RADIUS, nodes.size() * 28.0d);
        double area = Math.pow(radius * 2.0d, 2.0d);
        double k = Math.sqrt(area / nodes.size());

        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            displacement.clear();
            for (GraphViewState.Node node : nodes) {
                displacement.put(node.id(), new Vec(0.0d, 0.0d));
            }
            applyRepulsion(nodes, positions, displacement, k);
            applyAttraction(state, positions, displacement, k);
            double temperature = MAX_STEP * (1.0d - ((double) iteration / ITERATIONS));
            for (GraphViewState.Node node : nodes) {
                long id = node.id();
                Vec delta = displacement.get(id);
                double length = delta.length();
                if (length <= EPSILON) {
                    continue;
                }
                double step = Math.min(length, Math.max(1.0d, temperature));
                positions.put(id, positions.get(id).add(delta.scale(step / length)));
            }
        }

        return new GraphLayoutResult(normalize(positions));
    }

    private Map<Long, Vec> initialCircle(List<GraphViewState.Node> nodes) {
        Map<Long, Vec> result = new LinkedHashMap<>();
        double radius = Math.max(MIN_RADIUS, nodes.size() * 28.0d);
        for (int index = 0; index < nodes.size(); index++) {
            double angle = (Math.PI * 2.0d * index / nodes.size()) - Math.PI / 2.0d;
            result.put(nodes.get(index).id(), new Vec(Math.cos(angle) * radius, Math.sin(angle) * radius));
        }
        return result;
    }

    private void applyRepulsion(
            List<GraphViewState.Node> nodes,
            Map<Long, Vec> positions,
            Map<Long, Vec> displacement,
            double k) {
        for (int leftIndex = 0; leftIndex < nodes.size(); leftIndex++) {
            long leftId = nodes.get(leftIndex).id();
            for (int rightIndex = leftIndex + 1; rightIndex < nodes.size(); rightIndex++) {
                long rightId = nodes.get(rightIndex).id();
                Vec delta = positions.get(leftId).subtract(positions.get(rightId));
                double distance = Math.max(EPSILON, delta.length());
                double force = (k * k) / distance;
                Vec vector = delta.scale(force / distance);
                displacement.put(leftId, displacement.get(leftId).add(vector));
                displacement.put(rightId, displacement.get(rightId).subtract(vector));
            }
        }
    }

    private void applyAttraction(
            GraphViewState state,
            Map<Long, Vec> positions,
            Map<Long, Vec> displacement,
            double k) {
        for (GraphViewState.Edge edge : state.edges()) {
            if (edge.fromId() == edge.toId()) {
                continue;
            }
            Vec from = positions.get(edge.fromId());
            Vec to = positions.get(edge.toId());
            if (from == null || to == null) {
                continue;
            }
            Vec delta = from.subtract(to);
            double distance = Math.max(EPSILON, delta.length());
            double force = (distance * distance) / k;
            Vec vector = delta.scale(force / distance);
            displacement.put(edge.fromId(), displacement.get(edge.fromId()).subtract(vector));
            displacement.put(edge.toId(), displacement.get(edge.toId()).add(vector));
        }
    }

    private Map<Long, Point2D> normalize(Map<Long, Vec> positions) {
        double minX = positions.values().stream().mapToDouble(Vec::x).min().orElse(0.0d);
        double minY = positions.values().stream().mapToDouble(Vec::y).min().orElse(0.0d);
        List<Map.Entry<Long, Vec>> entries = new ArrayList<>(positions.entrySet());
        Map<Long, Point2D> result = new LinkedHashMap<>();
        for (Map.Entry<Long, Vec> entry : entries) {
            Vec point = entry.getValue();
            result.put(entry.getKey(), new Point2D(point.x() - minX + PADDING, point.y() - minY + PADDING));
        }
        return result;
    }

    public record GraphLayoutResult(Map<Long, Point2D> positions) {
        public GraphLayoutResult {
            positions = Map.copyOf(positions);
        }
    }

    private record Vec(double x, double y) {
        private Vec add(Vec other) {
            return new Vec(x + other.x, y + other.y);
        }

        private Vec subtract(Vec other) {
            return new Vec(x - other.x, y - other.y);
        }

        private Vec scale(double factor) {
            return new Vec(x * factor, y * factor);
        }

        private double length() {
            return Math.hypot(x, y);
        }
    }
}
