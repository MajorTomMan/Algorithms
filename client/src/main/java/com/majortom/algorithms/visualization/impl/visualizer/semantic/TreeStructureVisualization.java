package com.majortom.algorithms.visualization.impl.visualizer.semantic;

import com.majortom.algorithms.visualization.impl.visualizer.tree.TreeElkLayout;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.fx.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.layout.DetachedMetrics;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JavaFX-neutral tree layout semantics. */
public final class TreeStructureVisualization implements StructureVisualization<TreeViewState> {
    private static final double MIN_RADIUS = 24.0d;
    private static final double LABEL_PADDING = 18.0d;

    @Override
    public LayoutRequest captureLayout(TreeViewState state, RenderCaptureContext context) {
        List<Long> order = orderedNodeIds(state);
        List<LayoutElement> nodes = new ArrayList<>(order.size());
        for (Long id : order) {
            TreeViewState.Node node = state.nodes().get(id);
            if (node == null) continue;
            double diameter = Math.max(MIN_RADIUS * 2.0d,
                    DetachedMetrics.boxWidth(node.value().text(), context.contentStyle(),
                            MIN_RADIUS * 2.0d, LABEL_PADDING));
            nodes.add(new LayoutElement(TreeElkLayout.nodeId(id), quantize(diameter), quantize(diameter)));
        }

        List<LayoutLink> links = new ArrayList<>();
        for (Long id : order) {
            TreeViewState.Node node = state.nodes().get(id);
            if (node == null) continue;
            if (state.kind() == TreeViewState.Kind.GENERAL) {
                for (int index = 0; index < node.childIds().size(); index++) {
                    Long targetId = node.childIds().get(index);
                    if (targetId == null || !state.nodes().containsKey(targetId)) continue;
                    links.add(new LayoutLink(routeId("child", index, node.id(), targetId),
                            TreeElkLayout.nodeId(node.id()), TreeElkLayout.nodeId(targetId),
                            "CHILD", index));
                }
            } else {
                addLayoutLink(links, state, node.id(), node.leftId(), "left", "LEFT", 0);
                addLayoutLink(links, state, node.id(), node.rightId(), "right", "RIGHT", 1);
            }
        }
        return new LayoutRequest(context.requestId(), context.sessionId(), context.modelRevision(),
                context.geometryRevision(), TreeElkLayout.ID, nodes, links,
                Map.of("kind", state.kind().name()));
    }

    private static void addLayoutLink(List<LayoutLink> links, TreeViewState state, long sourceId,
            Long targetId, String routeRelation, String relation, int index) {
        if (targetId == null || !state.nodes().containsKey(targetId)) return;
        links.add(new LayoutLink(routeId(routeRelation, index, sourceId, targetId),
                TreeElkLayout.nodeId(sourceId), TreeElkLayout.nodeId(targetId), relation, index));
    }

    private static List<Long> orderedNodeIds(TreeViewState state) {
        List<Long> order = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        if (state.rootId() != null) visit(state.rootId(), state, visited, order);
        state.nodes().keySet().stream().sorted(Comparator.naturalOrder())
                .forEach(id -> visit(id, state, visited, order));
        return order;
    }

    private static void visit(long id, TreeViewState state, Set<Long> visited, List<Long> order) {
        if (!state.nodes().containsKey(id) || !visited.add(id)) return;
        order.add(id);
        TreeViewState.Node node = state.nodes().get(id);
        if (state.kind() == TreeViewState.Kind.GENERAL) {
            for (Long childId : node.childIds()) if (childId != null) visit(childId, state, visited, order);
        } else {
            if (node.leftId() != null) visit(node.leftId(), state, visited, order);
            if (node.rightId() != null) visit(node.rightId(), state, visited, order);
        }
    }

    private static String routeId(String relation, int index, long sourceId, long targetId) {
        return "tree:" + relation + ":" + index + ":" + sourceId + ":" + targetId;
    }

    private static double quantize(double value) { return Math.rint(value * 100.0d) / 100.0d; }
}
