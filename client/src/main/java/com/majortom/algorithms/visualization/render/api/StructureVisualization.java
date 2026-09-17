package com.majortom.algorithms.visualization.render.api;


/**
 * JavaFX-neutral structure semantics for one visualization family.
 *
 * <p>This side only describes factual layout input. It never creates or mutates JavaFX nodes and
 * never owns viewport, camera, lifecycle or commit timing.</p>
 */
@FunctionalInterface
public interface StructureVisualization<S> {
    LayoutRequest captureLayout(S snapshot, RenderCaptureContext context);
}
