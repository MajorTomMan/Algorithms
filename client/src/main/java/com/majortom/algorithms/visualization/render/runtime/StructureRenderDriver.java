package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderPort;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.RenderStatus;
import java.util.logging.Logger;
import com.majortom.algorithms.visualization.render.api.StructurePresenter;
import java.util.Objects;

/** Owns structure render submission state and keeps FX renderers passive. */
public final class StructureRenderDriver<S> {
    private static final Logger LOG = Logger.getLogger(StructureRenderDriver.class.getName());
    private final RenderSessionId sessionId;
    private final RenderPort renderPort;
    private final StructurePresenter<S> presenter;

    private S lastData;
    private S lastSubmittedState;
    private boolean attached;
    private boolean disposed;

    public StructureRenderDriver(
            RenderSessionId sessionId, RenderPort renderPort, StructurePresenter<S> presenter) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.renderPort = Objects.requireNonNull(renderPort, "renderPort");
        this.presenter = Objects.requireNonNull(presenter, "presenter");
    }

    /** Stores the latest immutable snapshot and submits it only while the surface is active. */
    public synchronized void render(S data) {
        if (disposed) return;
        lastData = Objects.requireNonNull(data, "data");
        requestRender();
    }

    /** Replays the latest snapshot through the presenter, typically after surface activation. */
    public synchronized void requestRender() {
        if (disposed || !attached || lastData == null) return;
        S current = lastData;
        RenderIntent intent = Objects.requireNonNull(
                presenter.present(sessionId, lastSubmittedState, current), "presenter result");
        lastSubmittedState = current;
        submit(intent);
    }

    /** Requests a presentation-only commit for renderer-local UI state such as selection. */
    public synchronized void requestPresentation() {
        if (disposed || !attached || lastData == null) return;
        submit(new PresentationRenderIntent<>(sessionId, lastData, true));
    }

    private void submit(RenderIntent intent) {
        renderPort.submit(intent).whenComplete((result, failure) -> {
            if (failure != null) {
                LOG.warning("Render submission failed for " + sessionId + ": " + failure);
            } else if (result != null && result.status() == RenderStatus.FAILED) {
                LOG.warning("Render request failed for " + sessionId + ": " + result.error());
            }
        });
    }

    public synchronized void attach() {
        if (!disposed) attached = true;
    }

    public synchronized void detach() {
        attached = false;
    }

    /** Forces the next submitted snapshot through the presenter's initial/revisit path. */
    public synchronized void resetPresentationHistory() {
        lastSubmittedState = null;
    }

    public synchronized void dispose() {
        disposed = true;
        attached = false;
        lastData = null;
        lastSubmittedState = null;
    }
}
