package com.majortom.algorithms.visualization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.majortom.algorithms.core.api.AlgorithmInput;
import com.majortom.algorithms.core.api.AlgorithmProvider;
import com.majortom.algorithms.core.domain.execution.RunCancelledEvent;
import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.domain.execution.RunFailedEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvent;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionStatus;
import com.majortom.algorithms.core.runtime.ResourceUsage;
import com.majortom.algorithms.library.catalog.ProviderCatalog;
import com.majortom.algorithms.core.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.core.runtime.ExecutionSummary;
import com.majortom.algorithms.core.runtime.ExecutionTiming;
import com.majortom.algorithms.visualization.impl.controller.BaseModuleController;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.runtime.ExecutionSession;
import com.majortom.algorithms.visualization.runtime.LocalAlgorithmExecution;
import com.majortom.algorithms.visualization.runtime.PlaybackController;
import com.majortom.algorithms.visualization.runtime.ReducedEventTimeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.ResourceBundle;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** Shared JavaFX shell around the UI-neutral provider/runtime/event pipeline. */
public abstract class BaseController<S> implements Initializable {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final List<ClientRunRecord<?>> EXECUTION_ARCHIVE = new ArrayList<>();
    private static final int MAXIMUM_ARCHIVED_EXECUTIONS = 20;

    protected final DoubleProperty delayMs = new SimpleDoubleProperty(50.0d);
    protected ExecutionStatistics stats = ExecutionStatistics.empty();
    protected final BaseVisualizer<S> visualizer;

    protected Label statsLabel;
    protected TextArea logArea;
    protected Slider delaySlider;
    protected Slider timelineSlider;
    protected HBox customControlBox;
    protected Button startBtn;
    protected Button pauseBtn;
    protected Button resetBtn;
    protected Button replayBtn;
    protected Button exportBtn;
    protected Button compareBtn;

    private final LocalAlgorithmExecution execution = new LocalAlgorithmExecution();
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final BooleanProperty paused = new SimpleBooleanProperty(false);
    private ExecutionSession currentSession;
    private ClientRunRecord<S> lastExecution;
    private ReducedEventTimeline<S> lastTimeline;
    private PlaybackController<S> replayController;
    private boolean updatingTimelineSlider;
    private final AtomicLong executionDelayMillis = new AtomicLong(50L);
    private final ChangeListener<Number> delaySliderListener = (observable, oldValue, newValue) -> {
        executionDelayMillis.set(Math.max(0L, newValue.longValue()));
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
        this.visualizer = visualizer;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupI18n();
    }

    protected final void startAlgorithm(
            String algorithmId,
            AlgorithmInput input,
            Supplier<? extends EventReducer<S>> reducerFactory) {
        if (disposed) {
            throw new IllegalStateException("Controller is disposed");
        }
        stopAlgorithm();
        clearExecutionState();
        stats = ExecutionStatistics.empty();
        refreshStatsDisplay();

        AlgorithmProvider<?, ?> provider = ProviderCatalog.production().require(algorithmId);
        EventReducer<S> liveReducer = reducerFactory.get();
        running.set(true);
        paused.set(false);
        appendLog("Started: " + algorithmId);

        currentSession = execution.start(
                provider.invoker(),
                input,
                liveReducer,
                this::renderState,
                executionDelayMillis::get);
        ExecutionSession session = currentSession;
        session.completion().whenComplete((result, error) -> Platform.runLater(
                () -> finishExecution(session, algorithmId, input, reducerFactory, result, error)));
    }

