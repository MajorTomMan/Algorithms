package com.majortom.algorithms.visualization.render.runtime;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.ContentStyleSnapshot;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderPort;
import com.majortom.algorithms.visualization.render.api.RenderResult;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.RenderStatus;
import com.majortom.algorithms.visualization.render.api.StructuralChange;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.api.ViewportRenderIntent;
import com.majortom.algorithms.visualization.render.diagnostics.RenderTrace;
import com.majortom.algorithms.visualization.render.fx.FxExecutor;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceRegistry;
import com.majortom.algorithms.visualization.render.fx.PulseBarrier;
import com.majortom.algorithms.visualization.render.fx.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.fx.RenderCommitContext;
import com.majortom.algorithms.visualization.render.layout.LayoutEngine;
import com.majortom.algorithms.visualization.render.layout.LayoutEngineRegistry;
import com.majortom.algorithms.visualization.render.viewport.CameraManager;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.render.viewport.CameraState;
import com.majortom.algorithms.visualization.render.viewport.ViewportSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Single-control-plane render runtime. No stage blocks another thread; every boundary is a
 * CompletionStage continuation.
 */
public final class DefaultRenderFramework implements RenderPort, AutoCloseable {
    private static final double MIN_CAMERA_SCALE = 0.10d;
    private static final double MAX_CAMERA_SCALE = 8.0d;

    private final RenderScheduler scheduler;
    private final LayoutExecutor layoutExecutor;
    private final FxExecutor fxExecutor;
    private final PulseBarrier pulseBarrier;
    private final FxSurfaceRegistry surfaces;
    private final LayoutEngineRegistry layoutEngines;
    private final CameraManager cameraManager;
    private final RenderTrace trace;
    private final RenderSessionRegistry sessions = new RenderSessionRegistry();
    private final Map<RenderSessionId, RenderMailbox> mailboxes = new HashMap<>();
    private final AtomicLong transactionSequence = new AtomicLong();
    private final AtomicLong requestSequence = new AtomicLong();
    private ContentStyleSnapshot contentStyle;

