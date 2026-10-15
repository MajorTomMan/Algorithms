package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.PresentationCursor;
import com.majortom.algorithms.visualization.render.viewport.CameraState;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.render.viewport.ViewportSnapshot;

final class RenderSession {
  final RenderSessionId id;
  long generation;
  long modelRevision;
  long geometryRevision;
  long layoutRevision;
  long presentationRevision;
  long presentationCursorRevision;
  long committedRevision;
  private boolean active;
  LayoutRequest lastLayoutRequest;
  LayoutResult layout;
  ViewportSnapshot viewport;
  CameraState camera;
  CameraPolicy geometryCameraPolicy = CameraPolicy.ENSURE_VISIBLE;
  Object latestStructuralSnapshot;
  PresentationCursor presentationCursor;

  RenderSession(RenderSessionId id) {
    this.id = id;
  }

  void activate() {
    if (!active) {
      generation++;
      active = true;
    }
  }

  void deactivate() {
    generation++;
    active = false;
  }

  boolean active() {
    return active;
  }
}
