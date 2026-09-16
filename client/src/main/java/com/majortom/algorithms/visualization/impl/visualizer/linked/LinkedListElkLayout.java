package com.majortom.algorithms.visualization.impl.visualizer.linked;

import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.layout.LayoutEngine;

/** Compatibility adapter for the retired ELK-backed linked-list layout. */
public final class LinkedListElkLayout implements LayoutEngine {
  public static final String ID = LinkedListLayout.ID;
  private final LinkedListLayout delegate = new LinkedListLayout();

  @Override
  public String id() {
    return ID;
  }

  @Override
  public LayoutResult layout(LayoutRequest request) {
    return delegate.layout(request);
  }

  public static String nodeId(long id) {
    return LinkedListLayout.nodeId(id);
  }
}
