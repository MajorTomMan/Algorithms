package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.viewport.CameraState;
import com.majortom.algorithms.visualization.render.viewport.ViewportSnapshot;

final class RenderSession {
  final RenderSessionId id;
  long generation;
  long modelRevision;
  long geometryRevision;
  long layoutRevision;
  long presentationRevision;
  long viewportRevision;
  long committedRevision;
  RenderSessionState state = RenderSessionState.INACTIVE;
  LayoutRequest lastLayoutRequest;
  LayoutResult layout;
  ViewportSnapshot viewport;
  CameraState camera;
  Object latestStructuralSnapshot;

  RenderSession(RenderSessionId id) {
    this.id = id;
  }

  void activate() {
    if (state != RenderSessionState.ACTIVE) {
      generation++;
      state = RenderSessionState.ACTIVE;
    }
  }

  void deactivate() {
    generation++;
    state = RenderSessionState.INACTIVE;
  }

  boolean active() {
    return state == RenderSessionState.ACTIVE;
  }
}
