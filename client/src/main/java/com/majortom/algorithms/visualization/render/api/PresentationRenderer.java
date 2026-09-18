package com.majortom.algorithms.visualization.render.api;

import java.util.concurrent.CompletionStage;

/** Commit adapter for a generic presentation model. Invoked only through the RenderFramework FX executor. */
@FunctionalInterface
public interface PresentationRenderer<M> {
  CompletionStage<Void> commit(M model, RenderCommitContext context);
}
