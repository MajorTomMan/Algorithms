package com.majortom.algorithms.visualization.render.layout;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutMetadataKeys;
import com.majortom.algorithms.visualization.render.api.LinearLayoutDirection;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic JavaFX-neutral layout for strictly linear structures. */
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

    LinearLayoutDirection direction = LinearLayoutDirection.parse(request.metadata().get(LayoutMetadataKeys.DIRECTION));
    double padding = positiveDouble(request.metadata().get(LayoutMetadataKeys.PADDING), PADDING);
    double spacing = nonNegativeDouble(request.metadata().get(LayoutMetadataKeys.SPACING), 0.0d);
    double primaryExtent = primaryExtent(input, direction.horizontal(), spacing);

    Map<String, ElementGeometry> elements = new LinkedHashMap<>();
    double cursor = 0.0d;
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    for (LayoutElement element : input) {
      double x;
      double y;
      if (direction.horizontal()) {
        x = direction.reverse()
            ? padding + primaryExtent - cursor - element.width()
            : padding + cursor;
        y = padding;
        cursor += element.width() + spacing;
      } else {
        x = padding;
        y = direction.reverse()
            ? padding + primaryExtent - cursor - element.height()
            : padding + cursor;
        cursor += element.height() + spacing;
      }

      ElementGeometry geometry =
          new ElementGeometry(element.id(), x, y, element.width(), element.height());
      elements.put(element.id(), geometry);
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

  private static double primaryExtent(
      List<LayoutElement> elements, boolean horizontal, double spacing) {
    double extent = spacing * Math.max(0, elements.size() - 1);
    for (LayoutElement element : elements) {
      extent += horizontal ? element.width() : element.height();
    }
    return extent;
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
