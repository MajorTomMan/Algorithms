package com.majortom.algorithms.visualization.impl.visualizer.linked;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.layout.LayoutEngine;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic JavaFX-neutral horizontal layout for linked-list structures. */
public final class LinkedListLayout implements LayoutEngine {
  public static final String ID = "linked-list";
  private static final double PADDING = 34.0d;
  private static final double ELEMENT_SPACING = 46.0d;

  @Override
  public String id() {
    return ID;
  }

  @Override
  public LayoutResult layout(LayoutRequest request) {
    if (request.elements().isEmpty()) {
      return new LayoutResult(request.requestId(), request.modelRevision(), Map.of(), List.of(),
          BoundsSnapshot.empty());
    }

    Map<String, ElementGeometry> elements = new LinkedHashMap<>();
    double x = PADDING;
    double maxX = PADDING;
    double maxY = PADDING;
    for (LayoutElement element : request.elements()) {
      ElementGeometry geometry =
          new ElementGeometry(element.id(), x, PADDING, element.width(), element.height());
      elements.put(element.id(), geometry);
      maxX = Math.max(maxX, geometry.x() + geometry.width());
      maxY = Math.max(maxY, geometry.y() + geometry.height());
      x += element.width() + ELEMENT_SPACING;
    }

    List<EdgeGeometry> edges = new ArrayList<>(request.links().size());
    for (LayoutLink link : request.links()) {
      ElementGeometry source = elements.get(link.sourceId());
      ElementGeometry target = elements.get(link.targetId());
      if (source == null || target == null)
        continue;
      edges.add(new EdgeGeometry(link.id(), List.of(
          new EdgeGeometry.Point(source.x() + source.width(), source.y() + source.height() / 2.0d),
          new EdgeGeometry.Point(target.x(), target.y() + target.height() / 2.0d))));
    }

    BoundsSnapshot bounds = new BoundsSnapshot(PADDING, PADDING,
        Math.max(0.0d, maxX - PADDING), Math.max(0.0d, maxY - PADDING));
    return new LayoutResult(request.requestId(), request.modelRevision(), elements, edges, bounds);
  }

  public static String nodeId(long id) {
    return "linked:" + id;
  }
}
