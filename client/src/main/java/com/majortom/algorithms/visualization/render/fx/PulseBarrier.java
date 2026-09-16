package com.majortom.algorithms.visualization.render.fx;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Non-blocking hand-off to the next FX queue turn after scene mutations. */
public final class PulseBarrier {
    private final FxExecutor fxExecutor;

    public PulseBarrier(FxExecutor fxExecutor) {
        this.fxExecutor = Objects.requireNonNull(fxExecutor, "fxExecutor");
    }

    public CompletionStage<Void> await() {
        return fxExecutor.defer(() -> {});
    }
}
