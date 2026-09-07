package com.majortom.algorithms.visualization.impl.visualizer.graph;

import com.majortom.algorithms.visualization.common.layout.EdgeRoute;
import com.majortom.algorithms.visualization.common.layout.ElementBounds;
import com.majortom.algorithms.visualization.common.layout.LayoutResult;
import javafx.geometry.Point2D;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.alg.layered.options.OrderingStrategy;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.options.EdgeRouting;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkBendPoint;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Graph-specific layout selector. Directed acyclic graphs use ELK's layered/Sugiyama geometry;
 * every other graph uses deterministic force-directed topology geometry so cycles and branching
 * remain visible without inventing tree semantics.
 */
public final class GraphElkLayout {
    private static final double PADDING = 42.0d;
    private static final double NODE_SPACING = 42.0d;
    private static final double LAYER_SPACING = 64.0d;
    private static final double COMPONENT_SPACING = 72.0d;
    private static final int RANDOM_SEED = 1;
    private final GraphTopologyLayout topologyLayout = new GraphTopologyLayout();

    public LayoutResult layout(LayoutRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.nodes().isEmpty()) return new LayoutResult(Map.of(), Map.of());
        if (request.directed() && isDirectedAcyclic(request)) {
            return layeredLayout(request);
        } else {
            return topologyLayout.layout(request);
        }
    }

    private LayoutResult layeredLayout(LayoutRequest request) {
        ElkNode graph = ElkGraphUtil.createGraph();
        graph.setProperty(CoreOptions.ALGORITHM, LayeredOptions.ALGORITHM_ID);
        graph.setProperty(CoreOptions.DIRECTION, Direction.RIGHT);
        graph.setProperty(CoreOptions.EDGE_ROUTING, EdgeRouting.ORTHOGONAL);
        graph.setProperty(CoreOptions.PADDING, new ElkPadding(PADDING));
        graph.setProperty(CoreOptions.RANDOM_SEED, RANDOM_SEED);
        graph.setProperty(CoreOptions.SPACING_NODE_NODE, NODE_SPACING);
        graph.setProperty(CoreOptions.SPACING_COMPONENT_COMPONENT, COMPONENT_SPACING);
        graph.setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, LAYER_SPACING);
        graph.setProperty(LayeredOptions.CONSIDER_MODEL_ORDER_STRATEGY, OrderingStrategy.NODES_AND_EDGES);

        Map<Long, ElkNode> elkNodes = new LinkedHashMap<>();
        for (NodeSize nodeSize : request.nodes()) {
            ElkNode node = ElkGraphUtil.createNode(graph);
            node.setIdentifier(nodeId(nodeSize.id()));
            node.setDimensions(nodeSize.width(), nodeSize.height());
            elkNodes.put(nodeSize.id(), node);
        }
        for (Link link : request.links()) {
            ElkNode source = elkNodes.get(link.sourceId());
            ElkNode target = elkNodes.get(link.targetId());
            if (source == null || target == null) continue;
            ElkEdge edge = ElkGraphUtil.createSimpleEdge(source, target);
            edge.setIdentifier(link.id());
        }

        new RecursiveGraphLayoutEngine().layout(graph, new BasicProgressMonitor());
        Map<String, ElementBounds> elements = new LinkedHashMap<>();
        elkNodes.forEach((id, node) -> elements.put(nodeId(id),
                new ElementBounds(nodeId(id), node.getX(), node.getY(), node.getWidth(), node.getHeight())));
        Map<String, EdgeRoute> edges = new LinkedHashMap<>();
        for (ElkEdge edge : graph.getContainedEdges()) {
            if (edge.getIdentifier() == null || edge.getSections().isEmpty()) continue;
            ElkEdgeSection section = edge.getSections().getFirst();
            List<Point2D> points = new ArrayList<>();
            points.add(new Point2D(section.getStartX(), section.getStartY()));
            for (ElkBendPoint bendPoint : section.getBendPoints()) {
                points.add(new Point2D(bendPoint.getX(), bendPoint.getY()));
            }
            points.add(new Point2D(section.getEndX(), section.getEndY()));
            edges.put(edge.getIdentifier(), new EdgeRoute(edge.getIdentifier(), points));
        }
        return new LayoutResult(elements, edges);
    }

    private boolean isDirectedAcyclic(LayoutRequest request) {
        Map<Long, Integer> indegree = new LinkedHashMap<>();
        Map<Long, List<Long>> outgoing = new LinkedHashMap<>();
        request.nodes().stream().sorted(Comparator.comparingLong(NodeSize::id)).forEach(node -> {
            indegree.put(node.id(), 0);
            outgoing.put(node.id(), new ArrayList<>());
        });
        for (Link link : request.links()) {
            if (!indegree.containsKey(link.sourceId()) || !indegree.containsKey(link.targetId())) continue;
            if (link.sourceId() == link.targetId()) return false;
            outgoing.get(link.sourceId()).add(link.targetId());
            indegree.compute(link.targetId(), (ignored, current) -> current + 1);
        }
        ArrayDeque<Long> ready = new ArrayDeque<>();
        indegree.entrySet().stream().filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey).sorted().forEach(ready::addLast);
        int visited = 0;
        while (!ready.isEmpty()) {
            long current = ready.removeFirst();
            visited++;
            outgoing.get(current).stream().sorted().forEach(target -> {
                int next = indegree.compute(target, (ignored, value) -> value - 1);
                if (next == 0) ready.addLast(target);
            });
        }
        return visited == indegree.size();
    }

    public record LayoutRequest(boolean directed, List<NodeSize> nodes, List<Link> links) {
        public LayoutRequest {
            nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
            links = List.copyOf(Objects.requireNonNull(links, "links"));
        }
        public static LayoutRequest empty() { return new LayoutRequest(false, List.of(), List.of()); }
    }

    public record NodeSize(long id, double width, double height) {
        public NodeSize {
            if (id <= 0) throw new IllegalArgumentException("node id must be positive");
            if (!(width > 0.0d) || !(height > 0.0d)) {
                throw new IllegalArgumentException("node size must be positive: " + id);
            }
        }
    }

    /** label is presentation-only and reserved for future weights/edge metadata. */
    public record Link(String id, long edgeId, long sourceId, long targetId, String label) {
        public Link(String id, long edgeId, long sourceId, long targetId) {
            this(id, edgeId, sourceId, targetId, null);
        }
        public Link {
            Objects.requireNonNull(id, "id");
            if (label == null || label.isBlank()) {
                label = null;
            } else {
                label = label;
            }
        }
    }

    public static String nodeId(long id) { return "graph:node:" + id; }
    public static String edgeId(long id) { return "graph:edge:" + id; }
}
