package com.majortom.algorithms.visualization.impl.visualizer.linked;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.DecorationGeometry;
import com.majortom.algorithms.visualization.render.api.DecorationLayoutResult;
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
          BoundsSnapshot.empty(), emptyRoleDecorations());
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
    return new LayoutResult(request.requestId(), request.modelRevision(), elements, edges, bounds,
        roleDecorations(request, elements));
  }

  private static DecorationLayoutResult roleDecorations(
      LayoutRequest request, Map<String, ElementGeometry> elements) {
    ElementGeometry head = elements.get(request.elements().getFirst().id());
    ElementGeometry tail = elements.get(request.elements().getLast().id());
    if (head == null || tail == null) {
      return emptyRoleDecorations();
    }

    return DecorationLayoutResult.ofElements(List.of(
        new DecorationGeometry(
            LinkedListDecorationIds.HEAD,
            head.x() + LinkedListLayoutMetrics.ROLE_HEAD_OFFSET_X,
            Math.max(2.0d, head.y() + LinkedListLayoutMetrics.ROLE_HEAD_OFFSET_Y),
            0.0d,
            0.0d),
        new DecorationGeometry(
            LinkedListDecorationIds.TAIL,
            tail.x() + tail.width() + LinkedListLayoutMetrics.ROLE_TAIL_OFFSET_X,
            tail.y() + tail.height() + LinkedListLayoutMetrics.ROLE_TAIL_OFFSET_Y,
            0.0d,
            0.0d)));
  }

  private static DecorationLayoutResult emptyRoleDecorations() {
    return DecorationLayoutResult.ofElements(List.of(
        new DecorationGeometry(
            LinkedListDecorationIds.HEAD,
            LinkedListLayoutMetrics.EMPTY_HEAD_X,
            LinkedListLayoutMetrics.EMPTY_HEAD_Y,
            0.0d,
            0.0d),
        new DecorationGeometry(
            LinkedListDecorationIds.TAIL,
            LinkedListLayoutMetrics.EMPTY_TAIL_X,
            LinkedListLayoutMetrics.EMPTY_TAIL_Y,
            0.0d,
            0.0d)));
  }

  public static String nodeId(long id) {
    return LinkedListVisualIds.node(id);
  }
}
