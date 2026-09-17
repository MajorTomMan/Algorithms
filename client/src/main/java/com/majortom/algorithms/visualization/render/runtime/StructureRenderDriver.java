package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderPort;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import java.util.Objects;

/**
 * Owns render submission state and lifecycle outside the JavaFX visualizer.
 *
 * <p>The driver caches the latest factual state while detached, delegates structural/presentation
 * classification to a presenter, and is the only module-side owner of RenderPort submission.</p>
 */
public final class StructureRenderDriver<S> {
  private final RenderSessionId sessionId;
  private final RenderPort renderPort;
  private final StructureRenderPresenter<S> presenter;
  private S currentState;
  private boolean attached;
  private boolean disposed;

  public StructureRenderDriver(
      RenderSessionId sessionId, RenderPort renderPort, StructureRenderPresenter<S> presenter) {
    this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
    this.renderPort = Objects.requireNonNull(renderPort, "renderPort");
    this.presenter = Objects.requireNonNull(presenter, "presenter");
  }

  public synchronized void render(S state) {
    Objects.requireNonNull(state, "state");
    if (disposed) return;
    S previous = currentState;
    currentState = state;
    if (attached) submit(presenter.present(sessionId, previous, state));
  }

  public synchronized void attach() {
    if (disposed || attached) return;
    attached = true;
    if (currentState != null) submit(presenter.present(sessionId, null, currentState));
  }

  public synchronized void detach() {
    attached = false;
  }

  /** Re-submit the current factual state as presentation-only after an FX-local interaction. */
  public synchronized void requestPresentation() {
    if (disposed || !attached || currentState == null) return;
    submit(new PresentationRenderIntent<>(sessionId, currentState));
  }

  public synchronized void reset() {
    currentState = null;
  }

  public synchronized void dispose() {
    disposed = true;
    attached = false;
    currentState = null;
  }

  private void submit(RenderIntent intent) {
    renderPort.submit(Objects.requireNonNull(intent, "intent"));
  }
}
