package com.majortom.algorithms.visualization.impl.visualizer.graph;

import com.majortom.algorithms.visualization.render.api.DecorationGeometry;
import com.majortom.algorithms.visualization.render.api.DecorationInput;
import com.majortom.algorithms.visualization.render.api.DecorationLayoutResult;
import com.majortom.algorithms.visualization.render.api.EdgeDecorationGeometry;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JavaFX-neutral placement pass for graph decorations that depend on resolved topology geometry.
 *
 * <p>The graph engine owns topology. This pass places node-id reservations and chooses stable
 * normal offsets for edge labels while avoiding factual node bounds and already placed labels.</p>
 */
final class GraphLabelLayout {

  LayoutResult resolve(LayoutRequest request, LayoutResult base) {
    if (request.decorations().isEmpty()) {
      return base;
    }

    List<Rect> occupied = new ArrayList<>();
    for (ElementGeometry element : base.elements().values()) {
      occupied.add(expanded(rect(element), GraphLayoutMetrics.EDGE_LABEL_COLLISION_PADDING));
    }

    Map<String, DecorationGeometry> elementDecorations =
        new LinkedHashMap<>(base.decorations().elements());
    Map<String, EdgeDecorationGeometry> edgeDecorations =
        new LinkedHashMap<>(base.decorations().edges());

    placeNodeDecorations(request, base, occupied, elementDecorations);

    Map<String, EdgeGeometry> routes = new LinkedHashMap<>();
    for (EdgeGeometry edge : base.edges()) {
      routes.put(edge.id(), edge);
    }

    Map<String, DecorationInput> edgeLabels = new LinkedHashMap<>();
    for (DecorationInput input : request.decorations()) {
      if (input.kind() == DecorationInput.Kind.EDGE_LABEL) {
        edgeLabels.put(input.anchorId(), input);
      }
    }

    for (LayoutLink link : request.links()) {
      DecorationInput input = edgeLabels.get(link.id());
      if (input == null) {
        continue;
      }

      Anchor anchor = labelAnchor(link, routes.get(link.id()), base.elements());
      if (anchor == null || anchor.tangentLength() == 0.0d) {
        continue;
      }

      double[] candidates = candidates(link.id());
      double bestOffset = candidates[0];
      double bestScore = Double.POSITIVE_INFINITY;
      Rect bestBounds = null;
      for (double candidate : candidates) {
        Rect bounds = expanded(labelBounds(anchor, candidate, input.width(), input.height()),
            GraphLayoutMetrics.EDGE_LABEL_COLLISION_PADDING);
        double score = overlapScore(bounds, occupied);
        if (score < bestScore) {
          bestScore = score;
          bestOffset = candidate;
          bestBounds = bounds;
        }
        if (score == 0.0d) {
          break;
        }
      }

      edgeDecorations.put(input.id(),
          new EdgeDecorationGeometry(input.id(), link.id(), bestOffset));
      if (bestBounds != null) {
        occupied.add(bestBounds);
      }
    }

    DecorationLayoutResult decorations =
        new DecorationLayoutResult(elementDecorations, edgeDecorations);
    if (decorations.equals(base.decorations())) {
      return base;
    }
    return new LayoutResult(base.requestId(), base.modelRevision(), base.elements(), base.edges(),
        base.bounds(), decorations);
  }

  private void placeNodeDecorations(
      LayoutRequest request,
      LayoutResult base,
      List<Rect> occupied,
      Map<String, DecorationGeometry> output) {
    boolean reserveForCollisions = request.elements().size() <= 12;
    for (DecorationInput input : request.decorations()) {
      if (input.kind() != DecorationInput.Kind.NODE_BELOW) {
        continue;
      }
      ElementGeometry element = base.elements().get(input.anchorId());
      if (element == null) {
        continue;
      }

      double x = element.x() + (element.width() - input.width()) / 2.0d;
      double y = element.y() + element.height() + GraphLayoutMetrics.NODE_ID_GAP;
      DecorationGeometry geometry =
          new DecorationGeometry(input.id(), x, y, input.width(), input.height());
      output.put(input.id(), geometry);
      if (reserveForCollisions) {
        occupied.add(expanded(new Rect(x, y, input.width(), input.height()),
            GraphLayoutMetrics.EDGE_LABEL_COLLISION_PADDING));
      }
    }
  }

