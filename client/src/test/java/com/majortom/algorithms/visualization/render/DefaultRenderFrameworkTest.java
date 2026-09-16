package com.majortom.algorithms.visualization.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.ContentStyleSnapshot;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderResult;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.RenderStatus;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.api.ViewportRenderIntent;
import com.majortom.algorithms.visualization.render.diagnostics.RenderTrace;
import com.majortom.algorithms.visualization.render.fx.FxExecutor;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceRegistry;
import com.majortom.algorithms.visualization.render.fx.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.fx.RenderCommitContext;
import com.majortom.algorithms.visualization.render.layout.LayoutEngine;
import com.majortom.algorithms.visualization.render.layout.LayoutEngineRegistry;
import com.majortom.algorithms.visualization.render.runtime.DefaultRenderFramework;
import com.majortom.algorithms.visualization.render.runtime.LayoutExecutor;
import com.majortom.algorithms.visualization.render.runtime.RenderScheduler;
import com.majortom.algorithms.visualization.render.viewport.CameraManager;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.render.viewport.CameraState;
import com.majortom.algorithms.visualization.render.viewport.ViewportInsets;
import com.majortom.algorithms.visualization.render.viewport.ViewportSnapshot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

class DefaultRenderFrameworkTest {
    private DefaultRenderFramework framework;

    @AfterEach
    void closeFramework() {
        if (framework != null) framework.close();
    }

    @Test
    void presentationAndViewportNeverInvokeLayout() {
        Fixture fixture = fixture(new CountingLayoutEngine());
        RenderResult first =
                fixture.framework
                        .submit(
                                new StructuralRenderIntent<>(
                                        fixture.id, "model-a", CameraPolicy.FIT_CONTENT, true))
                        .toCompletableFuture()
                        .join();
        assertEquals(RenderStatus.PRESENTED, first.status());
        assertEquals(1L, fixture.trace.layoutInvocationCount(fixture.id));

        RenderResult presentation =
                fixture.framework
                        .submit(new PresentationRenderIntent<>(fixture.id, "compare-a-b"))
                        .toCompletableFuture()
                        .join();
        assertEquals(RenderStatus.PRESENTED, presentation.status());
        assertEquals(1L, fixture.trace.layoutInvocationCount(fixture.id));

        RenderResult viewport =
                fixture.framework
                        .submit(
                                new ViewportRenderIntent(
                                        fixture.id,
                                        new ViewportSnapshot(1280, 720, ViewportInsets.none()),
                                        CameraPolicy.KEEP))
                        .toCompletableFuture()
                        .join();
        assertEquals(RenderStatus.NO_OP, viewport.status());
        assertEquals(1L, fixture.trace.layoutInvocationCount(fixture.id));
        assertEquals(1, fixture.target.presentationCommits.get());
    }

    @Test
    void identicalGeometryReusesCachedLayout() {
        Fixture fixture = fixture(new CountingLayoutEngine());
        RenderResult first =
                fixture.framework
                        .submit(
                                new StructuralRenderIntent<>(
                                        fixture.id, "first", CameraPolicy.FIT_CONTENT, true))
                        .toCompletableFuture()
                        .join();
        RenderResult second =
                fixture.framework
                        .submit(
                                new StructuralRenderIntent<>(
                                        fixture.id, "second", CameraPolicy.KEEP, false))
                        .toCompletableFuture()
                        .join();

        assertTrue(first.layoutChanged());
        assertFalse(second.layoutChanged());
        assertEquals(1L, fixture.trace.layoutInvocationCount(fixture.id));
        assertEquals(2, fixture.target.layoutCommits.get());
    }

    @Test
    void staleGenerationCannotCommitAfterDeactivate() throws Exception {
        BlockingLayoutEngine engine = new BlockingLayoutEngine();
        Fixture fixture = fixture(engine);
        CompletionStage<RenderResult> result =
                fixture.framework.submit(
                        new StructuralRenderIntent<>(
                                fixture.id, "slow", CameraPolicy.FIT_CONTENT, true));

        assertTrue(engine.started.await(3, TimeUnit.SECONDS));
        fixture.framework.deactivateSession(fixture.id).toCompletableFuture().join();
        engine.release.countDown();

        RenderResult terminal = result.toCompletableFuture().get(3, TimeUnit.SECONDS);
        assertEquals(RenderStatus.CANCELLED, terminal.status());
        assertEquals(0, fixture.target.layoutCommits.get());
    }