    public final void stopAlgorithm() {
        stopReplay();
        if (currentSession != null) {
            currentSession.close();
        }
        currentSession = null;
        running.set(false);
        paused.set(false);
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
        if (!hasExecutionData() || running.get()) {
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
        replayController.seek(index);
        syncTimelineSlider(index, size);
    }

    public final boolean hasExecutionData() {
        return lastExecution != null && lastTimeline != null && !lastTimeline.isEmpty();
    }

    public final void exportExecution() {
        if (!hasExecutionData()) {
            appendLog("Nothing to export.");
            return;
        }
        try {
            Path exportDir = Path.of("exports");
            Files.createDirectories(exportDir);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path file = exportDir.resolve(moduleId() + "_" + lastExecution.algorithmId()
                    + "_" + timestamp + ".json");
            JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), exportPayload());
            appendLog("Exported: " + file);
        } catch (IOException exception) {
            handleAlgorithmError(exception);
        }
    }

    public final void compareExecutions() {
        if (!hasExecutionData()) {
            appendLog("No execution data available for comparison.");
            return;
        }
        List<ClientRunRecord<?>> candidates;
        synchronized (EXECUTION_ARCHIVE) {
            candidates = EXECUTION_ARCHIVE.stream()
                    .filter(record -> record != lastExecution)
                    .filter(record -> record.moduleId().equals(lastExecution.moduleId()))
                    .filter(record -> record.inputSignature().equals(lastExecution.inputSignature()))
                    .toList();
        }
        if (candidates.isEmpty()) {
            appendLog("No comparable executions found for the same input.");
            return;
        }
        appendLog("Comparison for input " + lastExecution.inputSignature() + ":");
        appendLog(describeRecord(lastExecution));
        for (ClientRunRecord<?> record : candidates) {
            appendLog(describeRecord(record));
        }
    }

    private void finishExecution(
            ExecutionSession session,
            String algorithmId,
            AlgorithmInput input,
            Supplier<? extends EventReducer<S>> reducerFactory,
            ExecutionResult result,
            Throwable error) {
        if (session != currentSession) {
            return;
        }
        List<ExecutionEvent> events = session.events();
        session.closeObserver();
        session.close();
        running.set(false);
        paused.set(false);
        currentSession = null;
        EventReducer<S> reducer = reducerFactory.get();
        ReducedEventTimeline<S> timeline = new ReducedEventTimeline<>(events, reducer);
        stats = timeline.statistics();
        Duration eventSpan = stats.eventSpan();
        ExecutionSummary summary = ExecutionSummary.from(stats, session.resourceUsage()).withTiming(
                ExecutionTiming.of(
                        eventSpan,
                        session.totalDuration(),
                        Optional.empty()));
        lastExecution = new ClientRunRecord<>(
                moduleId(), algorithmId, inputSignature(input), events, summary);
        lastTimeline = timeline;
        replacePlaybackController(reducer, events);
        synchronized (EXECUTION_ARCHIVE) {
            EXECUTION_ARCHIVE.add(lastExecution);
            while (EXECUTION_ARCHIVE.size() > MAXIMUM_ARCHIVED_EXECUTIONS) {
                EXECUTION_ARCHIVE.removeFirst();
            }
        }
        prepareTimelineControls();
        if (!timeline.isEmpty()) {
            replayController.seek(timeline.size() - 1);
            syncTimelineSlider(timeline.size() - 1, timeline.size());
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
            onAlgorithmFinished();
        }
        refreshStatsDisplay();
    }

    private void renderState(S state) {
        if (visualizer != null && state != null) {
            visualizer.render(state, null, null);
        }
    }

    private void stopReplay() {
        if (replayController != null) {
            replayController.pause();
        }
        paused.set(false);
    }

    private void replacePlaybackController(EventReducer<S> reducer, List<ExecutionEvent> events) {
        if (replayController != null) {
            replayController.close();
        }
        replayController = new PlaybackController<>(reducer, state -> {
            renderState(state);
            PlaybackController<S> active = replayController;
            if (active != null) {
                syncTimelineSlider(active.currentIndex(), active.frameCount());
                refreshStatsDisplay();
            }
        });
        replayController.load(events);
        updatePlaybackSpeed(delayMs.get());
    }

    private void updatePlaybackSpeed(double requestedDelayMillis) {
        PlaybackController<S> active = replayController;
        if (active == null) {
            return;
        }
        double effectiveDelayMillis = Math.max(1.0d, requestedDelayMillis);
        active.setSpeed(100.0d / effectiveDelayMillis);
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
        lastExecution = null;
        lastTimeline = null;
        if (timelineSlider != null) {
            timelineSlider.setDisable(true);
            updatingTimelineSlider = true;
            timelineSlider.setValue(0.0d);
            updatingTimelineSlider = false;
        }
    }

    private Map<String, Object> exportPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("moduleId", lastExecution.moduleId());
        payload.put("algorithmId", lastExecution.algorithmId());
        payload.put("inputSignature", lastExecution.inputSignature());
        payload.put("stats", lastExecution.stats());
        payload.put("summary", executionSummary());
        List<Map<String, Object>> eventItems = new ArrayList<>();
        for (ExecutionEvent event : lastExecution.events()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("runId", event.runId());
            item.put("sequence", event.sequence());
            item.put("occurredAt", event.occurredAt().toString());
            item.put("eventType", event.payload().getClass().getName());
            item.put("payload", event.payload());
            eventItems.add(item);
        }
        payload.put("events", eventItems);
        return payload;
    }

    private String describeRecord(ClientRunRecord<?> record) {
        ExecutionSummary summary = record.summary();
        ExecutionTiming timing = summary.timing();
        return String.format(
                "%s | event-span=%dms | total=%s | playback=%s | cpu=%s | memory=%s | events=%d | frames=%d | compares=%d",
                record.algorithmId(), timing.eventSpan().toMillis(),
                formatDuration(timing.totalDuration()), formatDuration(timing.playbackDuration()),
                formatNanos(summary.resources().cpuTimeNanos()),
                formatBytes(summary.resources().peakMemoryBytes()),
                record.stats().totalEventCount(), record.stats().visualFrameCount(),
                record.stats().metric("comparisons"));
    }

    private String inputSignature(AlgorithmInput input) {
        return Integer.toHexString(input.toString().hashCode());
    }

    protected final void appendLog(String message) {
        if (logArea == null) {
            return;
        }
        Runnable task = () -> logArea.appendText(
                String.format("[%tT] %s%n", System.currentTimeMillis(), message));
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    public final void dispatchVisualizerAction(VisualizationActionType actionType) {
        dispatchVisualizerEvent(buildVisualizationEvent(actionType, Map.of()));
    }

    public final void dispatchVisualizerAction(VisualizationActionType actionType, Map<String, Object> metadata) {
        dispatchVisualizerEvent(buildVisualizationEvent(actionType, metadata));
    }

    public final void dispatchVisualizerEvent(VisualizationEvent event) {
        if (visualizer != null) {
            visualizer.onControlAction(event);
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

    private VisualizationEvent buildVisualizationEvent(
            VisualizationActionType actionType,
            Map<String, Object> metadata) {
        return VisualizationEvent.of(
                actionType, moduleId(), getClass().getSimpleName(), running.get(), paused.get(), metadata);
    }

    protected void refreshStatsDisplay() {
        if (statsLabel != null) {
            String message = formatStatsMessage();
            if (lastExecution != null) {
                message = message + " | " + formatSummaryMessage(executionSummary());
            }
            statsLabel.setText(message);
        }
    }

    private void setupGlobalButtonActions() {
        if (startBtn != null) {
            startBtn.setOnAction(event -> {
                dispatchVisualizerAction(VisualizationActionType.EXECUTION_START);
                handleAlgorithmStart();
            });
        }
        if (pauseBtn != null) {
            pauseBtn.setOnAction(event -> {
                VisualizationActionType action = VisualizationActionType.EXECUTION_PAUSE;
                if (paused.get()) {
                    action = VisualizationActionType.EXECUTION_RESUME;
                }
                dispatchVisualizerAction(action);
                togglePause();
            });
        }
        if (resetBtn != null) {
            resetBtn.setOnAction(event -> {
                dispatchVisualizerAction(VisualizationActionType.EXECUTION_RESET);
                stopAlgorithm();
                clearExecutionState();
                ((BaseModuleController<S>) this).resetModule();
            });
        }
        if (replayBtn != null) {
            replayBtn.setOnAction(event -> {
                dispatchVisualizerAction(VisualizationActionType.EXECUTION_REPLAY);
                toggleReplay();
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

    protected void onAlgorithmFinished() {
        appendLog(String.format("Finished. Event span: %dms", executionSummary()
                .timing().eventSpan().toMillis()));
    }

    protected void handleAlgorithmError(Throwable error) {
        appendLog("Runtime Error: " + error.getMessage());
        error.printStackTrace();
    }

    public final void setUIReferences(
            Label statsLabel,
            TextArea logArea,
            Slider delaySlider,
            Slider timelineSlider,
            HBox customBox,
            Button startBtn,
            Button pauseBtn,
            Button resetBtn,
            Button replayBtn,
            Button exportBtn,
            Button compareBtn) {
        this.statsLabel = statsLabel;
        this.logArea = logArea;
        this.delaySlider = delaySlider;
        this.timelineSlider = timelineSlider;
        this.customControlBox = customBox;
        this.startBtn = startBtn;
        this.pauseBtn = pauseBtn;
        this.resetBtn = resetBtn;
        this.replayBtn = replayBtn;
        this.exportBtn = exportBtn;
        this.compareBtn = compareBtn;
        if (delaySlider != null) {
            delayMs.bind(delaySlider.valueProperty());
            executionDelayMillis.set(Math.max(0L, Math.round(delaySlider.getValue())));
            delaySlider.valueProperty().addListener(delaySliderListener);
        }
        if (timelineSlider != null) {
            timelineSlider.setDisable(true);
            timelineSlider.valueProperty().addListener(timelineSliderListener);
        }
        setupGlobalButtonActions();
        refreshStatsDisplay();
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

    /** Returns the shared summary retained for the latest local execution. */
    public final ExecutionSummary executionSummary() {
        if (lastExecution == null) {
            return ExecutionSummary.empty();
        }
        ExecutionSummary summary = lastExecution.summary();
        if (replayController == null) {
            return summary;
        }
        ExecutionTiming timing = summary.timing().withPlaybackDuration(
                replayController.playbackDuration());
        return summary.withTiming(timing);
    }

    public final ReadOnlyBooleanProperty runningProperty() {
        return running;
    }

    public final ReadOnlyBooleanProperty pausedProperty() {
        return paused;
    }

    public final boolean isRunning() {
        return running.get();
    }

    public final boolean isPaused() {
        return paused.get();
    }

    private record ClientRunRecord<T>(
            String moduleId,
            String algorithmId,
            String inputSignature,
            List<ExecutionEvent> events,
            ExecutionSummary summary) {

        private ExecutionStatistics stats() {
            return summary.statistics();
        }
    }

    private String formatSummaryMessage(ExecutionSummary summary) {
        ExecutionTiming timing = summary.timing();
        ResourceUsage resources = summary.resources();
        return String.format(
                "%s | %s | %s | %s | %s",
                I18N.text("stats.event.span", timing.eventSpan().toMillis()),
                I18N.text("stats.total.time", formatDuration(timing.totalDuration())),
                I18N.text("stats.playback.time", formatDuration(timing.playbackDuration())),
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
