package com.majortom.algorithms.visualization.render.layout;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.alg.layered.options.OrderingStrategy;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

/** JavaFX-neutral ELK layout used by Array and later linear structures. */
public final class LinearLayoutEngine implements LayoutEngine {
  public static final String ID = "linear";
  private static final double PADDING = 24.0d;

  @Override
  public String id() {
    return ID;
  }

  @Override
  public LayoutResult layout(LayoutRequest request) {
    List<LayoutElement> input = request.elements();
    if (input.isEmpty()) {
      return new LayoutResult(request.requestId(), request.modelRevision(), Map.of(), List.of(),
          BoundsSnapshot.empty());
    }

    ElkNode graph = ElkGraphUtil.createGraph();
    graph.setProperty(CoreOptions.ALGORITHM, LayeredOptions.ALGORITHM_ID);
    Direction direction =
        switch (request.metadata().getOrDefault("direction", "RIGHT").toUpperCase()) {
          case "DOWN" -> Direction.DOWN;
          case "LEFT" -> Direction.LEFT;
          case "UP" -> Direction.UP;
          default -> Direction.RIGHT;
        };
    double padding = positiveDouble(request.metadata().get("padding"), PADDING);
    double spacing = nonNegativeDouble(request.metadata().get("spacing"), 0.0d);
    graph.setProperty(CoreOptions.DIRECTION, direction);
    graph.setProperty(CoreOptions.PADDING, new ElkPadding(padding));
    graph.setProperty(CoreOptions.RANDOM_SEED, 1);
    graph.setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, spacing);
    graph.setProperty(
        LayeredOptions.CONSIDER_MODEL_ORDER_STRATEGY, OrderingStrategy.NODES_AND_EDGES);

    Map<String, ElkNode> nodes = new LinkedHashMap<>();
    ElkNode previous = null;
    for (LayoutElement element : input) {
      ElkNode node = ElkGraphUtil.createNode(graph);
      node.setIdentifier(element.id());
      node.setDimensions(element.width(), element.height());
      nodes.put(element.id(), node);
      if (previous != null)
        ElkGraphUtil.createSimpleEdge(previous, node);
      previous = node;
    }

    new RecursiveGraphLayoutEngine().layout(graph, new BasicProgressMonitor());

    Map<String, ElementGeometry> elements = new LinkedHashMap<>();
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    for (Map.Entry<String, ElkNode> entry : nodes.entrySet()) {
      ElkNode node = entry.getValue();
      ElementGeometry geometry = new ElementGeometry(
          entry.getKey(), node.getX(), node.getY(), node.getWidth(), node.getHeight());
      elements.put(entry.getKey(), geometry);
      minX = Math.min(minX, geometry.x());
      minY = Math.min(minY, geometry.y());
      maxX = Math.max(maxX, geometry.x() + geometry.width());
      maxY = Math.max(maxY, geometry.y() + geometry.height());
    }
    BoundsSnapshot bounds =
        new BoundsSnapshot(minX, minY, Math.max(0.0d, maxX - minX), Math.max(0.0d, maxY - minY));
    return new LayoutResult(
        request.requestId(), request.modelRevision(), elements, List.of(), bounds);
  }
  private static double positiveDouble(String raw, double fallback) {
    try {
      double value = raw == null ? fallback : Double.parseDouble(raw);
      return value > 0.0d ? value : fallback;
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }

  private static double nonNegativeDouble(String raw, double fallback) {
    try {
      double value = raw == null ? fallback : Double.parseDouble(raw);
      return value >= 0.0d ? value : fallback;
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }
}