    @Test
    void contentStyleChangeInvalidatesGeometryWithoutRepeatedRelayout() {
        Fixture fixture = fixture(new CountingLayoutEngine());
        RenderResult first =
                fixture.framework
                        .submit(
                                new StructuralRenderIntent<>(
                                        fixture.id, "model", CameraPolicy.FIT_CONTENT, true))
                        .toCompletableFuture()
                        .join();
        assertEquals(RenderStatus.PRESENTED, first.status());
        assertEquals(1L, fixture.trace.layoutInvocationCount(fixture.id));

        fixture.framework
                .updateContentStyle(new ContentStyleSnapshot(18.0d, "Noto Sans CJK SC", "Inter"))
                .toCompletableFuture()
                .join();
        assertEquals(2L, fixture.trace.layoutInvocationCount(fixture.id));

        fixture.framework
                .updateContentStyle(new ContentStyleSnapshot(18.0d, "Noto Sans CJK SC", "Inter"))
                .toCompletableFuture()
                .join();
        assertEquals(2L, fixture.trace.layoutInvocationCount(fixture.id));
    }

    @Test
    void newerStructuralSubmissionMakesInFlightLayoutStaleBeforeCommit() throws Exception {
        BlockingLayoutEngine engine = new BlockingLayoutEngine();
        Fixture fixture = fixture(engine);
        CompletionStage<RenderResult> older =
                fixture.framework.submit(
                        new StructuralRenderIntent<>(
                                fixture.id, "older", CameraPolicy.FIT_CONTENT, true));
        assertTrue(engine.started.await(3, TimeUnit.SECONDS));

        CompletionStage<RenderResult> newer =
                fixture.framework.submit(
                        new StructuralRenderIntent<>(
                                fixture.id, "newer", CameraPolicy.ENSURE_VISIBLE, false));
        engine.release.countDown();

        assertEquals(
                RenderStatus.CANCELLED,
                older.toCompletableFuture().get(3, TimeUnit.SECONDS).status());
        assertEquals(
                RenderStatus.PRESENTED,
                newer.toCompletableFuture().get(3, TimeUnit.SECONDS).status());
        assertEquals(1, fixture.target.layoutCommits.get());
        assertEquals(
                1,
                fixture.target.revealCalls.get(),
                "authoritative successor must reveal a world hidden by a superseded initial frame");
        assertTrue(fixture.target.worldVisible);
        assertEquals(
                1.35d,
                fixture.target.camera.scale(),
                0.0001d,
                "first committed layout must fit even when its successor intent says"
                    + " ENSURE_VISIBLE");
    }

    @Test
    void staleSurfaceCannotUnregisterItsReplacement() {
        CountingLayoutEngine engine = new CountingLayoutEngine();
        RenderSessionId id = RenderSessionId.of("REPLACED");
        RenderTrace trace = new RenderTrace();
        FxSurfaceRegistry registry = new FxSurfaceRegistry();
        framework =
                new DefaultRenderFramework(
                        new RenderScheduler(),
                        new LayoutExecutor(2),
                        new DirectFxExecutor(),
                        registry,
                        new LayoutEngineRegistry().register(engine),
                        new CameraManager(),
                        trace);

        FakeTarget previous = new FakeTarget(id, engine.id());
        FakeTarget replacement = new FakeTarget(id, engine.id());
        framework.registerSurface(id, previous).toCompletableFuture().join();
        framework.registerSurface(id, replacement).toCompletableFuture().join();
        framework.unregisterSurface(id, previous).toCompletableFuture().join();
        framework.activateSession(id).toCompletableFuture().join();

        RenderResult result =
                framework
                        .submit(
                                new StructuralRenderIntent<>(
                                        id, "replacement", CameraPolicy.FIT_CONTENT, true))
                        .toCompletableFuture()
                        .join();

        assertEquals(RenderStatus.PRESENTED, result.status());
        assertEquals(0, previous.layoutCommits.get());
        assertEquals(1, replacement.layoutCommits.get());
        assertTrue(previous.viewportListener == null);
        assertTrue(replacement.viewportListener != null);
    }

    @Test
    void sessionRevisitRestoresCameraAndReusesLayout() {
        CountingLayoutEngine engine = new CountingLayoutEngine();
        Fixture fixture = fixture(engine);
        RenderResult first =
                fixture.framework
                        .submit(
                                new StructuralRenderIntent<>(
                                        fixture.id, "first", CameraPolicy.RESTORE, true))
                        .toCompletableFuture()
                        .join();
        assertEquals(RenderStatus.PRESENTED, first.status());
        assertEquals(1L, fixture.trace.layoutInvocationCount(fixture.id));

        CameraState userCamera = new CameraState(1.75d, 42.0d, -19.0d);
        fixture.target.camera = userCamera;
        fixture.framework
                .submit(
                        new ViewportRenderIntent(
                                fixture.id, fixture.target.viewportSnapshot(), CameraPolicy.KEEP))
                .toCompletableFuture()
                .join();

        fixture.framework.deactivateSession(fixture.id).toCompletableFuture().join();
        fixture.framework
                .unregisterSurface(fixture.id, fixture.target)
                .toCompletableFuture()
                .join();
        FakeTarget revisited = new FakeTarget(fixture.id, engine.id());
        fixture.framework.registerSurface(fixture.id, revisited).toCompletableFuture().join();
        fixture.framework.activateSession(fixture.id).toCompletableFuture().join();

        RenderResult second =
                fixture.framework
                        .submit(
                                new StructuralRenderIntent<>(
                                        fixture.id, "second", CameraPolicy.RESTORE, true))
                        .toCompletableFuture()
                        .join();
        assertEquals(RenderStatus.PRESENTED, second.status());
        assertFalse(second.layoutChanged());
        assertEquals(1L, fixture.trace.layoutInvocationCount(fixture.id));
        assertEquals(userCamera, revisited.camera);
    }

