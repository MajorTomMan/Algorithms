package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.layout.LayoutEngine;
import com.majortom.algorithms.visualization.render.layout.LayoutEngineRegistry;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 纯布局计算的调度与复用边界，不拥有 JavaFX 场景或相机。
 *
 * <p>对比相邻 LayoutRequest 的几何输入，允许复用已有 LayoutResult；
 * 不可复用时从 LayoutEngineRegistry 选择引擎并交给 LayoutExecutor 后台计算。
 * RenderFramework 决定提交哪个 layout revision、什么时候在 FX 线程应用。
 *
 * <p>模型修订号和几何状态需被渲染控制面正确关联，
 * 避免过期布局覆盖新的 GraphViewState。
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
        // Metadata is usually tiny; reject changed layout settings before comparing
        // potentially large node/edge lists on every model capture.
        return previous.engineId().equals(current.engineId())
                && previous.geometryRevision() == current.geometryRevision()
                && previous.metadata().equals(current.metadata())
                && previous.elements().equals(current.elements())
                && previous.links().equals(current.links())
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
