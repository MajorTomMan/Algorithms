package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderPort;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import java.util.Objects;

/**
 * Owns factual render state, module lifecycle and RenderPort submission outside JavaFX renderers.
 */
public final class StructureRenderDriver<S> {
  private final RenderSessionId sessionId;
  private final RenderPort renderPort;
  private final StructureRenderPresenter<S> presenter;
  private S currentState;
  private S previousSubmittedState;
  private boolean attached;
  private boolean disposed;

  public StructureRenderDriver(
      RenderSessionId sessionId, RenderPort renderPort, StructureRenderPresenter<S> presenter) {
    this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
    this.renderPort = Objects.requireNonNull(renderPort, "renderPort");
    this.presenter = Objects.requireNonNull(presenter, "presenter");
  }

  public synchronized void render(S state) {
    if (disposed) return;
    currentState = Objects.requireNonNull(state, "state");
    if (attached) submitCurrent();
  }

  public synchronized S currentState() {
    return currentState;
  }

  public synchronized void attach(String ignoredModuleId) {
    if (!disposed) attached = true;
  }

  public synchronized void detach(String ignoredModuleId) {
    attached = false;
  }

  /** Called after RenderSurfaceHost has activated the session. */
  public synchronized void requestCurrent() {
    if (disposed || !attached || currentState == null) return;
    submitCurrent();
  }

  /** FX-local selections/overlays are presentation-only and never choose camera/layout policy. */
  public synchronized void presentCurrent() {
    if (disposed || !attached || currentState == null) return;
    renderPort.submit(new PresentationRenderIntent<>(sessionId, currentState));
  }

  public synchronized void reset() {
    currentState = null;
    previousSubmittedState = null;
  }

  public synchronized void dispose() {
    disposed = true;
    attached = false;
    currentState = null;
    previousSubmittedState = null;
  }

  private void submitCurrent() {
    renderPort.submit(presenter.present(sessionId, previousSubmittedState, currentState));
    previousSubmittedState = currentState;
  }
}
