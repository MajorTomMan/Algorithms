package com.majortom.algorithms.visualization.impl.visualizer.graph;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded-cost fallback for dense general graphs. Stable grid positions, no pairwise forces,
 * no ELK invocation. Every vertex and edge stays in the semantic graph, and parallel routes
 * can still be resolved by GraphParallelEdgeLayout.
 */
final class GraphDenseLayout {
  private static final double PADDING = 42.0d;
  private static final double GAP = 46.0d;

  LayoutResult layout(LayoutRequest request) {
    List<LayoutElement> sorted = request.elements().stream()
        .sorted(Comparator.comparing(LayoutElement::id)).toList();
    if (sorted.isEmpty()) {
      return new LayoutResult(request.requestId(), request.modelRevision(),
          Map.of(), List.of(), BoundsSnapshot.empty());
    }
    int columns = Math.max(1, (int) Math.ceil(Math.sqrt(sorted.size())));
    double maxWidth = sorted.stream().mapToDouble(LayoutElement::width).max().orElse(56.0d);
    double maxHeight = sorted.stream().mapToDouble(LayoutElement::height).max().orElse(56.0d);
    double strideX = maxWidth + GAP;
    double strideY = maxHeight + GAP;
    Map<String, ElementGeometry> geometry = new LinkedHashMap<>(sorted.size());
    double right = 0.0d, bottom = 0.0d;
    for (int index = 0; index < sorted.size(); index++) {
      LayoutElement node = sorted.get(index);
      int column = index % columns;
      int row = index / columns;
      double x = PADDING + column * strideX + (maxWidth - node.width()) / 2.0d;
      double y = PADDING + row * strideY + (maxHeight - node.height()) / 2.0d;
      geometry.put(node.id(), new ElementGeometry(node.id(), x, y, node.width(), node.height()));
      right = Math.max(right, x + node.width());
      bottom = Math.max(bottom, y + node.height());
    }
    return new LayoutResult(request.requestId(), request.modelRevision(), geometry,
        List.of(), new BoundsSnapshot(0.0d, 0.0d, right + PADDING, bottom + PADDING));
  }
}