    private Fixture fixture(LayoutEngine engine) {
        RenderSessionId id = RenderSessionId.of("TEST");
        RenderTrace trace = new RenderTrace();
        FakeTarget target = new FakeTarget(id, engine.id());
        framework =
                new DefaultRenderFramework(
                        new RenderScheduler(),
                        new LayoutExecutor(2),
                        new DirectFxExecutor(),
                        new FxSurfaceRegistry(),
                        new LayoutEngineRegistry().register(engine),
                        new CameraManager(),
                        trace);
        framework.registerSurface(id, target).toCompletableFuture().join();
        framework.activateSession(id).toCompletableFuture().join();
        return new Fixture(framework, id, trace, target);
    }

    private record Fixture(
            DefaultRenderFramework framework,
            RenderSessionId id,
            RenderTrace trace,
            FakeTarget target) {}

    private static class CountingLayoutEngine implements LayoutEngine {
        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public String id() {
            return "test";
        }

        @Override
        public LayoutResult layout(LayoutRequest request) {
            invocations.incrementAndGet();
            LayoutElement element = request.elements().getFirst();
            ElementGeometry geometry =
                    new ElementGeometry(element.id(), 20, 30, element.width(), element.height());
            return new LayoutResult(
                    request.requestId(),
                    request.modelRevision(),
                    Map.of(element.id(), geometry),
                    List.of(),
                    new BoundsSnapshot(20, 30, element.width(), element.height()));
        }
    }

    private static final class BlockingLayoutEngine extends CountingLayoutEngine {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public LayoutResult layout(LayoutRequest request) {
            started.countDown();
            try {
                if (!release.await(3, TimeUnit.SECONDS))
                    throw new IllegalStateException("test layout was not released");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(interrupted);
            }
            return super.layout(request);
        }
    }

    private static final class FakeTarget implements FxSurfaceAdapter<String> {
        private final RenderSessionId id;
        private final String engineId;
        private final AtomicInteger layoutCommits = new AtomicInteger();
        private final AtomicInteger presentationCommits = new AtomicInteger();
        private final AtomicInteger revealCalls = new AtomicInteger();
        private volatile boolean worldVisible = true;
        private CameraState camera = new CameraState(1.0d, 0.0d, 0.0d);
        private Consumer<ViewportSnapshot> viewportListener;

        private FakeTarget(RenderSessionId id, String engineId) {
            this.id = id;
            this.engineId = engineId;
        }

        @Override
        public LayoutRequest captureLayout(String snapshot, RenderCaptureContext context) {
            return new LayoutRequest(
                    context.requestId(),
                    id,
                    context.modelRevision(),
                    context.geometryRevision(),
                    engineId,
                    List.of(new LayoutElement("n", 100, 40)),
                    Map.of());
        }

        @Override
        public CompletionStage<Void> commitLayout(
                String snapshot, LayoutPatch patch, RenderCommitContext context) {
            layoutCommits.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> commitPresentation(
                String snapshot, RenderCommitContext context) {
            presentationCommits.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public ViewportSnapshot viewportSnapshot() {
            return new ViewportSnapshot(800, 600, ViewportInsets.none());
        }

        @Override
        public CameraState cameraState() {
            return camera;
        }

        @Override
        public void applyCameraState(CameraState cameraState) {
            camera = cameraState;
        }

        @Override
        public boolean userControlledCamera() {
            return false;
        }

        @Override
        public void prepareInitialFrame() {
            worldVisible = false;
        }

        @Override
        public void revealFrame() {
            worldVisible = true;
            revealCalls.incrementAndGet();
        }

        @Override
        public void setViewportListener(Consumer<ViewportSnapshot> listener) {
            viewportListener = listener;
        }
    }

    private static final class DirectFxExecutor implements FxExecutor {
        @Override
        public CompletionStage<Void> execute(Runnable action) {
            action.run();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> defer(Runnable action) {
            action.run();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public <T> CompletionStage<T> supply(Supplier<T> supplier) {
            return CompletableFuture.completedFuture(supplier.get());
        }

        @Override
        public boolean isFxThread() {
            return true;
        }
    }
}
