package com.majortom.algorithms.visualization.render.api;

/**
 * JavaFX-neutral policy that turns successive structure snapshots into framework render intents.
 * The presenter decides structural vs. presentational work and camera policy; FX renderers do not.
 */
@FunctionalInterface
public interface StructurePresenter<S> {
    RenderIntent present(RenderSessionId sessionId, S previous, S current);
}
