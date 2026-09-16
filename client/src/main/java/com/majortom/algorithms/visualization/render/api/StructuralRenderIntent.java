package com.majortom.algorithms.visualization.render.api;

import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import java.util.Objects;

public record StructuralRenderIntent<S>(RenderSessionId sessionId, S snapshot,
    CameraPolicy cameraPolicy, boolean initialFrame, StructuralChange change)
    implements RenderIntent {
  public StructuralRenderIntent(
      RenderSessionId sessionId, S snapshot, CameraPolicy cameraPolicy, boolean initialFrame) {
    this(sessionId, snapshot, cameraPolicy, initialFrame, StructuralChange.MODEL);
  }

  public StructuralRenderIntent {
    Objects.requireNonNull(sessionId, "sessionId");
    Objects.requireNonNull(snapshot, "snapshot");
    Objects.requireNonNull(cameraPolicy, "cameraPolicy");
    Objects.requireNonNull(change, "change");
  }

  @Override
  public RenderIntentKind kind() {
    return RenderIntentKind.STRUCTURAL;
  }
}
