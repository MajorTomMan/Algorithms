package com.majortom.algorithms.visualization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.majortom.algorithms.core.domain.execution.RunCancelledEvent;
import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.domain.execution.RunFailedEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionRecording;
import com.majortom.algorithms.core.runtime.ExecutionRecordingState;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.logging.LogEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.runtime.ExecutionStatus;
import com.majortom.algorithms.core.runtime.ResourceUsage;
import com.majortom.algorithms.core.registry.ModuleLoader;
import com.majortom.algorithms.core.registry.ModuleRegistry;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.logging.LogView;
import com.majortom.algorithms.core.runtime.ExecutionOperation;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.core.runtime.ExecutionSummary;
import com.majortom.algorithms.core.runtime.ExecutionTiming;
import com.majortom.algorithms.core.runtime.StatisticsReducer;
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
import javafx.application.Platform;
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
import javafx.beans.value.ChangeListener;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.ResourceBundle;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** Shared JavaFX shell around the UI-neutral registry/runtime/event pipeline. */
public abstract class BaseController<S> implements Initializable {

    private static final long LIVE_STATS_REFRESH_INTERVAL_NANOS = 50_000_000L;
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ModuleRegistry MODULE_REGISTRY = ModuleLoader.load();
    private static final RunHistoryService DEFAULT_EXECUTION_HISTORY =
            new InMemoryRunHistoryService(RunHistoryPolicy.desktopDefault());
    private static final InputFingerprint DEFAULT_INPUT_FINGERPRINT =
            new JacksonSha256InputFingerprint(JSON_MAPPER);
    private static final ExecutionExportCodec DEFAULT_EXPORT_CODEC = new ExecutionExportCodec(JSON_MAPPER);
    private static final ExecutionExporter DEFAULT_EXECUTION_EXPORTER =
            new JsonExecutionExporter(java.nio.file.Path.of("exports"), JSON_MAPPER, DEFAULT_EXPORT_CODEC);

    protected final DoubleProperty delayMs = new SimpleDoubleProperty(50.0d);
    protected ExecutionStatistics stats = ExecutionStatistics.empty();
    protected final BaseVisualizer<S> visualizer;

    protected Label statsLabel;
    protected LogView logView;
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
    private final RunHistoryService executionHistory;
    private final InputFingerprint inputFingerprintService;
    private final ExecutionExporter executionExporter;
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
    private ReducedEventTimeline<S> lastTimeline;
    private PlaybackController<S> replayController;
    private boolean updatingTimelineSlider;
    private long lastLiveStatsRefreshNanos;
    private final AtomicLong livePlaybackDelayMillis = new AtomicLong(50L);
    private final ChangeListener<Number> delaySliderListener = (observable, oldValue, newValue) -> {
        livePlaybackDelayMillis.set(Math.max(0L, newValue.longValue()));
        updatePlaybackSpeed(newValue.doubleValue());
    };
    private final ChangeListener<Number> timelineSliderListener =
            (observable, oldValue, newValue) -> {
                if (!updatingTimelineSlider && !running.get()
                        && timelineSlider != null && timelineSlider.isValueChanging()) {
                    seekTimeline(newValue.doubleValue());
                }
            };
    private boolean disposed;

    protected BaseController(BaseVisualizer<S> visualizer) {
        this(
                visualizer,
                new LocalClientExecutionService(),
                DEFAULT_EXECUTION_HISTORY,
                DEFAULT_INPUT_FINGERPRINT,
                DEFAULT_EXECUTION_EXPORTER);
    }

