package com.majortom.algorithms.visualization.impl.visualizer.semantic;

import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphElkLayout;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.layout.DetachedMetrics;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** JavaFX-neutral graph layout semantics. */
public final class GraphStructureVisualization implements StructureVisualization<GraphViewState> {
    private static final double MIN_RADIUS = 28.0d;
    private static final double LABEL_PADDING = 24.0d;

    @Override
    public LayoutRequest captureLayout(GraphViewState state, RenderCaptureContext context) {
        List<GraphViewState.Node> orderedNodes = state.nodes().stream()
                .sorted(Comparator.comparingLong(GraphViewState.Node::id)).toList();
        List<LayoutElement> nodes = new ArrayList<>(orderedNodes.size());
        for (GraphViewState.Node node : orderedNodes) {
            double diameter = Math.max(MIN_RADIUS * 2.0d,
                    DetachedMetrics.boxWidth(node.value().text(), context.contentStyle(),
                            MIN_RADIUS * 2.0d, LABEL_PADDING));
            nodes.add(new LayoutElement(GraphElkLayout.nodeId(node.id()),
                    quantize(diameter), quantize(diameter)));
        }
        Set<String> available = nodes.stream().map(LayoutElement::id).collect(Collectors.toSet());
        List<LayoutLink> links = state.edges().stream()
                .filter(edge -> available.contains(GraphElkLayout.nodeId(edge.fromId()))
                        && available.contains(GraphElkLayout.nodeId(edge.toId())))
                .sorted(Comparator.comparingLong(GraphViewState.Edge::id))
                .map(edge -> new LayoutLink(GraphElkLayout.edgeId(edge.id()),
                        GraphElkLayout.nodeId(edge.fromId()), GraphElkLayout.nodeId(edge.toId())))
                .toList();
        return new LayoutRequest(context.requestId(), context.sessionId(), context.modelRevision(),
                context.geometryRevision(), GraphElkLayout.ID, nodes, links,
                Map.of("directed", Boolean.toString(state.directed())));
    }

    private static double quantize(double value) { return Math.rint(value * 100.0d) / 100.0d; }
}
