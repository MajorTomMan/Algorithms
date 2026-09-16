package com.majortom.algorithms.visualization.render.layout;

import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;

public interface LayoutEngine {
  String id();
  LayoutResult layout(LayoutRequest request);
}
