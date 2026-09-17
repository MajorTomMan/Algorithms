package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;

/** JavaFX-neutral policy that translates factual state changes into render intents. */
@FunctionalInterface
public interface StructurePresenter<S> {
  RenderIntent present(RenderSessionId sessionId, S previous, S current);
}
