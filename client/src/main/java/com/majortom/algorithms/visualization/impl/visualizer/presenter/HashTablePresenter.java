package com.majortom.algorithms.visualization.impl.visualizer.presenter;

import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.api.StructurePresenter;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.runtime.hash.HashTableViewState;

public final class HashTablePresenter implements StructurePresenter<HashTableViewState> {
  @Override
  public RenderIntent present(
      RenderSessionId sessionId, HashTableViewState previous, HashTableViewState current) {
    if (previous == null) {
      return new StructuralRenderIntent<>(
          sessionId, current, CameraPolicy.RESTORE_OR_FIT_IF_READABLE, true);
    }
    boolean structural = previous.capacity() != current.capacity()
        || !previous.buckets().equals(current.buckets());
    if (!structural) {
      return new PresentationRenderIntent<>(sessionId, current);
    }
    return new StructuralRenderIntent<>(
        sessionId, current, CameraPolicy.ENSURE_VISIBLE_IF_READABLE, false);
  }
}
