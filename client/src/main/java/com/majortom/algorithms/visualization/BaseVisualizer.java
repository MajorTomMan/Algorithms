package com.majortom.algorithms.visualization;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.fx.FxStructureRenderer;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import javafx.scene.layout.StackPane;

/** Passive JavaFX renderer for one structure family. Render submission is owned outside FX. */
public abstract class BaseVisualizer<S> extends StackPane implements FxStructureRenderer<S> {
  private Runnable presentationInvalidationHandler = () -> {};

  public abstract RenderSessionId sessionId();
  public abstract StructureVisualization<S> structureVisualization();
  public abstract FxSurfaceAdapter fxSurfaceAdapter();

  /** Installed by the controller-owned render driver for FX-local presentation changes. */
  public final void setPresentationInvalidationHandler(Runnable handler) {
    presentationInvalidationHandler = handler == null ? () -> {} : handler;
  }

  /** Requests a presentation-only re-commit without exposing RenderPort or render intents to FX. */
  protected final void invalidatePresentation() {
    presentationInvalidationHandler.run();
  }

  public void setPlaybackSpeed(double speed) {}
  public void setScrubbing(boolean scrubbing) {}
  public void setViewportObstructionInsets(javafx.geometry.Insets insets) {}
  public void onVisualizationReset() {}

  /** Definitively releases visualizer-owned resources on the FX thread. */
  public void dispose() {
    presentationInvalidationHandler = () -> {};
  }
}
