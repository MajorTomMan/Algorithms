package com.majortom.algorithms.visualization.render.layout;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps requested element extents in deterministic world space without JavaFX measurement or ELK.
 */
public final class FixedLayoutEngine implements LayoutEngine {
  public static final String ID = "fixed";

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
    double y = 0.0d;
    double maxWidth = 0.0d;
    for (LayoutElement element : request.elements()) {
      elements.put(element.id(),
          new ElementGeometry(element.id(), 0.0d, y, element.width(), element.height()));
      y += element.height();
      maxWidth = Math.max(maxWidth, element.width());
    }
    return new LayoutResult(request.requestId(), request.modelRevision(), elements, List.of(),
        new BoundsSnapshot(0.0d, 0.0d, maxWidth, y));
  }
}