    protected BaseController(
            BaseVisualizer<S> visualizer,
            ClientExecutionService execution,
            RunHistoryService executionHistory,
            InputFingerprint inputFingerprint,
            ExecutionExporter executionExporter) {
        this.visualizer = visualizer;
        this.execution = Objects.requireNonNull(execution, "execution");
        this.executionHistory = Objects.requireNonNull(executionHistory, "executionHistory");
        this.inputFingerprintService = Objects.requireNonNull(inputFingerprint, "inputFingerprint");
        this.executionExporter = Objects.requireNonNull(executionExporter, "executionExporter");
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
        clearExecutionState();
        liveVisualFrameCount = 0L;
        liveEventIndex = -1;
        presentationEventIndex = -1;
        lastLiveStatsRefreshNanos = 0L;
        refreshStatsDisplay();

        EventReducer<S> liveReducer = reducerFactory.get();
        running.set(true);
        paused.set(false);
        updatePlaybackButtonState();
        appendLog("Started: " + algorithmId);

        currentSession = execution.start(
                algorithmId,
                operation,
                liveReducer,
                this::consumeLiveEvent,
                this::renderLiveState,
                this::updateLiveStatistics,
                livePlaybackDelayMillis::get);
        ExecutionHandle session = currentSession;
        session.presentationCompletion().whenComplete((result, error) -> Platform.runLater(
                () -> finishExecution(session, algorithmId, input, reducerFactory, result, error)));
    }

    protected final <T> T module(String key, Class<T> contract) {
        return MODULE_REGISTRY.create(key, contract);
    }

    /** Executes one editable structure mutation through the shared Runtime and records its event history. */
    protected final boolean executeStructureOperation(String operationId, ExecutionOperation<?> operation) {
        if (disposed) {
            throw new IllegalStateException("Controller is disposed");
        }
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(operation, "operation");
        invalidateExecutionForStructureChange();
        String runtimeOperationId = "structure." + moduleId() + "." + operationId;
        ExecutionResult result = new ExecutionRuntime().execute(
                runtimeOperationId, moduleId(), structureTimeline, RunControl.unrestricted(), operation);
        if (result.status() == ExecutionStatus.COMPLETED) {
            return true;
        }
        String message = result.failure().map(failure -> failure.message()).orElse("Structure operation failed");
        appendLog(message);
        return false;
    }

