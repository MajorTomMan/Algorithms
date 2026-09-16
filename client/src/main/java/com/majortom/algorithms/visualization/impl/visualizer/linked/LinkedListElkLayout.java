package com.majortom.algorithms.visualization.impl.visualizer.linked;

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

/** JavaFX-neutral linked-list ELK engine owned by RenderFramework. */
public final class LinkedListElkLayout implements LayoutEngine {
    public static final String ID = "linked-list";
    private static final double PADDING = 34.0d;
    private static final double ELEMENT_SPACING = 46.0d;

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

        ElkNode graph = ElkGraphUtil.createGraph();
        graph.setProperty(CoreOptions.ALGORITHM, LayeredOptions.ALGORITHM_ID);
        graph.setProperty(CoreOptions.DIRECTION, Direction.RIGHT);
        graph.setProperty(CoreOptions.EDGE_ROUTING, EdgeRouting.ORTHOGONAL);
        graph.setProperty(CoreOptions.PADDING, new ElkPadding(PADDING));
        graph.setProperty(CoreOptions.RANDOM_SEED, 1);
        graph.setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, ELEMENT_SPACING);
        graph.setProperty(
                LayeredOptions.CONSIDER_MODEL_ORDER_STRATEGY, OrderingStrategy.NODES_AND_EDGES);

        Map<String, ElkNode> elkNodes = new LinkedHashMap<>();
        for (LayoutElement element : request.elements()) {
            ElkNode node = ElkGraphUtil.createNode(graph);
            node.setIdentifier(element.id());
            node.setDimensions(element.width(), element.height());
            elkNodes.put(element.id(), node);
        }

        for (LayoutLink link : request.links()) {
            ElkNode source = elkNodes.get(link.sourceId());
            ElkNode target = elkNodes.get(link.targetId());
            if (source == null || target == null) continue;
            ElkEdge edge = ElkGraphUtil.createSimpleEdge(source, target);
            edge.setIdentifier(link.id());
        }

        new RecursiveGraphLayoutEngine().layout(graph, new BasicProgressMonitor());

        Map<String, ElementGeometry> elements = new LinkedHashMap<>();
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, ElkNode> entry : elkNodes.entrySet()) {
            ElkNode node = entry.getValue();
            ElementGeometry geometry =
                    new ElementGeometry(
                            entry.getKey(),
                            node.getX(),
                            node.getY(),
                            node.getWidth(),
                            node.getHeight());
            elements.put(entry.getKey(), geometry);
            minX = Math.min(minX, geometry.x());
            minY = Math.min(minY, geometry.y());
            maxX = Math.max(maxX, geometry.x() + geometry.width());
            maxY = Math.max(maxY, geometry.y() + geometry.height());
        }

        List<EdgeGeometry> edges = new ArrayList<>();
        for (ElkEdge edge : graph.getContainedEdges()) {
            if (edge.getIdentifier() == null || edge.getSections().isEmpty()) continue;
            ElkEdgeSection section = edge.getSections().getFirst();
            List<EdgeGeometry.Point> points = new ArrayList<>();
            points.add(new EdgeGeometry.Point(section.getStartX(), section.getStartY()));
            for (ElkBendPoint bend : section.getBendPoints()) {
                points.add(new EdgeGeometry.Point(bend.getX(), bend.getY()));
            }
            points.add(new EdgeGeometry.Point(section.getEndX(), section.getEndY()));
            edges.add(new EdgeGeometry(edge.getIdentifier(), points));
        }

        BoundsSnapshot bounds =
                new BoundsSnapshot(
                        minX, minY, Math.max(0.0d, maxX - minX), Math.max(0.0d, maxY - minY));
        return new LayoutResult(
                request.requestId(), request.modelRevision(), elements, edges, bounds);
    }

    public static String nodeId(long id) {
        return "linked:" + id;
    }
}