  private Anchor labelAnchor(
      LayoutLink link, EdgeGeometry route, Map<String, ElementGeometry> elements) {
    if (link.sourceId().equals(link.targetId())) {
      return null;
    }

    List<Point> points = new ArrayList<>();
    if (route != null && route.points().size() >= 2) {
      for (EdgeGeometry.Point point : route.points()) {
        points.add(new Point(point.x(), point.y()));
      }
    } else {
      ElementGeometry source = elements.get(link.sourceId());
      ElementGeometry target = elements.get(link.targetId());
      if (source == null || target == null) {
        return null;
      }
      points.add(center(source));
      points.add(center(target));
    }
    return polylineMidpoint(points);
  }

  private static Rect labelBounds(Anchor anchor, double offset, double width, double height) {
    double tangentLength = anchor.tangentLength();
    double nx = -anchor.dy() / tangentLength;
    double ny = anchor.dx() / tangentLength;
    double cx = anchor.x() + nx * offset;
    double cy = anchor.y() + ny * offset;
    return new Rect(cx - width / 2.0d, cy - height / 2.0d, width, height);
  }

  private static Anchor polylineMidpoint(List<Point> points) {
    double total = 0.0d;
    for (int index = 1; index < points.size(); index++) {
      total += distance(points.get(index - 1), points.get(index));
    }
    if (total == 0.0d) {
      return null;
    }

    double remaining = total / 2.0d;
    for (int index = 1; index < points.size(); index++) {
      Point start = points.get(index - 1);
      Point end = points.get(index);
      double segment = distance(start, end);
      if (remaining <= segment || index == points.size() - 1) {
        double fraction = segment == 0.0d ? 0.5d
            : Math.max(0.0d, Math.min(1.0d, remaining / segment));
        double x = start.x() + (end.x() - start.x()) * fraction;
        double y = start.y() + (end.y() - start.y()) * fraction;
        return new Anchor(x, y, end.x() - start.x(), end.y() - start.y());
      }
      remaining -= segment;
    }
    return null;
  }

  private static double[] candidates(String edgeId) {
    double first = preferredOffset(edgeId);
    double opposite = -first;
    double secondMagnitude =
        GraphLayoutMetrics.EDGE_LABEL_OFFSET + GraphLayoutMetrics.EDGE_LABEL_OFFSET_STEP;
    double thirdMagnitude = secondMagnitude + GraphLayoutMetrics.EDGE_LABEL_OFFSET_STEP;
    double sign = first < 0.0d ? -1.0d : 1.0d;
    return new double[] {
        first,
        opposite,
        sign * secondMagnitude,
        -sign * secondMagnitude,
        sign * thirdMagnitude,
        -sign * thirdMagnitude
    };
  }

  private static double preferredOffset(String edgeId) {
    int separator = edgeId.lastIndexOf(':');
    if (separator >= 0 && separator + 1 < edgeId.length()) {
      try {
        long id = Long.parseLong(edgeId.substring(separator + 1));
        return (id & 1L) == 0L
            ? GraphLayoutMetrics.EDGE_LABEL_OFFSET
            : -GraphLayoutMetrics.EDGE_LABEL_OFFSET;
      } catch (NumberFormatException ignored) {
        // Fall back to a stable string-based side for non-numeric visual ids.
      }
    }
    return (edgeId.hashCode() & 1) == 0
        ? GraphLayoutMetrics.EDGE_LABEL_OFFSET
        : -GraphLayoutMetrics.EDGE_LABEL_OFFSET;
  }

  private static Rect rect(ElementGeometry element) {
    return new Rect(element.x(), element.y(), element.width(), element.height());
  }

  private static Point center(ElementGeometry element) {
    return new Point(
        element.x() + element.width() / 2.0d,
        element.y() + element.height() / 2.0d);
  }

  private static Rect expanded(Rect bounds, double padding) {
    return new Rect(
        bounds.x() - padding,
        bounds.y() - padding,
        bounds.width() + padding * 2.0d,
        bounds.height() + padding * 2.0d);
  }

  private static double overlapScore(Rect candidate, List<Rect> occupied) {
    double score = 0.0d;
    for (Rect other : occupied) {
      double width = Math.min(candidate.maxX(), other.maxX())
          - Math.max(candidate.x(), other.x());
      double height = Math.min(candidate.maxY(), other.maxY())
          - Math.max(candidate.y(), other.y());
      if (width > 0.0d && height > 0.0d) {
        score += width * height;
      }
    }
    return score;
  }

  private static double distance(Point left, Point right) {
    return Math.hypot(right.x() - left.x(), right.y() - left.y());
  }

  private record Point(double x, double y) {}

  private record Anchor(double x, double y, double dx, double dy) {
    double tangentLength() {
      return Math.hypot(dx, dy);
    }
  }

  private record Rect(double x, double y, double width, double height) {
    double maxX() {
      return x + width;
    }

    double maxY() {
      return y + height;
    }
  }
}
