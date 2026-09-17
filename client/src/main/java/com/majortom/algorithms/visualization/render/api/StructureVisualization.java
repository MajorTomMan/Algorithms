package com.majortom.algorithms.visualization.render.api;

import com.majortom.algorithms.visualization.render.fx.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.fx.RenderCommitContext;
import java.util.concurrent.CompletionStage;

/**
 * Structure-specific render semantics. It describes layout input and applies authoritative
 * structure/presentation commits, but it does not own viewport or camera behavior.
 */
public interface StructureVisualization<S> {
    LayoutRequest captureLayout(S snapshot, RenderCaptureContext context);
    CompletionStage<Void> commitLayout(S snapshot, LayoutPatch patch, RenderCommitContext context);
    CompletionStage<Void> commitPresentation(S snapshot, RenderCommitContext context);
}
