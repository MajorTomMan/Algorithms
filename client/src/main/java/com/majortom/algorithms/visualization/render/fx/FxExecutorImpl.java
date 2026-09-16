package com.majortom.algorithms.visualization.render.fx;

import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public final class FxExecutorImpl implements FxExecutor {
    @Override
    public CompletionStage<Void> execute(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            return runNow(action);
        }
        return defer(action);
    }

    @Override
    public CompletionStage<Void> defer(Runnable action) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Platform.runLater(
                () -> {
                    try {
                        action.run();
                        future.complete(null);
                    } catch (Throwable failure) {
                        future.completeExceptionally(failure);
                    }
                });
        return future;
    }

    @Override
    public <T> CompletionStage<T> supply(Supplier<T> supplier) {
        if (Platform.isFxApplicationThread()) {
            try {
                return CompletableFuture.completedFuture(supplier.get());
            } catch (Throwable failure) {
                CompletableFuture<T> future = new CompletableFuture<>();
                future.completeExceptionally(failure);
                return future;
            }
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(
                () -> {
                    try {
                        future.complete(supplier.get());
                    } catch (Throwable failure) {
                        future.completeExceptionally(failure);
                    }
                });
        return future;
    }

    @Override
    public boolean isFxThread() {
        return Platform.isFxApplicationThread();
    }

    private CompletionStage<Void> runNow(Runnable action) {
        try {
            action.run();
            return CompletableFuture.completedFuture(null);
        } catch (Throwable failure) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(failure);
            return future;
        }
    }
}
