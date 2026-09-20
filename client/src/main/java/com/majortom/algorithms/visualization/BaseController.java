package com.majortom.algorithms.visualization;

import com.majortom.algorithms.visualization.render.fx.FxDispatch;
import com.majortom.algorithms.visualization.render.api.RenderPort;
import com.majortom.algorithms.visualization.render.api.PresentationCursorPort;
import com.majortom.algorithms.visualization.render.api.StructurePresenter;
import com.majortom.algorithms.visualization.render.fx.RenderSurface;
import com.majortom.algorithms.visualization.render.runtime.RenderSurfaceHost;
import com.majortom.algorithms.visualization.render.runtime.StructureRenderDriver;
import com.majortom.algorithms.visualization.render.api.PresentationCursor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.majortom.algorithms.telemetry.api.TelemetryScopeId;
import com.majortom.algorithms.telemetry.memory.api.MemoryCapabilities;
import com.majortom.algorithms.telemetry.memory.api.MemoryFacts;
import com.majortom.algorithms.telemetry.memory.analysis.MemoryAllocationAnalysis;
import com.majortom.algorithms.telemetry.memory.analysis.StructureFootprint;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.EventSink;
import com.majortom.algorithms.core.runtime.ExecutionAnchorRecorder;
import com.majortom.algorithms.core.runtime.ExecutionAnchorTimeline;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.logging.LogEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.runtime.ExecutionStatus;
import com.majortom.algorithms.core.runtime.ResourceUsage;
import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.logging.LogView;
import com.majortom.algorithms.visualization.metrics.RuntimeMetricTracker;
import com.majortom.algorithms.visualization.metrics.RuntimeOverviewModel;
import com.majortom.algorithms.visualization.metrics.RuntimeOverviewService;
import com.majortom.algorithms.visualization.metrics.RuntimeOverviewText;
import com.majortom.algorithms.core.runtime.ExecutionOperation;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.core.runtime.ExecutionSummary;
import com.majortom.algorithms.core.runtime.ExecutionTiming;
import com.majortom.algorithms.core.runtime.RunControl;
import com.majortom.algorithms.core.timeline.Timeline;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.execution.ClientExecutionRecord;
import com.majortom.algorithms.visualization.execution.ClientExecutionService;
import com.majortom.algorithms.visualization.execution.ExecutionHandle;
import com.majortom.algorithms.visualization.execution.ExecutionExportCodec;
import com.majortom.algorithms.visualization.execution.ExecutionExporter;
import com.majortom.algorithms.visualization.execution.InMemoryRunHistoryService;
import com.majortom.algorithms.visualization.execution.InputFingerprint;
import com.majortom.algorithms.visualization.execution.JacksonSha256InputFingerprint;
import com.majortom.algorithms.visualization.execution.JsonExecutionExporter;
import com.majortom.algorithms.visualization.execution.LocalClientExecutionService;
import com.majortom.algorithms.visualization.execution.RunHistoryPolicy;
import com.majortom.algorithms.visualization.execution.RunHistoryService;
import com.majortom.algorithms.visualization.runtime.PlaybackController;
import com.majortom.algorithms.visualization.runtime.ReducedEventTimeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/** Shared JavaFX shell around the UI-neutral registry/runtime/event pipeline. */
public abstract class BaseController<S> implements Initializable {

    private static final long LIVE_STATS_REFRESH_INTERVAL_NANOS = 50_000_000L;
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ComponentRegistry COMPONENTS = ComponentDiscovery.discover();
    private static final RunHistoryService DEFAULT_EXECUTION_HISTORY =
            new InMemoryRunHistoryService(RunHistoryPolicy.desktopDefault());
    private static final InputFingerprint DEFAULT_INPUT_FINGERPRINT =
            new JacksonSha256InputFingerprint(JSON_MAPPER);
    private static final ExecutionExportCodec DEFAULT_EXPORT_CODEC = new ExecutionExportCodec(JSON_MAPPER);
    private static final ExecutionExporter DEFAULT_EXECUTION_EXPORTER =
            new JsonExecutionExporter(java.nio.file.Path.of("exports"), JSON_MAPPER, DEFAULT_EXPORT_CODEC);
    private static final RuntimeOverviewService RUNTIME_OVERVIEW = new RuntimeOverviewService();

    protected final DoubleProperty delayMs = new SimpleDoubleProperty(50.0d);
    protected ExecutionStatistics stats = ExecutionStatistics.empty();
    protected final BaseVisualizer<S> visualizer;
    private final RenderSurfaceHost renderSurfaceHost;
    private final RenderSurface<S> renderSurface;
    private final StructureRenderDriver<S> renderDriver;
    private final PresentationCursorPort presentationCursorPort;

    protected Label statsLabel;
    private Runnable statisticsRefresh = () -> {};
    protected LogView logView;
    protected LogView structureLogView;
    private final ControllerLogChannels logChannels = new ControllerLogChannels();
    protected Slider delaySlider;
    protected Slider timelineSlider;
    protected HBox customControlBox;
    protected Button startBtn;
    protected Button pauseBtn;
    protected Button resetBtn;
    protected Button replayBtn;
    protected Button stepBackwardBtn;
    protected Button stepForwardBtn;
    protected Button exportBtn;
    protected Button compareBtn;

    private final ClientExecutionService execution;
    private final ControllerExecutionArchive executionArchive;
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final BooleanProperty paused = new SimpleBooleanProperty(false);
    private final LongProperty structureRevision = new SimpleLongProperty();
    private final ObjectProperty<EventEnvelope> presentationEvent = new SimpleObjectProperty<>();
    private int presentationEventIndex = -1;
    private int liveEventIndex = -1;
    private final Timeline structureTimeline = new Timeline();
    private S latestViewState;
    private long liveVisualFrameCount;
    private S latestStructureState;
    private ExecutionHandle currentSession;
    private ClientExecutionRecord lastExecution;
    private ExecutionAnchorTimeline lastExecutionAnchors;
    private ExecutionAnchorTimeline lastStructureExecutionAnchors;
    private ReducedEventTimeline<S> lastTimeline;
    private PlaybackController<S> replayController;
    private final ControllerReplayControls<S> replayControls;
    private long lastLiveStatsRefreshNanos;
    private final RuntimeMetricTracker runtimeMetricTracker = new RuntimeMetricTracker();
    private final ControllerMemoryProfile memoryProfile = new ControllerMemoryProfile(this::refreshStatsDisplay);
    private boolean disposed;

