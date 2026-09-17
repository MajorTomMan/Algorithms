package com.majortom.algorithms.visualization;

import com.majortom.algorithms.visualization.animation.api.AnimationControl;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.fx.FxStructureRenderer;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import javafx.scene.layout.StackPane;

/** Passive JavaFX renderer base. Render submission and lifecycle state live outside the FX renderer. */
public abstract class BaseVisualizer<S> extends StackPane implements FxStructureRenderer<S> {
    private boolean disposed;

    /** Stable routing identity used when composing the hosted RenderSurface. */
    public abstract RenderSessionId sessionId();

    /** JavaFX-neutral structure semantics hosted beside this FX renderer. */
    public abstract StructureVisualization<S> structureVisualization();

    /** The viewport/camera adapter hosted beside this FX renderer. */
    public abstract FxSurfaceAdapter fxSurfaceAdapter();

    protected AnimationControl animationControl() { return AnimationControl.NONE; }

    public final void setPlaybackSpeed(double speed) { animationControl().setSpeed(speed); }
    public final void setScrubbing(boolean scrubbing) { animationControl().setScrubbing(scrubbing); }
    public final void pauseAnimations() { animationControl().pause(); }
    public final void resumeAnimations() { animationControl().resume(); }
    public void setViewportObstructionInsets(javafx.geometry.Insets insets) {}
    public void onVisualizationReset() { animationControl().reset(); }

    /** Definitively releases renderer-owned resources. Called on the surface lifecycle thread. */
    public void dispose() {
        if (disposed) return;
        animationControl().dispose();
        disposed = true;
    }

    protected final boolean isDisposed() { return disposed; }
}
