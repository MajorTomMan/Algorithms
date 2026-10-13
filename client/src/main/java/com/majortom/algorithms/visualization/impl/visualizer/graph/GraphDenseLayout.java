package com.majortom.algorithms.visualization.impl.visualizer.graph;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic near-linear layout of very large graphs.
 *
 * Uses a union-find pass over the already-declared links, packs disconnected components
 * independently, then lays out each component on a compact grid. It never performs
 * pairwise force-repulsion or deletes topology. Small graphs still use their original layout.
 */
final class GraphDenseLayout {
  private static final double PADDING = 42.0d;
  private static final double GAP = 46.0d;
  private static final double COMPONENT_SPACING = 84.0d;

  LayoutResult layout(LayoutRequest request) {
    List<LayoutElement> sorted = request.elements().stream()
        .sorted(Comparator.comparing(LayoutElement::id)).toList();
    if (sorted.isEmpty()) {
      return new LayoutResult(request.requestId(), request.modelRevision(),
          Map.of(), List.of(), BoundsSnapshot.empty());
    }

    int count = sorted.size();
    Map<String, Integer> indices = new LinkedHashMap<>(count);
    for (int index = 0; index < count; index++) indices.put(sorted.get(index).id(), index);
    DisjointSets sets = new DisjointSets(count);
    for (LayoutLink edge : request.links()) {
      Integer from = indices.get(edge.sourceId());
      Integer to = indices.get(edge.targetId());
      if (from != null && to != null) sets.join(from, to);
    }

    Map<Integer, List<LayoutElement>> components = new LinkedHashMap<>();
    for (int index = 0; index < count; index++) {
      components.computeIfAbsent(sets.find(index), ignored -> new ArrayList<>())
          .add(sorted.get(index));
    }
    double maxWidth = sorted.stream().mapToDouble(LayoutElement::width).max().orElse(56.0d);
    double maxHeight = sorted.stream().mapToDouble(LayoutElement::height).max().orElse(56.0d);
    double strideX = maxWidth + GAP;
    double strideY = maxHeight + GAP;

    List<Component> packed = new ArrayList<>(components.size());
    double totalArea = 0.0d;
    double widest = 0.0d;
    for (List<LayoutElement> nodes : components.values()) {
      int cols = Math.max(1, (int) Math.ceil(Math.sqrt(nodes.size())));
      int rows = (nodes.size() + cols - 1) / cols;
      double width = cols * strideX - GAP;
      double height = rows * strideY - GAP;
      packed.add(new Component(nodes, cols, width, height));
      totalArea += (width + COMPONENT_SPACING) * (height + COMPONENT_SPACING);
      widest = Math.max(widest, width);
    }
    double rowLimit = Math.max(widest, Math.sqrt(totalArea) * 1.45d);
    Map<String, ElementGeometry> result = new LinkedHashMap<>(count);
    double x = PADDING, y = PADDING, rowHeight = 0.0d;
    double right = 0.0d, bottom = 0.0d;
    for (Component component : packed) {
      if (x > PADDING && x + component.width() > PADDING + rowLimit) {
        x = PADDING;
        y += rowHeight + COMPONENT_SPACING;
        rowHeight = 0.0d;
      }
      for (int index = 0; index < component.nodes().size(); index++) {
        LayoutElement node = component.nodes().get(index);
        int column = index % component.columns();
        int row = index / component.columns();
        double nx = x + column * strideX + (maxWidth - node.width()) / 2.0d;
        double ny = y + row * strideY + (maxHeight - node.height()) / 2.0d;
        result.put(node.id(), new ElementGeometry(
            node.id(), nx, ny, node.width(), node.height()));
        right = Math.max(right, nx + node.width());
        bottom = Math.max(bottom, ny + node.height());
      }
      x += component.width() + COMPONENT_SPACING;
      rowHeight = Math.max(rowHeight, component.height());
    }
    return new LayoutResult(request.requestId(), request.modelRevision(), result,
        List.of(), new BoundsSnapshot(0.0d, 0.0d, right + PADDING, bottom + PADDING));
  }

  private record Component(List<LayoutElement> nodes, int columns, double width, double height) {}

  private static final class DisjointSets {
    private final int[] parent;
    private final int[] size;
    DisjointSets(int count) {
      parent = new int[count];
      size = new int[count];
      for (int i = 0; i < count; i++) { parent[i] = i; size[i] = 1; }
    }
    int find(int index) {
      int root = index;
      while (root != parent[root]) root = parent[root];
      while (index != root) {
        int next = parent[index];
        parent[index] = root;
        index = next;
      }
      return root;
    }
    void join(int first, int second) {
      int a = find(first), b = find(second);
      if (a == b) return;
      if (size[a] < size[b]) { int temp = a; a = b; b = temp; }
      parent[b] = a;
      size[a] += size[b];
    }
  }
}
