package com.majortom.algorithms.visualization.impl.visualizer.semantic;

import com.majortom.algorithms.visualization.render.api.LayoutMetadataKeys;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.domain.relation.LinkedRelationTypes;
import com.majortom.algorithms.visualization.impl.visualizer.linked.LinkedListLayout;
import com.majortom.algorithms.visualization.impl.visualizer.linked.LinkedListVisualIds;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.layout.DetachedMetrics;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JavaFX-neutral linked-list layout semantics. */
public final class LinkedListStructureVisualization implements StructureVisualization<LinkedListViewState> {
    private static final double MIN_NODE_WIDTH = 112.0d;
    private static final double MIN_NODE_HEIGHT = 72.0d;
    private static final double LABEL_HORIZONTAL_PADDING = 40.0d;

    @Override
    public LayoutRequest captureLayout(LinkedListViewState state, RenderCaptureContext context) {
        List<Long> order = orderedNodeIds(state);
        List<LayoutElement> elements = new ArrayList<>(order.size());
        for (Long id : order) {
            LinkedListViewState.Node node = state.nodes().get(id);
            if (node == null) continue;
            double width = DetachedMetrics.boxWidth(node.value().text(), context.contentStyle(),
                    MIN_NODE_WIDTH, LABEL_HORIZONTAL_PADDING);
            elements.add(new LayoutElement(LinkedListLayout.nodeId(id), width,
                    Math.max(MIN_NODE_HEIGHT, 54.0d + context.contentStyle().fontSize())));
        }

        List<LayoutLink> links = new ArrayList<>();
        for (Long id : order) {
            LinkedListViewState.Node node = state.nodes().get(id);
            if (node != null && node.nextId() != null && state.nodes().containsKey(node.nextId())) {
                links.add(new LayoutLink(LinkedListVisualIds.nextEdge(node.id(), node.nextId()),
                        LinkedListLayout.nodeId(node.id()), LinkedListLayout.nodeId(node.nextId()),
                        LinkedRelationTypes.NEXT, links.size()));
            }
        }
        return new LayoutRequest(context.requestId(), context.sessionId(), context.modelRevision(),
                context.geometryRevision(), LinkedListLayout.ID, elements, links,
                Map.of(LayoutMetadataKeys.STRUCTURE, StructureIds.LINKED_LIST));
    }

    private static List<Long> orderedNodeIds(LinkedListViewState state) {
        List<Long> order = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        List<LinkedListViewState.Node> roots = state.nodes().values().stream()
                .filter(node -> node.previousId() == null)
                .sorted(Comparator.comparingLong(LinkedListViewState.Node::id)).toList();
        for (LinkedListViewState.Node root : roots) followNext(root.id(), state, visited, order);
        state.nodes().keySet().stream().sorted().forEach(id -> followNext(id, state, visited, order));
        return order;
    }

    private static void followNext(long startId, LinkedListViewState state,
            Set<Long> visited, List<Long> order) {
        Long currentId = startId;
        while (currentId != null && state.nodes().containsKey(currentId) && visited.add(currentId)) {
            order.add(currentId);
            currentId = state.nodes().get(currentId).nextId();
        }
    }


}