    protected BaseController(
            BaseVisualizer<S> visualizer,
            StructurePresenter<S> presenter,
            RenderPort renderPort,
            RenderSurfaceHost renderSurfaceHost) {
        this(
                visualizer,
                presenter,
                renderPort,
                renderSurfaceHost,
                new LocalClientExecutionService(),
                DEFAULT_EXECUTION_HISTORY,
                DEFAULT_INPUT_FINGERPRINT,
                DEFAULT_EXECUTION_EXPORTER);
    }

    protected BaseController(
            BaseVisualizer<S> visualizer,
            StructurePresenter<S> presenter,
            RenderPort renderPort,
            RenderSurfaceHost renderSurfaceHost,
            ClientExecutionService execution,
            RunHistoryService executionHistory,
            InputFingerprint inputFingerprint,
            ExecutionExporter executionExporter) {
        this.visualizer = visualizer;
        this.replayControls = new ControllerReplayControls<>(visualizer, running::get,
                this::hasExecutionData, this::updatePlaybackSpeed, this::seekTimelineDuringDrag);
        this.renderSurfaceHost = Objects.requireNonNull(renderSurfaceHost, "renderSurfaceHost");
        if (visualizer == null) {
            this.renderSurface = null;
            this.renderDriver = null;
        } else {
            this.renderSurface = new RenderSurface<>(visualizer.sessionId(), visualizer.structureVisualization(), visualizer, visualizer.fxSurfaceAdapter());
            this.renderDriver = new StructureRenderDriver<>(
                    visualizer.sessionId(),
                    Objects.requireNonNull(renderPort, "renderPort"),
                    Objects.requireNonNull(presenter, "presenter"));
        }
        this.presentationCursorPort = renderPort instanceof PresentationCursorPort cursorPort
                ? cursorPort
                : null;
        this.execution = Objects.requireNonNull(execution, "execution");
        this.executionArchive = new ControllerExecutionArchive(executionHistory, inputFingerprint,
                executionExporter, this::appendLog, this::handleAlgorithmError);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupI18n();
    }

    protected final void startAlgorithm(
            String algorithmId,
            Object input,
            ExecutionOperation<?> operation,
            Supplier<? extends EventReducer<S>> reducerFactory) {
        if (disposed) {
            throw new IllegalStateException("Controller is disposed");
        }
        stopAlgorithm();
        activateAlgorithmLog(algorithmId);
        clearExecutionState();
        liveVisualFrameCount = 0L;
        liveEventIndex = -1;
        presentationEventIndex = -1;
        lastLiveStatsRefreshNanos = 0L;
        refreshStatsDisplay();

        EventReducer<S> liveReducer = reducerFactory.get();
        runtimeMetricTracker.reset();
        runtimeMetricTracker.observe(RUNTIME_OVERVIEW.structures(), structureLogScopeId(), liveReducer.initialState());
        running.set(true);
        paused.set(false);
        updatePlaybackButtonState();
        appendLog("Started: " + algorithmId);

        TelemetryScopeId memoryScope = TelemetryScopeId.algorithm(structureLogScopeId(), algorithmId);
        ExecutionOperation<?> profiledOperation = profileOperation(
                memoryScope,
                operation,
                true);
        currentSession = execution.start(
                algorithmId,
                profiledOperation,
                liveReducer,
                this::consumeLiveEvent,
                this::renderLiveState,
                this::updateLiveStatistics,
                replayControls::liveDelayMillis);
        ExecutionHandle session = currentSession;
        session.presentationCompletion().whenComplete((result, error) -> FxDispatch.defer(
                () -> finishExecution(session, algorithmId, input, reducerFactory, result, error)));
    }

    protected final <T> T structure(String id, Class<T> contract) {
        return COMPONENTS.createStructure(id, contract);
    }

    protected final AlgorithmDescriptor algorithm(String id, Class<?> valueType) {
        return COMPONENTS.requireAlgorithm(StructureModule.fromId(moduleId()), valueType, id);
    }

    /** Executes one editable structure mutation through the shared Runtime and records its event history. */
    protected final boolean executeStructureOperation(String operationId, ExecutionOperation<?> operation) {
        if (disposed) {
            throw new IllegalStateException("Controller is disposed");
        }
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(operation, "operation");
        invalidateExecutionForStructureChange();
        int eventStart = structureTimeline.events().size();
        String runtimeOperationId = "structure." + moduleId() + "." + operationId;
        ExecutionOperation<?> profiledOperation = profileOperation(
                TelemetryScopeId.structure(structureLogScopeId(), operationId),
                operation,
                false);
        ExecutionAnchorRecorder executionAnchorRecorder = new ExecutionAnchorRecorder();
        EventSink structureEventSink = event -> {
            structureTimeline.accept(event);
            executionAnchorRecorder.accept(event);
            publishPresentationCursor(PresentationCursor.completedEvent(
                    event.runId(), event.sequence(), PresentationCursor.Mode.LIVE));
        };
        ExecutionResult result = new ExecutionRuntime().execute(
                runtimeOperationId, moduleId(), structureEventSink, RunControl.unrestricted(), profiledOperation);
        lastStructureExecutionAnchors = executionAnchorRecorder.snapshot().orElse(null);
        logChannels.appendStructureEventsSince(structureTimeline.events(), eventStart);
        if (result.status() == ExecutionStatus.COMPLETED) {
            return true;
        }
        String message = result.failure().map(failure -> failure.message()).orElse("Structure operation failed");
        appendStructureLog(message);
        return false;
    }

