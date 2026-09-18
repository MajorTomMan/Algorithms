package com.majortom.algorithms.visualization.render.api;

public sealed interface RenderIntent permits StructuralRenderIntent, PresentationRenderIntent,
    PresentationSurfaceRenderIntent, ViewportRenderIntent {
  RenderSessionId sessionId();
  RenderIntentKind kind();
}
