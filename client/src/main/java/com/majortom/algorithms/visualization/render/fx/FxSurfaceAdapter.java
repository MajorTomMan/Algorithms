package com.majortom.algorithms.visualization.render.fx;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.render.viewport.CameraState;
import com.majortom.algorithms.visualization.render.viewport.ViewportSnapshot;
import java.util.function.Consumer;

/**
 * FX-thread-only viewport/camera adapter for one visual surface.
 *
 * <p>Structure semantics live in {@link StructureVisualization}; this interface owns only
 * factual world extent, viewport observation and camera application.</p>
 */
public interface FxSurfaceAdapter {
    void applyPrimaryContentBounds(BoundsSnapshot bounds);
    ViewportSnapshot viewportSnapshot();
    CameraState cameraState();
    void applyCameraState(CameraState cameraState);
    boolean userControlledCamera();
    void prepareInitialFrame();
    void revealFrame();
    void setViewportListener(Consumer<ViewportSnapshot> listener);
    default void setCameraCommandListener(Consumer<CameraPolicy> listener) {}
}
