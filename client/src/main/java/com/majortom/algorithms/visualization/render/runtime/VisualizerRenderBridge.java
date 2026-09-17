package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.BaseVisualizer;

/** Binds a passive FX visualizer to its external structure render driver. */
public final class VisualizerRenderBridge {
  private VisualizerRenderBridge() {}

  public static <S> StructureRenderDriver<S> bind(
      BaseVisualizer<S> visualizer, StructureRenderDriver<S> driver) {
    visualizer.bindRenderCallbacks(
        driver::render,
        driver::currentState,
        driver::presentCurrent,
        driver::requestCurrent,
        driver::attach,
        driver::detach,
        driver::reset,
        driver::dispose);
    return driver;
  }
}
