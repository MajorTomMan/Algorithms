package com.majortom.algorithms.visualization.impl.visualizer.graph;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic JavaFX-neutral force-directed geometry for general graph topology. */
final class GraphTopologyLayout {
  private static final double PADDING = 42.0d;
  private static final double NODE_SPACING = 42.0d;
  private static final double COMPONENT_SPACING = 84.0d;
  private static final double MIN_EDGE_LENGTH = 104.0d;
  private static final double PACKING_FACTOR = 1.45d;

  LayoutResult layout(LayoutRequest request) {
    Map<String, LayoutElement> nodesById = new LinkedHashMap<>();
    request.elements()
        .stream()
        .sorted(Comparator.comparing(LayoutElement::id))
        .forEach(node -> nodesById.put(node.id(), node));
    List<Component> components = components(nodesById.keySet(), request.links());
    // Assign each edge once instead of scanning every edge for every component.
    // The per-component lists keep input edge order, including parallel edges and loops.
    Map<String, Integer> componentByNode = new HashMap<>();
    List<List<LayoutLink>> componentLinks = new ArrayList<>(components.size());
    for (int index = 0; index < components.size(); index++) {
      componentLinks.add(new ArrayList<>());
      for (String id : components.get(index).nodeIds()) componentByNode.put(id, index);
    }
    for (LayoutLink link : request.links()) {
      Integer sourceComponent = componentByNode.get(link.sourceId());
      if (sourceComponent != null
          && sourceComponent.equals(componentByNode.get(link.targetId()))) {
        componentLinks.get(sourceComponent).add(link);
      }
    }

    List<ComponentLayout> layouts = new ArrayList<>(components.size());
    for (int index = 0; index < components.size(); index++) {
      List<LayoutElement> nodes = components.get(index).nodeIds().stream()
          .map(nodesById::get).toList();
      layouts.add(force(nodes, componentLinks.get(index)));
    }
    return pack(request, layouts);
  }

  private List<Component> components(Set<String> nodeIds, List<LayoutLink> links) {
    Map<String, Set<String>> adjacency = new LinkedHashMap<>();
    nodeIds.stream().sorted().forEach(id -> adjacency.put(id, new LinkedHashSet<>()));
    for (LayoutLink link : links) {
      if (!adjacency.containsKey(link.sourceId()) || !adjacency.containsKey(link.targetId())
          || link.sourceId().equals(link.targetId()))
        continue;
      adjacency.get(link.sourceId()).add(link.targetId());
      adjacency.get(link.targetId()).add(link.sourceId());
    }
    Set<String> visited = new HashSet<>();
    List<Component> result = new ArrayList<>();
    for (String start : adjacency.keySet()) {
      if (!visited.add(start))
        continue;
      ArrayDeque<String> queue = new ArrayDeque<>();
      queue.add(start);
      List<String> ids = new ArrayList<>();
      while (!queue.isEmpty()) {
        String current = queue.removeFirst();
        ids.add(current);
        adjacency.get(current).stream().sorted().forEach(next -> {
          if (visited.add(next))
            queue.addLast(next);
        });
      }
      ids.sort(String::compareTo);
      result.add(new Component(Set.copyOf(ids), ids.getFirst()));
    }
    result.sort(Comparator.comparing(Component::stableKey));
    return result;
  }

  private ComponentLayout force(List<LayoutElement> nodes, List<LayoutLink> links) {
    List<LayoutElement> ordered =
        nodes.stream().sorted(Comparator.comparing(LayoutElement::id)).toList();
    if (ordered.size() == 1) {
      LayoutElement node = ordered.getFirst();
      ElementGeometry geometry =
          new ElementGeometry(node.id(), 0.0d, 0.0d, node.width(), node.height());
      return new ComponentLayout(Map.of(node.id(), geometry), node.width(), node.height());
    }

    // Dimensions are constant across iterations; compute each node's diameter once.
    double[] diameters = new double[ordered.size()];
    double maxDiameter = 0.0d;
    for (int index = 0; index < ordered.size(); index++) {
      diameters[index] = Math.max(ordered.get(index).width(), ordered.get(index).height());
      maxDiameter = Math.max(maxDiameter, diameters[index]);
    }
    double ideal = Math.max(MIN_EDGE_LENGTH, maxDiameter + NODE_SPACING);
    double idealSquared = ideal * ideal;
    double radius = Math.max(ideal, ideal * ordered.size() / (2.0d * Math.PI));
    Map<String, Point> positions = new LinkedHashMap<>();
    for (int index = 0; index < ordered.size(); index++) {
      double angle = -Math.PI / 2.0d + index * (2.0d * Math.PI / ordered.size());
      positions.put(
          ordered.get(index).id(), new Point(Math.cos(angle) * radius, Math.sin(angle) * radius));
    }

    List<LayoutLink> forceLinks = links.stream()
                                      .filter(link
                                          -> !link.sourceId().equals(link.targetId())
                                              && positions.containsKey(link.sourceId())
                                              && positions.containsKey(link.targetId()))
                                      .toList();
    // Reuse displacement vectors instead of allocating a map and N points per iteration.
    Map<String, Point> delta = new LinkedHashMap<>();
    for (LayoutElement node : ordered) delta.put(node.id(), new Point(0.0d, 0.0d));
    int iterations = ordered.size() <= 30 ? 180 : ordered.size() <= 80 ? 130 : 90;
    double temperature = ideal * 0.72d;
    for (int iteration = 0; iteration < iterations; iteration++) {
      for (Point movement : delta.values()) {
        movement.x = 0.0d;
        movement.y = 0.0d;
      }
      for (int leftIndex = 0; leftIndex < ordered.size(); leftIndex++) {
        LayoutElement left = ordered.get(leftIndex);
        Point lp = positions.get(left.id());
        for (int rightIndex = leftIndex + 1; rightIndex < ordered.size(); rightIndex++) {
          LayoutElement right = ordered.get(rightIndex);
          Point rp = positions.get(right.id());
          double dx = lp.x - rp.x;
          double dy = lp.y - rp.y;
          double distance = Math.max(0.01d, Math.hypot(dx, dy));
          double nx = dx / distance;
          double ny = dy / distance;
          double repulsion = idealSquared / distance;
          double minimum = (diameters[leftIndex] + diameters[rightIndex]) / 2.0d + 18.0d;
          if (distance < minimum)
            repulsion += (minimum - distance) * 8.0d;
          delta.get(left.id()).add(nx * repulsion, ny * repulsion);
          delta.get(right.id()).add(-nx * repulsion, -ny * repulsion);
        }
      }
      for (LayoutLink link : forceLinks) {
        Point source = positions.get(link.sourceId());
        Point target = positions.get(link.targetId());
        double dx = source.x - target.x;
        double dy = source.y - target.y;
        double distance = Math.max(0.01d, Math.hypot(dx, dy));
        double nx = dx / distance;
        double ny = dy / distance;
        double attraction = distance * distance / ideal;
        delta.get(link.sourceId()).add(-nx * attraction, -ny * attraction);
        delta.get(link.targetId()).add(nx * attraction, ny * attraction);
      }
      for (LayoutElement node : ordered) {
        Point position = positions.get(node.id());
        Point movement = delta.get(node.id());
        movement.add(-position.x * 0.035d, -position.y * 0.035d);
        double magnitude = Math.hypot(movement.x, movement.y);
        if (magnitude > 0.0d) {
          double step = Math.min(magnitude, temperature);
          position.add(movement.x / magnitude * step, movement.y / magnitude * step);
        }
      }
      temperature = Math.max(0.75d, temperature * 0.955d);
    }
    return bounds(ordered, positions);
  }

