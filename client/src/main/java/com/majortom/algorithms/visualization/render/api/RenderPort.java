package com.majortom.algorithms.visualization.render.api;

import java.util.concurrent.CompletionStage;

public interface RenderPort {
  CompletionStage<RenderResult> submit(RenderIntent intent);
}
