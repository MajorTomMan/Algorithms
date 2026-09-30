package com.majortom.algorithms.visualization;

import com.majortom.algorithms.visualization.animation.api.AnimationControl;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.fx.FxStructureRenderer;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;

/** Passive JavaFX renderer base. Render submission and lifecycle state live outside the FX renderer. */
public abstract class BaseVisualizer<S> extends StackPane implements FxStructureRenderer<S> {
    private boolean disposed;
    private VisualizationSurface hostedSurface;

    /** Stable routing identity used when composing the hosted RenderSurface. */
    public abstract RenderSessionId sessionId();

    /** JavaFX-neutral structure semantics hosted beside this FX renderer. */
    public abstract StructureVisualization<S> structureVisualization();

    /** The viewport/camera adapter hosted beside this FX renderer. */
    public abstract FxSurfaceAdapter fxSurfaceAdapter();

    protected final void installSurface(VisualizationSurface surface) {
        installSurface(surface, null);
    }

    protected final void installSurface(VisualizationSurface surface, Insets safeInsets) {
        Objects.requireNonNull(surface, "surface");
        if (hostedSurface != null && hostedSurface != surface) {
            throw new IllegalStateException("A visualizer can host only one VisualizationSurface");
        }
        hostedSurface = surface;
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        if (safeInsets != null) {
            surface.setSafeInsets(safeInsets);
        }
        surface.setFrameworkManagedCamera(true);
    }

    protected AnimationControl animationControl() { return AnimationControl.NONE; }

    public final void setPlaybackSpeed(double speed) { animationControl().setSpeed(speed); }
    public final void setScrubbing(boolean scrubbing) { animationControl().setScrubbing(scrubbing); }
    public final void pauseAnimations() { animationControl().pause(); }
    public final void resumeAnimations() { animationControl().resume(); }
    public final void stepAnimations() { animationControl().step(); }

    public void setViewportObstructionInsets(Insets insets) {
        if (hostedSurface != null) {
            hostedSurface.setObstructionInsets(Objects.requireNonNull(insets, "insets"));
        }
    }

    public void onVisualizationReset() { animationControl().reset(); }

    /** Definitively releases renderer-owned resources. Called on the surface lifecycle thread. */
    public void dispose() {
        if (disposed) return;
        animationControl().dispose();
        if (hostedSurface != null) {
            hostedSurface.prefWidthProperty().unbind();
            hostedSurface.prefHeightProperty().unbind();
        }
        disposed = true;
    }

    protected final boolean isDisposed() { return disposed; }
}
