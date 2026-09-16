package com.majortom.algorithms.visualization.render.api;

import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.render.viewport.ViewportSnapshot;
import java.util.Objects;

public record ViewportRenderIntent(RenderSessionId sessionId, ViewportSnapshot viewport,
    CameraPolicy cameraPolicy) implements RenderIntent {
  public ViewportRenderIntent {
    Objects.requireNonNull(sessionId, "sessionId");
    Objects.requireNonNull(viewport, "viewport");
    Objects.requireNonNull(cameraPolicy, "cameraPolicy");
  }
  @Override
  public RenderIntentKind kind() {
    return RenderIntentKind.VIEWPORT;
  }
}
