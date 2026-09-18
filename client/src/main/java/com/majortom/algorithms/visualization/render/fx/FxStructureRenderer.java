package com.majortom.algorithms.visualization.render.fx;

import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import java.util.concurrent.CompletionStage;

/** FX-thread commit adapter for one structure family. */
public interface FxStructureRenderer<S> {
    CompletionStage<Void> commitLayout(S snapshot, LayoutPatch patch, RenderCommitContext context);
    CompletionStage<Void> commitPresentation(S snapshot, RenderCommitContext context);
}