    /** Records a non-mutation auxiliary event such as snapshot lifecycle state. */
    public final boolean recordAuxiliaryEvent(String operationId, ExecutionEvent event) {
        Objects.requireNonNull(event, "event");
        int eventStart = structureTimeline.events().size();
        String runtimeOperationId = "structure." + moduleId() + "." + operationId;
        ExecutionResult result = new ExecutionRuntime().execute(
                runtimeOperationId, moduleId(), structureTimeline, RunControl.unrestricted(), () -> {
                    ExecutionEvents.emit(event);
                    return null;
                });
        logChannels.appendStructureEventsSince(structureTimeline.events(), eventStart);
        return result.status() == ExecutionStatus.COMPLETED;
    }

    private ExecutionOperation<?> profileOperation(TelemetryScopeId scope, ExecutionOperation<?> operation,
            boolean exposeAsActiveAlgorithm) {
        return memoryProfile.profileOperation(scope, operation, exposeAsActiveAlgorithm);
    }

    /** Returns the complete structure-operation history retained by this controller. */
    public final List<EventEnvelope> structureEvents() {
        return structureTimeline.events();
    }

    public final void stopAlgorithm() {
        stopReplay();
        if (currentSession != null) {
            currentSession.close();
        }
        currentSession = null;
        running.set(false);
        paused.set(false);
        updatePlaybackButtonState();
    }

    public final void togglePause() {
        if (replayController != null && !running.get()) {
            if (replayController.isPlaying()) {
                replayController.pause();
                if (visualizer != null) visualizer.pauseAnimations();
                paused.set(true);
                republishCurrentPresentationCursor(PresentationCursor.Mode.REPLAY);
            } else {
                replayController.play();
                if (visualizer != null) visualizer.resumeAnimations();
                paused.set(false);
                republishCurrentPresentationCursor(PresentationCursor.Mode.REPLAY);
            }
            refreshStatsDisplay();
            return;
        }
        if (currentSession == null || !running.get()) {
            return;
        }
        if (paused.get()) {
            currentSession.resumeExecution();
            if (visualizer != null) visualizer.resumeAnimations();
            paused.set(false);
            republishCurrentPresentationCursor(PresentationCursor.Mode.LIVE);
        } else {
            currentSession.pauseExecution();
            if (visualizer != null) visualizer.pauseAnimations();
            paused.set(true);
            republishCurrentPresentationCursor(PresentationCursor.Mode.LIVE);
        }
        updatePlaybackButtonState();
    }

    public final void toggleReplay() {
        if (!hasExecutionData() || running.get()) {
            return;
        }
        if (replayController.isPlaying()) {
            replayController.pause();
            if (visualizer != null) visualizer.pauseAnimations();
            paused.set(true);
            republishCurrentPresentationCursor(PresentationCursor.Mode.REPLAY);
            refreshStatsDisplay();
            return;
        }
        if (visualizer != null) visualizer.resumeAnimations();
        if (replayController.currentIndex() + 1 >= replayController.frameCount()) {
            replayController.restart();
            if (replayController.stepForward()) {
                syncTimelineSlider(replayController.currentIndex(), replayController.frameCount());
            }
        }
        replayController.play();
        paused.set(false);
        republishCurrentPresentationCursor(PresentationCursor.Mode.REPLAY);
        refreshStatsDisplay();
    }

    /** Advances one replay frame while leaving playback paused. */
    public final boolean stepForward() {
        if (running.get()) {
            if (currentSession == null || !paused.get()) {
                return false;
            }
            if (visualizer != null) visualizer.stepAnimations();
            currentSession.stepExecution();
            return true;
        }
        if (!hasExecutionData()) {
            return false;
        }
        boolean canAdvance = replayController.currentIndex() + 1 < replayController.frameCount();
        if (canAdvance && visualizer != null) visualizer.stepAnimations();
        boolean advanced = replayController.stepForward();
        paused.set(true);
        if (advanced) {
            syncTimelineSlider(replayController.currentIndex(), replayController.frameCount());
        }
        refreshStatsDisplay();
        return advanced;
    }

    /** Rewinds one replay frame while leaving playback paused. */
    public final boolean stepBackward() {
        if (!hasExecutionData() || running.get()) {
            return false;
        }
        boolean canRewind = replayController.currentIndex() > 0;
        if (canRewind && visualizer != null) visualizer.stepAnimations();
        boolean rewound = replayController.stepBackward();
        paused.set(true);
        if (rewound) {
            syncTimelineSlider(replayController.currentIndex(), replayController.frameCount());
        }
        refreshStatsDisplay();
        return rewound;
    }

    /** Moves the replay cursor to the first available frame and leaves playback paused. */
    public final boolean jumpToStart() {
        if (!hasExecutionData() || running.get() || replayController == null) {
            return false;
        }
        stopReplay();
        beginScrubbing();
        long generation = replayControls.scrubGeneration();
        boolean moved = seekReplayFrame(0);
        releaseScrubbingAfterQueuedRender(generation);
        paused.set(true);
        if (moved) {
            syncTimelineSlider(0, replayController.frameCount());
        }
        refreshStatsDisplay();
        return moved;
    }

    /** Moves the replay cursor to the last available frame and leaves playback paused. */
    public final boolean jumpToEnd() {
        if (!hasExecutionData() || running.get() || replayController == null) {
            return false;
        }
        stopReplay();
        beginScrubbing();
        long generation = replayControls.scrubGeneration();
        int last = Math.max(0, replayController.frameCount() - 1);
        boolean moved = seekReplayFrame(last);
        releaseScrubbingAfterQueuedRender(generation);
        paused.set(true);
        if (moved) {
            syncTimelineSlider(last, replayController.frameCount());
        }
        refreshStatsDisplay();
        return moved;
    }

