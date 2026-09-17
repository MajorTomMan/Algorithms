package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;

/** Decides how one factual structure state should enter the render framework. */
@FunctionalInterface
public interface StructureRenderPresenter<S> {
  RenderIntent present(RenderSessionId sessionId, S previous, S current);
}