    /** Records a non-mutation auxiliary event such as snapshot lifecycle state. */
    public final boolean recordAuxiliaryEvent(String operationId, ExecutionEvent event) {
        Objects.requireNonNull(event, "event");
        String runtimeOperationId = "structure." + moduleId() + "." + operationId;
        ExecutionResult result = new ExecutionRuntime().execute(
                runtimeOperationId, moduleId(), structureTimeline, RunControl.unrestricted(), () -> {
                    ExecutionEvents.emit(event);
                    return null;
                });
        return result.status() == ExecutionStatus.COMPLETED;
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
                paused.set(true);
            } else {
                replayController.play();
                paused.set(false);
            }
            refreshStatsDisplay();
            return;
        }
        if (currentSession == null || !running.get()) {
            return;
        }
        if (paused.get()) {
            currentSession.resumeExecution();
            paused.set(false);
        } else {
            currentSession.pauseExecution();
            paused.set(true);
        }
        updatePlaybackButtonState();
    }

    public final void toggleReplay() {
        if (!hasExecutionData() || running.get()) {
            return;
        }
        if (replayController.isPlaying()) {
            replayController.pause();
            paused.set(true);
            refreshStatsDisplay();
            return;
        }
        if (replayController.currentIndex() + 1 >= replayController.frameCount()) {
            replayController.restart();
            if (replayController.stepForward()) {
                syncTimelineSlider(replayController.currentIndex(), replayController.frameCount());
            }
        }
        replayController.play();
        paused.set(false);
        refreshStatsDisplay();
    }

    /** Advances one replay frame while leaving playback paused. */
    public final boolean stepForward() {
        if (running.get()) {
            if (currentSession == null || !paused.get()) {
                return false;
            }
            currentSession.stepExecution();
            return true;
        }
        if (!hasExecutionData()) {
            return false;
        }
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
        boolean rewound = replayController.stepBackward();
        paused.set(true);
        if (rewound) {
            syncTimelineSlider(replayController.currentIndex(), replayController.frameCount());
        }
        refreshStatsDisplay();
        return rewound;
    }

    public final void seekTimeline(double progress) {
        if (!hasExecutionData()) {
            return;
        }
        stopReplay();
        int size = lastTimeline.size();
        int index = (int) Math.round(progress * (size - 1));
        index = Math.max(0, Math.min(size - 1, index));
        if (visualizer != null) {
            visualizer.setScrubbing(true);
        }
        boolean sought;
        try {
            sought = seekReplayFrame(index);
        } finally {
            releaseScrubbingAfterQueuedRender();
        }
        if (!sought) {
            return;
        }
        paused.set(true);
        syncTimelineSlider(index, size);
    }


    /**
     * Visualizers render through Platform.runLater. Keep scrub mode active until that queued draw
     * has consumed the absolute replay state, then release it on the following FX queue turn.
     */
    private void releaseScrubbingAfterQueuedRender() {
        BaseVisualizer<S> scrubVisualizer = visualizer;
        if (scrubVisualizer == null) {
            return;
        }
        Platform.runLater(() -> scrubVisualizer.setScrubbing(false));
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
        if (!hasExecutionRecord()) {
            appendLog("Nothing to export.");
            return;
        }
        try {
            java.nio.file.Path file = executionExporter.export(lastExecution, executionSummary());
            appendLog("Exported: " + file);
        } catch (IOException exception) {
            handleAlgorithmError(exception);
        }
    }

    public final void compareExecutions() {
        if (!hasExecutionRecord()) {
            appendLog("No execution data available for comparison.");
            return;
        }
        List<ClientExecutionRecord> candidates = executionHistory.comparableWith(lastExecution);
        if (candidates.isEmpty()) {
            appendLog("No comparable executions found for the same input.");
            return;
        }
        appendLog("Comparison for input " + lastExecution.inputFingerprint() + ":");
        appendLog(describeRecord(lastExecution));
        for (ClientExecutionRecord record : candidates) {
            appendLog(describeRecord(record));
        }
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
        lastExecution = createExecutionRecord(algorithmId, input, result, error, summary, events, timeline.size());
        lastTimeline = timeline;
        replacePlaybackController(reducer, events);
        if (lastExecution != null) {
            executionHistory.add(lastExecution);
        }
        prepareTimelineControls();
        if (!timeline.isEmpty()) {
            int lastFrame = timeline.size() - 1;
            if (seekReplayFrame(lastFrame)) {
                syncTimelineSlider(lastFrame, timeline.size());
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
            if (envelope.event() instanceof LogEvent logEvent && logView != null) {
                logView.append(logEvent, envelope.timestamp());
            }
        };
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    private void renderLiveState(S state) {
        liveVisualFrameCount++;
        renderViewState(state);
    }

    private void renderState(S state) {
        renderViewState(state);
    }

    protected final long visualFrameCount() {
        if (running.get()) {
            return liveVisualFrameCount;
        }
        return lastTimeline == null ? 0L : lastTimeline.size();
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
            visualizer.render(state);
        }
    }

    /** Stores an editable structure state without changing the algorithm cursor. */
    protected final void renderStructureState(S state) {
        storeStructureState(state);
        if (state != null && visualizer != null) {
            visualizer.render(state);
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
            visualizer.render(latestViewState);
        }
    }

    /** Hook for modules that need to rebuild their structure projection. */
    protected void restoreStructureState() {
        if (latestStructureState != null && visualizer != null) {
            visualizer.render(latestStructureState);
            return;
        }
        if (latestViewState != null && visualizer != null) {
            visualizer.render(latestViewState);
        }
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
                    presentationEvent.set(timeline.event(frameIndex));
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

    private void prepareTimelineControls() {
        if (timelineSlider == null) {
            return;
        }
        timelineSlider.setDisable(!hasExecutionData());
        updatingTimelineSlider = true;
        if (hasExecutionData()) {
            timelineSlider.setValue(1.0d);
        } else {
            timelineSlider.setValue(0.0d);
        }
        updatingTimelineSlider = false;
    }

    private void syncTimelineSlider(int index, int size) {
        if (timelineSlider == null) {
            return;
        }
        updatingTimelineSlider = true;
        double value = 0.0d;
        if (size > 1) {
            value = (double) index / (double) (size - 1);
        }
        timelineSlider.setValue(value);
        timelineSlider.setDisable(running.get() || !hasExecutionData());
        updatingTimelineSlider = false;
    }

    private void clearExecutionState() {
        if (replayController != null) {
            replayController.close();
            replayController = null;
        }
        stats = ExecutionStatistics.empty();
        lastExecution = null;
        lastTimeline = null;
        latestViewState = null;
        presentationEventIndex = -1;
        liveEventIndex = -1;
        presentationEvent.set(null);
        if (timelineSlider != null) {
            timelineSlider.setDisable(true);
            updatingTimelineSlider = true;
            timelineSlider.setValue(0.0d);
            updatingTimelineSlider = false;
        }
    }

    private String describeRecord(ClientExecutionRecord record) {
        ExecutionSummary summary = record.recording().summary();
        ExecutionTiming timing = summary.timing();
        return String.format(
                "%s | event-span=%dms | total=%s | cpu=%s | memory=%s | events=%d | frames=%d | compares=%d",
                record.operationId(), timing.eventSpan().toMillis(),
                formatDuration(timing.totalDuration()),
                formatNanos(summary.resources().cpuTimeNanos()),
                formatBytes(summary.resources().peakMemoryBytes()),
                record.recording().statistics().totalEventCount(),
                record.visualFrameCount(),
                record.recording().statistics().metric("comparisons"));
    }

    private String inputFingerprint(Object input) {
        return inputFingerprintService.fingerprint(input);
    }

    protected final void appendLog(String message) {
        if (logView == null) {
            return;
        }
        Runnable task = () -> logView.appendSystem(message);
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    public final void dispatchVisualizerReset() {
        if (visualizer != null) {
            visualizer.onVisualizationReset();
        }
    }

    public final void dispatchVisualizerAttached() {
        if (visualizer != null) {
            visualizer.onModuleAttached(moduleId());
        }
    }

    public final void dispatchVisualizerDetached() {
        dispose();
        if (visualizer != null) {
            visualizer.onModuleDetached(moduleId());
            visualizer.dispose();
        }
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
        if (delaySlider != null) {
            delaySlider.valueProperty().removeListener(delaySliderListener);
        }
        if (timelineSlider != null) {
            timelineSlider.valueProperty().removeListener(timelineSliderListener);
        }
        if (delayMs.isBound()) {
            delayMs.unbind();
        }
        clearGlobalButtonActions();
    }

    protected void refreshStatsDisplay() {
        if (statsLabel != null) {
            String message = formatStatsMessage();
            if (lastExecution != null) {
                message = message + " | " + formatSummaryMessage(executionSummary());
            }
            statsLabel.setText(message);
        }
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
        this.logView = controls.logView();
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
        if (this.delaySlider != null) {
            delayMs.bind(delaySlider.valueProperty());
            livePlaybackDelayMillis.set(Math.max(0L, Math.round(delaySlider.getValue())));
            delaySlider.valueProperty().addListener(delaySliderListener);
        }
        if (this.timelineSlider != null) {
            timelineSlider.setDisable(true);
            timelineSlider.valueProperty().addListener(timelineSliderListener);
        }
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
        if (visualizer != null) {
            visualizer.setScrubbing(true);
        }
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
            releaseScrubbingAfterQueuedRender();
        }
        paused.set(true);
        presentationEventIndex = exactEventIndex;
        presentationEvent.set(lastTimeline.events().get(exactEventIndex));
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
        if (lastExecution == null) return "No result yet.";
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

    /** Family-specific structure summary for the presentation shell. */
    public String structureSummaryText() {
        return formatStatsMessage();
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

    private ClientExecutionRecord createExecutionRecord(
            String operationId,
            Object input,
            ExecutionResult result,
            Throwable error,
            ExecutionSummary summary,
            List<EventEnvelope> events,
            long visualFrameCount) {
        if (events.isEmpty()) {
            return null;
        }
        if (!hasTerminalLifecycleEvent(events)) {
            // An external Error may interrupt a run before its terminal lifecycle event.
            // Keep the visual timeline available, but do not publish an invalid history record.
            return null;
        }
        ExecutionRecordingState state = recordingState(result, error);
        EventEnvelope firstEvent = events.getFirst();
        ExecutionStatistics authoritativeStatistics = summary.statistics();
        ExecutionSummary recordingSummary = ExecutionSummary.from(
                authoritativeStatistics, summary.resources()).withTiming(
                ExecutionTiming.of(
                        authoritativeStatistics.eventSpan(),
                        summary.timing().totalDuration()));
        ExecutionRecording recording = new ExecutionRecording(
                firstEvent.runId(), operationId, state, authoritativeStatistics, recordingSummary, events);
        ExecutionResult effectiveResult = result;
        if (effectiveResult == null) {
            String message = "Execution failed";
            String exceptionType = RuntimeException.class.getName();
            if (error != null) {
                if (error.getMessage() != null && !error.getMessage().isBlank()) {
                    message = error.getMessage();
                }
                exceptionType = error.getClass().getName();
            }
            effectiveResult = ExecutionResult.failed(
                    new com.majortom.algorithms.core.runtime.ExecutionFailure(
                            "client.execution.failed",
                            message,
                            exceptionType));
        }
        return new ClientExecutionRecord(
                moduleId(), operationId, inputFingerprint(input), effectiveResult, recording, visualFrameCount);
    }

    private boolean hasTerminalLifecycleEvent(List<EventEnvelope> events) {
        for (EventEnvelope event : events) {
            if (event.event() instanceof RunCompletedEvent
                    || event.event() instanceof RunCancelledEvent
                    || event.event() instanceof RunFailedEvent) {
                return true;
            }
        }
        return false;
    }

    private ExecutionRecordingState recordingState(ExecutionResult result, Throwable error) {
        if (error != null) {
            return ExecutionRecordingState.FAILED;
        }
        if (result == null) {
            return ExecutionRecordingState.FAILED;
        }
        return switch (result.status()) {
            case COMPLETED -> ExecutionRecordingState.COMPLETED;
            case CANCELLED -> ExecutionRecordingState.CANCELLED;
            case FAILED -> ExecutionRecordingState.FAILED;
        };
    }

    private String formatSummaryMessage(ExecutionSummary summary) {
        ExecutionTiming timing = summary.timing();
        ResourceUsage resources = summary.resources();
        return String.format(
                "%s | %s | %s | %s | %s",
                I18N.text("stats.event.span", timing.eventSpan().toMillis()),
                I18N.text("stats.total.time", formatDuration(timing.totalDuration())),
                I18N.text("stats.playback.time", formatDuration(currentPlaybackDuration())),
                I18N.text("stats.cpu.time", formatNanos(resources.cpuTimeNanos())),
                I18N.text("stats.memory.peak", formatBytes(resources.peakMemoryBytes())));
    }

    private String formatDuration(Optional<Duration> duration) {
        if (duration.isEmpty()) {
            return I18N.text("stats.unavailable");
        }
        return duration.orElseThrow().toMillis() + "ms";
    }

    private String formatNanos(OptionalLong nanos) {
        if (nanos.isEmpty()) {
            return I18N.text("stats.unavailable");
        }
        return Duration.ofNanos(nanos.orElseThrow()).toMillis() + "ms";
    }

    private String formatBytes(OptionalLong bytes) {
        if (bytes.isEmpty()) {
            return I18N.text("stats.unavailable");
        }
        long value = bytes.orElseThrow();
        if (value < 1024L) {
            return value + "B";
        }
        long kilobytes = value / 1024L;
        if (kilobytes < 1024L) {
            return kilobytes + "KB";
        }
        return String.format("%.1fMB", kilobytes / 1024.0d);
    }

}
