package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.layout.LayoutEngine;
import com.majortom.algorithms.visualization.render.layout.LayoutEngineRegistry;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * JavaFX-neutral coordinator for layout selection, reuse and background execution.
 *
 * <p>The render framework owns transaction flow and commit timing. This class only decides whether
 * a layout can be reused, selects the requested layout engine and schedules layout work.</p>
 */
public final class LayoutCoordinator implements AutoCloseable {
    private final LayoutExecutor executor;
    private final LayoutEngineRegistry engines;

    public LayoutCoordinator(LayoutExecutor executor, LayoutEngineRegistry engines) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.engines = Objects.requireNonNull(engines, "engines");
    }

    public LayoutSubmission submit(
            LayoutRequest request,
            LayoutRequest previousRequest,
            LayoutResult previousResult,
            long modelRevision) {
        Objects.requireNonNull(request, "request");

        if (previousRequest != null
                && previousResult != null
                && sameGeometryInput(previousRequest, request)) {
            return new LayoutSubmission(
                    false,
                    CompletableFuture.completedFuture(
                            previousResult.withModelRevision(modelRevision)));
        }

        LayoutEngine engine = engines.require(request.engineId());
        return new LayoutSubmission(true, executor.submit(engine, request));
    }

    private static boolean sameGeometryInput(LayoutRequest previous, LayoutRequest current) {
        return previous.engineId().equals(current.engineId())
                && previous.geometryRevision() == current.geometryRevision()
                && previous.elements().equals(current.elements())
                && previous.links().equals(current.links())
                && previous.metadata().equals(current.metadata())
                && previous.decorations().equals(current.decorations());
    }

    @Override
    public void close() {
        executor.close();
    }

    public record LayoutSubmission(
            boolean layoutRequired,
            CompletionStage<LayoutResult> result) {
        public LayoutSubmission {
            Objects.requireNonNull(result, "result");
        }
    }
}