    /**
     * Requests a graceful end of the active execution while preserving the
     * events already produced. The normal completion path converts them into
     * a replayable timeline.
     */
    public final void endAlgorithm() {
        stopReplay();
        if (currentSession != null && running.get()) {
            currentSession.close();
        }
    }

    /** Clears the current execution/replay session without resetting editable structure data. */
    public final void closeExecution() {
        stopAlgorithm();
        clearExecutionState();
        restoreStructureState();
        refreshStatsDisplay();
    }

    public final void seekTimeline(double progress) {
        if (!hasExecutionData()) {
            return;
        }
        boolean dragActive = replayControls.isSliderChanging();
        if (!dragActive) {
            beginScrubbing();
        }
        seekTimelineDuringDrag(progress);
        if (!dragActive) {
            releaseScrubbingAfterQueuedRender(replayControls.scrubGeneration());
        }
    }

    private void seekTimelineDuringDrag(double progress) {
        if (!hasExecutionData()) {
            return;
        }
        stopReplay();
        int size = lastTimeline.size();
        int index = (int) Math.round(progress * (size - 1));
        index = Math.max(0, Math.min(size - 1, index));
        if (!seekReplayFrame(index)) {
            return;
        }
        paused.set(true);
        syncTimelineSlider(index, size);
    }

    private void beginScrubbing() { replayControls.beginScrubbing(); }

    private void releaseScrubbingAfterQueuedRender(long generation) {
        replayControls.releaseScrubbingAfterQueuedRender(generation);
    }

    public final boolean hasExecutionData() {
        return hasPlaybackData();
    }

    /** Returns whether a visible timeline can be replayed, even for an incomplete run. */
    public final boolean hasPlaybackData() {
        return lastTimeline != null && !lastTimeline.isEmpty() && replayController != null;
    }

    /** Returns whether the run has a validated, terminal execution record. */
    public final boolean hasExecutionRecord() {
        return lastExecution != null;
    }

    public final void exportExecution() {
        executionArchive.export(lastExecution, executionSummary());
    }

    public final void compareExecutions() {
        executionArchive.compare(lastExecution);
    }

    private void finishExecution(
            ExecutionHandle session,
            String algorithmId,
            Object input,
            Supplier<? extends EventReducer<S>> reducerFactory,
            ExecutionResult result,
            Throwable error) {
        if (session != currentSession) {
            return;
        }
        List<EventEnvelope> events = session.events();
        lastExecutionAnchors = session.executionAnchors().orElse(null);
        session.closeObserver();
        session.close();
        running.set(false);
        paused.set(false);
        updatePlaybackButtonState();
        currentSession = null;
        EventReducer<S> reducer = reducerFactory.get();
        ReducedEventTimeline<S> timeline = new ReducedEventTimeline<>(events, reducer);
        stats = timeline.statistics();
        Duration eventSpan = stats.eventSpan();
        ExecutionSummary summary = ExecutionSummary.from(stats, session.resourceUsage()).withTiming(
                ExecutionTiming.of(eventSpan, session.totalDuration()));
        lastExecution = executionArchive.createRecord(
                moduleId(), algorithmId, input, result, error, summary, events, lastExecutionAnchors, timeline.size());
        lastTimeline = timeline;
        replacePlaybackController(reducer, events);
        if (lastExecution != null) {
            executionArchive.retain(lastExecution);
        }
        prepareTimelineControls();
        if (!timeline.isEmpty()) {
            int lastFrame = timeline.size() - 1;
            if (seekReplayFrame(lastFrame)) {
                syncTimelineSlider(lastFrame, timeline.size());
                refreshPresentationSelection();
            }
        }
        if (error != null) {
            Throwable cause = error;
            if (error instanceof CompletionException && error.getCause() != null) {
                cause = error.getCause();
            }
            handleAlgorithmError(cause);
        } else if (result != null && result.failure().isPresent()) {
            appendLog("Runtime Error: " + result.failure().get().message());
        } else if (result != null && result.status() == ExecutionStatus.CANCELLED) {
            appendLog("Cancelled: " + algorithmId);
        } else {
            onAlgorithmFinished(result);
        }
        refreshStatsDisplay();
    }

    private void consumeLiveEvent(EventEnvelope envelope) {
        Runnable task = () -> {
            int eventIndex = ++liveEventIndex;
            if (!(envelope.event() instanceof LogEvent)) {
                presentationEventIndex = eventIndex;
                presentationEvent.set(envelope);
            }
            if (envelope.event() instanceof LogEvent logEvent) {
                logChannels.appendAlgorithmEvent(logEvent, envelope.timestamp());
            }
            publishPresentationCursor(PresentationCursor.completedEvent(
                    envelope.runId(), envelope.sequence(), PresentationCursor.Mode.LIVE));
        };
        if (FxDispatch.isFxThread()) {
            task.run();
        } else {
            FxDispatch.defer(task);
        }
    }

    private void renderLiveState(S state) {
        liveVisualFrameCount++;
        runtimeMetricTracker.observe(RUNTIME_OVERVIEW.structures(), structureLogScopeId(), state);
        renderViewState(state);
    }

    private void renderState(S state) {
        renderViewState(state);
    }

    protected final long visualFrameCount() {
        if (running.get()) {
            return liveVisualFrameCount;
        }
        if (lastTimeline == null) {
            return 0L;
        } else {
            return lastTimeline.size();
        }
    }

