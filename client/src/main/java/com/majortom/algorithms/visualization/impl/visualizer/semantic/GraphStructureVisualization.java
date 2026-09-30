package com.majortom.algorithms.visualization.impl.visualizer.semantic;

import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphDecorationIds;
import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphElkLayout;
import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphLayoutMetrics;
import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphVisualText;
import com.majortom.algorithms.visualization.render.api.DecorationInput;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutMetadataKeys;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
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

    @Override
    public LayoutRequest captureLayout(GraphViewState state, RenderCaptureContext context) {
        List<GraphViewState.Node> orderedNodes = state.nodes().stream()
                .sorted(Comparator.comparingLong(GraphViewState.Node::id)).toList();
        List<LayoutElement> nodes = new ArrayList<>(orderedNodes.size());
        List<DecorationInput> decorations = new ArrayList<>();

        for (GraphViewState.Node node : orderedNodes) {
            String nodeId = GraphElkLayout.nodeId(node.id());
            double minimumDiameter = GraphLayoutMetrics.MIN_NODE_RADIUS * 2.0d;
            double diameter = Math.max(minimumDiameter,
                    DetachedMetrics.boxWidth(node.value().text(), context.contentStyle(),
                            minimumDiameter, GraphLayoutMetrics.NODE_LABEL_PADDING));
            nodes.add(new LayoutElement(nodeId, quantize(diameter), quantize(diameter)));

            String nodeIdText = "#" + node.id();
            decorations.add(new DecorationInput(
                    GraphDecorationIds.nodeId(nodeId),
                    nodeId,
                    DecorationInput.Kind.NODE_BELOW,
                    quantize(DetachedMetrics.textWidth(nodeIdText, context.contentStyle())
                            + GraphLayoutMetrics.DECORATION_HORIZONTAL_PADDING),
                    quantize(DetachedMetrics.textHeight(context.contentStyle())
                            + GraphLayoutMetrics.DECORATION_VERTICAL_PADDING)));
        }

        Set<String> available = nodes.stream().map(LayoutElement::id).collect(Collectors.toSet());
        List<GraphViewState.Edge> orderedEdges = state.edges().stream()
                .filter(edge -> available.contains(GraphElkLayout.nodeId(edge.fromId()))
                        && available.contains(GraphElkLayout.nodeId(edge.toId())))
                .sorted(Comparator.comparingLong(GraphViewState.Edge::id))
                .toList();

        List<LayoutLink> links = new ArrayList<>(orderedEdges.size());
        for (GraphViewState.Edge edge : orderedEdges) {
            String edgeId = GraphElkLayout.edgeId(edge.id());
            links.add(new LayoutLink(edgeId,
                    GraphElkLayout.nodeId(edge.fromId()),
                    GraphElkLayout.nodeId(edge.toId())));

            String weight = GraphVisualText.weight(edge.weight());
            if (weight != null) {
                decorations.add(new DecorationInput(
                        GraphDecorationIds.edgeLabel(edgeId),
                        edgeId,
                        DecorationInput.Kind.EDGE_LABEL,
                        quantize(DetachedMetrics.textWidth(weight, context.contentStyle())
                                + GraphLayoutMetrics.DECORATION_HORIZONTAL_PADDING),
                        quantize(DetachedMetrics.textHeight(context.contentStyle())
                                + GraphLayoutMetrics.DECORATION_VERTICAL_PADDING)));
            }
        }

        return new LayoutRequest(context.requestId(), context.sessionId(), context.modelRevision(),
                context.geometryRevision(), GraphElkLayout.ID, nodes, links,
                Map.of(LayoutMetadataKeys.DIRECTED, Boolean.toString(state.directed())),
                decorations);
    }

    private static double quantize(double value) {
        return Math.rint(value * 100.0d) / 100.0d;
    }
}
