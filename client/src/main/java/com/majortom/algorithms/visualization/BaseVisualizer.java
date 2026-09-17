package com.majortom.algorithms.visualization;

import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.fx.FxStructureRenderer;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.layout.StackPane;

/** Passive JavaFX renderer. Factual state, lifecycle and RenderPort submission live in a driver. */
public abstract class BaseVisualizer<S> extends StackPane implements FxStructureRenderer<S> {
  private Consumer<S> renderHandler = ignored -> {};
  private Supplier<S> stateSupplier = () -> null;
  private Runnable presentationInvalidationHandler = () -> {};
  private Runnable renderRequestHandler = () -> {};
  private Consumer<String> attachedHandler = ignored -> {};
  private Consumer<String> detachedHandler = ignored -> {};
  private Runnable driverResetHandler = () -> {};
  private Runnable driverDisposeHandler = () -> {};

  public abstract RenderSessionId sessionId();
  public abstract StructureVisualization<S> structureVisualization();
  public abstract FxSurfaceAdapter fxSurfaceAdapter();

  /** Framework wiring only; render policy remains outside FX. */
  public final void bindRenderCallbacks(
      Consumer<S> renderHandler,
      Supplier<S> stateSupplier,
      Runnable presentationInvalidationHandler,
      Runnable renderRequestHandler,
      Consumer<String> attachedHandler,
      Consumer<String> detachedHandler,
      Runnable driverResetHandler,
      Runnable driverDisposeHandler) {
    this.renderHandler = renderHandler == null ? ignored -> {} : renderHandler;
    this.stateSupplier = stateSupplier == null ? () -> null : stateSupplier;
    this.presentationInvalidationHandler =
        presentationInvalidationHandler == null ? () -> {} : presentationInvalidationHandler;
    this.renderRequestHandler = renderRequestHandler == null ? () -> {} : renderRequestHandler;
    this.attachedHandler = attachedHandler == null ? ignored -> {} : attachedHandler;
    this.detachedHandler = detachedHandler == null ? ignored -> {} : detachedHandler;
    this.driverResetHandler = driverResetHandler == null ? () -> {} : driverResetHandler;
    this.driverDisposeHandler = driverDisposeHandler == null ? () -> {} : driverDisposeHandler;
  }

  /** Compatibility entrypoint for controllers; it only forwards factual state to the driver. */
  public final void render(S data) {
    renderHandler.accept(data);
  }

  /** Reads driver-owned factual state for FX-local hit testing/selection only. */
  protected final S currentState() {
    return stateSupplier.get();
  }

  /** Requests a presentation-only commit; FX never constructs a RenderIntent. */
  protected final void invalidatePresentation() {
    presentationInvalidationHandler.run();
  }

  /** Called by BaseController only after RenderSurfaceHost activation. */
  final void requestRender() {
    renderRequestHandler.run();
  }

  public void onModuleAttached(String moduleId) {
    attachedHandler.accept(moduleId);
  }

  public void onModuleDetached(String moduleId) {
    detachedHandler.accept(moduleId);
  }

  public void setPlaybackSpeed(double speed) {}
  public void setScrubbing(boolean scrubbing) {}
  public void setViewportObstructionInsets(javafx.geometry.Insets insets) {}

  /** Driver reset is guaranteed even when a concrete renderer has FX-local reset work. */
  public final void onVisualizationReset() {
    driverResetHandler.run();
    resetFxState();
  }

  protected void resetFxState() {}

  /** Definitively releases visualizer-owned resources on the FX thread. */
  public void dispose() {
    driverDisposeHandler.run();
    renderHandler = ignored -> {};
    stateSupplier = () -> null;
    presentationInvalidationHandler = () -> {};
    renderRequestHandler = () -> {};
    attachedHandler = ignored -> {};
    detachedHandler = ignored -> {};
    driverResetHandler = () -> {};
    driverDisposeHandler = () -> {};
  }
}
