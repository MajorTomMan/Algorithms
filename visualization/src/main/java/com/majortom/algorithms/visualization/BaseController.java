package com.majortom.algorithms.visualization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.export.StructureSnapshotSerializer;
import com.majortom.algorithms.core.runtime.ExecutionArchive;
import com.majortom.algorithms.core.runtime.ExecutionContext;
import com.majortom.algorithms.core.runtime.ExecutionControl;
import com.majortom.algorithms.core.runtime.ExecutionException;
import com.majortom.algorithms.core.runtime.ExecutionFrame;
import com.majortom.algorithms.core.runtime.ExecutionRecord;
import com.majortom.algorithms.core.runtime.ExecutionStatsSnapshot;
import com.majortom.algorithms.core.runtime.ExecutionTimeline;
import com.majortom.algorithms.visualization.impl.controller.BaseModuleController;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 算法可视化控制器基类
 * 职责：定义统一的执行上下文、统计展示、回放、导出与比较能力。
 */
public abstract class BaseController<S extends BaseStructure<?>> implements Initializable {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    protected static final ExecutionArchive EXECUTION_ARCHIVE = new ExecutionArchive();

    protected final DoubleProperty delayMs = new SimpleDoubleProperty(50.0);
    protected ExecutionStatsSnapshot stats = ExecutionStatsSnapshot.empty();

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

    protected ExecutionContext<S> executionContext;
    protected ExecutionRecord<S> lastExecution;

    private Timeline replayTimeline;
    private boolean updatingTimelineSlider;

    public BaseController(BaseVisualizer<S> visualizer) {
        this.visualizer = visualizer;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupI18n();
    }

    protected final void renderFrame(ExecutionFrame<S> frame) {
        this.stats = frame.stats();
        if (visualizer != null) {
            visualizer.render(frame.snapshot(), frame.focusA(), frame.focusB());
        }
        refreshStatsDisplay();
        syncTimelineSlider(frame);
    }

    public final void startAlgorithm(BaseAlgorithms<S> algorithm, S data) {
        stopAlgorithm();
        clearExecutionState();
        resetStats();

        String inputSignature = executionInputSignature(data);
        final ExecutionContext<S> context = new ExecutionContext<>(
                moduleId(),
                executionAlgorithmId(algorithm),
                inputSignature,
                new ExecutionControl<>() {
                    @Override
                    public void acceptFrame(ExecutionFrame<S> frame, boolean awaitStep) {
                        Runnable task = () -> {
                            if (executionContext == null) {
                                return;
                            }
                            renderFrame(frame);
                        };
                        if (awaitStep) {
                            AlgorithmThreadManager.syncAndWait(task);
                        } else {
                            AlgorithmThreadManager.postStatus(task);
                        }
                    }

                    @Override
                    public boolean isCancelled() {
                        return Thread.currentThread().isInterrupted();
                    }

                    @Override
                    public long getDelayMillis() {
                        return AlgorithmThreadManager.getDelay();
                    }
                });
        this.executionContext = context;

        algorithm.setExecutionContext(context);

        appendLog("Started: " + executionAlgorithmId(algorithm));

        AlgorithmThreadManager.run(() -> {
            try {
                executeAlgorithm(algorithm, data);
            } catch (BaseAlgorithms.AlgorithmInterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                if (!"execution.cancelled".equals(e.getCode())) {
                    handleAlgorithmError(e);
                }
            } catch (Exception e) {
                handleAlgorithmError(e);
            }
        }, () -> {
            this.lastExecution = context.finish();
            this.executionContext = null;
            EXECUTION_ARCHIVE.add(lastExecution);
            this.stats = lastExecution.summary();
            refreshStatsDisplay();
            prepareTimelineControls();
            onAlgorithmFinished();
        });
    }

    public void stopAlgorithm() {
        stopReplay();
        AlgorithmThreadManager.stopAll();
    }

    public void togglePause() {
        if (AlgorithmThreadManager.isPaused()) {
            AlgorithmThreadManager.resume();
        } else {
            AlgorithmThreadManager.pause();
        }
    }

    public void toggleReplay() {
        if (!hasExecutionData() || AlgorithmThreadManager.isRunning()) {
            return;
        }
        if (replayTimeline != null && replayTimeline.getStatus() == Animation.Status.RUNNING) {
            stopReplay();
            return;
        }

        List<ExecutionFrame<S>> frames = lastExecution.timeline().frames();
        if (frames.isEmpty()) {
            return;
        }

        replayTimeline = new Timeline(new KeyFrame(
                Duration.millis(Math.max(40L, AlgorithmThreadManager.getDelay())),
                event -> advanceReplayFrame()));
        replayTimeline.setCycleCount(Animation.INDEFINITE);
        renderFrame(frames.get(0));
        replayTimeline.play();
    }

