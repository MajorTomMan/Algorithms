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

/**
 * JavaFX 画布的被动承载基类；不拥有算法事件、结构真值或渲染调度权。
 *
 * <p>各可视化器实现 StructureVisualization（非 FX 的布局语义）和
 * FxStructureRenderer（FX 场景提交），由 RenderSurfaceHost 统一组装。
 * 摄像机/视口通过 FxSurfaceAdapter 对接；普通模块控制器只提交 ViewState。
 *
 * <p>生命周期必须经 RenderFramework attach/detach；
 * {@link #dispose()} 释放 renderer 资源后不得继续提交新画面。
 */
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
