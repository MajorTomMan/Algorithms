package com.majortom.algorithms.visualization.render.api;

import java.util.concurrent.CompletionStage;

/** RenderFramework ownership boundary for the current execution presentation position. */
public interface PresentationCursorPort {
  CompletionStage<Void> publishPresentationCursor(
      RenderSessionId sessionId, PresentationCursor cursor);

  CompletionStage<Void> clearPresentationCursor(RenderSessionId sessionId);

}