    /**
     * Updates the module statistics from the live reduction cursor. The
     * callback is delivered through the execution service's UI dispatcher,
     * just like the corresponding view-state callback.
     */
    private void updateLiveStatistics(ExecutionStatistics liveStatistics) {
        if (!running.get()) {
            return;
        }
        stats = Objects.requireNonNull(liveStatistics, "liveStatistics");
        long now = System.nanoTime();
        if (lastLiveStatsRefreshNanos == 0L
                || now - lastLiveStatsRefreshNanos >= LIVE_STATS_REFRESH_INTERVAL_NANOS) {
            lastLiveStatsRefreshNanos = now;
            refreshStatsDisplay();
        }
    }

    /** Stores the semantic state separately from the visualizer's drawing cache. */
    protected final void renderViewState(S state) {
        if (state == null) {
            return;
        }
        latestViewState = state;
        if (visualizer != null) {
            renderDriver.render(state);
        }
        onPresentationStateChanged(state);
    }

    /** Called whenever the visible Algorithm presentation state advances or seeks to another frame. */
    protected void onPresentationStateChanged(S state) {
    }

    /** Re-resolves a persistent user selection against the currently visible Algorithm frame. */
    private void refreshPresentationSelection() {
        if (latestViewState != null) {
            onPresentationStateChanged(latestViewState);
        }
    }

    /** Renders a transient read-only preview without changing structure or algorithm cursors. */
    protected final void renderPreviewState(S state) {
        if (state != null && visualizer != null) {
            renderDriver.render(state);
        }
    }

    /** Stores an editable structure state without changing the algorithm cursor. */
    protected final void renderStructureState(S state) {
        storeStructureState(state);
        if (state != null && visualizer != null) {
            renderDriver.render(state);
        }
    }

    /** Updates the editable structure while keeping the current Algorithm visual frame on screen. */
    protected final void storeStructureState(S state) {
        if (state == null) {
            return;
        }
        latestStructureState = state;
        structureRevision.set(structureRevision.get() + 1L);
    }

    /** Restores the structure page's state into the shared visualizer. */
    public final void showStructureState() {
        restoreStructureState();
    }

    /** Restores the latest algorithm frame or the module's selected algorithm-input preview. */
    public final void showAlgorithmState() {
        restoreAlgorithmState();
    }

    /** Hook for modules that can preview a selected structure snapshot before execution starts. */
    protected void restoreAlgorithmState() {
        if (latestViewState != null && visualizer != null) {
            renderDriver.render(latestViewState);
        }
    }

    /** Hook for modules that need to rebuild their structure projection. */
    protected void restoreStructureState() {
        if (latestStructureState != null && visualizer != null) {
            renderDriver.render(latestStructureState);
            return;
        }
        if (latestViewState != null && visualizer != null) {
            renderDriver.render(latestViewState);
        }
    }

    /** Requests a presentation-only commit after renderer-local UI state changes. */
    protected final void requestPresentationRender() {
        if (renderDriver != null) renderDriver.requestPresentation();
    }

    /** Returns the latest reducer state observed by this module. */
    protected final S latestViewState() {
        return latestViewState;
    }

    /** Returns the latest editable structure state retained by this module. */
    protected final S latestStructureState() {
        return latestStructureState;
    }

    /** Invalidates execution data after a module changes the algorithm input. */
    protected final void invalidateExecutionForInputChange() {
        stopAlgorithm();
        clearExecutionState();
        refreshStatsDisplay();
    }

    /** Invalidates algorithm state only when the current editable structure is the selected input. */
    protected final void invalidateExecutionForStructureChange() {
        stopAlgorithm();
        if (algorithmInputTracksCurrentStructure()) {
            clearExecutionState();
        }
        refreshStatsDisplay();
    }

    /** Modules with selectable saved-snapshot input override this to preserve independent algorithm state. */
    protected boolean algorithmInputTracksCurrentStructure() {
        return true;
    }

    private void stopReplay() {
        if (replayController != null) {
            replayController.pause();
        }
        if (visualizer != null) visualizer.resumeAnimations();
        paused.set(false);
    }