    public DefaultRenderFramework(
            RenderScheduler scheduler,
            LayoutExecutor layoutExecutor,
            FxExecutor fxExecutor,
            FxSurfaceRegistry surfaces,
            LayoutEngineRegistry layoutEngines,
            CameraManager cameraManager,
            RenderTrace trace) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.layoutExecutor = Objects.requireNonNull(layoutExecutor, "layoutExecutor");
        this.fxExecutor = Objects.requireNonNull(fxExecutor, "fxExecutor");
        this.pulseBarrier = new PulseBarrier(fxExecutor);
        this.surfaces = Objects.requireNonNull(surfaces, "surfaces");
        this.layoutEngines = Objects.requireNonNull(layoutEngines, "layoutEngines");
        this.cameraManager = Objects.requireNonNull(cameraManager, "cameraManager");
        this.trace = Objects.requireNonNull(trace, "trace");
    }

    public <S> CompletionStage<Void> registerSurface(
            RenderSessionId id, FxSurfaceAdapter<S> adapter) {
        return fxExecutor.execute(
                () -> {
                    surfaces.register(id, adapter);
                    adapter.setViewportListener(
                            viewport ->
                                    submit(
                                            new ViewportRenderIntent(
                                                    id, viewport, CameraPolicy.KEEP)));
                });
    }

    public CompletionStage<Void> unregisterSurface(RenderSessionId id) {
        return fxExecutor.execute(() -> surfaces.unregister(id));
    }

    public CompletionStage<Void> activateSession(RenderSessionId id) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        scheduler.execute(
                () -> {
                    RenderSession session = sessions.getOrCreate(id);
                    session.activate();
                    drain(id);
                    future.complete(null);
                });
        return future;
    }

    public CompletionStage<Void> deactivateSession(RenderSessionId id) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        scheduler.execute(
                () -> {
                    RenderSession session = sessions.getOrCreate(id);
                    session.deactivate();
                    RenderMailbox mailbox = mailboxes.get(id);
                    if (mailbox != null) mailbox.cancelPending();
                    future.complete(null);
                });
        return future;
    }

    @Override
    public CompletionStage<RenderResult> submit(RenderIntent intent) {
        Objects.requireNonNull(intent, "intent");
        CompletableFuture<RenderResult> future = new CompletableFuture<>();
        scheduler.execute(
                () -> {
                    RenderSession session = sessions.getOrCreate(intent.sessionId());
                    RenderMailbox mailbox =
                            mailboxes.computeIfAbsent(
                                    intent.sessionId(), ignored -> new RenderMailbox());
                    long modelRevision = session.modelRevision;
                    long geometryRevision = session.geometryRevision;
                    if (intent instanceof StructuralRenderIntent<?> structural) {
                        // Reserve revisions at submission time. Any newer structural fact or
                        // measurement invalidation immediately makes an older in-flight
                        // transaction logically stale; physical interruption is optional.
                        session.authoritativeTransactionId = -1L;
                        session.latestStructuralSnapshot = structural.snapshot();
                        if (structural.change() == StructuralChange.MODEL) {
                            modelRevision = ++session.modelRevision;
                        } else {
                            geometryRevision = ++session.geometryRevision;
                        }
                    }
                    mailbox.enqueue(
                            new RenderMailbox.Submission(
                                    intent, future, modelRevision, geometryRevision));
                    if (session.active()) drain(intent.sessionId());
                });
        return future;
    }

    /** UI environment input: only geometry-relevant content style reaches the render runtime. */
    public CompletionStage<Void> updateContentStyle(ContentStyleSnapshot next) {
        Objects.requireNonNull(next, "next");
        CompletableFuture<Void> future = new CompletableFuture<>();
        scheduler.execute(
                () -> {
                    if (next.equals(contentStyle)) {
                        future.complete(null);
                        return;
                    }
                    contentStyle = next;
                    java.util.List<CompletionStage<RenderResult>> refreshes =
                            new java.util.ArrayList<>();
                    for (RenderSession session : sessions.values()) {
                        if (!session.active() || session.latestStructuralSnapshot == null) continue;
                        refreshes.add(
                                submit(
                                        new StructuralRenderIntent<>(
                                                session.id,
                                                session.latestStructuralSnapshot,
                                                CameraPolicy.ENSURE_VISIBLE,
                                                false,
                                                StructuralChange.GEOMETRY)));
                    }
                    if (refreshes.isEmpty()) {
                        future.complete(null);
                        return;
                    }
                    CompletableFuture.allOf(
                                    refreshes.stream()
                                            .map(CompletionStage::toCompletableFuture)
                                            .toArray(CompletableFuture[]::new))
                            .whenComplete(
                                    (ignored, failure) -> {
                                        if (failure == null) future.complete(null);
                                        else future.completeExceptionally(failure);
                                    });
                });
        return future;
    }

    public RenderTrace trace() {
        return trace;
    }

    public FxExecutor fxExecutor() {
        return fxExecutor;
    }

    private void drain(RenderSessionId sessionId) {
        RenderSession session = sessions.getOrCreate(sessionId);
        RenderMailbox mailbox =
                mailboxes.computeIfAbsent(sessionId, ignored -> new RenderMailbox());
        if (!session.active() || mailbox.inFlight) return;
        RenderMailbox.Submission submission = mailbox.poll();
        if (submission == null) return;
        mailbox.inFlight = true;
        CompletionStage<RenderResult> stage;
        try {
            stage = process(session, submission);
        } catch (Throwable failure) {
            stage = CompletableFuture.failedFuture(failure);
        }
        stage.whenComplete(
                (result, failure) ->
                        scheduler.execute(
                                () -> {
                                    mailbox.inFlight = false;
                                    if (failure != null) {
                                        submission
                                                .future()
                                                .complete(
                                                        new RenderResult(
                                                                RenderStatus.FAILED,
                                                                sessionId,
                                                                session.committedRevision,
                                                                false,
                                                                session.layout == null
                                                                        ? BoundsSnapshot.empty()
                                                                        : session.layout.bounds(),
                                                                failure));
                                    } else {
                                        submission.future().complete(result);
                                    }
                                    drain(sessionId);
                                }));
    }

    private CompletionStage<RenderResult> process(
            RenderSession session, RenderMailbox.Submission submission) {
        RenderIntent intent = submission.intent();
        if (intent instanceof StructuralRenderIntent<?> structural) {
            return processStructural(
                    session, structural, submission.modelRevision(), submission.geometryRevision());
        }
        if (intent instanceof PresentationRenderIntent<?> presentation)
            return processPresentation(session, presentation);
        if (intent instanceof ViewportRenderIntent viewport)
            return processViewport(session, viewport);
        return CompletableFuture.failedFuture(
                new IllegalArgumentException("Unsupported render intent: " + intent));
    }

    private <S> CompletionStage<RenderResult> processStructural(
            RenderSession session,
            StructuralRenderIntent<S> intent,
            long modelRevision,
            long geometryRevision) {
        long generation = session.generation;
        long transactionId = transactionSequence.incrementAndGet();
        session.authoritativeTransactionId = transactionId;
        RenderTransaction transaction =
                new RenderTransaction(
                        transactionId,
                        session.id,
                        intent.kind(),
                        generation,
                        modelRevision,
                        geometryRevision,
                        System.nanoTime());
        trace(transaction, RenderPipeline.CAPTURE);

        CompletionStage<Void> prepareStage =
                intent.initialFrame()
                        ? fxExecutor.execute(
                                () -> surfaces.<S>require(session.id).prepareInitialFrame())
                        : CompletableFuture.completedFuture(null);

        RenderCaptureContext captureContext =
                new RenderCaptureContext(
                        transactionId,
                        session.id,
                        generation,
                        modelRevision,
                        geometryRevision,
                        new com.majortom.algorithms.visualization.render.api.LayoutRequestId(
                                requestSequence.incrementAndGet()),
                        intent.initialFrame());

        return prepareStage
                .thenCompose(
                        ignored ->
                                fxExecutor.supply(
                                        () ->
                                                surfaces.<S>require(session.id)
                                                        .captureLayout(
                                                                intent.snapshot(), captureContext)))
                .thenCompose(
                        request ->
                                onScheduler(
                                        () ->
                                                continueStructuralAfterCapture(
                                                        session, transaction, intent, request)))
                .thenCompose(stage -> stage);
    }

    private <S> CompletionStage<RenderResult> continueStructuralAfterCapture(
            RenderSession session,
            RenderTransaction transaction,
            StructuralRenderIntent<S> intent,
            LayoutRequest request) {
        if (!authoritative(session, transaction)) return completedCancelled(session);
        trace(transaction, RenderPipeline.DESCRIBE);

        boolean layoutRequired =
                session.layout == null
                        || session.lastLayoutRequest == null
                        || !sameGeometryInput(session.lastLayoutRequest, request);
        CompletionStage<LayoutResult> layoutStage;
        if (layoutRequired) {
            LayoutEngine engine = layoutEngines.require(request.engineId());
            trace.layoutInvoked(session.id);
            trace(transaction, RenderPipeline.WAIT_LAYOUT);
            layoutStage = layoutExecutor.submit(engine, request);
        } else {
            layoutStage =
                    CompletableFuture.completedFuture(
                            session.layout.withModelRevision(transaction.modelRevision()));
        }

        return layoutStage
                .thenCompose(
                        layoutResult ->
                                onScheduler(
                                        () ->
                                                continueStructuralAfterLayout(
                                                        session,
                                                        transaction,
                                                        intent,
                                                        request,
                                                        layoutResult,
                                                        layoutRequired)))
                .thenCompose(stage -> stage);
    }

    private <S> CompletionStage<RenderResult> continueStructuralAfterLayout(
            RenderSession session,
            RenderTransaction transaction,
            StructuralRenderIntent<S> intent,
            LayoutRequest request,
            LayoutResult layoutResult,
            boolean layoutChanged) {
        if (!authoritative(session, transaction)) return completedCancelled(session);
        trace(transaction, RenderPipeline.RESOLVE_LAYOUT);
        LayoutPatch patch = LayoutPatch.from(layoutResult);
        long nextLayoutRevision = session.layoutRevision + (layoutChanged ? 1L : 0L);
        RenderCommitContext commitContext =
                new RenderCommitContext(
                        transaction.id(),
                        session.id,
                        session.generation,
                        transaction.modelRevision(),
                        nextLayoutRevision,
                        session.presentationRevision,
                        intent.initialFrame());
        trace(transaction, RenderPipeline.WAIT_APPLY);

        return fxExecutor
                .supply(
                        () ->
                                surfaces.<S>require(session.id)
                                        .commitLayout(intent.snapshot(), patch, commitContext))
                .thenCompose(stage -> stage)
                .thenCompose(ignored -> pulseBarrier.await())
                .thenCompose(
                        ignored ->
                                onScheduler(
                                        () ->
                                                continueStructuralAfterApply(
                                                        session,
                                                        transaction,
                                                        intent,
                                                        request,
                                                        layoutResult,
                                                        layoutChanged)))
                .thenCompose(stage -> stage);
    }

    private <S> CompletionStage<RenderResult> continueStructuralAfterApply(
            RenderSession session,
            RenderTransaction transaction,
            StructuralRenderIntent<S> intent,
            LayoutRequest request,
            LayoutResult layoutResult,
            boolean layoutChanged) {
        if (!authoritative(session, transaction)) return completedCancelled(session);
        trace(transaction, RenderPipeline.WAIT_PULSE);
        trace(transaction, RenderPipeline.CAMERA);
        return fxExecutor
                .supply(
                        () -> {
                            FxSurfaceAdapter<S> target = surfaces.require(session.id);
                            ViewportSnapshot viewport = target.viewportSnapshot();
                            CameraState current = target.cameraState();
                            CameraState resolved =
                                    cameraManager.resolve(
                                            intent.cameraPolicy(),
                                            layoutResult.bounds(),
                                            viewport,
                                            current,
                                            session.camera,
                                            target.userControlledCamera(),
                                            MIN_CAMERA_SCALE,
                                            MAX_CAMERA_SCALE);
                            if (!resolved.equals(current)) target.applyCameraState(resolved);
                            if (intent.initialFrame()) target.revealFrame();
                            return resolved;
                        })
                .thenCompose(
                        camera ->
                                onScheduler(
                                        () -> {
                                            if (!authoritative(session, transaction))
                                                return RenderResult.cancelled(session.id);
                                            session.lastLayoutRequest = request;
                                            session.layout = layoutResult;
                                            if (layoutChanged) session.layoutRevision++;
                                            session.camera = camera;
                                            session.committedRevision = transaction.modelRevision();
                                            trace(transaction, RenderPipeline.COMMIT);
                                            trace(transaction, RenderPipeline.PRESENTED);
                                            return new RenderResult(
                                                    RenderStatus.PRESENTED,
                                                    session.id,
                                                    session.committedRevision,
                                                    layoutChanged,
                                                    layoutResult.bounds(),
                                                    null);
                                        }));
    }

    private <S> CompletionStage<RenderResult> processPresentation(
            RenderSession session, PresentationRenderIntent<S> intent) {
        long transactionId = transactionSequence.incrementAndGet();
        long generation = session.generation;
        long modelRevision = session.modelRevision;
        RenderTransaction transaction =
                new RenderTransaction(
                        transactionId,
                        session.id,
                        intent.kind(),
                        generation,
                        modelRevision,
                        session.geometryRevision,
                        System.nanoTime());
        long presentationRevision = ++session.presentationRevision;
        RenderCommitContext context =
                new RenderCommitContext(
                        transactionId,
                        session.id,
                        generation,
                        modelRevision,
                        session.layoutRevision,
                        presentationRevision,
                        false);
        trace(transaction, RenderPipeline.WAIT_APPLY);
        return fxExecutor
                .supply(
                        () ->
                                surfaces.<S>require(session.id)
                                        .commitPresentation(intent.snapshot(), context))
                .thenCompose(stage -> stage)
                .thenCompose(
                        ignored ->
                                onScheduler(
                                        () -> {
                                            if (session.generation != generation
                                                    || !session.active())
                                                return RenderResult.cancelled(session.id);
                                            session.committedRevision =
                                                    Math.max(
                                                            session.committedRevision,
                                                            modelRevision);
                                            trace(transaction, RenderPipeline.PRESENTED);
                                            return new RenderResult(
                                                    RenderStatus.PRESENTED,
                                                    session.id,
                                                    modelRevision,
                                                    false,
                                                    session.layout == null
                                                            ? BoundsSnapshot.empty()
                                                            : session.layout.bounds(),
                                                    null);
                                        }));
    }

    private CompletionStage<RenderResult> processViewport(
            RenderSession session, ViewportRenderIntent intent) {
        session.viewport = intent.viewport();
        session.viewportRevision++;
        long generation = session.generation;
        long transactionId = transactionSequence.incrementAndGet();
        RenderTransaction transaction =
                new RenderTransaction(
                        transactionId,
                        session.id,
                        intent.kind(),
                        generation,
                        session.modelRevision,
                        session.geometryRevision,
                        System.nanoTime());
        if (session.layout == null) {
            return CompletableFuture.completedFuture(
                    new RenderResult(
                            RenderStatus.NO_OP,
                            session.id,
                            session.modelRevision,
                            false,
                            BoundsSnapshot.empty(),
                            null));
        }
        if (intent.cameraPolicy() == CameraPolicy.KEEP) {
            return fxExecutor
                    .supply(() -> surfaces.<Object>require(session.id).cameraState())
                    .thenCompose(
                            camera ->
                                    onScheduler(
                                            () -> {
                                                if (session.generation != generation
                                                        || !session.active())
                                                    return RenderResult.cancelled(session.id);
                                                session.camera = camera;
                                                return new RenderResult(
                                                        RenderStatus.NO_OP,
                                                        session.id,
                                                        session.modelRevision,
                                                        false,
                                                        session.layout.bounds(),
                                                        null);
                                            }));
        }
        return fxExecutor
                .supply(
                        () -> {
                            FxSurfaceAdapter<Object> target = surfaces.require(session.id);
                            CameraState current = target.cameraState();
                            CameraState resolved =
                                    cameraManager.resolve(
                                            intent.cameraPolicy(),
                                            session.layout.bounds(),
                                            intent.viewport(),
                                            current,
                                            session.camera,
                                            target.userControlledCamera(),
                                            MIN_CAMERA_SCALE,
                                            MAX_CAMERA_SCALE);
                            if (!resolved.equals(current)) target.applyCameraState(resolved);
                            return resolved;
                        })
                .thenCompose(
                        camera ->
                                onScheduler(
                                        () -> {
                                            if (session.generation != generation
                                                    || !session.active())
                                                return RenderResult.cancelled(session.id);
                                            session.camera = camera;
                                            return new RenderResult(
                                                    RenderStatus.PRESENTED,
                                                    session.id,
                                                    session.modelRevision,
                                                    false,
                                                    session.layout.bounds(),
                                                    null);
                                        }));
    }

    private boolean authoritative(RenderSession session, RenderTransaction transaction) {
        return session.active()
                && session.generation == transaction.generation()
                && session.modelRevision == transaction.modelRevision()
                && session.geometryRevision == transaction.geometryRevision()
                && session.authoritativeTransactionId == transaction.id();
    }

    private static boolean sameGeometryInput(LayoutRequest previous, LayoutRequest current) {
        return previous.engineId().equals(current.engineId())
                && previous.geometryRevision() == current.geometryRevision()
                && previous.elements().equals(current.elements())
                && previous.links().equals(current.links())
                && previous.metadata().equals(current.metadata());
    }

    private CompletionStage<RenderResult> completedCancelled(RenderSession session) {
        return CompletableFuture.completedFuture(RenderResult.cancelled(session.id));
    }

    private <T> CompletionStage<T> onScheduler(java.util.concurrent.Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        scheduler.execute(
                () -> {
                    try {
                        future.complete(task.call());
                    } catch (Throwable failure) {
                        future.completeExceptionally(failure);
                    }
                });
        return future;
    }

    private void trace(RenderTransaction transaction, RenderPipeline stage) {
        trace.stage(
                transaction.sessionId(),
                transaction.id(),
                stage.name(),
                transaction.generation(),
                transaction.modelRevision());
    }

    @Override
    public void close() {
        layoutExecutor.close();
        scheduler.close();
    }
}
