package com.majortom.algorithms.visualization.render.api;

public sealed interface RenderIntent permits StructuralRenderIntent, PresentationRenderIntent,
    ViewportRenderIntent {
  RenderSessionId sessionId();
  RenderIntentKind kind();
}