    /** Applies a replay seek without allowing reducer or renderer failures to escape JavaFX callbacks. */
    private boolean seekReplayFrame(int frameIndex) {
        if (replayController == null) {
            return false;
        }
        try {
            replayController.seek(frameIndex);
            return true;
        } catch (RuntimeException exception) {
            paused.set(true);
            syncTimelineSlider(replayController.currentIndex(), replayController.frameCount());
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = exception.getClass().getSimpleName();
            }
            appendLog("Replay Error: " + message);
            return false;
        }
    }

    private void replacePlaybackController(EventReducer<S> reducer, List<EventEnvelope> events) {
        if (replayController != null) {
            replayController.close();
        }
        replayController = new PlaybackController<>(reducer, state -> {
            renderState(state);
            PlaybackController<S> active = replayController;
            ReducedEventTimeline<S> timeline = lastTimeline;
            if (active != null) {
                int frameIndex = active.currentIndex();
                syncTimelineSlider(frameIndex, active.frameCount());
                refreshStatsDisplay();
                if (timeline != null && frameIndex >= 0 && frameIndex < timeline.size()) {
                    presentationEventIndex = timeline.eventIndex(frameIndex);
                    EventEnvelope event = timeline.event(frameIndex);
                    presentationEvent.set(event);
                    publishPresentationCursor(PresentationCursor.completedEvent(
                            event.runId(), event.sequence(), PresentationCursor.Mode.REPLAY));
                }
            }
        });
        replayController.load(events);
        updatePlaybackSpeed(delayMs.get());
    }

    private void updatePlaybackSpeed(double requestedDelayMillis) {
        double effectiveDelayMillis = Math.max(1.0d, requestedDelayMillis);
        double playbackSpeed = 100.0d / effectiveDelayMillis;
        if (visualizer != null) {
            visualizer.setPlaybackSpeed(playbackSpeed);
        }
        PlaybackController<S> active = replayController;
        if (active != null) {
            active.setSpeed(playbackSpeed);
        }
    }

    private void prepareTimelineControls() { replayControls.prepareTimelineControls(); }

    private void syncTimelineSlider(int index, int size) { replayControls.syncTimelineSlider(index, size); }

    private void clearExecutionState() {
        if (replayController != null) {
            replayController.close();
            replayController = null;
        }
        stats = ExecutionStatistics.empty();
        runtimeMetricTracker.reset();
        lastExecution = null;
        lastExecutionAnchors = null;
        lastTimeline = null;
        latestViewState = null;
        presentationEventIndex = -1;
        liveEventIndex = -1;
        presentationEvent.set(null);
        clearPresentationCursor();
        replayControls.clearTimeline();
    }

    protected final void appendLog(String message) {
        logChannels.appendAlgorithm(message);
    }

    /** Writes a structure-workspace message into this structure's independent log channel. */
    protected final void appendStructureLog(String message) {
        logChannels.appendStructure(message);
    }

    /** Main-workbench bridge for system messages that belong to the selected algorithm. */
    public final void appendAlgorithmSystemMessage(String message) {
        appendLog(message);
    }

    /** Main-workbench bridge for system messages that belong to the active structure. */
    public final void appendStructureSystemMessage(String message) {
        appendStructureLog(message);
    }

    /** Selects the algorithm's independent log without merging channels. */
    public final void activateAlgorithmLog(String algorithmId) {
        logChannels.activateAlgorithm(algorithmId);
    }

    /** Changes the owning structure variant for logging and telemetry. */
    protected final void setStructureLogScopeId(String structureId) {
        logChannels.setStructureScopeId(structureId);
    }

    public final String structureLogScopeId() {
        return logChannels.structureScope(moduleId());
    }

    /** Runtime capability snapshot for the cross-platform execution memory profiler. */
    public final MemoryCapabilities memoryCapabilities() { return memoryProfile.capabilities(); }

    /** Live algorithm allocation profile when running, otherwise the latest completed one. */
    public final Optional<MemoryFacts> algorithmMemoryProfile() {
        return memoryProfile.algorithmMemoryProfile(structureLogScopeId(), logChannels.activeAlgorithmId());
    }

    public final Optional<MemoryFacts> structureMemoryProfile() {
        return memoryProfile.structureMemoryProfile(structureLogScopeId());
    }

    public final com.majortom.algorithms.telemetry.runtime.TelemetryStore telemetryProfiles() {
        return memoryProfile.telemetryProfiles();
    }

    public final void setDeepMemoryAnalysisEnabled(boolean enabled) {
        memoryProfile.setDeepMemoryAnalysisEnabled(enabled);
    }

    public final boolean isDeepMemoryAnalysisEnabled() { return memoryProfile.isDeepMemoryAnalysisEnabled(); }

    public final Optional<MemoryAllocationAnalysis> algorithmDeepMemoryProfile() {
        return memoryProfile.algorithmDeepMemoryProfile(structureLogScopeId(), logChannels.activeAlgorithmId());
    }

    public final Optional<MemoryAllocationAnalysis> structureDeepMemoryProfile() {
        return memoryProfile.structureDeepMemoryProfile(structureLogScopeId());
    }

    public final boolean structureFootprintAvailable() { return memoryProfile.structureFootprintAvailable(); }

    public final CompletionStage<StructureFootprint> analyzeStructureFootprint() {
        return memoryProfile.analyzeStructureFootprint(structureMemoryRoot());
    }

    /** Concrete modules override this when their true editable structure root differs from the view state. */
    protected Object structureMemoryRoot() {
        return latestStructureState != null ? latestStructureState : latestViewState;
    }

    public final void dispatchVisualizerReset() {
        if (visualizer != null) {
            renderDriver.resetPresentationHistory();
            visualizer.onVisualizationReset();
        }
    }

    public final CompletionStage<Void> dispatchVisualizerAttached() {
        if (visualizer == null) return CompletableFuture.completedFuture(null);
        renderDriver.attach();
        return renderSurfaceHost.attach(renderSurface()).thenRun(renderDriver::requestRender);
    }

    public final CompletionStage<Void> dispatchVisualizerDetached() {
        dispose(); // Stop event production before the RenderSession is deactivated.
        if (visualizer == null) return CompletableFuture.completedFuture(null);
        renderDriver.detach();
        return renderSurfaceHost.detach(renderSurface(), () -> {
            visualizer.dispose();
            renderDriver.dispose();
        });
    }

    private RenderSurface<?> renderSurface() {
        if (renderSurface != null) return renderSurface;
        throw new IllegalStateException("No hosted RenderSurface is available");
    }

    public final void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        stopAlgorithm();
        if (replayController != null) {
            replayController.close();
            replayController = null;
        }
        execution.close();
        replayControls.dispose();
        if (delayMs.isBound()) {
            delayMs.unbind();
        }
        clearGlobalButtonActions();
    }

    protected void refreshStatsDisplay() {
        if (statsLabel != null) {
            statsLabel.setText(RuntimeOverviewText.format(runtimeOverview()));
        }
        statisticsRefresh.run();
        updatePlaybackButtonState();
    }

    private void updatePlaybackButtonState() {
        boolean playbackUnavailable = running.get() || !hasPlaybackData();
        if (pauseBtn != null) {
            boolean replayActive = !running.get()
                    && replayController != null
                    && (replayController.isPlaying() || paused.get());
            pauseBtn.setDisable(!running.get() && !replayActive);
        }
        if (replayBtn != null) {
            replayBtn.setDisable(playbackUnavailable);
        }
        if (stepBackwardBtn != null) {
            stepBackwardBtn.setDisable(playbackUnavailable);
        }
        if (stepForwardBtn != null) {
            boolean liveStepAvailable = running.get() && paused.get() && currentSession != null;
            boolean replayStepAvailable = !running.get() && hasPlaybackData();
            stepForwardBtn.setDisable(!liveStepAvailable && !replayStepAvailable);
        }
        boolean recordUnavailable = running.get() || !hasExecutionRecord();
        if (exportBtn != null) {
            exportBtn.setDisable(recordUnavailable);
        }
        if (compareBtn != null) {
            compareBtn.setDisable(recordUnavailable);
        }
    }

    private void setupGlobalButtonActions() {
        if (startBtn != null) {
            startBtn.setOnAction(event -> handleAlgorithmStart());
        }
        if (pauseBtn != null) {
            pauseBtn.setOnAction(event -> togglePause());
        }
        if (resetBtn != null) {
            resetBtn.setOnAction(event -> reset());
        }
        if (replayBtn != null) {
            replayBtn.setOnAction(event -> toggleReplay());
        }
        if (stepBackwardBtn != null) {
            stepBackwardBtn.setOnAction(event -> {
                if (!isRunning()) {
                    stepBackward();
                }
            });
        }
        if (stepForwardBtn != null) {
            stepForwardBtn.setOnAction(event -> {
                if (!isRunning() || isPaused()) {
                    stepForward();
                }
            });
        }
        if (exportBtn != null) {
            exportBtn.setOnAction(event -> exportExecution());
        }
        if (compareBtn != null) {
            compareBtn.setOnAction(event -> compareExecutions());
        }
    }

    private void clearGlobalButtonActions() {
        if (startBtn != null) {
            startBtn.setOnAction(null);
        }
        if (pauseBtn != null) {
            pauseBtn.setOnAction(null);
        }
        if (resetBtn != null) {
            resetBtn.setOnAction(null);
        }
        if (replayBtn != null) {
            replayBtn.setOnAction(null);
        }
        if (stepBackwardBtn != null) {
            stepBackwardBtn.setOnAction(null);
        }
        if (stepForwardBtn != null) {
            stepForwardBtn.setOnAction(null);
        }
        if (exportBtn != null) {
            exportBtn.setOnAction(null);
        }
        if (compareBtn != null) {
            compareBtn.setOnAction(null);
        }
    }

    protected abstract String moduleId();

    protected abstract String formatStatsMessage();

    public abstract void setupCustomControls(HBox container);

    protected abstract void setupI18n();

    public abstract void handleAlgorithmStart();

    protected void onAlgorithmFinished(ExecutionResult result) {
        appendLog(String.format("Finished. Event span: %dms", executionSummary()
                .timing().eventSpan().toMillis()));
    }

    protected void handleAlgorithmError(Throwable error) {
        appendLog("Runtime Error: " + error.getMessage());
        error.printStackTrace();
    }

    public final void setUIReferences(WorkbenchControls controls) {
        Objects.requireNonNull(controls, "controls");
        this.statsLabel = controls.statsLabel();
        this.statisticsRefresh = controls.statisticsRefresh();
        this.logView = controls.logView();
        this.structureLogView = controls.structureLogView();
        logChannels.bind(controls.logChannelStore(), logView, structureLogView, moduleId());
        this.delaySlider = controls.delaySlider();
        this.timelineSlider = controls.timelineSlider();
        this.customControlBox = controls.customControlBox();
        this.startBtn = controls.startButton();
        this.pauseBtn = controls.pauseButton();
        this.resetBtn = controls.resetButton();
        this.replayBtn = controls.replayButton();
        this.stepBackwardBtn = controls.stepBackwardButton();
        this.stepForwardBtn = controls.stepForwardButton();
        this.exportBtn = controls.exportButton();
        this.compareBtn = controls.compareButton();
        if (this.startBtn != null) {
            this.startBtn.setDisable(false);
        }
        if (this.delaySlider != null) delayMs.bind(delaySlider.valueProperty());
        replayControls.bind(delaySlider, timelineSlider);
        setupGlobalButtonActions();
        refreshStatsDisplay();
    }

    /** Executes the common reset sequence and delegates module data reset to a hook. */
    public final void reset() {
        stopAlgorithm();
        clearExecutionState();
        if (logView != null) {
            logView.getItems().clear();
        }
        resetModuleState();
        dispatchVisualizerReset();
        refreshStatsDisplay();
    }

    /** Hook for a module to regenerate or clear its own input data. */
    protected void resetModuleState() {
    }

    public final BaseVisualizer<S> getVisualizer() {
        return visualizer;
    }

    public final String getModuleId() {
        return moduleId();
    }

    public final Region getVisualizerView() {
        return visualizer;
    }

    /** Read-only execution stream for the Workbench timeline/event inspector. */
    public final List<EventEnvelope> executionEvents() {
        if (currentSession != null) {
            return List.copyOf(currentSession.events());
        }
        if (lastTimeline != null) {
            return lastTimeline.events();
        }
        return List.of();
    }

    /** Current event selected by the live presentation cursor or replay cursor. */
    public final EventEnvelope currentPresentationEvent() {
        EventEnvelope visible = presentationEvent.get();
        if (visible != null) {
            return visible;
        }
        if (lastTimeline != null && !lastTimeline.isEmpty()) {
            int index = lastTimeline.currentIndex();
            if (index < 0) {
                index = lastTimeline.size() - 1;
            }
            return lastTimeline.event(Math.max(0, Math.min(index, lastTimeline.size() - 1)));
        }
        return null;
    }

    /** Presentation-only cursor used by the Workbench event inspector/timeline shell. */
    public final ReadOnlyObjectProperty<EventEnvelope> presentationEventProperty() {
        return presentationEvent;
    }

    public final int presentationEventIndex() {
        return presentationEventIndex;
    }

    /** Exact authoritative event inspection with canvas state resolved at-or-before that event. */
    public final void seekEventIndex(int eventIndex) {
        if (lastTimeline == null || lastTimeline.events().isEmpty() || running.get()) {
            return;
        }
        int exactEventIndex = Math.max(0, Math.min(lastTimeline.events().size() - 1, eventIndex));
        int frameIndex = lastTimeline.frameIndexAtOrBeforeEvent(exactEventIndex);
        stopReplay();
        beginScrubbing();
        long generation = replayControls.scrubGeneration();
        try {
            if (frameIndex >= 0) {
                if (!seekReplayFrame(frameIndex)) {
                    return;
                }
                syncTimelineSlider(frameIndex, lastTimeline.size());
            } else if (visualizer != null) {
                renderState(lastTimeline.initialState());
            }
        } finally {
            releaseScrubbingAfterQueuedRender(generation);
        }
        paused.set(true);
        presentationEventIndex = exactEventIndex;
        EventEnvelope exactEvent = lastTimeline.events().get(exactEventIndex);
        presentationEvent.set(exactEvent);
        publishPresentationCursor(PresentationCursor.completedEvent(
                exactEvent.runId(), exactEvent.sequence(), PresentationCursor.Mode.REPLAY));
    }

    /** Monotonic anchors for the latest editable-structure operation. */
    public final Optional<ExecutionAnchorTimeline> latestStructureExecutionAnchors() {
        return Optional.ofNullable(lastStructureExecutionAnchors);
    }

    /** Monotonic algorithm anchors retained even when no terminal history record can be created. */
    public final Optional<ExecutionAnchorTimeline> latestExecutionAnchors() {
        if (currentSession != null) {
            Optional<ExecutionAnchorTimeline> live = currentSession.executionAnchors();
            if (live.isPresent()) return live;
        }
        return Optional.ofNullable(lastExecutionAnchors);
    }

    private void publishPresentationCursor(PresentationCursor cursor) {
        if (presentationCursorPort == null || visualizer == null || cursor == null) {
            return;
        }
        presentationCursorPort.publishPresentationCursor(visualizer.sessionId(), cursor);
    }

    private void republishCurrentPresentationCursor(PresentationCursor.Mode mode) {
        EventEnvelope event = currentPresentationEvent();
        if (event == null) {
            return;
        }
        publishPresentationCursor(PresentationCursor.completedEvent(
                event.runId(), event.sequence(), mode));
    }

    private void clearPresentationCursor() {
        if (presentationCursorPort == null || visualizer == null) {
            return;
        }
        presentationCursorPort.clearPresentationCursor(visualizer.sessionId());
    }

    public final String latestRunId() {
        List<EventEnvelope> events = executionEvents();
        if (!events.isEmpty()) return events.getFirst().runId();
        if (lastExecution != null) return lastExecution.recording().runId();
        return null;
    }

    public final String latestExecutionStatus() {
        if (lastExecution == null) return "IDLE";
        return lastExecution.result().status().name();
    }

    public final String latestResultText() {
        if (lastExecution == null) return I18N.text("label.workspace.result.none");
        ExecutionResult result = lastExecution.result();
        if (result.failure().isPresent()) {
            return result.failure().get().code() + "\n" + result.failure().get().message();
        }
        return result.output().map(String::valueOf).orElse(result.status().name());
    }

    /** Authoritative statistics at the current live/replay presentation cursor. */
    public final ExecutionStatistics currentExecutionStatistics() {
        return stats;
    }

    /** Semantic structure summary rendered from the same provider model as the metric tiles. */
    public String structureSummaryText() {
        return RuntimeOverviewText.formatStructure(structureOverview());
    }

    public String structurePrimaryCount() {
        return "—";
    }

    public String structureSecondaryCount() {
        return "—";
    }

    /** Returns the shared summary retained for the latest local execution. */
    public final ExecutionSummary executionSummary() {
        if (lastExecution == null) return ExecutionSummary.from(stats);
        return lastExecution.recording().summary();
    }

    /** Complete structured overview for the current algorithm frame (or structure state when idle). */
    public final RuntimeOverviewModel runtimeOverview() {
        S state = latestViewState != null ? latestViewState : latestStructureState;
        return buildRuntimeOverview(state);
    }

    /** Structured overview for the editable structure, independent of algorithm playback. */
    public final RuntimeOverviewModel structureOverview() {
        return buildRuntimeOverview(latestStructureState);
    }

    private RuntimeOverviewModel buildRuntimeOverview(S state) {
        return RUNTIME_OVERVIEW.build(
                structureLogScopeId(),
                state,
                structureEvents(),
                stats,
                executionEvents(),
                executionSummary(),
                currentPlaybackDuration(),
                runtimeMetricTracker.peaks());
    }

    private Optional<Duration> currentPlaybackDuration() {
        if (replayController == null) return Optional.empty();
        return Optional.of(replayController.playbackDuration());
    }

    public final ReadOnlyBooleanProperty runningProperty() {
        return running;
    }

    public final ReadOnlyBooleanProperty pausedProperty() {
        return paused;
    }

    /** Changes whenever the module publishes a new editable structure state. */
    public final ReadOnlyLongProperty structureRevisionProperty() {
        return structureRevision;
    }

    public final boolean isRunning() {
        return running.get();
    }

    public final boolean isPaused() {
        return paused.get();
    }

    /** Returns whether a recorded timeline is currently auto-playing. */
    public final boolean isPlaybackPlaying() {
        return replayController != null && replayController.isPlaying();
    }

    private String formatSummaryMessage(ExecutionSummary summary) {
        ExecutionTiming timing = summary.timing();
        ResourceUsage resources = summary.resources();
        return String.format(
                "%s | %s | %s | %s | %s",
                I18N.text("stats.event.span", timing.eventSpan().toMillis()),
                I18N.text("stats.total.time", ControllerExecutionArchive.formatDuration(timing.totalDuration())),
                I18N.text("stats.playback.time", ControllerExecutionArchive.formatDuration(currentPlaybackDuration())),
                I18N.text("stats.cpu.time", ControllerExecutionArchive.formatNanos(resources.cpuTimeNanos())),
                I18N.text("stats.memory.peak", ControllerExecutionArchive.formatBytes(resources.peakMemoryBytes())));
    }


}
