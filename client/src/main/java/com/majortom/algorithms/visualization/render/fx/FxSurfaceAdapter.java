package com.majortom.algorithms.visualization.render.fx;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.render.viewport.CameraState;
import com.majortom.algorithms.visualization.render.viewport.ViewportSnapshot;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * FX-thread-only bridge between the generic render runtime and one visual surface.
 * Implementations may touch JavaFX nodes only because every method is invoked by FxExecutor.
 */
public interface FxSurfaceAdapter<S> {
    LayoutRequest captureLayout(S snapshot, RenderCaptureContext context);
    CompletionStage<Void> commitLayout(S snapshot, LayoutPatch patch, RenderCommitContext context);
    CompletionStage<Void> commitPresentation(S snapshot, RenderCommitContext context);

    /** Supplies the factual structure extent independently of live SceneGraph decoration bounds. */
    default void applyPrimaryContentBounds(BoundsSnapshot bounds) {}
    ViewportSnapshot viewportSnapshot();
    CameraState cameraState();
    void applyCameraState(CameraState cameraState);
    boolean userControlledCamera();
    void prepareInitialFrame();
    void revealFrame();
    void setViewportListener(Consumer<ViewportSnapshot> listener);
    default void setCameraCommandListener(Consumer<CameraPolicy> listener) {}
}
