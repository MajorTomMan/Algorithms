package com.majortom.algorithms.visualization.impl.visualizer.tree;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.layout.LayoutEngine;

import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.alg.layered.options.OrderingStrategy;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.options.EdgeRouting;
import org.eclipse.elk.core.options.PortConstraints;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkBendPoint;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.eclipse.elk.graph.util.ElkGraphUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JavaFX-neutral tree layout engine owned by RenderFramework's Layout Pool. */
public final class TreeElkLayout implements LayoutEngine {
    public static final String ID = "tree";
    private static final double PADDING = 36.0d;
    private static final double NODE_SPACING = 42.0d;
    private static final double LEVEL_SPACING = 68.0d;
    private static final int RANDOM_SEED = 1;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public LayoutResult layout(LayoutRequest request) {
        if (request.elements().isEmpty()) {
            return new LayoutResult(
                    request.requestId(),
                    request.modelRevision(),
                    Map.of(),
                    List.of(),
                    BoundsSnapshot.empty());
        }
        boolean binary = "BINARY".equals(request.metadata().get("kind"));
        ElkNode graph = ElkGraphUtil.createGraph();
        graph.setProperty(CoreOptions.ALGORITHM, LayeredOptions.ALGORITHM_ID);
        graph.setProperty(CoreOptions.DIRECTION, Direction.DOWN);
        graph.setProperty(CoreOptions.EDGE_ROUTING, EdgeRouting.POLYLINE);
        graph.setProperty(CoreOptions.PADDING, new ElkPadding(PADDING));
        graph.setProperty(CoreOptions.RANDOM_SEED, RANDOM_SEED);
        graph.setProperty(CoreOptions.SPACING_NODE_NODE, NODE_SPACING);
        graph.setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, LEVEL_SPACING);
        graph.setProperty(
                LayeredOptions.CONSIDER_MODEL_ORDER_STRATEGY, OrderingStrategy.NODES_AND_EDGES);
        graph.setProperty(LayeredOptions.CROSSING_MINIMIZATION_FORCE_NODE_MODEL_ORDER, true);

        Map<String, ElkNode> elkNodes = new LinkedHashMap<>();
        for (LayoutElement element : request.elements()) {
            ElkNode node = ElkGraphUtil.createNode(graph);
            node.setIdentifier(element.id());
            node.setDimensions(element.width(), element.height());
            elkNodes.put(element.id(), node);
        }

        Map<String, ElkPort> sourcePorts =
                binary ? createOrderedSourcePorts(request.links(), elkNodes) : Map.of();
        for (LayoutLink link : request.links()) {
            ElkNode source = elkNodes.get(link.sourceId());
            ElkNode target = elkNodes.get(link.targetId());
            if (source == null || target == null) continue;
            ElkPort sourcePort = sourcePorts.get(link.id());
            ElkEdge edge =
                    sourcePort == null
                            ? ElkGraphUtil.createSimpleEdge(source, target)
                            : ElkGraphUtil.createSimpleEdge(sourcePort, target);
            edge.setIdentifier(link.id());
        }

        new RecursiveGraphLayoutEngine().layout(graph, new BasicProgressMonitor());

        Map<String, ElementGeometry> elements = new LinkedHashMap<>();
        elkNodes.forEach(
                (id, node) ->
                        elements.put(
                                id,
                                new ElementGeometry(
                                        id,
                                        node.getX(),
                                        node.getY(),
                                        node.getWidth(),
                                        node.getHeight())));
        List<EdgeGeometry> edges = new ArrayList<>();
        for (ElkEdge edge : graph.getContainedEdges()) {
            if (edge.getIdentifier() == null || edge.getSections().isEmpty()) continue;
            ElkEdgeSection section = edge.getSections().getFirst();
            List<EdgeGeometry.Point> points = new ArrayList<>();
            points.add(new EdgeGeometry.Point(section.getStartX(), section.getStartY()));
            for (ElkBendPoint bendPoint : section.getBendPoints()) {
                points.add(new EdgeGeometry.Point(bendPoint.getX(), bendPoint.getY()));
            }
            points.add(new EdgeGeometry.Point(section.getEndX(), section.getEndY()));
            edges.add(new EdgeGeometry(edge.getIdentifier(), points));
        }
        return new LayoutResult(
                request.requestId(),
                request.modelRevision(),
                elements,
                edges,
                contentBounds(elements));
    }

    private Map<String, ElkPort> createOrderedSourcePorts(
            List<LayoutLink> links, Map<String, ElkNode> elkNodes) {
        Map<String, List<LayoutLink>> outgoing = new LinkedHashMap<>();
        for (LayoutLink link : links)
            outgoing.computeIfAbsent(link.sourceId(), ignored -> new ArrayList<>()).add(link);
        Map<String, ElkPort> ports = new LinkedHashMap<>();
        for (Map.Entry<String, List<LayoutLink>> entry : outgoing.entrySet()) {
            ElkNode source = elkNodes.get(entry.getKey());
            if (source == null) continue;
            List<LayoutLink> ordered = new ArrayList<>(entry.getValue());
            ordered.sort(Comparator.comparingInt(LayoutLink::order).thenComparing(LayoutLink::id));
            source.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_ORDER);
            for (int ordinal = 0; ordinal < ordered.size(); ordinal++) {
                LayoutLink link = ordered.get(ordinal);
                ElkPort port = ElkGraphUtil.createPort(source);
                port.setIdentifier("port:" + link.id());
                port.setDimensions(1.0d, 1.0d);
                port.setProperty(CoreOptions.PORT_SIDE, PortSide.SOUTH);
                port.setProperty(CoreOptions.PORT_INDEX, ordered.size() - 1 - ordinal);
                ports.put(link.id(), port);
            }
        }
        return ports;
    }

    private static BoundsSnapshot contentBounds(Map<String, ElementGeometry> elements) {
        if (elements.isEmpty()) return BoundsSnapshot.empty();
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (ElementGeometry element : elements.values()) {
            minX = Math.min(minX, element.x());
            minY = Math.min(minY, element.y());
            maxX = Math.max(maxX, element.x() + element.width());
            maxY = Math.max(maxY, element.y() + element.height());
        }
        return new BoundsSnapshot(minX, minY, maxX - minX, maxY - minY);
    }

    public static String nodeId(long id) {
        return "tree:" + id;
    }
}
