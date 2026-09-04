package com.majortom.algorithms.visualization.impl.visualizer.linked;

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Linked-list-specific transient ELK adapter with routed next/previous/self-loop edges. */
public final class LinkedListElkLayout {
    private static final double PADDING = 34.0d;
    private static final double ELEMENT_SPACING = 46.0d;
    private static final int RANDOM_SEED = 1;

    public LayoutResult layout(LayoutRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.nodes().isEmpty()) {
            return new LayoutResult(Map.of(), Map.of());
        }

        ElkNode graph = ElkGraphUtil.createGraph();
        graph.setProperty(CoreOptions.ALGORITHM, LayeredOptions.ALGORITHM_ID);
        graph.setProperty(CoreOptions.DIRECTION, Direction.RIGHT);
        graph.setProperty(CoreOptions.EDGE_ROUTING, EdgeRouting.ORTHOGONAL);
        graph.setProperty(CoreOptions.PADDING, new ElkPadding(PADDING));
        graph.setProperty(CoreOptions.RANDOM_SEED, RANDOM_SEED);
        graph.setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, ELEMENT_SPACING);
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
            if (source == null || target == null) {
                continue;
            }
            ElkEdge edge = ElkGraphUtil.createSimpleEdge(source, target);
            edge.setIdentifier(link.id());
        }

        new RecursiveGraphLayoutEngine().layout(graph, new BasicProgressMonitor());

        Map<String, ElementBounds> elements = new LinkedHashMap<>();
        elkNodes.forEach((id, node) -> elements.put(nodeId(id),
                new ElementBounds(nodeId(id), node.getX(), node.getY(), node.getWidth(), node.getHeight())));

        Map<String, EdgeRoute> edges = new LinkedHashMap<>();
        for (ElkEdge edge : graph.getContainedEdges()) {
            if (edge.getIdentifier() == null || edge.getSections().isEmpty()) {
                continue;
            }
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

    public record LayoutRequest(List<NodeSize> nodes, List<Link> links) {
        public LayoutRequest {
            nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
            links = List.copyOf(Objects.requireNonNull(links, "links"));
        }

        public static LayoutRequest empty() {
            return new LayoutRequest(List.of(), List.of());
        }
    }

    public record NodeSize(long id, double width, double height) {
        public NodeSize {
            if (id <= 0) {
                throw new IllegalArgumentException("node id must be positive");
            }
            if (!(width > 0.0d) || !(height > 0.0d)) {
                throw new IllegalArgumentException("node size must be positive: " + id);
            }
        }
    }

    public record Link(String id, long sourceId, long targetId) {
        public Link {
            Objects.requireNonNull(id, "id");
        }
    }

    public static String nodeId(long id) {
        return "linked:" + id;
    }
}