  private ComponentLayout bounds(List<LayoutElement> nodes, Map<String, Point> positions) {
    double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
    for (LayoutElement node : nodes) {
      Point p = positions.get(node.id());
      minX = Math.min(minX, p.x - node.width() / 2.0d);
      minY = Math.min(minY, p.y - node.height() / 2.0d);
      maxX = Math.max(maxX, p.x + node.width() / 2.0d);
      maxY = Math.max(maxY, p.y + node.height() / 2.0d);
    }
    Map<String, ElementGeometry> elements = new LinkedHashMap<>();
    for (LayoutElement node : nodes) {
      Point p = positions.get(node.id());
      double x = p.x - node.width() / 2.0d - minX;
      double y = p.y - node.height() / 2.0d - minY;
      elements.put(node.id(), new ElementGeometry(node.id(), x, y, node.width(), node.height()));
    }
    return new ComponentLayout(elements, Math.max(1.0d, maxX - minX), Math.max(1.0d, maxY - minY));
  }

  private LayoutResult pack(LayoutRequest request, List<ComponentLayout> components) {
    if (components.isEmpty()) {
      return new LayoutResult(request.requestId(), request.modelRevision(), Map.of(), List.of(),
          BoundsSnapshot.empty());
    }
    double totalArea = components.stream()
                           .mapToDouble(component
                               -> (component.width() + COMPONENT_SPACING)
                                   * (component.height() + COMPONENT_SPACING))
                           .sum();
    double maxWidth = components.stream().mapToDouble(ComponentLayout::width).max().orElse(1.0d);
    double rowLimit = Math.max(maxWidth, Math.sqrt(totalArea) * PACKING_FACTOR);
    Map<String, ElementGeometry> packed = new LinkedHashMap<>();
    double x = PADDING, y = PADDING, rowHeight = 0.0d;
    for (ComponentLayout component : components) {
      if (x > PADDING && x + component.width() > PADDING + rowLimit) {
        x = PADDING;
        y += rowHeight + COMPONENT_SPACING;
        rowHeight = 0.0d;
      }
      for (ElementGeometry bounds : component.elements().values()) {
        packed.put(bounds.id(),
            new ElementGeometry(
                bounds.id(), bounds.x() + x, bounds.y() + y, bounds.width(), bounds.height()));
      }
      x += component.width() + COMPONENT_SPACING;
      rowHeight = Math.max(rowHeight, component.height());
    }
    BoundsSnapshot bounds = contentBounds(packed);
    return new LayoutResult(
        request.requestId(), request.modelRevision(), packed, List.of(), bounds);
  }

  private static BoundsSnapshot contentBounds(Map<String, ElementGeometry> elements) {
    if (elements.isEmpty())
      return BoundsSnapshot.empty();
    double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
    for (ElementGeometry element : elements.values()) {
      minX = Math.min(minX, element.x());
      minY = Math.min(minY, element.y());
      maxX = Math.max(maxX, element.x() + element.width());
      maxY = Math.max(maxY, element.y() + element.height());
    }
    return new BoundsSnapshot(minX, minY, maxX - minX, maxY - minY);
  }

  private record Component(Set<String> nodeIds, String stableKey) {}
  private record
      ComponentLayout(Map<String, ElementGeometry> elements, double width, double height) {}

  private static final class Point {
    private double x;
    private double y;
    private Point(double x, double y) {
      this.x = x;
      this.y = y;
    }
    private void add(double dx, double dy) {
      x += dx;
      y += dy;
    }
  }
}