    public void seekTimeline(double progress) {
        if (!hasExecutionData()) {
            return;
        }
        stopReplay();

        int size = lastExecution.timeline().size();
        if (size == 0) {
            return;
        }

        int index = (int) Math.round(progress * (size - 1));
        index = Math.max(0, Math.min(size - 1, index));
        renderFrame(lastExecution.timeline().get(index));
    }

    public boolean hasExecutionData() {
        return lastExecution != null && !lastExecution.timeline().isEmpty();
    }

    public void exportExecution() {
        if (!hasExecutionData()) {
            appendLog("Nothing to export.");
            return;
        }

        try {
            Path exportDir = Path.of("exports");
            Files.createDirectories(exportDir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path file = exportDir.resolve(moduleId() + "_" + lastExecution.algorithmId() + "_" + timestamp + ".json");
            JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), buildExecutionExportPayload());
            appendLog("Exported: " + file);
        } catch (IOException e) {
            handleAlgorithmError(e);
        }
    }

    public void compareExecutions() {
        if (!hasExecutionData()) {
            appendLog("No execution data available for comparison.");
            return;
        }

        List<ExecutionRecord<? extends BaseStructure<?>>> candidates = EXECUTION_ARCHIVE
                .comparableRecords(lastExecution);
        if (candidates.isEmpty()) {
            appendLog("No comparable executions found for the same input.");
            return;
        }

        appendLog("Comparison for input " + lastExecution.inputSignature().substring(0, 8) + ":");
        appendLog(describeRecord(lastExecution));
        for (ExecutionRecord<? extends BaseStructure<?>> record : candidates) {
            appendLog(describeRecord(record));
        }
    }

    private void advanceReplayFrame() {
        if (!hasExecutionData()) {
            stopReplay();
            return;
        }

        int currentIndex = currentTimelineIndex();
        int nextIndex = currentIndex + 1;
        if (nextIndex >= lastExecution.timeline().size()) {
            stopReplay();
            return;
        }
        renderFrame(lastExecution.timeline().get(nextIndex));
    }

    private int currentTimelineIndex() {
        if (!hasExecutionData() || timelineSlider == null) {
            return 0;
        }
        int size = lastExecution.timeline().size();
        if (size <= 1) {
            return 0;
        }
        return (int) Math.round(timelineSlider.getValue() * (size - 1));
    }

    private void stopReplay() {
        if (replayTimeline != null) {
            replayTimeline.stop();
            replayTimeline = null;
        }
    }

    private void prepareTimelineControls() {
        if (timelineSlider != null) {
            timelineSlider.setDisable(!hasExecutionData());
            updatingTimelineSlider = true;
            timelineSlider.setValue(hasExecutionData() ? 1.0 : 0.0);
            updatingTimelineSlider = false;
        }
    }

    private void syncTimelineSlider(ExecutionFrame<S> frame) {
        if (timelineSlider == null) {
            return;
        }
        int size = Math.max(1, currentTimelineSize());
        updatingTimelineSlider = true;
        timelineSlider.setValue(size == 1 ? 0.0 : (double) frame.index() / (double) (size - 1));
        timelineSlider.setDisable(AlgorithmThreadManager.isRunning() || !hasExecutionData());
        updatingTimelineSlider = false;
    }

    private void clearExecutionState() {
        stopReplay();
        executionContext = null;
        lastExecution = null;
        if (timelineSlider != null) {
            timelineSlider.setDisable(true);
            updatingTimelineSlider = true;
            timelineSlider.setValue(0.0);
            updatingTimelineSlider = false;
        }
    }

    private int currentTimelineSize() {
        ExecutionTimeline<S> timeline = currentTimeline();
        return timeline == null ? 0 : timeline.size();
    }

    private ExecutionTimeline<S> currentTimeline() {
        if (executionContext != null && !executionContext.timeline().isEmpty()) {
            return executionContext.timeline();
        }
        return lastExecution == null ? null : lastExecution.timeline();
    }

    private Map<String, Object> buildExecutionExportPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("moduleId", lastExecution.moduleId());
        payload.put("algorithmId", lastExecution.algorithmId());
        payload.put("inputSignature", lastExecution.inputSignature());
        payload.put("summary", statsPayload(lastExecution.summary()));
        payload.put("messages", lastExecution.messages());

        List<Map<String, Object>> frames = lastExecution.timeline().frames().stream().map(frame -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("index", frame.index());
            item.put("timestampOffsetMillis", frame.timestampOffsetMillis());
            item.put("label", frame.label());
            item.put("focusA", frame.focusA());
            item.put("focusB", frame.focusB());
            item.put("stats", statsPayload(frame.stats()));
            item.put("snapshot", StructureSnapshotSerializer.serializeStructure(frame.snapshot()));
            return item;
        }).toList();

        payload.put("frames", frames);
        return payload;
    }

    private Map<String, Object> statsPayload(ExecutionStatsSnapshot stats) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("durationMillis", stats.durationMillis());
        payload.put("frameCount", stats.frameCount());
        payload.put("compareCount", stats.compareCount());
        payload.put("actionCount", stats.actionCount());
        payload.put("extras", stats.extras());
        return payload;
    }

    private String describeRecord(ExecutionRecord<? extends BaseStructure<?>> record) {
        return String.format(
                "%s | duration=%dms | frames=%d | actions=%d | compares=%d",
                record.algorithmId(),
                record.summary().durationMillis(),
                record.summary().frameCount(),
                record.summary().actionCount(),
                record.summary().compareCount());
    }

    protected final void appendLog(String message) {
        if (logArea != null) {
            Platform.runLater(
                    () -> logArea.appendText(String.format("[%tT] %s%n", System.currentTimeMillis(), message)));
        }
    }

    protected void refreshStatsDisplay() {
        if (statsLabel != null) {
            statsLabel.setText(formatStatsMessage());
        }
    }

    private void resetStats() {
        this.stats = ExecutionStatsSnapshot.empty();
    }

    private void setupGlobalButtonActions() {
        if (startBtn != null) {
            startBtn.setOnAction(e -> handleAlgorithmStart());
        }
        if (pauseBtn != null) {
            pauseBtn.setOnAction(e -> togglePause());
        }
        if (resetBtn != null) {
            resetBtn.setOnAction(e -> {
                stopAlgorithm();
                clearExecutionState();
                ((BaseModuleController<S>) this).resetModule();
            });
        }
        if (replayBtn != null) {
            replayBtn.setOnAction(e -> toggleReplay());
        }
        if (exportBtn != null) {
            exportBtn.setOnAction(e -> exportExecution());
        }
        if (compareBtn != null) {
            compareBtn.setOnAction(e -> compareExecutions());
        }
    }

    protected String executionAlgorithmId(BaseAlgorithms<S> algorithm) {
        return algorithm.getClass().getSimpleName();
    }

    protected String executionInputSignature(S data) {
        return StructureSnapshotSerializer.signatureFor((BaseStructure<?>) data.copy());
    }

    protected abstract String moduleId();

    protected abstract String formatStatsMessage();

    public abstract void setupCustomControls(HBox container);

    protected abstract void setupI18n();

    protected abstract void executeAlgorithm(BaseAlgorithms<S> algorithm, S data);

    public abstract void handleAlgorithmStart();

    protected void onAlgorithmFinished() {
        appendLog(String.format("Finished. Duration: %dms", stats.durationMillis()));
    }

    protected void handleAlgorithmError(Exception e) {
        appendLog("Runtime Error: " + e.getMessage());
        e.printStackTrace();
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

        if (this.delaySlider != null) {
            this.delayMs.bind(this.delaySlider.valueProperty());
            this.delayMs.addListener((obs, oldVal, newVal) -> AlgorithmThreadManager.setDelay(newVal.longValue()));
            AlgorithmThreadManager.setDelay(delayMs.longValue());
        }

        if (this.timelineSlider != null) {
            this.timelineSlider.setDisable(true);
            this.timelineSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!updatingTimelineSlider
                        && !AlgorithmThreadManager.isRunning()
                        && this.timelineSlider.isValueChanging()) {
                    seekTimeline(newVal.doubleValue());
                }
            });
        }

        setupGlobalButtonActions();
        this.initialize(null, null);
    }

    public final BaseVisualizer<S> getVisualizer() {
        return visualizer;
    }

    public final Region getVisualizerView() {
        if (visualizer instanceof Region region) {
            return region;
        }
        throw new IllegalStateException("Visualizer must extend Region for layout integration.");
    }
}
