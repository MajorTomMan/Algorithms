package com.majortom.algorithms.visualization.impl.visualizer.graph;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preserves distinct visual routes for parallel and opposing directed edges.
 * Single-edge graphs retain their original routing unchanged.
 */
final class GraphParallelEdgeLayout {
  private static final double EDGE_SPACING = 32.0d;
  private record Pair(String first, String second) {
    static Pair of(String a, String b) {
      return a.compareTo(b) <= 0 ? new Pair(a, b) : new Pair(b, a);
    }
  }

  LayoutResult spread(LayoutRequest request, LayoutResult layout) {
    Map<Pair, List<LayoutLink>> groups = new LinkedHashMap<>();
    for (LayoutLink link : request.links()) {
      if (link.sourceId().equals(link.targetId())) continue;
      groups.computeIfAbsent(Pair.of(link.sourceId(), link.targetId()),
          ignored -> new ArrayList<>()).add(link);
    }
    if (groups.values().stream().noneMatch(group -> group.size() > 1))
      return layout;

    Map<String, EdgeGeometry> routes = new LinkedHashMap<>();
    for (EdgeGeometry edge : layout.edges()) routes.put(edge.id(), edge);
    boolean changed = false;
    for (Map.Entry<Pair, List<LayoutLink>> entry : groups.entrySet()) {
      List<LayoutLink> parallel = entry.getValue();
      if (parallel.size() <= 1) continue;
      ElementGeometry first = layout.elements().get(entry.getKey().first());
      ElementGeometry second = layout.elements().get(entry.getKey().second());
      if (first == null || second == null) continue;
      double ax = first.x() + first.width() / 2.0d;
      double ay = first.y() + first.height() / 2.0d;
      double bx = second.x() + second.width() / 2.0d;
      double by = second.y() + second.height() / 2.0d;
      double length = Math.hypot(bx - ax, by - ay);
      if (length < 0.0001d) continue;
      double nx = -(by - ay) / length;
      double ny = (bx - ax) / length;
      List<LayoutLink> sorted = parallel.stream()
          .sorted(Comparator.comparing(LayoutLink::id)).toList();
      for (int index = 0; index < sorted.size(); index++) {
        LayoutLink edge = sorted.get(index);
        // The perpendicular is computed using the canonical pair orientation, so
        // opposite-direction edges never collapse onto the same centerline.
        double offset = (index - (sorted.size() - 1.0d) / 2.0d) * EDGE_SPACING;
        ElementGeometry source = layout.elements().get(edge.sourceId());
        ElementGeometry target = layout.elements().get(edge.targetId());
        double sx = source.x() + source.width() / 2.0d;
        double sy = source.y() + source.height() / 2.0d;
        double tx = target.x() + target.width() / 2.0d;
        double ty = target.y() + target.height() / 2.0d;
        double dx = (tx - sx) / length;
        double dy = (ty - sy) / length;
        double sourceRadius = Math.min(source.width(), source.height()) / 2.0d;
        double targetRadius = Math.min(target.width(), target.height()) / 2.0d;
        var points = List.of(
            new EdgeGeometry.Point(sx + dx * sourceRadius, sy + dy * sourceRadius),
            new EdgeGeometry.Point((sx + tx) / 2.0d + nx * offset,
                                   (sy + ty) / 2.0d + ny * offset),
            new EdgeGeometry.Point(tx - dx * targetRadius, ty - dy * targetRadius));
        routes.put(edge.id(), new EdgeGeometry(edge.id(), points));
        changed = true;
      }
    }
    if (!changed) return layout;
    // Preserve original route order, then append routes absent from the base layout.
    List<EdgeGeometry> output = new ArrayList<>();
    for (LayoutLink link : request.links()) {
      EdgeGeometry route = routes.remove(link.id());
      if (route != null) output.add(route);
    }
    output.addAll(routes.values());

    BoundsSnapshot previous = layout.bounds();
    double minX = previous.minX(), minY = previous.minY();
    double maxX = previous.maxX(), maxY = previous.maxY();
    for (EdgeGeometry route : output)
      for (EdgeGeometry.Point point : route.points()) {
        minX = Math.min(minX, point.x());
        minY = Math.min(minY, point.y());
        maxX = Math.max(maxX, point.x());
        maxY = Math.max(maxY, point.y());
      }
    return new LayoutResult(layout.requestId(), layout.modelRevision(), layout.elements(),
        output, new BoundsSnapshot(minX, minY, maxX - minX, maxY - minY), layout.decorations());
  }
}
