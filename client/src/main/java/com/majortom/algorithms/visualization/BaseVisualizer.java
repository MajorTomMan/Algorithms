package com.majortom.algorithms.visualization;

import javafx.scene.layout.StackPane;

/** Common lifecycle base for structure visualizers. Rendering technology stays in subclasses. */
public abstract class BaseVisualizer<S> extends StackPane {
    private S lastData;
    private boolean moduleAttached;
    private boolean disposed;

    /** Stores factual state and submits it when the module is attached. */
    public final void render(S data) {
        lastData = data;
        requestRender();
    }

    /** Every concrete structure visualizer submits immutable state through its injected RenderPort. */
    protected abstract void submitFrameworkRender(S data);

    protected final void requestRender() {
        if (disposed || !moduleAttached || lastData == null) return;
        submitFrameworkRender(lastData);
    }

    protected final S currentState() { return lastData; }

    public void setPlaybackSpeed(double speed) {}
    public void setScrubbing(boolean scrubbing) {}
    public void setViewportObstructionInsets(javafx.geometry.Insets insets) {}
    public void onVisualizationReset() {}

    /** Marks the module active. RenderSurfaceHost activates the RenderSession before requestRender(). */
    public void onModuleAttached(String moduleId) { moduleAttached = true; }

    /** Stops this visualizer from publishing additional render work. */
    public void onModuleDetached(String moduleId) { moduleAttached = false; }

    /** Definitively releases visualizer-owned resources. Called on the FX thread by RenderSurfaceHost. */
    public void dispose() {
        if (disposed) return;
        disposed = true;
        moduleAttached = false;
    }

    protected final boolean isModuleAttached() { return moduleAttached; }
    protected final boolean isDisposed() { return disposed; }
}
