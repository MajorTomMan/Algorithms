package com.majortom.algorithms.visualization.impl.visualizer.linear;

import com.majortom.algorithms.visualization.common.layout.ElementBounds;
import com.majortom.algorithms.visualization.common.layout.LayoutResult;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.alg.layered.options.OrderingStrategy;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Stack/Queue-specific transient ELK adapter. Ordering is supplied by the family, geometry by ELK. */
public final class StackQueueElkLayout {
    private static final double PADDING = 30.0d;
    private static final double ELEMENT_SPACING = 28.0d;
    private static final int RANDOM_SEED = 1;

    public LayoutResult layoutStack(List<ElementSize> elements) {
        return layout(elements, Direction.DOWN);
    }

    public LayoutResult layoutQueue(List<ElementSize> elements) {
        return layout(elements, Direction.RIGHT);
    }

    private LayoutResult layout(List<ElementSize> elements, Direction direction) {
        List<ElementSize> input = List.copyOf(Objects.requireNonNull(elements, "elements"));
        if (input.isEmpty()) {
            return new LayoutResult(Map.of(), Map.of());
        }

        ElkNode graph = ElkGraphUtil.createGraph();
        graph.setProperty(CoreOptions.ALGORITHM, LayeredOptions.ALGORITHM_ID);
        graph.setProperty(CoreOptions.DIRECTION, direction);
        graph.setProperty(CoreOptions.PADDING, new ElkPadding(PADDING));
        graph.setProperty(CoreOptions.RANDOM_SEED, RANDOM_SEED);
        graph.setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, ELEMENT_SPACING);
        graph.setProperty(LayeredOptions.CONSIDER_MODEL_ORDER_STRATEGY, OrderingStrategy.NODES_AND_EDGES);

        Map<String, ElkNode> nodes = new LinkedHashMap<>();
        ElkNode previous = null;
        for (ElementSize element : input) {
            ElkNode node = ElkGraphUtil.createNode(graph);
            node.setIdentifier(element.id());
            node.setDimensions(element.width(), element.height());
            nodes.put(element.id(), node);
            if (previous != null) {
                ElkGraphUtil.createSimpleEdge(previous, node);
            }
            previous = node;
        }

        new RecursiveGraphLayoutEngine().layout(graph, new BasicProgressMonitor());

        Map<String, ElementBounds> bounds = new LinkedHashMap<>();
        nodes.forEach((id, node) -> bounds.put(id,
                new ElementBounds(id, node.getX(), node.getY(), node.getWidth(), node.getHeight())));
        return new LayoutResult(bounds, Map.of());
    }

    public record ElementSize(String id, double width, double height) {
        public ElementSize {
            Objects.requireNonNull(id, "id");
            if (!(width > 0.0d) || !(height > 0.0d)) {
                throw new IllegalArgumentException("element size must be positive: " + id);
            }
        }
    }
}
